package com.icms.icmsTransfer.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class IcmsTransferService {

    /*
     * Number of records processed in one batch.
     */
    private static final int CHUNK_SIZE = 1000;

    /*
     * Sync process identifier.
     */
    private static final String PROCESS_NAME =
            "ICMS_FAILURE_TRANSFER";

    /*
     * PostgreSQL Source Database
     */
    @Qualifier("sourceJdbcTemplate")
    private final JdbcTemplate sourceJdbcTemplate;

    /*
     * Oracle Target Database
     */
    @Qualifier("targetJdbcTemplate")
    private final JdbcTemplate targetJdbcTemplate;

    public void transfer() {

        log.info("===== ICMS Transfer Started =====");

        /*
         * Get last transferred source ID
         * from sync tracker table.
         *
         * WHY?
         * ----
         * This is safer than:
         *      MAX(SMMS_ID)
         *
         * Because manual entries
         * in target table
         * cannot break sync.
         */
        Long lastTransferredId =
                getLastTransferredId();

        log.info(
                "Last Transferred SMMS_ID : {}",
                lastTransferredId
        );

        /*
         * Fetch only NEW records
         * after 2024-08-26.
         *
         * IMPORTANT:
         * ----------
         * Only records after checkpoint
         * are fetched.
         *
         * No repeated scan.
         */
        String fetchSql =
                "SELECT * " +
                        "FROM icms_failure_details " +
                        "WHERE id > ? " +
                        "AND event_date >= DATE '2024-08-26' " +
                        "ORDER BY id " +
                        "LIMIT ?";

        int totalFetched = 0;
        int totalInserted = 0;
        int totalFailed = 0;

        /*
         * Chunk processing loop.
         */
        while (true) {

            /*
             * Fetch next chunk from PostgreSQL.
             */
            List<Map<String, Object>> rows =
                    sourceJdbcTemplate.queryForList(
                            fetchSql,
                            lastTransferredId,
                            CHUNK_SIZE
                    );

            /*
             * Stop when no records remain.
             */
            if (rows.isEmpty()) {

                log.info("No more new records found.");
                break;
            }

            log.info(
                    "Processing Chunk Size : {}",
                    rows.size()
            );

            totalFetched += rows.size();

            /*
             * Process records one by one.
             */
            for (Map<String, Object> row : rows) {

                try {

                    /*
                     * Source PostgreSQL ID.
                     */
                    Long smmsId =
                            ((Number) row.get("id"))
                                    .longValue();

                    /*
                     * LinkedHashMap preserves insertion order.
                     */
                    LinkedHashMap<String, Object> orderedMap =
                            new LinkedHashMap<>();

                    /*
                     * Store source ID
                     * into target SMMS_ID.
                     *
                     * IMPORTANT:
                     * ----------
                     * Target ID is auto generated.
                     */
                    orderedMap.put(
                            "SMMS_ID",
                            convertValue(smmsId)
                    );

                    /*
                     * Copy all remaining columns.
                     */
                    row.forEach((key, value) -> {

                        String upperKey =
                                key.toUpperCase();

                        /*
                         * Skip source ID.
                         */
                        if ("ID".equals(upperKey)) {
                            return;
                        }

                        orderedMap.put(
                                upperKey,
                                convertValue(value)
                        );
                    });

                    /*
                     * Build Oracle MERGE query.
                     *
                     * Prevents duplicate insert.
                     */
                    String mergeSql =
                            buildMergeQuery(orderedMap);

                    /*
                     * Execute MERGE query.
                     */
                    targetJdbcTemplate.update(
                            mergeSql,
                            orderedMap.values().toArray()
                    );

                    totalInserted++;

                    /*
                     * Update sync checkpoint.
                     *
                     * WHY IMPORTANT?
                     * --------------
                     * If application crashes,
                     * next execution starts
                     * from this ID.
                     */
                    updateLastTransferredId(smmsId);

                    /*
                     * Update local variable
                     * for next fetch cycle.
                     */
                    lastTransferredId = smmsId;

                } catch (Exception e) {

                    totalFailed++;

                    log.error(
                            "Error processing source ID : {}",
                            row.get("id"),
                            e
                    );
                }
            }

            log.info(
                    "Chunk Completed Successfully."
            );
        }

        /*
         * Final execution summary.
         */
        log.info("=================================");
        log.info("Total Fetched  : {}", totalFetched);
        log.info("Total Inserted : {}", totalInserted);
        log.info("Total Failed   : {}", totalFailed);
        log.info("===== ICMS Transfer Completed =====");
    }

    /*
     * Get last transferred source ID
     * from sync tracker table.
     */
    private Long getLastTransferredId() {

        Long lastId =
                targetJdbcTemplate.queryForObject(

                        "SELECT LAST_SMMS_ID " +
                                "FROM ICMS_SYNC_TRACKER " +
                                "WHERE PROCESS_NAME = ?",

                        Long.class,
                        PROCESS_NAME
                );

        return lastId == null ? 0L : lastId;
    }

    /*
     * Update sync checkpoint.
     */
    private void updateLastTransferredId(
            Long smmsId
    ) {

        targetJdbcTemplate.update(

                "UPDATE ICMS_SYNC_TRACKER " +
                        "SET LAST_SMMS_ID = ?, " +
                        "LAST_UPDATED_TIME = CURRENT_TIMESTAMP " +
                        "WHERE PROCESS_NAME = ?",

                smmsId,
                PROCESS_NAME
        );
    }

    /*
     * Dynamic Oracle MERGE Query Builder.
     *
     * Duplicate check happens on:
     *      SMMS_ID
     */
    private String buildMergeQuery(
            LinkedHashMap<String, Object> row
    ) {

        String columns =
                String.join(", ", row.keySet());

        String sourceColumns =
                row.keySet()
                        .stream()
                        .map(k -> "? AS " + k)
                        .collect(Collectors.joining(", "));

        String values =
                row.keySet()
                        .stream()
                        .map(k -> "src." + k)
                        .collect(Collectors.joining(", "));

        return
                "MERGE INTO ICMS_FAILURE_DETAILS target " +
                        "USING (SELECT " + sourceColumns + " FROM dual) src " +
                        "ON (target.SMMS_ID = src.SMMS_ID) " +
                        "WHEN NOT MATCHED THEN " +
                        "INSERT (" + columns + ") " +
                        "VALUES (" + values + ")";
    }

    /*
     * DB datatype conversion.
     */
    private Object convertValue(Object value) {

        /*
         * Null handling.
         */
        if (value == null) {
            return null;
        }

        /*
         * Timestamp conversion.
         */
        if (value instanceof Timestamp timestamp) {

            return new Timestamp(
                    timestamp.getTime()
            );
        }

        /*
         * Boolean to Number conversion.
         */
        if (value instanceof Boolean bool) {

            return bool ? 1 : 0;
        }

        return value;
    }
}