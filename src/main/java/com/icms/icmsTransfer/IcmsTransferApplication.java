package com.icms.icmsTransfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IcmsTransferApplication {

	public static void main(String[] args) {
		SpringApplication.run(IcmsTransferApplication.class, args);
	}

}
