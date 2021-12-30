package com.example.feetballfootball.api.leaguestanding

data class Standings (
    var rank: Int,
    var team: Team,
    var points: Int,
    var goalsDiff: Int,
    var form: String,
    var all: All
)