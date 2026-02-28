package com.example.feetballfootball.fragment.Leagues

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.feetballfootball.databinding.FragmentLeagueStandingBinding
import com.example.feetballfootball.viewModel.StandingViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

private const val TAG = "LeagueStandingFragment"
private const val ARG_LEAGUE_ID = "league_id"

class LeagueStandingFragment : Fragment() {
    private var leagueId: Int? = 0
    private var leagueCodeMap = mapOf(39 to "Premier League", 140 to "LA LIGA", 135 to "SERIE A", 78 to "BUNDESLIGA", 61 to "LIGUE 1")
    private val tabTexts: List<String> = listOf("팀 순위", "개인 순위")

    private var _binding: FragmentLeagueStandingBinding? = null
    private val binding get() = _binding!!

    private lateinit var standingViewModel: StandingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        leagueId = arguments?.getInt(ARG_LEAGUE_ID)
        standingViewModel = ViewModelProvider(this).get(StandingViewModel::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeagueStandingBinding.inflate(inflater, container, false)

//        val window: Window = requireActivity().window
//        WindowInsetsControllerCompat(window, binding.leagueStandingContainer).isAppearanceLightStatusBars = false
//        window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.black)


        binding.leagueTitleTextview.text = leagueCodeMap[leagueId]

        val adapter = TwoPagerAdapter(requireActivity(), 2, leagueId!!)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) {tabs, position ->
            tabs.text = tabTexts[position]
        }.attach()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.viewPager.setCurrentItem(tab!!.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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