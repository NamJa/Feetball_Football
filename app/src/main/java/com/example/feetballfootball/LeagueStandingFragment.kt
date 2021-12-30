package com.example.feetballfootball

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

private const val TAG = "LeagueStandingFragment"
private const val ARG_LEAGUE_ID = "league_id"

class LeagueStandingFragment : Fragment() {
    private var leagueId: Int? = 0
    private var leagueCodeMap = mapOf(1 to 39, 2 to 140, 3 to 135, 4 to 78, 5 to 61)

    private lateinit var textview : TextView
    private var footballDataFetchr: FootballDataFetchr = FootballDataFetchr()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        leagueId = arguments?.getInt(ARG_LEAGUE_ID)
        Log.d(TAG, "received ${leagueId}")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_league_standing, container, false)
        textview = view.findViewById(R.id.league_stangind)
        textview.text = leagueId.toString()
        footballDataFetchr.fetchLeagueStandings("39", "2021")

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(leagueId: Int): LeagueStandingFragment {
            val args = Bundle().apply {
                putInt(ARG_LEAGUE_ID, leagueId)
            }
            return LeagueStandingFragment().apply {
                arguments = args
            }

        }
    }

}