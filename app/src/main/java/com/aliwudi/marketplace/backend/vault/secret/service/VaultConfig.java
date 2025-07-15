/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.aliwudi.marketplace.backend.vault.secret.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

import java.net.URI;
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.core.VaultKeyValueOperationsSupport;

@Configuration
public class VaultConfig {
    private final String url = "http://127.0.0.1:8200";
    private final String token = " hvs.CAESIMV-1KPR9-pKZVMbLyG2H_UWy96thQqhM4_Lyj1MjeaJGh4KHGh2cy5KTnA4Z0Zqc2hudUtUb2xQMmRMcWtyam8";
    
    @Bean
    public VaultTemplate vaultTemplate() {
        VaultEndpoint endpoint = VaultEndpoint.from(URI.create(url));
        return new VaultTemplate(endpoint, new TokenAuthentication(token));
    }
   
    @Bean
    public VaultKeyValueOperations keyValueOperations(VaultTemplate vaultTemplate) {
        return vaultTemplate.opsForKeyValue("secret", VaultKeyValueOperationsSupport.KeyValueBackend.KV_2);
    }

}

