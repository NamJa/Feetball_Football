package com.example.feetballfootball.fragment.fixture

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.feetballfootball.adapter.PlayerLineupAdapter
import com.example.feetballfootball.api.fixturedetail.FixtureDetailResponse
import com.example.feetballfootball.api.fixturedetail.PlayerData
import com.example.feetballfootball.api.fixturedetail.PlayersByTeamData
import com.example.feetballfootball.databinding.FragmentFixtureDetailLineupBinding
import com.example.feetballfootball.databinding.LineupRowRecyclerItemBinding
import com.example.feetballfootball.viewModel.FixtureDetailViewModel
import com.squareup.picasso.Picasso

private const val ARG_FIXTURE_ID = "fixture_id"

class FixtureDetailLineupFragment : Fragment() {
    private var fixtureID: Int = 0
    private lateinit var fixtureDetailViewModel: FixtureDetailViewModel
    private lateinit var fixtureDetailLiveData: LiveData<List<FixtureDetailResponse>>

    private var _binding: FragmentFixtureDetailLineupBinding? = null
    private val binding get() = _binding!!

    var startLineup: MutableList<MutableList<PlayerData>> = mutableListOf()
    var playerData: MutableList<PlayerData> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fixtureID = arguments?.getInt(ARG_FIXTURE_ID) ?: 0
        fixtureDetailViewModel = ViewModelProvider(requireActivity()).get(FixtureDetailViewModel::class.java)
        fixtureDetailLiveData = fixtureDetailViewModel.fetchFixtureDetailLiveData(fixtureID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFixtureDetailLineupBinding.inflate(inflater, container, false)

        fixtureDetailLiveData.observe(
            viewLifecycleOwner,
            Observer {
                if (it.isEmpty()) return@Observer
                var lineupData = it[0].lineups[0]

                for (teamIndex in 0..1) {
                    var row = "1"
                    startLineup = mutableListOf()
                    playerData = mutableListOf()
                    Log.d("startllineup", "startllineup1"+startLineup.size.toString())
                    for (i in 0..11) {
                        if (i == 11) {
                            startLineup.add(playerData)
                            break
                        }
                        if (row == it[0].lineups[teamIndex].startXI[i].player.grid.split(":")[0]) {
                            playerData.add(it[0].lineups[teamIndex].startXI[i])
                        } else {
                            row = it[0].lineups[teamIndex].startXI[i].player.grid.split(":")[0]
                            startLineup.add(playerData)
                            playerData = mutableListOf()
                            playerData.add(it[0].lineups[teamIndex].startXI[i])

                        }
                    }
                    if (teamIndex == 0) {
                        val teamRating = it[0].players[teamIndex]
                        binding.hometeamLineupTextview.text = it[0].lineups[teamIndex].team.name
                        binding.hometeamFormationTextview.text = it[0].lineups[teamIndex].formation
                        binding.homeLineupRecyclerView.adapter = StartingLineupRowAdapter(startLineup, teamIndex, teamRating)
                        binding.homeLineupRecyclerView.layoutManager = LinearLayoutManager(context)
                        startLineup = mutableListOf()
                        playerData = mutableListOf()
                    } else {
                        val teamRating = it[0].players[teamIndex]
                        binding.awayteamLineupTextview.text = it[0].lineups[teamIndex].team.name
                        binding.awayteamFormationTextview.text = it[0].lineups[teamIndex].formation
                        binding.awayLineupRecyclerView.adapter = StartingLineupRowAdapter(startLineup, teamIndex, teamRating)
                        binding.awayLineupRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
                    }

                    // 감독 및 교체선수 명단 구성
                    playerData = mutableListOf()
                    for (i in it[0].lineups[teamIndex].substitutes.indices) {
                        playerData.add(it[0].lineups[teamIndex].substitutes[i])
                    }
                    if(teamIndex == 0) {
                        val teamRating = it[0].players[teamIndex]

                        binding.homeCoachName.text = it[0].lineups[teamIndex].coach.name
                        Picasso.get()
                            .load(it[0].lineups[teamIndex].coach.photo)
                            .into(binding.homeCoachImageview)
                        binding.homeCoachImageview.clipToOutline = true

                        binding.homeSubstitutesRecyclerview.adapter = PlayerLineupAdapter(requireContext(), playerData, teamRating, isSubsitute = true)
                        binding.homeSubstitutesRecyclerview.layoutManager = LinearLayoutManager(context)
                    } else {
                        val teamRating = it[0].players[teamIndex]

                        binding.awayCoachName.text = it[0].lineups[teamIndex].coach.name
                        Picasso.get()
                            .load(it[0].lineups[teamIndex].coach.photo)
                            .into(binding.awayCoachImageview)
                        binding.awayCoachImageview.clipToOutline = true

                        binding.awaySubstitutesRecyclerview.adapter = PlayerLineupAdapter(requireContext(), playerData, teamRating, isSubsitute = true)
                        binding.awaySubstitutesRecyclerview.layoutManager = LinearLayoutManager(context)
                    }

                }
            }
        )

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class StartingLineupRowAdapter(val startXI: List<List<PlayerData>>, var teamIndex: Int, var teamRating: PlayersByTeamData): RecyclerView.Adapter<StartingLineupRowAdapter.RowHolder>() {
        inner class RowHolder(val binding: LineupRowRecyclerItemBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
            val binding = LineupRowRecyclerItemBinding.inflate(layoutInflater, parent, false)
            return RowHolder(binding)
        }

        override fun onBindViewHolder(holder: RowHolder, position: Int) {
            holder.binding.rowRecyclerview.adapter = PlayerLineupAdapter(requireContext(), startXI[position], teamRating, isSubsitute = false)
            if(teamIndex == 0) { // 홈 팀일 경우
                holder.binding.rowRecyclerview.layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            } else {
                holder.binding.rowRecyclerview.layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, true)
            }

        }

        override fun getItemCount(): Int {
            return startXI.size
        }
    }



    companion object {
        @JvmStatic
        fun newInstance(fixtureID: Int): FixtureDetailLineupFragment {
            val args = Bundle().apply {
                putInt(ARG_FIXTURE_ID, fixtureID)
            }
            return FixtureDetailLineupFragment().apply {
                arguments = args
            }
        }
    }
}