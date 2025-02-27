package com.chase1st.feetballfootball.util

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.chase1st.feetballfootball.api.FixtureResponse
import com.chase1st.feetballfootball.api.FootballApi
import com.chase1st.feetballfootball.api.FootballResponse
import com.chase1st.feetballfootball.api.fixturedetail.FixtureDetailResponse
import com.chase1st.feetballfootball.api.fixturedetail.MatchDetailResponse
import com.chase1st.feetballfootball.api.leaguestanding.LeagueStandingsResponse
import com.chase1st.feetballfootball.api.leaguestanding.StandingResponse
import com.chase1st.feetballfootball.api.leaguestanding.Standings
import com.chase1st.feetballfootball.api.playerstanding.PlayerStandingResponse
import com.chase1st.feetballfootball.api.playerstanding.PlayerStandingStatistics
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
private const val FETCHDETAILDATA = "fetchFixtureDetailData"
private const val API_KEY = "b4be97da4d76733e9ca2391bb8794e5c"

class FootballDataFetchr {
    /**
     *  * ***************  League  ******************
     * 39: EPL, 140: LA LIGA,
     * 135: SERIE A, 78: BUNDESLIGA,
     * 61: LIGUE 1,
     * ****************  Cup  ********************
     * 45: FA CUP, 48: League CUP,
     * 528: Community Shield, 143: Copa del Rey,
     * 81: DFB Pokal, 137: Coppa Italia,
     * 66: Couppe de France,
     * **********  UEFA LEAGUE & CUP  ************
     * 2: UEFA Champions League, 3: UEFA Europa League, 531: Super Cup, 848: UEFA Europa Conference League
     * **********  A-Match  ************
     * 30: World Cup-Qualification Asia, 31: World Cup-Qualification CONCACAF, 32: World Cup-Qualification Europe, 33: World Cup Qualification Oceania, 34: World Cup - Qualification South America
     * */
    val leagueCodeList: List<Int> =
        mutableListOf(39, 140, 135, 78, 61, 45, 48, 528, 143, 81, 137, 66, 2, 3, 531, 848, 30, 32, 34, 33)
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
//    fun fetchFootballFixturesExecute(date: String, season: Int) : MutableLiveData<Array<MutableList<FixtureResponse>?>> {
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
//                        fixtureResultLiveData.value = 1
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
        date: String
    ): Array<MutableList<FixtureResponse>?> {
        var footballDataByLeague = arrayOfNulls<MutableList<FixtureResponse>>(leagueCodeList.size)

        thread {
            var data: List<FixtureResponse> = emptyList()
            val response = footballApi.fetchAllFixtures(date, timezone = "Asia/Seoul").execute()
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

    /* 세부 경기 데이터 parsing */
    fun fetchFixtureDetailData(id: Int): LiveData<List<FixtureDetailResponse>> {
        val fixtureDetailLiveData: MutableLiveData<List<FixtureDetailResponse>> = MutableLiveData()

        footballApi.fetchFixtureDetail(id, timezone = "Asia/Seoul").enqueue(object : Callback<MatchDetailResponse> {
            override fun onResponse(
                call: Call<MatchDetailResponse>,
                response: Response<MatchDetailResponse>
            ) {
                if(response.isSuccessful) {
                    val matchDetailResponse: MatchDetailResponse? = response.body()
                    val fixtureDetailResponse: List<FixtureDetailResponse>? = matchDetailResponse?.response
                    fixtureDetailResponse?.let {
//                        Log.d(FETCHDETAILDATA, it[0].players[0].team.name)
//                        Log.d(FETCHDETAILDATA, it[0].players[0].players[0].player.name)
//                        Log.d(FETCHDETAILDATA, it[0].players[0].players[0].statistics[0].games.rating.toString())
                        // 경기가 연기된 경우에는 위의 로깅 구문이 null값으로 들어온다.
                        fixtureDetailLiveData.value = it
                    }
                }
            }

            override fun onFailure(call: Call<MatchDetailResponse>, t: Throwable) {
                Log.e(FETCHDETAILDATA, "세부 경기 데이터 수신 실패", t)
            }
        })
        return fixtureDetailLiveData
    }

    /* 리그 득점 및 어시스트 순위*/
    fun fetchPlayerScorerData(league: Int, season: Int): LiveData<List<PlayerStandingStatistics>> {

        val playerScorerLiveData: MutableLiveData<List<PlayerStandingStatistics>> = MutableLiveData()

        footballApi.fetchTopScorers(league, season).enqueue(object : Callback<PlayerStandingResponse> {
            override fun onResponse(
                call: Call<PlayerStandingResponse>,
                response: Response<PlayerStandingResponse>
            ) {
                if(response.isSuccessful) {
                    val standingResponse: PlayerStandingResponse? = response.body()
                    val playerStandingResponse: List<PlayerStandingStatistics>? = standingResponse?.response
                    playerStandingResponse?.let {
                        try {
                            Log.d("playerData", it[1].player.name)
                            Log.d("playerData", it[1].statistics[0].goals.total.toString())
                            playerScorerLiveData.value = it
                        } catch (e: IndexOutOfBoundsException) {
                            Log.d("playerData", "리그 득점 데이터 로딩중...")
                        }
                    }
                }
            }

            override fun onFailure(call: Call<PlayerStandingResponse>, t: Throwable) {
                Log.e(FETCHDETAILDATA, "득점 데이터 수신 실패", t)
            }
        })
        return playerScorerLiveData
    }

    fun fetchPlayerAssistData(league: Int, season: Int): LiveData<List<PlayerStandingStatistics>> {
        val playerAssistLiveData: MutableLiveData<List<PlayerStandingStatistics>> = MutableLiveData()
        footballApi.fetchTopAssists(league, season).enqueue(object : Callback<PlayerStandingResponse>{
            override fun onResponse(
                call: Call<PlayerStandingResponse>,
                response: Response<PlayerStandingResponse>
            ) {
                if (response.isSuccessful) {
                    val standingResponse: PlayerStandingResponse? = response.body()
                    val playerStandingResponse: List<PlayerStandingStatistics>? = standingResponse?.response
                    playerStandingResponse?.let {
                        try {
                            Log.d("playerData", it[1].player.name)
                            Log.d("playerData", it[1].statistics[0].goals.total.toString())
                            playerAssistLiveData.value = it
                        } catch (e: IndexOutOfBoundsException) {
                            Log.d("playerData", "리그 어시스트 데이터 로딩중...")
                        }
                    }
                }
            }

            override fun onFailure(call: Call<PlayerStandingResponse>, t: Throwable) {
                Log.e(FETCHDETAILDATA, "어시스트 데이터 수신 실패", t)
            }
        })
        return playerAssistLiveData
    }
}