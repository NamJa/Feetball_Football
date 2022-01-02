package com.example.feetballfootball

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.feetballfootball.api.FixtureResponse
import com.example.feetballfootball.api.FootballApi
import com.example.feetballfootball.api.FootballResponse
import com.example.feetballfootball.api.leaguestanding.LeagueStandingsResponse
import com.example.feetballfootball.api.leaguestanding.StandingResponse
import com.example.feetballfootball.api.leaguestanding.Standings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.concurrent.thread

private const val TAG = "FootballDataFetchr"
private const val FETCHSTANDING = "fetchLeagueStandings"
private const val API_KEY = "b4be97da4d76733e9ca2391bb8794e5c"

class FootballDataFetchr {
    val leagueCodeList: List<Int> =
        mutableListOf(39, 140, 135, 78, 61, 45, 48, 528, 143, 81, 137, 66, 2, 3, 531, 848)
    private var fixtureResultLiveData: MutableLiveData<Int> = MutableLiveData()

    private val footballApi: FootballApi

    init {
        val client = OkHttpClient.Builder().addInterceptor(object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
                val original = chain.request()
                val request = original.newBuilder()
                    .header("x-rapidapi-key", API_KEY)
                    .header("x-rapidapi-host", "v3.football.api-sports.io")
                    .header("Authorization", "Basic Og==")
                    .build()
                return chain.proceed(request)
            }
        }).build()

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://v3.football.api-sports.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        footballApi = retrofit.create(FootballApi::class.java)
    }

    // /* async 함수 */
//    fun fetchFootballFixtures(date: String, season: Int) : MutableLiveData<Array<MutableList<FixtureResponse>?>> {
////        var footballDataByLeague: MutableList<List<FixtureResponse>> = mutableListOf(listOf<FixtureResponse>())
//        var footballDataByLeague = arrayOfNulls<MutableList<FixtureResponse>>(leagueCodeList.size)
//
//        var footballLiveData: MutableLiveData<Array<MutableList<FixtureResponse>?>> = MutableLiveData()
//        footballApi.fetchAllFixtures(date, season, timezone="Asia/Seoul").enqueue(object : Callback<FootballResponse> {
//            override fun onResponse(
//                call: Call<FootballResponse>,
//                response: Response<FootballResponse>
//            ) {
//                if (response.isSuccessful) {
//                    val footballResponse: FootballResponse? = response.body()
//                    val fixtureResponse: List<FixtureResponse>? = footballResponse?.response
//                    Log.d(TAG, "received result")
//                    fixtureResponse?.let { footballData ->
//                        for(i in 0 until footballData.size)
//                        {
//                            if(footballData[i].league.id !in leagueCodeList){
//                                continue
//                            } else {
//                                if(footballDataByLeague[leagueCodeList.indexOf(footballData[i].league.id)] == null) {
//                                    footballDataByLeague[leagueCodeList.indexOf(footballData[i].league.id)] = mutableListOf(footballData[i])
//                                } else {
//                                    footballDataByLeague[leagueCodeList.indexOf(footballData[i].league.id)]?.add(footballData[i])
//                                }
//                            }
//                        }
//                        footballLiveData.value = footballDataByLeague
//                        resultLiveData.value = 1
//                    }
//                }
//            }
//
//            override fun onFailure(call: Call<FootballResponse>, t: Throwable) {
//                Log.e(TAG, "failed to parse data", t)
//            }
//        })
//        return footballLiveData
//    }

    /*동기 실행 함수*/
    /* 리그 일정 데이터(전 리그)*/
    fun fetchFootballFixturesExecute(
        date: String,
        season: Int
    ): Array<MutableList<FixtureResponse>?> {
        var footballDataByLeague = arrayOfNulls<MutableList<FixtureResponse>>(leagueCodeList.size)

        thread {
            var data: List<FixtureResponse> = emptyList()
            val response = footballApi.fetchAllFixtures(date, season, timezone = "Asia/Seoul").execute()
            CoroutineScope(Dispatchers.Main).launch {
                if(response.isSuccessful) {
                    val footballResponse: FootballResponse? = response.body()
                    val fixtureResponse: List<FixtureResponse>? = footballResponse?.response
                    fixtureResponse?.let { footballData ->
                        for(i in 0 until footballData.size)
                        {
                            if(footballData[i].league.id !in leagueCodeList){
                                continue
                            } else {
                                if(footballDataByLeague[leagueCodeList.indexOf(footballData[i].league.id)] == null) {
                                    footballDataByLeague[leagueCodeList.indexOf(footballData[i].league.id)] = mutableListOf(footballData[i])
                                } else {
                                    footballDataByLeague[leagueCodeList.indexOf(footballData[i].league.id)]?.add(footballData[i])
                                }
                            }
                        }
                        fixtureResultLiveData.value = 1
                    }
                } else {
                    data = response.body()?.response ?: emptyList()
                }
            }
        }
        return footballDataByLeague
    }

    fun getResultData(): MutableLiveData<Int> {
        return fixtureResultLiveData
    }

    /* 리그 순위 데이터 parsing */
    fun fetchLeagueStandings(league: Int, season: Int): LiveData<List<Standings>> {
        var standingLiveData: MutableLiveData<List<Standings>> = MutableLiveData()

        footballApi.fetchStands(league, season).enqueue(object : Callback<LeagueStandingsResponse> {
            override fun onResponse(
                call: Call<LeagueStandingsResponse>,
                response: Response<LeagueStandingsResponse>
            ) {
                if (response.isSuccessful) {
                    val leagueStandingsResponse: LeagueStandingsResponse? = response.body()
                    leagueStandingsResponse?.let {
                        Log.d(FETCHSTANDING, it.response.toString())
                    }
                    val standingResponse: List<StandingResponse>? = leagueStandingsResponse?.response
                    standingResponse?.let {
                        Log.d(FETCHSTANDING, it.toString())
//                        Log.d(FETCHSTANDING, it.get(0).league.standings.get(0).get(0).team.name)
                        //                        필수                     필수    순위
                        // json 구조가 좀 이상함
                        val res = it.get(0).league.standings.get(0)
                        standingLiveData.value = res
                    }
                }
            }
            override fun onFailure(call: Call<LeagueStandingsResponse>, t: Throwable) {
                Log.e(FETCHSTANDING, "리그 순위 데이터 수신 실패", t)
            }
        })
        return standingLiveData
    }
}