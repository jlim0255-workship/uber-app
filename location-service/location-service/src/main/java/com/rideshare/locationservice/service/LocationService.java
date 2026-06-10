package com.rideshare.locationservice.service;

import com.rideshare.locationservice.dto.DriverLocationRequest;
import com.rideshare.locationservice.dto.NearByDriverResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationService {

    private final RedisTemplate<String, String> redisTemplate;

    // redis key for all driver locations
    private static final String DRIVERS_GEO_KEY = "drivers:locations";

    /**
     * Update driver location in Redis
     * Called every 3 seconds by driver's phone
     * Maps to Redis GEOADD command
     */

    public void updateDriverLocation(DriverLocationRequest driverLocationRequest){
        log.info("Updating location for driver: {}", driverLocationRequest.getDriverId());

        // IMPORTANT: longitude FIRST, latitude SECOND in GEOSPATIAL standard
        // 1) get the driverLocationRequest with ID, longitude and latitude
        Point driverPoint = new Point(
            driverLocationRequest.getLongitude(),
            driverLocationRequest.getLatitude()
        );

        // opsForGeo is special command in JAVA
        //  2) to add the driver location to Redis based on this driverID
        //  , we use the GEOADD command through redisTemplate
        redisTemplate.opsForGeo().add(
            DRIVERS_GEO_KEY, // type <String, String>
            driverPoint, // Point instance
            driverLocationRequest.getDriverId() // based on this ID
        );

        log.info("Location updated for driver: {}", driverLocationRequest.getDriverId());
    }

    /**
     *Find nearby drivers within given radius
     * Called by Matching service on ride request
     * Maps to redis GEORADIUS command
     */
    public List<NearByDriverResponse> findNearbyDrivers(
        double longitude,
        double latitude,
        double radiusInKm
    ){
        log.info("Finding drivers near lat {} long: {} within {} km", latitude, longitude, radiusInKm);

        Circle searchArea = new Circle(
            new Point(longitude, latitude),
            new Distance(radiusInKm, Metrics.KILOMETERS)
        );

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(
                        DRIVERS_GEO_KEY,
                        searchArea,
                        RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                .includeCoordinates()
                                .includeDistance()
                                .sortAscending()
                                .limit(10)
                );

        List<NearByDriverResponse> nearByDrivers = new ArrayList<>();

        if (results != null) {
            // this is a for each loop
            // for (obj from collection : the collection)
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult : results) {
                String driverId = geoResult.getContent().getName();
                Point location = geoResult.getContent().getPoint();
                double distance = geoResult.getDistance().getValue();

                nearByDrivers.add(new NearByDriverResponse(driverId, location.getX(), location.getY(), distance));
            }

            // or
//            results.getContent().forEach(result -> {
//                RedisGeoCommands.GeoLocation<String> location = result.getContent();
//                nearByDrivers.add(new NearByDriverResponse(
//                    location.getName(),
//                    location.getPoint().getX(),
//                    location.getPoint().getY(),
//                    result.getDistance().getValue()
//                ));
//            });
        }
        log.info("Found {} nearby drivers", nearByDrivers.size());

        return nearByDrivers;
    }

    /**
     * Remove drivers when they go offline
     * Maps to Redis ZREM command
     * */
    public void removeDriver(String driverId){
        log.info("Removing driver with ID: {}", driverId);
        redisTemplate.opsForGeo().remove(DRIVERS_GEO_KEY, driverId);
        log.info("Driver with ID: {} removed successfully", driverId);
    }




}
