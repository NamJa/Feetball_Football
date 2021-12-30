package com.example.feetballfootball

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

private const val TAG = "LeaguesFragment"
private const val EPL = 39
private const val LALIGA = 140
private const val SERIEA = 135
private const val BUNDES = 78
private const val LIGUE1 = 61

class LeaguesFragment : Fragment() {

    private lateinit var eplTextView : TextView
    private lateinit var laligaTextView : TextView
    private lateinit var serieaTextView : TextView
    private lateinit var bundesligaTextView : TextView
    private lateinit var ligue1TextView : TextView

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
    ): View? {
        val view = inflater.inflate(R.layout.fragment_leagues, container, false)
        setOnClickListener(view)
        return view
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    fun setOnClickListener(view: View) {
        eplTextView = view.findViewById(R.id.epl_textview)
        laligaTextView = view.findViewById(R.id.laliga_textview)
        serieaTextView = view.findViewById(R.id.serie_a_textview)
        bundesligaTextView = view.findViewById(R.id.bundesliga_textview)
        ligue1TextView = view.findViewById(R.id.ligue1_textview)

        eplTextView.setOnClickListener {
            callbacks?.onLeagueSelected(EPL)
        }
        laligaTextView.setOnClickListener {
            callbacks?.onLeagueSelected(LALIGA)
        }
        serieaTextView.setOnClickListener {
            callbacks?.onLeagueSelected(SERIEA)
        }
        bundesligaTextView.setOnClickListener {
            callbacks?.onLeagueSelected(BUNDES)
        }
        ligue1TextView.setOnClickListener {
            callbacks?.onLeagueSelected(LIGUE1)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = LeaguesFragment()
    }
}