package com.example.feetballfootball

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.feetballfootball.api.fixturedetail.FixtureDetailResponse
import com.google.android.material.appbar.AppBarLayout
import com.squareup.picasso.Picasso
import org.w3c.dom.Text

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
    private lateinit var homeShotAccuracy: TextView
    private lateinit var awayShotAccuracy: TextView
    private lateinit var homeShotsAccurByTotal: TextView
    private lateinit var awayShotsAccurByTotal: TextView
    private lateinit var homePass: TextView
    private lateinit var awayPass: TextView
    private lateinit var homeBlockedShot: TextView
    private lateinit var awayBlockedShot: TextView
    private lateinit var homeOffsideTextView: TextView
    private lateinit var awayOffsideTextView: TextView

    private lateinit var ballPossessionProgressBar: ProgressBar
    private lateinit var totalshootingProgressBar: ProgressBar
    private lateinit var cornerkickProgressBar: ProgressBar
    private lateinit var blockedShotProgressBar: ProgressBar
    private lateinit var offsideProgressBar: ProgressBar

    private var homeGoalPost: MutableList<LinearLayout> = mutableListOf()
    private  var awayGoalPost: MutableList<LinearLayout> = mutableListOf()




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


                /** appbarUpdateUI() 함수로 들어가야 할 내용들 **/
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
                            val extraTime = it.time.extra.toString() ?: ""
                            if (it.team.id == HomeAwayTeamIDList[0]) {
                                homeTeamScorer = WhoScoredByTeam(it.player.name, homeTeamScorer, it.time.elapsed, extraTime)
                            } else if(it.team.id == HomeAwayTeamIDList[1]) {
                                awayTeamScorer = WhoScoredByTeam(it.player.name, awayTeamScorer, it.time.elapsed, extraTime)
                            }
                        }
                    }
                }

                homeTeamScorerTextView.text = WriteWhoScoredOnTextView(homeTeamScorer)
                awayTeamScorerTextView.text = WriteWhoScoredOnTextView(awayTeamScorer)
                /** APP BAR LAYOUT -- END -- **/

                /** statisticsUI() 함수로 들어가야 할 내용들 **/
                /** STATISTICS -- START -- **/
                val homeStatistics = it[0].statistics[0].statistics
                val awayStatistics = it[0].statistics[1].statistics
                var homeTotalPasses = ""
                var awayTotalPasses = ""
                var homePassesAccurate = ""
                var awayPassesAccurate = ""
                // 흰색을 팀 컬러로 가져오는 경우, ui가 제대로 보이지 않기 때문에 검은색으로 설정
                val hometeamColor = if("#"+it[0].lineups[0].team.colors.teamColorCode.colorCode == "#ffffff") {
                                        "#000000"
                                    } else {
                                        "#"+it[0].lineups[0].team.colors.teamColorCode.colorCode
                                    }
                val awayteamColor = if("#"+it[0].lineups[1].team.colors.teamColorCode.colorCode == "#ffffff") {
                                        "#000000"
                                    } else {
                                        "#"+it[0].lineups[1].team.colors.teamColorCode.colorCode
                                    }
                var homeShotOnNum = ""
                var awayShotOnNum = ""
                var homeShotTotal = ""
                var awayShotTotal = ""
                Log.d(TAG, fixtureID.toString())
                for (i in 0 until homeStatistics.size) {
                    val homeTypeValue = if (homeStatistics[i].value == "null") { 0 } else { homeStatistics[i].value }
                    val awayTypeValue = if (awayStatistics[i].value == "null") { 0 } else { awayStatistics[i].value }

                    when(homeStatistics[i].type) {
                        "Shots on Goal" -> {
                            homeShotOn.text = homeTypeValue.toString()
                            awayShotOn.text = awayTypeValue.toString()
                            homeShotOnNum = homeTypeValue.toString()
                            awayShotOnNum = awayTypeValue.toString()
                        }
                        "Shots off Goal" -> {
                            homeShotOff.text = homeTypeValue.toString()
                            awayShotOff.text = awayTypeValue.toString()
                        }
                        "Total Shots" -> {
                            val homeProgress = homeTypeValue.toString().toInt()
                            val awayProgress = awayTypeValue.toString().toInt()
                            val maxProgress = homeProgress + awayTypeValue.toString().toInt()
                            homeTotalShooting.text = homeTypeValue.toString()
                            awayTotalShooting.text = awayTypeValue.toString()
                            setChangeProgressbarColor(totalshootingProgressBar, hometeamColor, awayteamColor, homeProgress, maxProgress)
                            homeShotTotal = homeProgress.toString()
                            awayShotTotal = awayProgress.toString()
                        }
                        "Blocked Shots" -> {
                            val homeBlocked = homeTypeValue.toString().toInt()
                            val awayBlocked = awayTypeValue.toString().toInt()
                            homeBlockedShot.text = homeTypeValue.toString()
                            awayBlockedShot.text = awayTypeValue.toString()
                            setChangeProgressbarColor(blockedShotProgressBar, hometeamColor, awayteamColor, homeBlocked, homeBlocked+awayBlocked)
                        }
//                        "Fouls" -> ""
                        "Corner Kicks" -> {
                            val homeProgress = homeTypeValue.toString().toInt()
                            val maxProgress = homeProgress + awayTypeValue.toString().toInt()
                            homeCornerKicks.text = homeTypeValue.toString()
                            awayCornerKicks.text = awayTypeValue.toString()
                            setChangeProgressbarColor(cornerkickProgressBar, hometeamColor, awayteamColor, homeProgress, maxProgress)
                        }
                        "Offsides" -> {
                            val homeOffside = homeTypeValue.toString().toInt()
                            val awayOffside = awayTypeValue.toString().toInt()
                            homeOffsideTextView.text = homeTypeValue.toString()
                            awayOffsideTextView.text = awayTypeValue.toString()
                            setChangeProgressbarColor(offsideProgressBar, hometeamColor, awayteamColor, homeOffside, homeOffside+awayOffside)
                        }
                        "Ball Possession" -> {
                            homeBallPossession.text = homeTypeValue.toString()
                            awayBallPossession.text = awayTypeValue.toString()
                            // background를 home으로 놓고 progress를 away의 수치로 설정하면 좌,우 배치에 맞게된다.
                            val awayProgress = awayTypeValue.toString().split("%")[0].toInt()
                            setChangeProgressbarColor(ballPossessionProgressBar, hometeamColor, awayteamColor, awayProgress, maxProgress = 100)
                        }
//                        "Yellow Cards" -> ""
//                        "Red Cards" -> ""
                        "Passes %" -> {
                            homeAccuracy.text = homeTypeValue.toString()
                            awayAccuracy.text = awayTypeValue.toString()
                        }
                        "Total passes" -> {
                            homeTotalPasses = homeTypeValue.toString()
                            awayTotalPasses = awayTypeValue.toString()
                        }
                        "Passes accurate" -> {
                            homePassesAccurate = homeTypeValue.toString()
                            awayPassesAccurate = awayTypeValue.toString()
                        }
                    }
                }
                homePass.text = getString(R.string.accurpass_by_total, homePassesAccurate, homeTotalPasses)
                awayPass.text = getString(R.string.accurpass_by_total, awayPassesAccurate, awayTotalPasses)
                val homeShotAccurPercent = ((homeShotOnNum.toDouble() / homeShotTotal.toDouble())*100).toInt().toString()
                val awayShotAccurPercent = ((awayShotOnNum.toDouble() / awayShotTotal.toDouble())*100).toInt().toString()
