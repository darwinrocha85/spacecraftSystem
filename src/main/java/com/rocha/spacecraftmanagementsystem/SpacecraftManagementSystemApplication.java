package com.rocha.spacecraftmanagementsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SpacecraftManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpacecraftManagementSystemApplication.class, args);
	}

}
