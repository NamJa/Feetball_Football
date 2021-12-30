package com.example.feetballfootball

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.feetballfootball.api.leaguestanding.StandingResponse
import com.example.feetballfootball.api.leaguestanding.Standings
import org.threeten.bp.Year

class StandingViewModel: ViewModel() {
    var currentYear = Year.now().value
    val standingLiveData: LiveData<List<Standings>>
    private val footballDataFetchr = FootballDataFetchr()


    init {
        standingLiveData = fetchStadingLiveData(league = 39)
    }
    fun fetchStadingLiveData(league: Int) : LiveData<List<Standings>> {
        return footballDataFetchr.fetchLeagueStandings(league, season = currentYear)
    }
}