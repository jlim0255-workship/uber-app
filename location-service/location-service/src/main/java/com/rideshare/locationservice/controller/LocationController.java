package com.rideshare.locationservice.controller;

import com.rideshare.locationservice.LocationServiceApplication;
import com.rideshare.locationservice.dto.DriverLocationRequest;
import com.rideshare.locationservice.dto.NearByDriverResponse;
import com.rideshare.locationservice.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


// type in these @, then ctrl + space to see suggestions
@RestController
@RequestMapping("/api/v1/locations")
@Slf4j
@RequiredArgsConstructor
public class LocationController {

    // 1) receive the driver data
    // 2) update the driver data to db
    // this variable is class of LocationService
    // instantiated to be used in the methods below?
    private final LocationService locationService;

    // driver phone calls this every 3 seconds
    @PostMapping("/drivers/update")
    public ResponseEntity<String> updateDriverLocation(
            @RequestBody DriverLocationRequest driverLocationRequest
            ){
        locationService.updateDriverLocation(driverLocationRequest);
        return ResponseEntity.ok("Driver Location updated");

    }

    // method 2
    // get nearby drivers when ride is requested
    // matching service calls this when ride is requested
    @GetMapping("/drivers/nearby")
    public ResponseEntity<List<NearByDriverResponse>> getNearByDrivers(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam (defaultValue = "5.0") double radiusInKm

    ){
        return ResponseEntity.ok(locationService.findNearbyDrivers(longitude, latitude, radiusInKm));
    }

    // method 3
    // when drivers go offline, remove it
    @DeleteMapping("/drivers/{driverID}")
    public ResponseEntity<String> removeDriver(
            @PathVariable String driverID
    ){
        locationService.removeDriver(driverID);
        return ResponseEntity.ok("Driver removed successfully");
    }

    

}
