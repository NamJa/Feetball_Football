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
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.feetballfootball.api.fixturedetail.FixtureDetailResponse
import com.example.feetballfootball.view_model.FixtureDetailViewModel

private const val ARG_FIXTURE_ID = "fixture_id"
private const val TAG = "FixtureDetailStatisticsFragment"

class FixtureDetailStatisticsFragment : Fragment() {

    private var fixtureID: Int = 0
    private lateinit var fixtureDetailViewModel: FixtureDetailViewModel
    private lateinit var fixtureDetailLiveData: LiveData<List<FixtureDetailResponse>>

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
    private lateinit var homeOffside: TextView
    private lateinit var awayOffside: TextView
    private lateinit var homeFoul: TextView
    private lateinit var awayFoul: TextView
    private lateinit var homeYellowcard: TextView
    private lateinit var awayYellowcard: TextView
    private lateinit var homeRedcard: TextView
    private lateinit var awayRedcard: TextView

    private lateinit var ballPossessionProgressBar: ProgressBar
    private lateinit var totalshootingProgressBar: ProgressBar
    private lateinit var cornerkickProgressBar: ProgressBar
    private lateinit var blockedShotProgressBar: ProgressBar
    private lateinit var offsideProgressBar: ProgressBar
    private lateinit var foulProgressBar: ProgressBar
    private lateinit var yellowcardProgressBar: ProgressBar
    private lateinit var redcardProgressBar: ProgressBar

    private var homeGoalPost: MutableList<LinearLayout> = mutableListOf()
    private  var awayGoalPost: MutableList<LinearLayout> = mutableListOf()

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
        val view = inflater.inflate(R.layout.fragment_fixture_detail_statistics, container, false)
        initView(view)

        fixtureDetailLiveData.observe(
            viewLifecycleOwner,
            Observer {
                /** statisticsUI() 함수로 들어가야 할 내용들 **/
                val homeStatistics = it[0].statistics[0].statistics
                val awayStatistics = it[0].statistics[1].statistics
                var homeTotalPasses = ""
                var awayTotalPasses = ""
                var homePassesAccurate = ""
                var awayPassesAccurate = ""
                // 흰색을 팀 컬러로 가져오는 경우, ui가 제대로 보이지 않기 때문에 검은색으로 설정
                var hometeamColor = if("#"+it[0].lineups[0].team.colors.teamColorCode.colorCode == "#ffffff") {
                    "#000000"
                } else {
                    "#"+it[0].lineups[0].team.colors.teamColorCode.colorCode
                }
                var awayteamColor = if("#"+it[0].lineups[1].team.colors.teamColorCode.colorCode == "#ffffff") {
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
                    val homeTypeValue = homeStatistics[i].value ?: 0
                    val awayTypeValue = awayStatistics[i].value ?: 0

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
                        "Corner Kicks" -> {
                            val homeProgress = homeTypeValue.toString().toInt()
                            val maxProgress = homeProgress + awayTypeValue.toString().toInt()
                            homeCornerKicks.text = homeTypeValue.toString()
                            awayCornerKicks.text = awayTypeValue.toString()
                            setChangeProgressbarColor(cornerkickProgressBar, hometeamColor, awayteamColor, homeProgress, maxProgress)
                        }
                        "Offsides" -> {
                            val offsideHome = homeTypeValue.toString().toInt()
                            val offsideAway = awayTypeValue.toString().toInt()
                            homeOffside.text = homeTypeValue.toString()
                            awayOffside.text = awayTypeValue.toString()
                            setChangeProgressbarColor(offsideProgressBar, hometeamColor, awayteamColor, offsideHome, offsideHome+offsideAway)
                        }
                        "Ball Possession" -> {
                            homeBallPossession.text = homeTypeValue.toString()
                            awayBallPossession.text = awayTypeValue.toString()
                            // background를 home으로 놓고 progress를 away의 수치로 설정하면 좌,우 배치에 맞게된다.
                            val awayProgress = awayTypeValue.toString().split("%")[0].toInt()
                            setChangeProgressbarColor(ballPossessionProgressBar, hometeamColor, awayteamColor, awayProgress, maxProgress = 100)
                        }
                        "Fouls" -> {
                            val foulHome = homeTypeValue.toString().toInt()
                            val foulAway = awayTypeValue.toString().toInt()
                            homeFoul.text = homeTypeValue.toString()
                            awayFoul.text = awayTypeValue.toString()
                            setChangeProgressbarColor(foulProgressBar, hometeamColor, awayteamColor, foulHome, foulHome+foulAway)
                        }
                        "Yellow Cards" -> {
                            val yellowHome = homeTypeValue.toString().toInt()
                            val yellowAway = awayTypeValue.toString().toInt()
                            homeYellowcard.text = homeTypeValue.toString()
                            awayYellowcard.text = awayTypeValue.toString()
                            setChangeProgressbarColor(yellowcardProgressBar, hometeamColor, awayteamColor, yellowHome, yellowHome+yellowAway)
                        }
                        "Red Cards" -> {
                            val redHome = homeTypeValue.toString().toInt()
                            val redAway = awayTypeValue.toString().toInt()
                            homeRedcard.text = homeTypeValue.toString()
                            awayRedcard.text = awayTypeValue.toString()
                            setChangeProgressbarColor(redcardProgressBar, hometeamColor, awayteamColor, redHome, redHome+redAway)
                        }
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
                homeShotAccuracy.text = homeShotAccurPercent + "%"
                awayShotAccuracy.text = awayShotAccurPercent + "%"
                homeShotsAccurByTotal.text = getString(R.string.accurshot_by_total, homeShotOnNum, homeShotTotal)
                awayShotsAccurByTotal.text = getString(R.string.accurshot_by_total, awayShotOnNum, awayShotTotal)
                setGoalPostColor(homeGoalPost, hometeamColor)
                setGoalPostColor(awayGoalPost, awayteamColor)
            }
        )

        return view
    }


