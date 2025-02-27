package com.chase1st.feetballfootball.api.fixturedetail

import com.chase1st.feetballfootball.api.Fixture
import com.chase1st.feetballfootball.api.Goals
import com.chase1st.feetballfootball.api.League
import com.chase1st.feetballfootball.api.Teams
// ㄴ 새로 만들어도 되지만 완벽하게 재사용이 가능하다.

class FixtureDetailResponse {
    lateinit var fixture: Fixture
    lateinit var league: League
    lateinit var teams: Teams
    lateinit var goals: Goals
    lateinit var events: List<Events>
    lateinit var lineups: List<Lineups>
    lateinit var statistics: List<Statistics>
    lateinit var players: List<PlayersByTeamData>
}