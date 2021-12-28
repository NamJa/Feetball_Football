package com.example.feetballfootball

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.feetballfootball.api.Fixture
import com.example.feetballfootball.api.FixtureResponse
import com.example.feetballfootball.api.FootballApi
import com.example.feetballfootball.api.FootballResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "FootballDataFetchr"

class FootballDataFetchr {
    private val footballApi: FootballApi
    private lateinit var footballDataRequest: Call<FootballResponse>

    init {
        val client = OkHttpClient.Builder().addInterceptor(object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
                val original = chain.request()
                val request = original.newBuilder()
                    .header("x-rapidapi-key", "8194c946e31c7c72e64bd0d85d6734a0")
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

    fun fetchFootballFixtures(date: String, league: Int, season: Int){
        val footballLiveData: MutableLiveData<List<FixtureResponse>> = MutableLiveData()

        footballApi.fetchFixtures(date, league, season, timezone="Asia/Seoul").enqueue(object : Callback<FootballResponse> {
            override fun onResponse(
                call: Call<FootballResponse>,
                response: Response<FootballResponse>
            ) {
                val footballResponse: FootballResponse? = response.body()
                val fixtureResponse: List<FixtureResponse>? = footballResponse?.response
                footballLiveData.value = fixtureResponse
                fixtureResponse?.let {
                    Log.d(TAG, it.get(3).teams.home.name + " " + it.get(3).teams.away.name)
                    Log.d(TAG, it.get(3).goals.home.toString() + " " + it.get(3).goals.away.toString())
                }
            }

            override fun onFailure(call: Call<FootballResponse>, t: Throwable) {
                Log.e(TAG, "failed to parse data", t)
            }
        })
        //return footballLiveData
    }
}