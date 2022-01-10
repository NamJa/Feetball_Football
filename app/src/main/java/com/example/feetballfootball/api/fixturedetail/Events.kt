package com.example.feetballfootball.api.fixturedetail

import com.example.feetballfootball.api.leaguestanding.Team

class Events {
    lateinit var time: Time
    lateinit var team: Team
    lateinit var player: EventPlayer
    lateinit var assist: Assist
    lateinit var type: String
    lateinit var detail: String

}