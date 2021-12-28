package com.example.feetballfootball

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.feetballfootball.api.FixtureResponse
import com.example.feetballfootball.api.FootballApi
import com.example.feetballfootball.api.FootballResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.threeten.bp.LocalDate
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "FootballDataFetchr"
private const val API_KEY = "8194c946e31c7c72e64bd0d85d6734a0"

class FootballDataFetchr {
    var leagueCodeList: MutableList<Int> = mutableListOf(39, 140, 135, 78, 61, 45, 48, 528, 143, 81, 137, 66, 2, 3, 531, 848)

    private val footballApi: FootballApi
    private lateinit var footballDataRequest: Call<FootballResponse>

    init {
        val client = OkHttpClient.Builder().addInterceptor(object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
                val original = chain.request()
                val request = original.newBuilder()
                    .header("x-rapidapi-key", API_KEY)
                    .header("x-rapidapi-host", "v3.football.api-sports.io")
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

    fun fetchFootballFixtures(date: String, league: Int, season: Int) : LiveData<List<FixtureResponse>> {
//        var footballData: List<FixtureResponse> = mutableListOf()
        var footballLiveData: MutableLiveData<List<FixtureResponse>> = MutableLiveData()
        footballApi.fetchFixtures(date, league, season, timezone="Asia/Seoul").enqueue(object : Callback<FootballResponse> {
            override fun onResponse(
                call: Call<FootballResponse>,
                response: Response<FootballResponse>
            ) {
                val footballResponse: FootballResponse? = response.body()
                val fixtureResponse: List<FixtureResponse>? = footballResponse?.response
                Log.d(TAG, "success")
                fixtureResponse?.let {
//                    footballData = it
                    footballLiveData.value = it
                }
            }

            override fun onFailure(call: Call<FootballResponse>, t: Throwable) {
                Log.e(TAG, "failed to parse data", t)
            }
        })
        return footballLiveData
    }

    fun allLeagueFixtureFetch(date: String, season: Int): MutableList<LiveData<List<FixtureResponse>>> {
        val allLeagueFixture: MutableList<LiveData<List<FixtureResponse>>> = mutableListOf()
//        val fixtureDataLiveData: MutableLiveData<MutableList<LiveData<List<FixtureResponse>>>> = MutableLiveData()
        leagueCodeList.forEach{
            allLeagueFixture.add(fetchFootballFixtures(date, it, season))
        }

        return allLeagueFixture
    }
}