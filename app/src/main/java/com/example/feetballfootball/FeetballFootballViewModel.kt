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
    var currentYear = Year.now().value
    //val fixtureData :MutableLiveData<Array<MutableList<FixtureResponse>?>>
    val fixtureDataExecute : Array<MutableList<FixtureResponse>?>
    var resultData: MutableLiveData<Int>
    private val footballDataFetchr = FootballDataFetchr()

    init {
        //fixtureData = footballDataFetchr.fetchFootballFixtures(currentDate, currentYear)
        fixtureDataExecute = footballDataFetchr.fetchFootballFixturesExecute(currentDate, currentYear)
        resultData = footballDataFetchr.getResultData()
    }


}