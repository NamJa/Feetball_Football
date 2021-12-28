package com.example.feetballfootball.api

import com.google.gson.annotations.SerializedName

data class TeamHome (
    var id: Int = 40,
    var name: String = "",
    @SerializedName("logo") var logoUrl: String = ""
)
