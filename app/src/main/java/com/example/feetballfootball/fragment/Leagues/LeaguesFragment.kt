package com.example.feetballfootball.fragment.Leagues

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.feetballfootball.databinding.FragmentLeaguesBinding

private const val TAG = "LeaguesFragment"
private const val EPL = 39
private const val LALIGA = 140
private const val SERIEA = 135
private const val BUNDES = 78
private const val LIGUE1 = 61

class LeaguesFragment : Fragment() {

    private var _binding: FragmentLeaguesBinding? = null
    private val binding get() = _binding!!

    interface Callbacks {
        fun onLeagueSelected(leagueId: Int)
    }

    private var callbacks: Callbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaguesBinding.inflate(inflater, container, false)
        setOnClickListener()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    private fun setOnClickListener() {
        binding.eplTextview.setOnClickListener {
            callbacks?.onLeagueSelected(EPL)
        }
        binding.laligaTextview.setOnClickListener {
            callbacks?.onLeagueSelected(LALIGA)
        }
        binding.serieATextview.setOnClickListener {
            callbacks?.onLeagueSelected(SERIEA)
        }
        binding.bundesligaTextview.setOnClickListener {
            callbacks?.onLeagueSelected(BUNDES)
        }
        binding.ligue1Textview.setOnClickListener {
            callbacks?.onLeagueSelected(LIGUE1)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = LeaguesFragment()
    }
}