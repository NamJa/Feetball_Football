package com.chase1st.feetballfootball.api.fixturedetail

import com.google.gson.annotations.SerializedName

class LineupTeam {
    var id: Int = 0 //default value
    lateinit var name: String
    @SerializedName("logo")
    lateinit var logoUrl: String
    lateinit var colors: TeamColors
}