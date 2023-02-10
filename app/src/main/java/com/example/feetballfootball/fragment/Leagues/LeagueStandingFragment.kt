package com.example.feetballfootball.fragment.Leagues

import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.feetballfootball.R
import com.example.feetballfootball.viewModel.StandingViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

private const val TAG = "LeagueStandingFragment"
private const val ARG_LEAGUE_ID = "league_id"

class LeagueStandingFragment : Fragment() {
    private var leagueId: Int? = 0
    private var leagueCodeMap = mapOf(39 to "Premier League", 140 to "LA LIGA", 135 to "SERIE A", 78 to "BUNDESLIGA", 61 to "LIGUE 1")
    private val tabTexts: List<String> = listOf("팀 순위", "개인 순위")

    private lateinit var mainContainer: LinearLayout

    private lateinit var leagueTitle : TextView
    private lateinit var tabs: TabLayout
    private lateinit var viewPager: ViewPager2

    private lateinit var standingViewModel: StandingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        leagueId = arguments?.getInt(ARG_LEAGUE_ID)
        standingViewModel = ViewModelProvider(this).get(StandingViewModel::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_league_standing, container, false)
        initView(view)

//        val window: Window = requireActivity().window
//        WindowInsetsControllerCompat(window, mainContainer).isAppearanceLightStatusBars = false
//        window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.black)


        leagueTitle.text = leagueCodeMap[leagueId]

        val adapter = TwoPagerAdapter(requireActivity(), 2, leagueId!!)
        viewPager.adapter = adapter
        TabLayoutMediator(tabs, viewPager) {tabs, position ->
            tabs.text = tabTexts[position]
        }.attach()

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                viewPager.setCurrentItem(tab!!.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        return view
    }
    fun initView(view: View) {
        mainContainer = view.findViewById(R.id.league_standing_container)
        leagueTitle = view.findViewById(R.id.league_title_textview)
        tabs = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)
    }


    companion object {
        @JvmStatic
        fun newInstance(leagueId: Int): LeagueStandingFragment {
            val args = Bundle().apply {
                putInt(ARG_LEAGUE_ID, leagueId)
            }
            return LeagueStandingFragment().apply {
                arguments = args
            }

        }
    }

}


class TwoPagerAdapter(
    fragmentActivity: FragmentActivity,
    val tabCount: Int,
    val leagueId: Int
): FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return tabCount
    }

    override fun createFragment(position: Int): Fragment {
        when(position) {
            0 -> {
                return LeagueClubsStandingFragment.newInstance(leagueId)
            }
            1 -> {
                return LeaguePlayerStandingFragment.newInstance(leagueId)
            }
            else -> {
                return LeagueClubsStandingFragment.newInstance(leagueId)
            }
        }
    }

}