package com.example.backendpfe.fraud;

import com.example.backendpfe.fraud.dto.FraudPredictionRequest;
import com.example.backendpfe.fraud.dto.FraudPredictionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class FraudDetectionClient {

    private final RestClient restClient;

    @Value("${fraud.ai.base-url}")
    private String fraudAiBaseUrl;

    public FraudPredictionResponse predict(FraudPredictionRequest request) {
        try {
            System.out.println("=== ENVOI VERS FASTAPI ===");
            System.out.println("URL = " + fraudAiBaseUrl + "/predict");
            System.out.println("BODY = " + request);

            FraudPredictionResponse response = restClient.post()
                    .uri(fraudAiBaseUrl + "/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(FraudPredictionResponse.class);

            System.out.println("=== REPONSE FASTAPI OK ===");
            System.out.println(response);

            return response;

        } catch (Exception e) {
            System.out.println("=== ERREUR APPEL FASTAPI ===");
            e.printStackTrace();
            throw e;
        }
    }
}