package com.aliwudi.marketplace.backend.vault.secret.service;

import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.util.Map;

@Service
public class VaultSecretService {

    private final VaultTemplate vaultTemplate;

    private static final String VAULT_PATH = "secret/data/test-service";

    public VaultSecretService(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    public void writeSecret(String key, String value) {
        Map<String, Object> data = Map.of(
                key, value
        );

        vaultTemplate.write(VAULT_PATH, Collections.singletonMap("data", data));

    }

    public String readSecret(String key) {
        VaultResponse response = vaultTemplate.read(VAULT_PATH);
        if (response != null && response.getData() != null) {
            Map<String, Object> responseData = (Map<String, Object>) response.getData().get("data");
            if (responseData != null) {
                return (String) responseData.get(key);
            }
        }
        return null;
    }

}
