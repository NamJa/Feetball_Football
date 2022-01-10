package com.example.feetballfootball.api.fixturedetail

import com.example.feetballfootball.api.Fixture
import com.example.feetballfootball.api.Goals
import com.example.feetballfootball.api.League
import com.example.feetballfootball.api.Teams
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