package com.example.feetballfootball.api

data class Fixture (
    var id: Int = 0,
    var referee: String = "",
    var timestamp: Int = 0,
    var venue: FixtureVenue,
    var status: FixtureStatus
)