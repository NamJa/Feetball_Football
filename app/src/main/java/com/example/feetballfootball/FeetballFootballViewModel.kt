package com.example.feetballfootball

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.feetballfootball.api.FixtureResponse
import com.example.feetballfootball.api.leaguestanding.StandingResponse
import org.threeten.bp.LocalDate
import org.threeten.bp.Year

class FeetballFootballViewModel: ViewModel() {
    var currentDate = LocalDate.now().toString()
    //val fixtureData :MutableLiveData<Array<MutableList<FixtureResponse>?>>
    val fixtureDataExecute : Array<MutableList<FixtureResponse>?>
    var resultData: MutableLiveData<Int>
    private val footballDataFetchr = FootballDataFetchr()

    val currentSeason = if (LocalDate.now().monthValue < 7) {
        Year.now().minusYears(1).value
    } else { Year.now().value }

    init {
        //fixtureData = footballDataFetchr.fetchFootballFixtures(currentDate, currentYear)
        fixtureDataExecute = fetchFixtureData(currentDate)
        resultData = footballDataFetchr.getResultData()
    }

    fun fetchFixtureData(date: String) : Array<MutableList<FixtureResponse>?> {
        return footballDataFetchr.fetchFootballFixturesExecute(date, currentSeason)
    }

}