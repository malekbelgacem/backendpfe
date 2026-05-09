package com.example.backendpfe.fraud.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FraudPredictionResponse {

    @JsonProperty("fraud_score")
    private Double fraudScore;

    @JsonProperty("risk_level")
    private String riskLevel;

    private Boolean alert;
}
