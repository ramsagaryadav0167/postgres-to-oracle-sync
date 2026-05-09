package com.icms.icmsTransfer.scheduler;

import com.icms.icmsTransfer.service.IcmsTransferService;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TransferScheduler {

    private final IcmsTransferService service;

    @Scheduled(cron = "${transfer.cron}")
    public void run() {
        service.transfer();
    }
}