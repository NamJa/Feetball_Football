package com.chase1st.feetballfootball.viewModel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.chase1st.feetballfootball.api.FixtureResponse
import com.chase1st.feetballfootball.util.FootballDataFetchr
import org.threeten.bp.LocalDate
import org.threeten.bp.Year

class FeetballFootballViewModel: ViewModel() {
    var currentDate = LocalDate.now().toString()
//    val fixtureData :MutableLiveData<Array<MutableList<FixtureResponse>?>>
    val fixtureDataExecute : Array<MutableList<FixtureResponse>?>
    var resultData: MutableLiveData<Int>
    private val footballDataFetchr = FootballDataFetchr()

    val currentSeason = if (LocalDate.now().monthValue < 7) {
        Year.now().minusYears(1).value
    } else { Year.now().value }

    init {
//        fixtureData = fetchAsyncFixtureData(currentDate)
        fixtureDataExecute = fetchFixtureData(currentDate)
        resultData = footballDataFetchr.getResultData()
        Log.d("ViewModel", currentDate.toString())
    }

    fun fetchFixtureData(date: String) : Array<MutableList<FixtureResponse>?> {
        currentDate = date
        return footballDataFetchr.fetchFootballFixturesExecute(date)
    }
}