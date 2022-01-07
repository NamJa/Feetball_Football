package com.example.feetballfootball

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.feetballfootball.api.leaguestanding.StandingResponse
import com.example.feetballfootball.api.leaguestanding.Standings
import com.example.feetballfootball.api.leaguestanding.Team
import com.squareup.picasso.Picasso
import org.w3c.dom.Text

private const val TAG = "LeagueStandingFragment"
private const val ARG_LEAGUE_ID = "league_id"

class LeagueStandingFragment : Fragment() {
    private var leagueId: Int? = 0
    private var leagueCodeMap = mapOf(39 to "Premier League", 140 to "LA LIGA", 135 to "SERIE A", 78 to "BUNDESLIGA", 61 to "LIGUE 1")
    private lateinit var standingLiveData : LiveData<List<Standings>>

    private lateinit var standingViewModel: StandingViewModel
    private lateinit var leagueTitle : TextView
    private lateinit var leagueStandingRecyclerview: RecyclerView

    private var footballDataFetchr = FootballDataFetchr()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        leagueId = arguments?.getInt(ARG_LEAGUE_ID)
        standingViewModel = ViewModelProvider(this).get(StandingViewModel::class.java)
        standingLiveData = standingViewModel.fetchStadingLiveData(leagueId!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_league_standing, container, false)

        leagueTitle = view.findViewById(R.id.league_title_textview)
        leagueStandingRecyclerview = view.findViewById(R.id.league_standing_recyclerview)

        leagueStandingRecyclerview.layoutManager = LinearLayoutManager(context)
        leagueTitle.text = leagueCodeMap[leagueId]


        standingLiveData.observe(
            viewLifecycleOwner,
            Observer {
                updateUI(it)
            }
        )

        return view
    }

    private fun updateUI(data: List<Standings>) {
        val adapter = LeagueStandingRecyclerViewAdapter(data)
        leagueStandingRecyclerview.adapter = adapter
    }

    private inner class StandItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val posColor: TextView
        val rank: TextView
        val teamLogo: ImageView
        val teamName: TextView
        val played: TextView
        val win: TextView
        val draw: TextView
        val lose: TextView
        val goalDiff: TextView
        val points: TextView

        init {
            posColor = itemView.findViewById(R.id.pos_color_textview)
            rank = itemView.findViewById(R.id.rank_textview)
            teamLogo = itemView.findViewById(R.id.team_logo)
            teamName = itemView.findViewById(R.id.team_name_textview)
            played = itemView.findViewById(R.id.played_textview)
            win = itemView.findViewById(R.id.win_textview)
            draw = itemView.findViewById(R.id.draw_textview)
            lose = itemView.findViewById(R.id.lose_textview)
            goalDiff = itemView.findViewById(R.id.goalDiff_textview)
            points = itemView.findViewById(R.id.points_textview)
        }

        fun bindLogo(team: Team) {
            Picasso.get()
                .load(team.logo)
                .resize(100,100)
                .into(teamLogo)
        }
    }

    private inner class LeagueStandingRecyclerViewAdapter(var data: List<Standings>) : RecyclerView.Adapter<StandItemHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StandItemHolder {
            val view = layoutInflater.inflate(R.layout.standing_item, parent, false)
            return StandItemHolder(view)
        }

        override fun onBindViewHolder(holder: StandItemHolder, position: Int) {
            data.get(position).description?.let { description ->
                if(description.startsWith('P')){
                    if(description.contains("Champ")) {
                        holder.posColor.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.teal_200))
                    } else if (description.contains("Europa")) {
                        holder.posColor.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.indigo_500))
                    } else {
                        holder.posColor.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_300))
                    }
                } else {
                    // 강등 시 색 지정
                    holder.posColor.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red_600))
                }
            }
            holder.rank.text =      data.get(position).rank.toString()
            holder.bindLogo(data.get(position).team)
            holder.teamName.text =  data.get(position).team.name
            holder.played.text =    data.get(position).all.played.toString()
            holder.win.text =       data.get(position).all.win.toString()
            holder.draw.text =      data.get(position).all.draw.toString()
            holder.lose.text =      data.get(position).all.lose.toString()
            holder.goalDiff.text =  data.get(position).goalsDiff.toString()
            holder.points.text =    data.get(position).points.toString()
        }

        override fun getItemCount(): Int {
            return data.size
        }
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