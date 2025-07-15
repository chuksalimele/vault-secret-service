/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.aliwudi.marketplace.backend.vault.secret.gui;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.HashMap;
import java.util.Map;

public class VaultTokenGenerator {

    public static String getVaultTokenFromAppRole(String vaultUrl, String roleId, String secretId) {
        String loginUrl = vaultUrl + "/v1/auth/approle/login";

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory());

        Map<String, String> body = new HashMap<>();
        body.put("role_id", roleId);
        body.put("secret_id", secretId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(loginUrl, HttpMethod.POST, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                Map auth = (Map) response.getBody().get("auth");
                return (String) auth.get("client_token");
            } else {
                throw new RuntimeException("Vault login failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error logging into Vault via AppRole: " + e.getMessage(), e);
        }
    }
}
