package com.example.feetballfootball

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.feetballfootball.api.FixtureResponse
import org.threeten.bp.LocalDate
import org.threeten.bp.Year

class FeetballFootballViewModel: ViewModel() {
    var currentDate = LocalDate.now().toString()
    var currentYear = Year.now().value
    val fixtureData: MutableList<LiveData<List<FixtureResponse>>>
    private val footballDataFetchr = FootballDataFetchr()

    init {
        fixtureData = footballDataFetchr.allLeagueFixtureFetch(currentDate, currentYear)
    }


}