package com.example.feetballfootball

import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.feetballfootball.api.fixturedetail.FixtureDetailResponse
import com.google.android.material.appbar.AppBarLayout
import com.squareup.picasso.Picasso
import org.apache.http.conn.scheme.LayeredSocketFactory

private const val TAG = "FixtureDetailFragment"
private const val ARG_FIXTURE_ID = "fixture_id"

class FixtureDetailFragment : Fragment() {
    private var fixtureID: Int = 0

    private lateinit var fixtureDetailViewModel: FixtureDetailViewModel
    private lateinit var fixtureDetailLiveData: LiveData<List<FixtureDetailResponse>>
    /*************** Appbar Layout *******************/
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
    /*************** Statistics *******************/
    private lateinit var homeBallPossession: TextView
    private lateinit var awayBallPossession: TextView
    private lateinit var homeTotalShooting: TextView
    private lateinit var awayTotalShooting: TextView
    private lateinit var homeCornerKicks: TextView
    private lateinit var awayCornerKicks: TextView
    private lateinit var homeShotOff: TextView
    private lateinit var awayShotOff: TextView
    private lateinit var homeShotOn: TextView
    private lateinit var awayShotOn: TextView
    private lateinit var homeAccuracy: TextView
    private lateinit var awayAccuracy: TextView
    private lateinit var homePass: TextView
    private lateinit var awayPass: TextView

    private lateinit var ballPossessionProgressBar: ProgressBar
    private lateinit var totalshootingProgressBar: ProgressBar
    private lateinit var cornerkickProgressBar: ProgressBar




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
                /** APP BAR LAYOUT -- START -- **/
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
                /** APP BAR LAYOUT -- END -- **/

                /** STATISTICS -- START -- **/



            }
        )
        return view
    }


    fun initView(view: View) {
        /*************** Appbar Layout *******************/
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
        /*************** Statistics *******************/
        homeBallPossession = view.findViewById(R.id.home_ball_possession)
        awayBallPossession = view.findViewById(R.id.away_ball_possession)
        homeTotalShooting = view.findViewById(R.id.home_total_shooting)
        awayTotalShooting = view.findViewById(R.id.away_total_shooting)
        homeCornerKicks = view.findViewById(R.id.home_cornerkicks)
        awayCornerKicks = view.findViewById(R.id.away_cornerkicks)
        homeShotOff = view.findViewById(R.id.home_shotoff)
        awayShotOff = view.findViewById(R.id.away_shotoff)
        homeShotOn = view.findViewById(R.id.home_shoton)
        awayShotOn = view.findViewById(R.id.away_shoton)
        homeAccuracy = view.findViewById(R.id.home_accuracy)
        awayAccuracy = view.findViewById(R.id.away_accuracy)
        homePass = view.findViewById(R.id.home_pass_accur_by_total)
        awayPass = view.findViewById(R.id.away_pass_accur_by_total)
        ballPossessionProgressBar = view.findViewById(R.id.ball_possession_progressbar)
        totalshootingProgressBar = view.findViewById(R.id.total_shooting_progressbar)
        cornerkickProgressBar = view.findViewById(R.id.cornerkicks_progressbar)

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

    fun setChangeProgressbarColor(progressBar: ProgressBar, homeTeamColor: String, awayTeamColor: String) {
        val progressBarDrawable = progressBar.progressDrawable as LayerDrawable
        val backgroundDrawable = progressBarDrawable.getDrawable(0) // 배경 progressbar
        val progressDrawable = progressBarDrawable.getDrawable(1)   // 진행된 progressbar

        if (progressBar.id != R.id.ball_possession_progressbar) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29+
                backgroundDrawable.colorFilter = BlendModeColorFilter(Color.parseColor(awayTeamColor), BlendMode.SRC_ATOP)
            } else {
                backgroundDrawable.setColorFilter(Color.parseColor(awayTeamColor), PorterDuff.Mode.SRC_IN)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                progressDrawable.colorFilter = BlendModeColorFilter(Color.parseColor(homeTeamColor), BlendMode.SRC_ATOP)
            } else {
                progressDrawable.setColorFilter(Color.parseColor(homeTeamColor), PorterDuff.Mode.SRC_IN)
            }
        } else  { // 볼 점유율 Progressbar 작업
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29+
                backgroundDrawable.colorFilter = BlendModeColorFilter(Color.parseColor(homeTeamColor), BlendMode.SRC_ATOP)
            } else {
                backgroundDrawable.setColorFilter(Color.parseColor(homeTeamColor), PorterDuff.Mode.SRC_IN)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                progressDrawable.colorFilter = BlendModeColorFilter(Color.parseColor(awayTeamColor), BlendMode.SRC_ATOP)
            } else {
                progressBarDrawable.setColorFilter(Color.parseColor(awayTeamColor), PorterDuff.Mode.SRC_IN)
            }
        }
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