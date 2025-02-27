package com.chase1st.feetballfootball.api.fixturedetail

class Lineups {
    lateinit var team: LineupTeam
    lateinit var coach: Coach
    lateinit var formation: String
    lateinit var startXI: List<PlayerData>
    lateinit var substitutes: List<PlayerData>
}