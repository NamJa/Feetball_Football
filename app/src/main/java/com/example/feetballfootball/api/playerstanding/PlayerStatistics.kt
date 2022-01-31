package com.example.feetballfootball.api.playerstanding

import com.example.feetballfootball.api.leaguestanding.Team

class PlayerStatistics {
    lateinit var team: Team
    lateinit var shots: Shots
    lateinit var goals: Goals
    lateinit var penalty: Penalty
}