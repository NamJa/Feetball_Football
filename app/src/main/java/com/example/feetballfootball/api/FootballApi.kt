package com.example.feetballfootball.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface FootballApi {

    // 리그 코드(id값)
    /*
        -- League --                --   Cup  --                --    Uefa Cup   --
        EPL: 39                     FA CUP: 45                  UEFA Champions League: 2
        la liga: 140 spain          League Cup: 48              UEFA Europa League: 3
        Serie A: 135                Community Shield: 528       UEFA Super Cup: 531
        Bundesliga 1: 78            Copa del Rey: 143           UEFA Europa Conference League: 848
        ligue 1: 61                 DFB Pokal: 81               UEFA Nations League: 5
                                    Coppa italia: 137
                                    Coupe de France: 66
    * */
    @GET("fixtures")
    fun fetchFixtures(
        @Query("date") date: String,
        @Query("league") league: Int,
        @Query("season") season: Int,
        @Query("timezone") timezone: String
    ): Call<FootballResponse>

    @GET("fixtures")
    fun fetchAllFixtures(
        @Query("date") date: String,
        @Query("season") season: Int,
        @Query("timezone") timezone: String
    ): Call<FootballResponse>
}