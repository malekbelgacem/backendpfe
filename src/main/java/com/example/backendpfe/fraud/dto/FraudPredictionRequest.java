package com.example.backendpfe.fraud.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FraudPredictionRequest {

    private Double amount;

    @JsonProperty("hour_of_day")
    private Integer hourOfDay;

    @JsonProperty("is_weekend")
    private Integer isWeekend;

    @JsonProperty("velocity_1h")
    private Double velocity1h;

    @JsonProperty("velocity_24h")
    private Double velocity24h;

    @JsonProperty("amount_to_avg_ratio")
    private Double amountToAvgRatio;

    @JsonProperty("new_device")
    private Integer newDevice;

    @JsonProperty("ip_risk_score")
    private Double ipRiskScore;

    @JsonProperty("distance_from_home_km")
    private Double distanceFromHomeKm;

    private String currency;
    private String channel;

    @JsonProperty("location_country")
    private String locationCountry;
}
