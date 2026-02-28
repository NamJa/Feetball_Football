package com.example.feetballfootball.fragment.Leagues

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.feetballfootball.R
import com.example.feetballfootball.api.playerstanding.PlayerStandingStatistics
import com.example.feetballfootball.databinding.FragmentLeaguePlayerStandingBinding
import com.example.feetballfootball.databinding.ScorerRecyclerItemBinding
import com.example.feetballfootball.viewModel.StandingViewModel
import com.squareup.picasso.Picasso

private const val ARG_LEAGUE_ID = "LEAGUE_ID"

class LeaguePlayerStandingFragment : Fragment() {
    private var leagueID: Int = 0

    private var _binding: FragmentLeaguePlayerStandingBinding? = null
    private val binding get() = _binding!!

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
    ): View {
        _binding = FragmentLeaguePlayerStandingBinding.inflate(inflater, container, false)

        playerTopScorerLiveData.observe(
            viewLifecycleOwner,
            Observer { topScorer ->
                binding.scorerRecyclerview.adapter = PlayerStandingRecyclerViewAdapter(calculateRanking(topScorer, false), topScorer, false)
                binding.scorerRecyclerview.layoutManager = LinearLayoutManager(context)
            }
        )

        playerTopAssistLiveData.observe(
            viewLifecycleOwner,
            Observer { topAssist ->
                binding.assistRecyclerview.adapter = PlayerStandingRecyclerViewAdapter(calculateRanking(topAssist, true), topAssist, true)
                binding.assistRecyclerview.layoutManager = LinearLayoutManager(context)
            }
        )

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

        private inner class ViewHolder(val binding: ScorerRecyclerItemBinding) : RecyclerView.ViewHolder(binding.root) {

            fun bind(position: Int) {
                binding.rankTextview.text = rankList[position]
                binding.playerNameTextview.text = playerData[position].player.name
                binding.playerGoalsTextview.text = playerData[position].statistics[0].goals.total.toString()
                binding.playerAssistTextview.text = playerData[position].statistics[0].goals.assists.toString()
                binding.totalShotsTextview.text = playerData[position].statistics[0].shots.total.toString()
                binding.onShotsTextview.text = playerData[position].statistics[0].shots.on.toString()
                binding.pkTextview.text = playerData[position].statistics[0].penalty.scored.toString()

                Picasso.get()
                    .load(playerData[position].statistics[0].team.logo)
                    .into(binding.teamLogoImageview)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = if(isAssist) {
                layoutInflater.inflate(R.layout.assist_recycler_item, parent, false)
            } else {
                layoutInflater.inflate(R.layout.scorer_recycler_item, parent, false)
            }
            return ViewHolder(ScorerRecyclerItemBinding.bind(view))
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