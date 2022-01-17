package com.example.feetballfootball

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.feetballfootball.api.fixturedetail.FixtureDetailResponse
import com.squareup.picasso.Picasso

private const val TAG = "FixtureDetailFragment"
private const val ARG_FIXTURE_ID = "fixture_id"

class FixtureDetailFragment : Fragment() {
    private var fixtureID: Int = 0

    private lateinit var fixtureDetailViewModel: FixtureDetailViewModel
    private lateinit var fixtureDetailLiveData: LiveData<List<FixtureDetailResponse>>

    private lateinit var homeTeamImageView: ImageView
    private lateinit var awayTeamImageView: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fixtureID = arguments?.getInt(ARG_FIXTURE_ID) ?: 0
        fixtureDetailViewModel = ViewModelProvider(this).get(FixtureDetailViewModel::class.java)
        fixtureDetailLiveData = fixtureDetailViewModel.fetchFixtureDetailLiveData(fixtureID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fixture_detail, container, false)

        val textView = view.findViewById<TextView>(R.id.IDtest_textview)
        textView.text = "수신한 경기 일정 ID: " + fixtureID.toString()
        homeTeamImageView = view.findViewById(R.id.homeTeam)
        awayTeamImageView = view.findViewById(R.id.awayTeam)

        fixtureDetailLiveData.observe(
            viewLifecycleOwner,
            Observer {
                Picasso.get()
                    .load(it.get(0).teams.home.logoUrl)
                    .resize(100, 100)
                    .into(homeTeamImageView)

                Picasso.get()
                    .load(it.get(0).teams.away.logoUrl)
                    .resize(100, 100)
                    .into(awayTeamImageView)
            }
        )

        return view
    }


    companion object {
        @JvmStatic
        fun newInstance(fixtureID: Int): FixtureDetailFragment {
            val args = Bundle().apply {
                putInt(ARG_FIXTURE_ID, fixtureID)
            }
            return FixtureDetailFragment().apply {
                arguments = args
            }
        }
    }
}