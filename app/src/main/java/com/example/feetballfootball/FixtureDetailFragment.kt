package com.example.feetballfootball

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.feetballfootball.api.fixturedetail.FixtureDetailResponse
import com.google.android.material.appbar.AppBarLayout
import com.squareup.picasso.Picasso

private const val TAG = "FixtureDetailFragment"
private const val ARG_FIXTURE_ID = "fixture_id"

class FixtureDetailFragment : Fragment() {
    private var fixtureID: Int = 0

    private lateinit var fixtureDetailViewModel: FixtureDetailViewModel
    private lateinit var fixtureDetailLiveData: LiveData<List<FixtureDetailResponse>>

    private lateinit var homeTeamTextView: TextView
    private lateinit var awayTeamTextView: TextView
    private lateinit var matchScoreTextView: TextView
    private lateinit var matchStatusTextView: TextView
    private lateinit var homeTeamScorerTextView: TextView
    private lateinit var awayTeamScorerTextView: TextView
    private lateinit var homeTeamImageView: ImageView
    private lateinit var awayTeamImageView: ImageView
    private lateinit var goalIcon: ImageView
    private lateinit var appBarLayout: AppBarLayout


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

        initView(view)

        fixtureDetailLiveData.observe(
            viewLifecycleOwner,
            Observer {


                // appbarUpdateUI() 함수로 들어가야 할 내용들
                // index 0: home, index 1: away
                val HomeAwayTeamIDList: MutableList<Int> = mutableListOf()

                var homeTeamScorer = mutableMapOf<String, MutableList<String>>()
                var awayTeamScorer = mutableMapOf<String, MutableList<String>>()
                Picasso.get()
                    .load(it.get(0).teams.home.logoUrl)
                    .resize(100, 100)
                    .into(homeTeamImageView)

                Picasso.get()
                    .load(it.get(0).teams.away.logoUrl)
                    .resize(100, 100)
                    .into(awayTeamImageView)

                HomeAwayTeamIDList.add(it.get(0).teams.home.id)
                HomeAwayTeamIDList.add(it.get(0).teams.away.id)

                homeTeamTextView.text = it.get(0).teams.home.name
                awayTeamTextView.text = it.get(0).teams.away.name
                matchStatusTextView.text =
                    if(it.get(0).fixture.status.short == "FT") { "종료됨" }
                    else {
                    matchStatusTextView.setTextSize(Dimension.SP, 20f)
                    "${it.get(0).fixture.status.elapsed}'"
                }
                matchScoreTextView.text = (it.get(0).goals.home.toString() + " - " + it.get(0).goals.away.toString())

                // 득점 기록 처리
                it.get(0).events?.let {
                    it.forEach{
                        if (it.type == "Goal") {
                            goalIcon.visibility = View.VISIBLE
                            if (it.team.id == HomeAwayTeamIDList[0]) {
                                homeTeamScorer = WhoScoredByTeam(it.player.name, homeTeamScorer, it.time.elapsed)
                            } else if(it.team.id == HomeAwayTeamIDList[1]) {
                                awayTeamScorer = WhoScoredByTeam(it.player.name, awayTeamScorer, it.time.elapsed)
                            }
                        }
                    }
                }

                homeTeamScorerTextView.text = WriteWhoScoredOnTextView(homeTeamScorer)
                awayTeamScorerTextView.text = WriteWhoScoredOnTextView(awayTeamScorer)
            }
        )
        return view
    }


    fun initView(view: View) {
        homeTeamTextView = view.findViewById(R.id.hometeam_textview)
        awayTeamTextView = view.findViewById(R.id.awayteam_textview)
        matchScoreTextView = view.findViewById(R.id.score_textview)
        matchStatusTextView = view.findViewById(R.id.matchElapsed)
        homeTeamScorerTextView = view.findViewById(R.id.hometeam_scorer_textview)
        awayTeamScorerTextView = view.findViewById(R.id.awayteam_scorer_textview)

        homeTeamImageView = view.findViewById(R.id.hometeam_imageview)
        awayTeamImageView = view.findViewById(R.id.awayteam_imageview)
        goalIcon = view.findViewById(R.id.goal_icon)
        appBarLayout = view.findViewById(R.id.appBarLayout)
    }

    fun WhoScoredByTeam(
        playerName: String,
        whoTeamScorer: MutableMap<String, MutableList<String>>,
        elapsed: Int
    ): MutableMap<String, MutableList<String>> {
        if (playerName !in whoTeamScorer.keys) { // 키가 존재하지 않을 때는 키를 추가
            val goalscored = mutableListOf<String>()
            goalscored.add(elapsed.toString())
            whoTeamScorer[playerName] = goalscored
        } else { // 키가 존재할 경우, 기존의 키가 가지고 있는 value값에 추가로 add한다.
            val goalscored = whoTeamScorer[playerName]!!
            goalscored.add(elapsed.toString())
            whoTeamScorer[playerName] = goalscored
        }
        return whoTeamScorer
    }

    fun WriteWhoScoredOnTextView(whoTeamScorer: MutableMap<String, MutableList<String>>) : String {
        var scorer = ""
        for (key in whoTeamScorer.keys) {
            scorer += "${key} "
            for (values in whoTeamScorer.getValue(key)) {
                scorer += "${values}' "
            }
            scorer += "\n"
        }
        return scorer
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