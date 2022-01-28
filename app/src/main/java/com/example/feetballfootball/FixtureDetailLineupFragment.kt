package com.example.feetballfootball

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.feetballfootball.api.fixturedetail.FixtureDetailResponse
import com.example.feetballfootball.api.fixturedetail.PlayerData
import com.example.feetballfootball.api.fixturedetail.PlayersByTeamData
import com.squareup.picasso.Picasso

private const val ARG_FIXTURE_ID = "fixture_id"

class FixtureDetailLineupFragment : Fragment() {
    private var fixtureID: Int = 0
    private lateinit var fixtureDetailViewModel: FixtureDetailViewModel
    private lateinit var fixtureDetailLiveData: LiveData<List<FixtureDetailResponse>>

    private lateinit var lineupHomeTeamName: TextView
    private lateinit var lineupAwayTeamName: TextView
    private lateinit var homeTeamFormation: TextView
    private lateinit var awayTeamFormation: TextView
    private lateinit var homeCoachName: TextView
    private lateinit var awayCoachName: TextView

    private lateinit var homeCoachImage: ImageView
    private lateinit var awayCoachImage: ImageView

    private lateinit var homeLineupRecyclerView: RecyclerView
    private lateinit var awayLineupRecyclerView: RecyclerView
    private lateinit var homeSubsLineupRecyclerView: RecyclerView
    private lateinit var awaySubsLineupRecyclerView: RecyclerView

    private var homeGoalPost: MutableList<LinearLayout> = mutableListOf()
    private  var awayGoalPost: MutableList<LinearLayout> = mutableListOf()
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
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fixture_detail_lineup, container, false)
        initView(view)

        fixtureDetailLiveData.observe(
            viewLifecycleOwner,
            Observer {
//                it[0].lineups[0].startXI[0]
//                val lineupData = it[0].lineups[0].startXI[0].player.grid
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
                        lineupHomeTeamName.text = it[0].lineups[teamIndex].team.name
                        homeTeamFormation.text = it[0].lineups[teamIndex].formation
                        homeLineupRecyclerView.adapter = StartingLineupRowAdapter(startLineup, teamIndex, teamRating)
                        homeLineupRecyclerView.layoutManager = LinearLayoutManager(context)
                        startLineup = mutableListOf()
                        playerData = mutableListOf()
                    } else {
                        val teamRating = it[0].players[teamIndex]
                        lineupAwayTeamName.text = it[0].lineups[teamIndex].team.name
                        awayTeamFormation.text = it[0].lineups[teamIndex].formation
                        awayLineupRecyclerView.adapter = StartingLineupRowAdapter(startLineup, teamIndex, teamRating)
                        awayLineupRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
                    }

                    // 감독 및 교체선수 명단 구성
                    playerData = mutableListOf()
                    for (i in it[0].lineups[teamIndex].substitutes.indices) {
                        playerData.add(it[0].lineups[teamIndex].substitutes[i])
                    }
                    if(teamIndex == 0) {
                        val teamRating = it[0].players[teamIndex]

                        homeCoachName.text = it[0].lineups[teamIndex].coach.name
                        Picasso.get()
                            .load(it[0].lineups[teamIndex].coach.photo)
                            .into(homeCoachImage)

                        homeSubsLineupRecyclerView.adapter = PlayerLineupAdapter(requireContext(), playerData, teamRating, isSubsitute = true)
                        homeSubsLineupRecyclerView.layoutManager = LinearLayoutManager(context)
                    } else {
                        val teamRating = it[0].players[teamIndex]

                        awayCoachName.text = it[0].lineups[teamIndex].coach.name
                        Picasso.get()
                            .load(it[0].lineups[teamIndex].coach.photo)
                            .into(awayCoachImage)

                        awaySubsLineupRecyclerView.adapter = PlayerLineupAdapter(requireContext(), playerData, teamRating, isSubsitute = true)
                        awaySubsLineupRecyclerView.layoutManager = LinearLayoutManager(context)
                    }

                }
            }
        )

        return view
    }

    fun initView(view: View) {
        lineupHomeTeamName = view.findViewById(R.id.hometeam_lineup_textview)
        lineupAwayTeamName = view.findViewById(R.id.awayteam_lineup_textview)
        homeTeamFormation = view.findViewById(R.id.hometeam_formation_textview)
        awayTeamFormation = view.findViewById(R.id.awayteam_formation_textview)
        homeCoachName = view.findViewById(R.id.home_coach_name)
        awayCoachName = view.findViewById(R.id.away_coach_name)

        homeCoachImage = view.findViewById(R.id.home_coach_imageview)
        awayCoachImage = view.findViewById(R.id.away_coach_imageview)

        homeLineupRecyclerView = view.findViewById(R.id.home_lineup_recyclerView)
        awayLineupRecyclerView = view.findViewById(R.id.away_lineup_recyclerView)
        homeSubsLineupRecyclerView = view.findViewById(R.id.home_substitutes_recyclerview)
        awaySubsLineupRecyclerView = view.findViewById(R.id.away_substitutes_recyclerview)
    }

    private inner class StartingLineupRowAdapter(val startXI: List<List<PlayerData>>, var teamIndex: Int, var teamRating: PlayersByTeamData): RecyclerView.Adapter<StartingLineupRowAdapter.RowHolder>() {
        inner class RowHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val rowRecylerView: RecyclerView
            init {
                rowRecylerView = itemView.findViewById(R.id.row_recyclerview)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
            val view = layoutInflater.inflate(R.layout.lineup_row_recycler_item, parent, false)
            return RowHolder(view)
        }

        override fun onBindViewHolder(holder: RowHolder, position: Int) {
            holder.rowRecylerView.adapter = PlayerLineupAdapter(requireContext(), startXI[position], teamRating, isSubsitute = false)
            if(teamIndex == 0) { // 홈 팀일 경우
                holder.rowRecylerView.layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            } else {
                holder.rowRecylerView.layoutManager =
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