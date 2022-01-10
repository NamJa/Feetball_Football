package com.example.feetballfootball.api.fixturedetail

import com.example.feetballfootball.api.leaguestanding.Team

class PlayersByTeamData {
    lateinit var team: Team
    lateinit var players: List<PlayerRatingData>
}