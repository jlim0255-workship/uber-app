package com.rideshare.rideservice.service;

import com.rideshare.rideservice.dto.RideRequest;
import com.rideshare.rideservice.dto.RideResponse;
import com.rideshare.rideservice.event.RideRequestedEvent;
import com.rideshare.rideservice.model.Ride;
import com.rideshare.rideservice.model.RideStatus;
import com.rideshare.rideservice.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RideService {
    // connection to db
    private final RideRepository rideRepository;
    private final KafkaTemplate<String, RideRequestedEvent> kafkaTemplate;

    private static final String RIDE_REQUESTED_TOPIC = "ride.requested";

    /**
     * step 1: create a ride event in DB
     * step 2: publish ride request to kafka
     * step 3: matching service
     */

    // create ride in DB with Requested.status
    public RideResponse requestRide(RideRequest request){
        log.info("New ride request from rider (passenger): {}", request.getRiderId());

        // save ride to db
        Ride ride = new Ride();
        ride.setRiderId(request.getRiderId());

        ride.setPickupLongitude(request.getPickupLongitude());
        ride.setPickupLatitude(request.getPickupLatitude());

        ride.setDropLongitude(request.getDropLongitude());
        ride.setDropLatitude(request.getDropLatitude());

        ride.setDropAddress(request.getDropAddress());
        ride.setStatus(RideStatus.REQUESTED); //1:39:25
        ride.setEstimatedFare(calculateEstimateFare(request));

        // TODO: why save db here 1
        // MEAT: save the ride to db, then create a saved ride instance
        Ride savedRide = rideRepository.save(ride);

        // step 2: publish ride request event to kafka
        // matching service will consume this event and find nearest driver
        RideRequestedEvent event = new RideRequestedEvent(
                savedRide.getId(),
                savedRide.getRiderId(),
                savedRide.getPickupLatitude(),
                savedRide.getPickupLongitude(),
                savedRide.getPickupAddress(),
                savedRide.getDropLatitude(),
                savedRide.getDropLongitude(),
                savedRide.getDropAddress()
        );

        kafkaTemplate.send(RIDE_REQUESTED_TOPIC, savedRide.getId(), event);

        log.info("RideRequestedEvent published to Kafka for ride: {}", savedRide.getId());

        // update status to Matching
        savedRide.setStatus(RideStatus.MATCHING);

        // TODO: why save db here 2 again?
        rideRepository.save(savedRide);

        return mapToResponse(savedRide);
    }

    // MEAT: we do not have this method inside our controller
    // change the status of the ride when SOME driver accepts the ride request
    // MATCHING -> ACCEPTED, assign the driver ID
    // this will be called inside matching service
    public void updateRideWithDriver(String rideId, String driverId){
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride Not Found"));

        ride.setDriverId(driverId);
        ride.setStatus(RideStatus.ACCEPTED);

        rideRepository.save(ride);
    }

    // CONTROLLER LOGIC 1:49:43
    public RideResponse startRide(String rideId){
        // get the ride from db repository
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride Not Found"));

        // check ride status
        // only process those != ACCEPTED
        if (ride.getStatus() != RideStatus.ACCEPTED){
            throw new RuntimeException("Ride cannot be started. Current status: " + ride.getStatus());
        }

        // set ride status to RIDE_STARTED
        ride.setStatus(RideStatus.RIDE_STARTED);
        ride.setStartedAt(LocalDateTime.now());
        rideRepository.save(ride);

        return mapToResponse(ride);
    }

    public RideResponse completeRide(String rideId){
        // get the ride from db repository
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride Not Found"));

        // check ride status
        // only process those != ACCEPTED
        if (ride.getStatus() != RideStatus.RIDE_STARTED){
            throw new RuntimeException("Ride cannot be completed. Current status: " + ride.getStatus());
        }

        // set ride status to
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());

        // get the actual fare
        ride.setActualFare(ride.getEstimatedFare());

        rideRepository.save(ride);
        return mapToResponse(ride);
    }

    public RideResponse cancelRide(String rideId){
        // get the ride from db repository
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride Not Found"));

        // set ride status to CANCELLED
        ride.setStatus(RideStatus.CANCELLED);

        rideRepository.save(ride);
        return mapToResponse(ride);
    }

    public RideResponse getRideById(String rideId){
        // get the ride from db repository
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride Not Found"));

        return mapToResponse(ride);
    }

    public List<RideResponse> getRidesByRider(String riderId){
        return rideRepository.findByRiderIdOrderByCreatedAtDesc(riderId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private double calculateEstimateFare(RideRequest request){
        // simplified haversine distance calculation
        double lat1 = Math.toRadians(request.getPickupLatitude());
        double lat2 = Math.toRadians(request.getDropLatitude());

        double lon1 = Math.toRadians(request.getPickupLongitude());
        double lon2 = Math.toRadians(request.getDropLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.pow(Math.sin(dLat / 2), 2)
                +Math.cos(lat1) * Math.cos(lat2)
                *Math.pow(Math.sin(dLon / 2), 2);

        double c = 2 * Math.asin(Math.sqrt(a));
        double distanceKm = 6371 * c;

        // base fare = 50 rs + 12 rs per km
        double fare = 50 + (distanceKm * 12);
        return Math.round(fare * 100.0) / 100.0;
    }



    // this is a wrapper to convert Ride object into a RideResponse object
    private RideResponse mapToResponse(Ride ride){
        RideResponse response = new RideResponse();
        response.setId(ride.getId());
        response.setRiderId(ride.getRiderId());
        response.setPickupLatitude(ride.getPickupLatitude());
        response.setPickupLongitude(ride.getPickupLongitude());
        response.setPickupAddress(ride.getPickupAddress());
        response.setDropLatitude(ride.getDropLatitude());
        response.setDropLongitude(ride.getDropLongitude());
        response.setDropAddress(ride.getDropAddress());
        response.setStatus(ride.getStatus());
        response.setEstimatedFare(ride.getEstimatedFare());

        return response;
    }

}
