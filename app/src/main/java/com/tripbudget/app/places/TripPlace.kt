package com.tripbudget.app.places

/** A searched place stored on a trip so header weather can follow it. */
data class TripPlace(
    val name: String,
    val lat: Double,
    val lng: Double,
)
