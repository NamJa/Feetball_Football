package com.example.feetballfootball

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

private const val ARG_LEAGUE_ID = "LEAGUE_ID"

class LeaguePlayerStandingFragment : Fragment() {
    private var leagueID: Int = 0

    private lateinit var scorerRecyclerView: RecyclerView
    private lateinit var assistRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        leagueID = arguments?.getInt(ARG_LEAGUE_ID) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_league_player_standing, container, false)
        initView(view)


        return view
    }

    fun initView(view: View){
        scorerRecyclerView = view.findViewById(R.id.score_textview)
        assistRecyclerView = view.findViewById(R.id.assist_recyclerview)
    }

    companion object {
        @JvmStatic
        fun newInstance(leagueId: Int) : LeaguePlayerStandingFragment {
            val args = Bundle().apply {
                putInt(ARG_LEAGUE_ID, leagueId)
            }
            return LeaguePlayerStandingFragment().apply {
                arguments = args
            }
        }
    }
}