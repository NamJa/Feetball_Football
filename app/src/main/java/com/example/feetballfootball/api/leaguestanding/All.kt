package com.example.feetballfootball.api.leaguestanding

data class All (
    var played: Int,
    var win: Int,
    var draw: Int,
    var lose: Int,
    var goals: Goals
)