package com.example.feetballfootball

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.feetballfootball.api.FixtureResponse
import org.threeten.bp.LocalDate
import org.threeten.bp.Year
import java.lang.IndexOutOfBoundsException

private const val TAG = "FixtureFragment"

class FixtureFragment : Fragment() {

    private lateinit var datas: List<List<FixtureResponse>>
    private lateinit var feetballfootballViewModel: FeetballFootballViewModel
    var currentDate = LocalDate.now().toString()
    var currentYear = Year.now().value
    val footballDataFetchr = FootballDataFetchr()
    private lateinit var fixtureViewModelData: MutableList<LiveData<List<FixtureResponse>>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        feetballfootballViewModel = ViewModelProvider(this).get(FeetballFootballViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("currentDate", currentDate)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_fixture, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        feetballfootballViewModel.fixtureData.forEach { leagues ->
            leagues.observe(
                viewLifecycleOwner,
                Observer {
                    try {
                        Log.d(TAG, it.get(0).league.name)
                    } catch (e: IndexOutOfBoundsException) {
                        Log.d(TAG, "일정이 없습니다.")
                        // 전부 이런 오류가 뜬다면 진짜로 경기가 없는거임
                    }
                }
            )
        }

//        datas.forEach {
//            try {
//                Log.d(TAG, it.get(0).league.name)
//            } catch (e: IndexOutOfBoundsException) {
//                Log.d(TAG, "일정이 없습니다.")
//            }
//        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = FixtureFragment()
    }
}