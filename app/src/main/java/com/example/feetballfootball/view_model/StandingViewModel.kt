package com.example.feetballfootball.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.feetballfootball.api.leaguestanding.Standings
import com.example.feetballfootball.api.playerstanding.PlayerStandingStatistics
import com.example.feetballfootball.util.FootballDataFetchr
import org.threeten.bp.LocalDate
import org.threeten.bp.Year

class StandingViewModel: ViewModel() {
    val clubStandingLiveData: LiveData<List<Standings>>
    val playerTopScorerLiveData: LiveData<List<PlayerStandingStatistics>>
    val playerTopAssistLiveData: LiveData<List<PlayerStandingStatistics>>
    private val footballDataFetchr = FootballDataFetchr()

    // 추춘제 리그 때문에 이렇게 연도 데이터 할당
    val currentSeason = if (LocalDate.now().monthValue < 8) {
        Year.now().minusYears(1).value
    } else { Year.now().value }

    init {
        clubStandingLiveData = fetchStadingLiveData(league = 39)
        playerTopScorerLiveData = fetchPlayerTopScorerLiveData(league = 39)
        playerTopAssistLiveData = fetchPlayerTopAssistLiveData(league = 39)
    }

    fun fetchStadingLiveData(league: Int) : LiveData<List<Standings>> {
        return footballDataFetchr.fetchLeagueStandings(league, season = currentSeason)
    }

    fun fetchPlayerTopScorerLiveData(league: Int) : LiveData<List<PlayerStandingStatistics>> {
        return footballDataFetchr.fetchPlayerScorerData(league, season = currentSeason)
    }
    fun fetchPlayerTopAssistLiveData(league: Int) : LiveData<List<PlayerStandingStatistics>> {
        return footballDataFetchr.fetchPlayerAssistData(league, season = currentSeason)
    }
}