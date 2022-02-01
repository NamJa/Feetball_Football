package com.example.feetballfootball

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.feetballfootball.api.playerstanding.PlayerStandingStatistics
import com.squareup.picasso.Picasso

private const val ARG_LEAGUE_ID = "LEAGUE_ID"

class LeaguePlayerStandingFragment : Fragment() {
    private var leagueID: Int = 0

    private lateinit var scorerRecyclerView: RecyclerView
    private lateinit var assistRecyclerView: RecyclerView

    private lateinit var standingViewModel: StandingViewModel

    private lateinit var playerTopScorerLiveData: LiveData<List<PlayerStandingStatistics>>
    private lateinit var playerTopAssistLiveData: LiveData<List<PlayerStandingStatistics>>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        leagueID = arguments?.getInt(ARG_LEAGUE_ID) ?: 0
        standingViewModel = ViewModelProvider(requireActivity()).get(StandingViewModel::class.java)
        playerTopScorerLiveData = standingViewModel.fetchPlayerTopScorerLiveData(leagueID)
        playerTopAssistLiveData = standingViewModel.fetchPlayerTopAssistLiveData(leagueID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_league_player_standing, container, false)
        initView(view)

        playerTopScorerLiveData.observe(
            viewLifecycleOwner,
            Observer { topScorer ->
                scorerRecyclerView.adapter = PlayerStandingRecyclerViewAdapter(calculateRanking(topScorer, false), topScorer, false)
                scorerRecyclerView.layoutManager = LinearLayoutManager(context)
            }
        )

        playerTopAssistLiveData.observe(
            viewLifecycleOwner,
            Observer { topAssist ->
                assistRecyclerView.adapter = PlayerStandingRecyclerViewAdapter(calculateRanking(topAssist, true), topAssist, true)
                assistRecyclerView.layoutManager = LinearLayoutManager(context)
            }
        )

        return view
    }

    fun initView(view: View){
        scorerRecyclerView = view.findViewById(R.id.scorer_recyclerview)
        assistRecyclerView = view.findViewById(R.id.assist_recyclerview)
    }

    fun calculateRanking(playerData: List<PlayerStandingStatistics>, isAssist: Boolean): List<String> {

        val rankList: MutableList<String> = mutableListOf()
        var temp = if (isAssist) {playerData[0].statistics[0].goals.assists } else { playerData[0].statistics[0].goals.total }
        var rank = 1
        var rankCnt = 0
        if (isAssist) {
            for (i in 0 until playerData.size) {
                if (temp == playerData[i].statistics[0].goals.assists) {
                    temp = playerData[i].statistics[0].goals.assists
                    rankCnt += 1
                    rankList.add(rank.toString())
                } else {
                    temp = playerData[i].statistics[0].goals.assists
                    if (rankCnt == 0) {
                        rank += 1
                        rankList.add(rank.toString())
                    } else {
                        rank += rankCnt
                        rankList.add(rank.toString())
                        rankCnt = 1
                    }
                }
            }
        } else {
            for (i in 0 until playerData.size) {
                if (temp == playerData[i].statistics[0].goals.total) {
                    temp = playerData[i].statistics[0].goals.total
                    rankCnt += 1
                    rankList.add(rank.toString())
                } else {
                    temp = playerData[i].statistics[0].goals.total
                    if (rankCnt == 0) {
                        rank += 1
                        rankList.add(rank.toString())
                    } else {
                        rank += rankCnt
                        rankList.add(rank.toString())
                        rankCnt = 1
                    }
                }
            }
        }
        return rankList
    }

    private inner class PlayerStandingRecyclerViewAdapter(
        val rankList: List<String>,
        val playerData: List<PlayerStandingStatistics>,
        val isAssist: Boolean): RecyclerView.Adapter<PlayerStandingRecyclerViewAdapter.ViewHolder>() {

        private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val rank: TextView
            val teamLogo: ImageView
            val playerName: TextView
            val playerGoals: TextView
            val playerAssist: TextView
            val totalShots: TextView
            val onShots: TextView
            val pk: TextView

            init {
                rank = view.findViewById(R.id.rank_textview)
                teamLogo = view.findViewById(R.id.team_logo_imageview)
                playerName = view.findViewById(R.id.player_name_textview)
                playerGoals = view.findViewById(R.id.player_goals_textview)
                playerAssist = view.findViewById(R.id.player_assist_textview)
                totalShots = view.findViewById(R.id.total_shots_textview)
                onShots = view.findViewById(R.id.on_shots_textview)
                pk = view.findViewById(R.id.pk_textview)
            }

            fun bind(position: Int) {
                rank.text = rankList[position]
                playerName.text = playerData[position].player.name
                playerGoals.text = playerData[position].statistics[0].goals.total.toString()
                playerAssist.text = playerData[position].statistics[0].goals.assists.toString()
                totalShots.text = playerData[position].statistics[0].shots.total.toString()
                onShots.text = playerData[position].statistics[0].shots.on.toString()
                pk.text = playerData[position].statistics[0].penalty.scored.toString()

                Picasso.get()
                    .load(playerData[position].statistics[0].team.logo)
                    .into(teamLogo)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            if(isAssist) {
                return ViewHolder(layoutInflater.inflate(R.layout.assist_recycler_item, parent, false))
            } else {
                return ViewHolder(layoutInflater.inflate(R.layout.scorer_recycler_item, parent, false))
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position)
        }

        override fun getItemCount(): Int {
            return playerData.size
        }
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