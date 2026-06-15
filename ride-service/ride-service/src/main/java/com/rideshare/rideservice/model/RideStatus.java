package com.rideshare.rideservice.model;

/**
 * Flow:
 * requested -> matching -> accepted -> driver arriving
 *           -> ride started -> completed
 *           -> cancelled (can happen at multiple stages
 *
 * */
public enum RideStatus {
    REQUESTED,
    MATCHING,
    ACCEPTED,
    DRIVER_ARRIVING,
    RIDE_STARTED,
    COMPLETED,
    CANCELLED
}
