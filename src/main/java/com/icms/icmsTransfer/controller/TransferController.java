package com.icms.icmsTransfer.controller;

import com.icms.icmsTransfer.service.IcmsTransferService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class TransferController {

    private final IcmsTransferService transferService;

    @GetMapping("/transfer/run")
    public String runTransfer() {

        transferService.transfer();

        return "ICMS Transfer Completed Successfully";
    }
}