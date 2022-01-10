package com.example.feetballfootball.api.fixturedetail

import com.google.gson.annotations.SerializedName

data class TeamColorCode (
    @SerializedName("primary") var colorCode: String
)