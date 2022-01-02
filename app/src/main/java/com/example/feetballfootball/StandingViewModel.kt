package com.example.feetballfootball

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.feetballfootball.api.leaguestanding.StandingResponse
import com.example.feetballfootball.api.leaguestanding.Standings
import org.threeten.bp.LocalDate
import org.threeten.bp.Year

class StandingViewModel: ViewModel() {
    val standingLiveData: LiveData<List<Standings>>
    private val footballDataFetchr = FootballDataFetchr()

    // 추춘제 리그 때문에 이렇게 연도 데이터 할당
    val currentSeason = if (LocalDate.now().monthValue < 7) {
        Year.now().minusYears(1).value
    } else { Year.now().value }

    init {
        standingLiveData = fetchStadingLiveData(league = 39)
    }
    fun fetchStadingLiveData(league: Int) : LiveData<List<Standings>> {
        return footballDataFetchr.fetchLeagueStandings(league, season = currentSeason)
    }
}