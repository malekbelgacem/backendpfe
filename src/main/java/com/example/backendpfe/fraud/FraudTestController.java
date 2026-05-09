package com.example.backendpfe.fraud;

import com.example.backendpfe.fraud.dto.FraudPredictionRequest;
import com.example.backendpfe.fraud.dto.FraudPredictionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fraud-test")
@RequiredArgsConstructor
public class FraudTestController {

    private final FraudDetectionClient fraudDetectionClient;

    @PostMapping
    public FraudPredictionResponse test(@RequestBody FraudPredictionRequest request) {
        System.out.println("=== REQUEST RECUE PAR SPRING ===");
        System.out.println(request);

        FraudPredictionResponse response = fraudDetectionClient.predict(request);

        System.out.println("=== REPONSE RECUE DE FASTAPI ===");
        System.out.println(response);

        return response;
    }
}