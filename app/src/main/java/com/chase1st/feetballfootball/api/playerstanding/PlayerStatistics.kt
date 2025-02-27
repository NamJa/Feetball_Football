package com.chase1st.feetballfootball.api.playerstanding

import com.chase1st.feetballfootball.api.leaguestanding.Team

class PlayerStatistics {
    lateinit var team: Team
    lateinit var shots: Shots
    lateinit var goals: Goals
    lateinit var penalty: Penalty
}