//                Log.d("homeShotAccurPercent", homeShotAccurPercent)
                var tempText = getString(R.string.accurshot_percent)
                homeShotAccuracy.text = homeShotAccurPercent + "%"
                awayShotAccuracy.text = awayShotAccurPercent + "%"
                homeShotsAccurByTotal.text = getString(R.string.accurshot_by_total, homeShotOnNum, homeShotTotal)
                awayShotsAccurByTotal.text = getString(R.string.accurshot_by_total, awayShotOnNum, awayShotTotal)
                setGoalPostColor(homeGoalPost, hometeamColor)
                setGoalPostColor(awayGoalPost, awayteamColor)

                /** STATISTICS -- END -- **/

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
        homeShotAccuracy = view.findViewById(R.id.home_shot_accuracy)
        awayShotAccuracy = view.findViewById(R.id.away_shot_accuracy)
        homeShotsAccurByTotal = view.findViewById(R.id.home_shots_accur_by_total)
        awayShotsAccurByTotal = view.findViewById(R.id.away_shots_accur_by_total)
        homeBlockedShot = view.findViewById(R.id.home_blocked_shots)
        awayBlockedShot = view.findViewById(R.id.away_blocked_shots)
        homeOffsideTextView = view.findViewById(R.id.home_offside)
        awayOffsideTextView = view.findViewById(R.id.away_offside)

        ballPossessionProgressBar = view.findViewById(R.id.ball_possession_progressbar)
        totalshootingProgressBar = view.findViewById(R.id.total_shooting_progressbar)
        cornerkickProgressBar = view.findViewById(R.id.cornerkicks_progressbar)
        blockedShotProgressBar = view.findViewById(R.id.blocked_shots_progressbar)
        offsideProgressBar = view.findViewById(R.id.offside_progressbar)

        homeGoalPost.add(view.findViewById(R.id.home_goalpost_shotoff_1))
        homeGoalPost.add(view.findViewById(R.id.home_goalpost_shotoff_2))
        homeGoalPost.add(view.findViewById(R.id.home_goalpost_shoton))
        awayGoalPost.add(view.findViewById(R.id.away_goalpost_shotoff_1))
        awayGoalPost.add(view.findViewById(R.id.away_goalpost_shotoff_2))
        awayGoalPost.add(view.findViewById(R.id.away_goalpost_shoton))
    }

    fun WhoScoredByTeam(
        playerName: String,
        whoTeamScorer: MutableMap<String, MutableList<String>>,
        elapsed: Int,
        extra: String
    ): MutableMap<String, MutableList<String>> {
        if (playerName !in whoTeamScorer.keys) { // 키가 존재하지 않을 때는 키를 추가
            val goalscored = mutableListOf<String>()
            if (extra != "null") {
                goalscored.add(elapsed.toString()+ "+" + extra)
            } else { goalscored.add(elapsed.toString()) }
            whoTeamScorer[playerName] = goalscored
        } else { // 키가 존재할 경우, 기존의 키가 가지고 있는 value값에 추가로 add한다.
            val goalscored = whoTeamScorer[playerName]!!
            if (extra != "null") {
                goalscored.add(elapsed.toString()+ "+" + extra)
            } else { goalscored.add(elapsed.toString()) }
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

    fun setChangeProgressbarColor(progressBar: ProgressBar, homeTeamColor: String, awayTeamColor: String, progress: Int, maxProgress: Int) {
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
        progressBar.max = maxProgress
        progressBar.progress = progress
    }

    fun setGoalPostColor(linear: List<LinearLayout>, teamColor: String) {
        linear.forEach { goalpost ->
            goalpost.setBackgroundColor(Color.parseColor(teamColor))
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