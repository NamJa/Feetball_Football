package com.chase1st.feetballfootball.api.fixturedetail

import com.google.gson.annotations.SerializedName

data class TeamColorCode (
    @SerializedName("primary") var colorCode: String
)