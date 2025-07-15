/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.aliwudi.marketplace.backend.vault.secret.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

import java.net.URI;

@SpringBootApplication
public class VaultApp implements CommandLineRunner {
    @Autowired
    private VaultSecretService vaultSecretService;

    
    public static void main(String[] args) {
        SpringApplication.run(VaultApp.class, args);
    }

    @Override
    public void run(String... args) {
        vaultSecretService.writeSecret("smtp.username", "support@example.com");
        String username = vaultSecretService.readSecret("smtp.username");
        System.out.println("smtp.username from Vault = " + username);
    }

}
