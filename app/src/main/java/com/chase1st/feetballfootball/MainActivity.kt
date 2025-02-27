package com.chase1st.feetballfootball

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.chase1st.feetballfootball.adapter.FixtureRecyclerViewAdapter
import com.chase1st.feetballfootball.fragment.Leagues.LeagueStandingFragment
import com.chase1st.feetballfootball.fragment.Leagues.LeaguesFragment
import com.chase1st.feetballfootball.fragment.fixture.FixtureDetailFragment
import com.chase1st.feetballfootball.fragment.fixture.FixtureFragment
import com.chase1st.feetballfootball.fragment.news.NewsFragment
import com.google.android.material.tabs.TabLayout
import com.jakewharton.threetenabp.AndroidThreeTen

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(), LeaguesFragment.Callbacks, FixtureRecyclerViewAdapter.Callbacks {
    private lateinit var tabLayout: TabLayout

    private var selectedTabPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            // Tab을 선택한 상태에서 DarkMode 전환 같은
            // onCreate()를 호출하는 동작을 하게 되면 0번째 인덱스의 탭을 선택하는 문제 해결
            selectedTabPosition = it.getInt("selectedTabPos")
        }
        setContentView(R.layout.activity_main)
        AndroidThreeTen.init(this)

        tabLayout = findViewById(R.id.bottom_tab_layout)

        val isFragmentContainerEmpty = savedInstanceState == null
        if (isFragmentContainerEmpty) {
            showFragment(tabPos = 0)
        }

        val tab: TabLayout.Tab = tabLayout.getTabAt(selectedTabPosition)!!
        tab.select()
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    showFragment(tab.position)
                    selectedTabPosition = tab.position
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        // fragment 생성 예정
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putInt("selectedTabPos", selectedTabPosition)
        }
        super.onSaveInstanceState(outState)
    }

    fun showFragment(tabPos: Int) {
        when(tabPos) {
            0 -> {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, FixtureFragment.newInstance())
                    .commit()
            }
            1 -> {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, LeaguesFragment.newInstance())
                    .commit()
            }
            else -> {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, NewsFragment.newInstance())
                    .commit()
            }
        }
    }

    override fun onLeagueSelected(leagueId: Int) {
        val fragment = LeagueStandingFragment.newInstance(leagueId)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onFixtureSelected(fixtureId: Int) {
//        Toast.makeText(this, "MainActivity.onFixtureSelected: $fixtureId", Toast.LENGTH_SHORT).show()
        val fragment = FixtureDetailFragment.newInstance(fixtureId)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}