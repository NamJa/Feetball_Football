package com.example.feetballfootball

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.feetballfootball.api.fixturedetail.FixtureDetailResponse

class FixtureDetailViewModel: ViewModel() {
    val fixtureDetailLiveData: LiveData<List<FixtureDetailResponse>>
    val footballDataFetchr = FootballDataFetchr()

    init {
        fixtureDetailLiveData = fetchFixtureDetailLiveData(fixtureID = 710369)
    }

    fun fetchFixtureDetailLiveData(fixtureID: Int): LiveData<List<FixtureDetailResponse>> {
        return footballDataFetchr.fetchFixtureDetailData(fixtureID)
    }
}