package com.example.feetballfootball.fragment.fixture

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
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.feetballfootball.R
import com.example.feetballfootball.api.fixturedetail.FixtureDetailResponse
import com.example.feetballfootball.databinding.FragmentFixtureDetailStatisticsBinding
import com.example.feetballfootball.viewModel.FixtureDetailViewModel

private const val ARG_FIXTURE_ID = "fixture_id"
private const val TAG = "FixtureDetailStatisticsFragment"

class FixtureDetailStatisticsFragment : Fragment() {

    private var fixtureID: Int = 0
    private lateinit var fixtureDetailViewModel: FixtureDetailViewModel
    private lateinit var fixtureDetailLiveData: LiveData<List<FixtureDetailResponse>>

    private var _binding: FragmentFixtureDetailStatisticsBinding? = null
    private val binding get() = _binding!!

    private val homeGoalPost: List<LinearLayout> by lazy {
        listOf(
            binding.homeGoalpostShotoff1,
            binding.homeGoalpostShotoff2,
            binding.homeGoalpostShoton
        )
    }
    private val awayGoalPost: List<LinearLayout> by lazy {
        listOf(
            binding.awayGoalpostShotoff1,
            binding.awayGoalpostShotoff2,
            binding.awayGoalpostShoton
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fixtureID = arguments?.getInt(ARG_FIXTURE_ID) ?: 0
        fixtureDetailViewModel = ViewModelProvider(requireActivity()).get(FixtureDetailViewModel::class.java)
        fixtureDetailLiveData = fixtureDetailViewModel.fetchFixtureDetailLiveData(fixtureID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFixtureDetailStatisticsBinding.inflate(inflater, container, false)

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
                            binding.homeShoton.text = homeTypeValue.toString()
                            binding.awayShoton.text = awayTypeValue.toString()
                            homeShotOnNum = homeTypeValue.toString()
                            awayShotOnNum = awayTypeValue.toString()
                        }
                        "Shots off Goal" -> {
                            binding.homeShotoff.text = homeTypeValue.toString()
                            binding.awayShotoff.text = awayTypeValue.toString()
                        }
                        "Total Shots" -> {
                            val homeProgress = homeTypeValue.toString().toInt()
                            val awayProgress = awayTypeValue.toString().toInt()
                            val maxProgress = homeProgress + awayTypeValue.toString().toInt()
                            binding.homeTotalShooting.text = homeTypeValue.toString()
                            binding.awayTotalShooting.text = awayTypeValue.toString()
                            setChangeProgressbarColor(binding.totalShootingProgressbar, hometeamColor, awayteamColor, homeProgress, maxProgress)
                            homeShotTotal = homeProgress.toString()
                            awayShotTotal = awayProgress.toString()
                        }
                        "Blocked Shots" -> {
                            val homeBlocked = homeTypeValue.toString().toInt()
                            val awayBlocked = awayTypeValue.toString().toInt()
                            binding.homeBlockedShots.text = homeTypeValue.toString()
                            binding.awayBlockedShots.text = awayTypeValue.toString()
                            setChangeProgressbarColor(binding.blockedShotsProgressbar, hometeamColor, awayteamColor, homeBlocked, homeBlocked+awayBlocked)
                        }
                        "Corner Kicks" -> {
                            val homeProgress = homeTypeValue.toString().toInt()
                            val maxProgress = homeProgress + awayTypeValue.toString().toInt()
                            binding.homeCornerkicks.text = homeTypeValue.toString()
                            binding.awayCornerkicks.text = awayTypeValue.toString()
                            setChangeProgressbarColor(binding.cornerkicksProgressbar, hometeamColor, awayteamColor, homeProgress, maxProgress)
                        }
                        "Offsides" -> {
                            val offsideHome = homeTypeValue.toString().toInt()
                            val offsideAway = awayTypeValue.toString().toInt()
                            binding.homeOffside.text = homeTypeValue.toString()
                            binding.awayOffside.text = awayTypeValue.toString()
                            setChangeProgressbarColor(binding.offsideProgressbar, hometeamColor, awayteamColor, offsideHome, offsideHome+offsideAway)
                        }
                        "Ball Possession" -> {
                            binding.homeBallPossession.text = homeTypeValue.toString()
                            binding.awayBallPossession.text = awayTypeValue.toString()
                            // background를 home으로 놓고 progress를 away의 수치로 설정하면 좌,우 배치에 맞게된다.
                            val awayProgress = awayTypeValue.toString().split("%")[0].toInt()
                            setChangeProgressbarColor(binding.ballPossessionProgressbar, hometeamColor, awayteamColor, awayProgress, maxProgress = 100)
                        }
                        "Fouls" -> {
                            val foulHome = homeTypeValue.toString().toInt()
                            val foulAway = awayTypeValue.toString().toInt()
                            binding.homeFoul.text = homeTypeValue.toString()
                            binding.awayFoul.text = awayTypeValue.toString()
                            setChangeProgressbarColor(binding.foulProgressbar, hometeamColor, awayteamColor, foulHome, foulHome+foulAway)
                        }
                        "Yellow Cards" -> {
                            val yellowHome = homeTypeValue.toString().toInt()
                            val yellowAway = awayTypeValue.toString().toInt()
                            binding.homeYellowcard.text = homeTypeValue.toString()
                            binding.awayYellowcard.text = awayTypeValue.toString()
                            setChangeProgressbarColor(binding.yellowcardProgressbar, hometeamColor, awayteamColor, yellowHome, yellowHome+yellowAway)
                        }
                        "Red Cards" -> {
                            val redHome = homeTypeValue.toString().toInt()
                            val redAway = awayTypeValue.toString().toInt()
                            binding.homeRedcard.text = homeTypeValue.toString()
                            binding.awayRedcard.text = awayTypeValue.toString()
                            setChangeProgressbarColor(binding.redcardProgressbar, hometeamColor, awayteamColor, redHome, redHome+redAway)
                        }
                        "Passes %" -> {
                            binding.homeAccuracy.text = homeTypeValue.toString()
                            binding.awayAccuracy.text = awayTypeValue.toString()
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
                binding.homePassAccurByTotal.text = getString(R.string.accurpass_by_total, homePassesAccurate, homeTotalPasses)
                binding.awayPassAccurByTotal.text = getString(R.string.accurpass_by_total, awayPassesAccurate, awayTotalPasses)
                val homeShotAccurPercent = ((homeShotOnNum.toDouble() / homeShotTotal.toDouble())*100).toInt().toString()
                val awayShotAccurPercent = ((awayShotOnNum.toDouble() / awayShotTotal.toDouble())*100).toInt().toString()
                binding.homeShotAccuracy.text = homeShotAccurPercent + "%"
                binding.awayShotAccuracy.text = awayShotAccurPercent + "%"
                binding.homeShotsAccurByTotal.text = getString(R.string.accurshot_by_total, homeShotOnNum, homeShotTotal)
                binding.awayShotsAccurByTotal.text = getString(R.string.accurshot_by_total, awayShotOnNum, awayShotTotal)
                setGoalPostColor(homeGoalPost, hometeamColor)
                setGoalPostColor(awayGoalPost, awayteamColor)
            }
        )

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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