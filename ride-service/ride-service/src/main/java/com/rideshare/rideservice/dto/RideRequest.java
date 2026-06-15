package com.rideshare.rideservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
// MEAT: this is getter and setters for RideRequest instance
public class RideRequest {
    @NotBlank(message = "Rider Id is required")
    private String riderId;

    @NotNull(message = "Pickup longitude is required")
    private double pickupLongitude;

    @NotNull(message = "Pickup latitude is required")
    private double pickupLatitude;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotNull(message = "Drop latitude is required")
    private double dropLatitude;

    @NotNull(message = "Drop longitude is required")
    private double dropLongitude;

    @NotBlank(message = "Drop address is required")
    private String dropAddress;
}
