package com.chase1st.feetballfootball.api.leaguestanding

import com.google.gson.annotations.SerializedName

data class Goals(
    @SerializedName("for")
    var gainGoals: Int,
    @SerializedName("against")
    var againstGoals: Int
)
// 득점
// 실점