    fun initView(view: View){
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
        homeOffside = view.findViewById(R.id.home_offside)
        awayOffside = view.findViewById(R.id.away_offside)
        homeFoul = view.findViewById(R.id.home_foul)
        awayFoul = view.findViewById(R.id.away_foul)
        homeYellowcard = view.findViewById(R.id.home_yellowcard)
        awayYellowcard = view.findViewById(R.id.away_yellowcard)
        homeRedcard = view.findViewById(R.id.home_redcard)
        awayRedcard = view.findViewById(R.id.away_redcard)

        ballPossessionProgressBar = view.findViewById(R.id.ball_possession_progressbar)
        totalshootingProgressBar = view.findViewById(R.id.total_shooting_progressbar)
        cornerkickProgressBar = view.findViewById(R.id.cornerkicks_progressbar)
        blockedShotProgressBar = view.findViewById(R.id.blocked_shots_progressbar)
        offsideProgressBar = view.findViewById(R.id.offside_progressbar)
        foulProgressBar = view.findViewById(R.id.foul_progressbar)
        yellowcardProgressBar = view.findViewById(R.id.yellowcard_progressbar)
        redcardProgressBar = view.findViewById(R.id.redcard_progressbar)

        homeGoalPost.add(view.findViewById(R.id.home_goalpost_shotoff_1))
        homeGoalPost.add(view.findViewById(R.id.home_goalpost_shotoff_2))
        homeGoalPost.add(view.findViewById(R.id.home_goalpost_shoton))
        awayGoalPost.add(view.findViewById(R.id.away_goalpost_shotoff_1))
        awayGoalPost.add(view.findViewById(R.id.away_goalpost_shotoff_2))
        awayGoalPost.add(view.findViewById(R.id.away_goalpost_shoton))
    }


    fun setChangeProgressbarColor(progressBar: ProgressBar, homeTeamColor: String, awayTeamColor: String, progress: Int, maxProgress: Int) {
        val progressBarDrawable = progressBar.progressDrawable as LayerDrawable
        val backgroundDrawable = progressBarDrawable.getDrawable(0) // 배경 progressbar
        val progressDrawable = progressBarDrawable.getDrawable(1)   // 진행된 progressbar
        val homeColor = if(maxProgress == 0) { "#616161"} else { homeTeamColor }
        val awayColor = if(maxProgress == 0) { "#616161"} else { awayTeamColor }

        if (progressBar.id != R.id.ball_possession_progressbar) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29+
                backgroundDrawable.colorFilter = BlendModeColorFilter(Color.parseColor(awayColor), BlendMode.SRC_ATOP)
            } else {
                backgroundDrawable.setColorFilter(Color.parseColor(awayColor), PorterDuff.Mode.SRC_IN)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                progressDrawable.colorFilter = BlendModeColorFilter(Color.parseColor(homeColor), BlendMode.SRC_ATOP)
            } else {
                progressDrawable.setColorFilter(Color.parseColor(homeColor), PorterDuff.Mode.SRC_IN)
            }
        } else  { // 볼 점유율 Progressbar 작업
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29+
                backgroundDrawable.colorFilter = BlendModeColorFilter(Color.parseColor(homeColor), BlendMode.SRC_ATOP)
            } else {
                backgroundDrawable.setColorFilter(Color.parseColor(homeColor), PorterDuff.Mode.SRC_IN)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                progressDrawable.colorFilter = BlendModeColorFilter(Color.parseColor(awayColor), BlendMode.SRC_ATOP)
            } else {
                progressDrawable.setColorFilter(Color.parseColor(awayColor), PorterDuff.Mode.SRC_IN)
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
        fun newInstance(fixtureID: Int): FixtureDetailStatisticsFragment {
            val args = Bundle().apply {
                putInt(ARG_FIXTURE_ID, fixtureID)
            }
            return FixtureDetailStatisticsFragment().apply {
                arguments = args
            }
        }
    }
}