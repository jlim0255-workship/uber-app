package com.rideshare.locationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


// this is going to receive the data

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DriverLocationRequest {
    private String driverId;
    private double latitude;
    private double longitude;
}
