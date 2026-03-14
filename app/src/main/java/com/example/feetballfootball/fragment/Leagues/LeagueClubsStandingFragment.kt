package com.example.feetballfootball.fragment.Leagues

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.feetballfootball.R
import com.example.feetballfootball.api.leaguestanding.Standings
import com.example.feetballfootball.api.leaguestanding.Team
import com.example.feetballfootball.databinding.FragmentLeagueClubsStandingBinding
import com.example.feetballfootball.databinding.StandingItemBinding
import com.example.feetballfootball.viewModel.StandingViewModel
import com.squareup.picasso.Picasso

private const val ARG_LEAGUE_ID = "LEAGUE_ID"

class LeagueClubsStandingFragment : Fragment() {

    private var leagueID: Int = 0
    private lateinit var standingLiveData : LiveData<List<Standings>>

    private lateinit var standingViewModel: StandingViewModel

    private var _binding: FragmentLeagueClubsStandingBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        leagueID = arguments?.getInt(ARG_LEAGUE_ID) ?: 0
        standingViewModel = ViewModelProvider(requireActivity()).get(StandingViewModel::class.java)
        standingLiveData = standingViewModel.fetchStadingLiveData(leagueID!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeagueClubsStandingBinding.inflate(inflater, container, false)

        binding.leagueStandingRecyclerview.layoutManager = LinearLayoutManager(context)

        standingLiveData.observe(
            viewLifecycleOwner,
            Observer {
                updateUI(it)
            }
        )

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateUI(data: List<Standings>) {
        val adapter = LeagueStandingRecyclerViewAdapter(data)
        binding.leagueStandingRecyclerview.adapter = adapter
    }


    private inner class StandItemHolder(val binding: StandingItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindLogo(team: Team) {
            Picasso.get()
                .load(team.logo)
                .resize(100,100)
                .into(binding.teamLogo)
        }
    }

    private inner class LeagueStandingRecyclerViewAdapter(var data: List<Standings>) : RecyclerView.Adapter<StandItemHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StandItemHolder {
            val binding = StandingItemBinding.inflate(layoutInflater, parent, false)
            return StandItemHolder(binding)
        }

        override fun onBindViewHolder(holder: StandItemHolder, position: Int) {
            data.get(position).description?.let { description ->
                if(description.startsWith('P')){
                    if(description.contains("Champ")) {
                        holder.binding.posColorTextview.setBackgroundColor(ContextCompat.getColor(requireContext(),
                            R.color.teal_200
                        ))
                    } else if (description.contains("Europa")) {
                        holder.binding.posColorTextview.setBackgroundColor(ContextCompat.getColor(requireContext(),
                            R.color.indigo_500
                        ))
                    } else {
                        holder.binding.posColorTextview.setBackgroundColor(ContextCompat.getColor(requireContext(),
                            R.color.green_300
                        ))
                    }
                } else {
                    // 강등 시 색 지정
                    holder.binding.posColorTextview.setBackgroundColor(ContextCompat.getColor(requireContext(),
                        R.color.red_600
                    ))
                }
            }
            holder.binding.rankTextview.text =      data.get(position).rank.toString()
            holder.bindLogo(data.get(position).team)
            holder.binding.teamNameTextview.text =  data.get(position).team.name
            holder.binding.playedTextview.text =    data.get(position).all.played.toString()
            holder.binding.winTextview.text =       data.get(position).all.win.toString()
            holder.binding.drawTextview.text =      data.get(position).all.draw.toString()
            holder.binding.loseTextview.text =      data.get(position).all.lose.toString()
            holder.binding.goalDiffTextview.text =  data.get(position).goalsDiff.toString()
            holder.binding.pointsTextview.text =    data.get(position).points.toString()
        }

        override fun getItemCount(): Int {
            return data.size
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(leagueId: Int) : LeagueClubsStandingFragment {
            val args = Bundle().apply {
                putInt(ARG_LEAGUE_ID, leagueId)
            }
            return LeagueClubsStandingFragment().apply {
                arguments = args
            }
        }
    }
}