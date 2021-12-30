package com.example.feetballfootball.api.leaguestanding

data class League (
    var id: Int,
    var name: String,
    var country: String,
    var logo: String,
    var flag: String,
    var season: String,
    var standings: List<List<Standings>>
)