package com.chase1st.feetballfootball.api.fixturedetail

import com.chase1st.feetballfootball.api.leaguestanding.Team

class PlayersByTeamData {
    lateinit var team: Team
    lateinit var players: List<PlayerRatingData>
}