package com.example.feetballfootball

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.feetballfootball.api.fixturedetail.FixtureDetailResponse
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import kotlin.math.abs

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
    private lateinit var toolbar: Toolbar
    private lateinit var tabs: TabLayout
    private lateinit var appBarLayout: AppBarLayout

    private val tabTexts: List<String> = listOf("라인업", "통계")

    private val fadeIn by lazy {
        AnimationUtils.loadAnimation(context, R.anim.fade_in)
    }
    private val fadeOut by lazy {
        AnimationUtils.loadAnimation(context, R.anim.fade_out)
    }
    private lateinit var viewPager: ViewPager2

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
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fixture_detail, container, false)

        initView(view)
        initAppbarLayoutAnimation()
        Log.d(TAG, fixtureID.toString())

        tabs.addTab(tabs.newTab().setText("라인업"))
        tabs.addTab(tabs.newTab().setText("통계"))


        val adapter = ThreePagerAdapter(requireActivity(), 2, fixtureID)
        viewPager.adapter = adapter

        TabLayoutMediator(tabs, viewPager) { tabs, position ->
            tabs.text = tabTexts[position]
        }.attach()

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                viewPager.setCurrentItem(tab!!.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })


        fixtureDetailLiveData.observe(
            viewLifecycleOwner,
            Observer {

                /** appbarUpdateUI() 함수로 들어가야 할 내용들 **/
                // index 0: home, index 1: away
                val HomeAwayTeamIDList: MutableList<Int> = mutableListOf()

                var homeTeamScorer = mutableMapOf<String, MutableList<String>>()
                var awayTeamScorer = mutableMapOf<String, MutableList<String>>()
                getAppBarYMax = appBarLayout.height - toolbar.height
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

            }
        )
        return view
    }


    fun initView(view: View) {
        /*************** Appbar Layout *******************/
        homeTeamTextView = view.findViewById(R.id.hometeam_textview)
        awayTeamTextView = view.findViewById(R.id.awayteam_textview)
        matchScoreTextView = view.findViewById(R.id.score_textview)
        matchStatusTextView = view.findViewById(R.id.match_status)
        homeTeamScorerTextView = view.findViewById(R.id.hometeam_scorer_textview)
        awayTeamScorerTextView = view.findViewById(R.id.awayteam_scorer_textview)
        homeTeamImageView = view.findViewById(R.id.hometeam_imageview)
        awayTeamImageView = view.findViewById(R.id.awayteam_imageview)
        goalIcon = view.findViewById(R.id.goal_icon)
        toolbar = view.findViewById(R.id.toolbar)
        tabs = view.findViewById(R.id.fixture_detail_tab_layout)
        appBarLayout = view.findViewById(R.id.appBarLayout)
        viewPager = view.findViewById(R.id.view_pager)
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
        appBarLayout.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                if(abs(verticalOffset) >= getAppBarYMax / 2) {
                    if (homeTeamTextView.visibility != View.INVISIBLE) {
                        homeTeamTextView.startAnimation(fadeOut)
                        awayTeamTextView.startAnimation(fadeOut)
                        matchStatusTextView.startAnimation(fadeOut)
                        homeTeamScorerTextView.startAnimation(fadeOut)
                        awayTeamScorerTextView.startAnimation(fadeOut)
                        goalIcon.startAnimation(fadeOut)

                        homeTeamTextView.visibility = View.INVISIBLE
                        awayTeamTextView.visibility = View.INVISIBLE
                        matchStatusTextView.visibility = View.INVISIBLE
                        homeTeamScorerTextView.visibility = View.INVISIBLE
                        awayTeamScorerTextView.visibility = View.INVISIBLE
                        goalIcon.visibility = View.INVISIBLE
                    }

                } else {
                    if(homeTeamTextView.visibility != View.VISIBLE) {
                        homeTeamTextView.startAnimation(fadeIn)
                        awayTeamTextView.startAnimation(fadeIn)
                        matchStatusTextView.startAnimation(fadeIn)
                        homeTeamScorerTextView.startAnimation(fadeIn)
                        awayTeamScorerTextView.startAnimation(fadeIn)
                        goalIcon.startAnimation(fadeIn)

                        homeTeamTextView.visibility = View.VISIBLE
                        awayTeamTextView.visibility = View.VISIBLE
                        matchStatusTextView.visibility = View.VISIBLE
                        homeTeamScorerTextView.visibility = View.VISIBLE
                        awayTeamScorerTextView.visibility = View.VISIBLE
                        goalIcon.visibility = View.VISIBLE
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
            0 -> {
                return FixtureDetailLineupFragment.newInstance(fixtureID)
            }
            1 -> {
                return FixtureDetailStatisticsFragment.newInstance(fixtureID)
            }
            else -> {
                return FixtureDetailLineupFragment.newInstance(fixtureID)
            }
        }
    }

}