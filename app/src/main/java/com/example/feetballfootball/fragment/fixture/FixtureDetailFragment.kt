package com.example.feetballfootball.fragment.fixture

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.annotation.Dimension
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.feetballfootball.R
import com.example.feetballfootball.api.fixturedetail.FixtureDetailResponse
import com.example.feetballfootball.databinding.FragmentFixtureDetailBinding
import com.example.feetballfootball.viewModel.FixtureDetailViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import kotlin.math.abs

private const val TAG = "FixtureDetailFragment"
private const val ARG_FIXTURE_ID = "fixture_id"

class FixtureDetailFragment : Fragment() {
    private var fixtureID: Int = 0
    private var tabCount: Int = 3
    private lateinit var fixtureDetailViewModel: FixtureDetailViewModel
    private lateinit var fixtureDetailLiveData: LiveData<List<FixtureDetailResponse>>

    private var _binding: FragmentFixtureDetailBinding? = null
    private val binding get() = _binding!!

    private val tabTexts: List<String> = listOf("이벤트", "라인업", "통계")

    private val fadeIn by lazy {
        AnimationUtils.loadAnimation(context, R.anim.fade_in)
    }
    private val fadeOut by lazy {
        AnimationUtils.loadAnimation(context, R.anim.fade_out)
    }

    private var getAppBarYMax = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fixtureID = arguments?.getInt(ARG_FIXTURE_ID) ?: 0
        fixtureDetailViewModel = ViewModelProvider(this).get(FixtureDetailViewModel::class.java)
        fixtureDetailLiveData = fixtureDetailViewModel.fetchFixtureDetailLiveData(fixtureID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFixtureDetailBinding.inflate(inflater, container, false)

        initAppbarLayoutAnimation()
        Log.d(TAG, fixtureID.toString())


        binding.viewPager.registerOnPageChangeCallback (object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.appBarLayout.setExpanded (true)
            }
        })

        fixtureDetailLiveData.observe(
            viewLifecycleOwner,
            Observer {
                if (it.isEmpty()) return@Observer

                /** appbarUpdateUI() 함수로 들어가야 할 내용들 **/
                // index 0: home, index 1: away
                val HomeAwayTeamIDList: MutableList<Int> = mutableListOf()

                var homeTeamScorer = mutableMapOf<String, MutableList<String>>()
                var awayTeamScorer = mutableMapOf<String, MutableList<String>>()
                getAppBarYMax = binding.appBarLayout.height - binding.toolbar.height
                Picasso.get()
                    .load(it.get(0).teams.home.logoUrl)
                    .resize(100, 100)
                    .into(binding.hometeamImageview)

                Picasso.get()
                    .load(it.get(0).teams.away.logoUrl)
                    .resize(100, 100)
                    .into(binding.awayteamImageview)

                HomeAwayTeamIDList.add(it.get(0).teams.home.id)
                HomeAwayTeamIDList.add(it.get(0).teams.away.id)

                binding.hometeamTextview.text = it.get(0).teams.home.name
                binding.awayteamTextview.text = it.get(0).teams.away.name
                binding.matchStatus.text =
                    if(it.get(0).fixture.status.short == "FT") { "종료됨" }
                    else {
                        binding.matchStatus.setTextSize(Dimension.SP, 20f)
                        "${it.get(0).fixture.status.elapsed}'"
                    }
                binding.scoreTextview.text = (it.get(0).goals.home.toString() + " - " + it.get(0).goals.away.toString())

                // 득점 기록 처리
                it.get(0).events?.let {
                    it.forEach{
                        if (it.type == "Goal" && it.detail != "Missed Penalty") {
                            binding.goalIcon.visibility = View.VISIBLE
                            val extraTime = it.time.extra.toString() ?: ""
                            if (it.team.id == HomeAwayTeamIDList[0]) {
                                homeTeamScorer = WhoScoredByTeam(it.player.name, homeTeamScorer, it.time.elapsed, extraTime)
                            } else if(it.team.id == HomeAwayTeamIDList[1]) {
                                awayTeamScorer = WhoScoredByTeam(it.player.name, awayTeamScorer, it.time.elapsed, extraTime)
                            }
                        }
                    }
                }

                binding.hometeamScorerTextview.text = WriteWhoScoredOnTextView(homeTeamScorer)
                binding.awayteamScorerTextview.text = WriteWhoScoredOnTextView(awayTeamScorer)

                // 간혹가다 라인업 및 통계 데이터가 제공되지 않는 경우가 있다.
                if (it.get(0).lineups.isEmpty())
                    tabCount -= 1
                if (it.get(0).statistics.isEmpty())
                    tabCount -= 1

                binding.viewPager.adapter = ThreePagerAdapter (requireActivity(), tabCount, fixtureID)
                binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

                TabLayoutMediator(binding.fixtureDetailTabLayout, binding.viewPager) { tabs, position ->
                    tabs.text = tabTexts[position]
                }.attach()

            }
        )
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

    fun initAppbarLayoutAnimation() {
        binding.appBarLayout.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                if(abs(verticalOffset) >= getAppBarYMax / 2) {
                    if (binding.hometeamTextview.visibility != View.INVISIBLE) {
                        binding.hometeamTextview.startAnimation(fadeOut)
                        binding.awayteamTextview.startAnimation(fadeOut)
                        binding.matchStatus.startAnimation(fadeOut)
                        binding.hometeamScorerTextview.startAnimation(fadeOut)
                        binding.awayteamScorerTextview.startAnimation(fadeOut)
                        binding.goalIcon.startAnimation(fadeOut)

                        binding.hometeamTextview.visibility = View.INVISIBLE
                        binding.awayteamTextview.visibility = View.INVISIBLE
                        binding.matchStatus.visibility = View.INVISIBLE
                        binding.hometeamScorerTextview.visibility = View.INVISIBLE
                        binding.awayteamScorerTextview.visibility = View.INVISIBLE
                        binding.goalIcon.visibility = View.INVISIBLE
                    }

                } else {
                    if(binding.hometeamTextview.visibility != View.VISIBLE) {
                        binding.hometeamTextview.startAnimation(fadeIn)
                        binding.awayteamTextview.startAnimation(fadeIn)
                        binding.matchStatus.startAnimation(fadeIn)
                        binding.hometeamScorerTextview.startAnimation(fadeIn)
                        binding.awayteamScorerTextview.startAnimation(fadeIn)
                        binding.goalIcon.startAnimation(fadeIn)

                        binding.hometeamTextview.visibility = View.VISIBLE
                        binding.awayteamTextview.visibility = View.VISIBLE
                        binding.matchStatus.visibility = View.VISIBLE
                        binding.hometeamScorerTextview.visibility = View.VISIBLE
                        binding.awayteamScorerTextview.visibility = View.VISIBLE
                        binding.goalIcon.visibility = View.VISIBLE
                    }
                }
            }
        )
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


class ThreePagerAdapter(
    fragmentActivity: FragmentActivity,
    val tabCount: Int,
    val fixtureID: Int
): FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return tabCount
    }

    override fun createFragment(position: Int): Fragment {
        when(position) {
            0 ->{
                return FixtureDetailEventsFragment.newInstance(fixtureID)
            }
            1 -> {
                return FixtureDetailLineupFragment.newInstance(fixtureID)
            }
            2 -> {
                return FixtureDetailStatisticsFragment.newInstance(fixtureID)
            }
            else -> {
                return FixtureDetailEventsFragment.newInstance(fixtureID)
            }
        }
    }

}