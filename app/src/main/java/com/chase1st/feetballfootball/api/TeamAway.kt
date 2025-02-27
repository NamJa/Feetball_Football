package com.chase1st.feetballfootball.api

import com.google.gson.annotations.SerializedName

data class TeamAway (
    var id: Int = 40,
    var name: String = "",
    @SerializedName("logo") var logoUrl: String = ""
)