package com.example.feetballfootball

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.material.tabs.TabItem
import com.google.android.material.tabs.TabLayout
import com.jakewharton.threetenabp.AndroidThreeTen
import org.threeten.bp.LocalDate
import org.threeten.bp.Year
import kotlin.reflect.typeOf

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(), LeaguesFragment.Callbacks, FixtureRecyclerViewAdapter.Callbacks {
    private lateinit var tabLayout: TabLayout

    val footballDataFetchr = FootballDataFetchr()
    private lateinit var tabItem1: TabItem
    private lateinit var tabItem2: TabItem
    private lateinit var tabItem3: TabItem
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AndroidThreeTen.init(this)

        tabLayout = findViewById(R.id.bottom_tab_layout)

        val isFragmentContainerEmpty = savedInstanceState == null
        if (isFragmentContainerEmpty) {
            showFragment(tabPos = 0)
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    showFragment(tab.position)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        //footballDataFetchr.fetchFootballFixtures("2021-12-27", 39, 2021)
        // fragment 생성 예정
    }
    fun showFragment(tabPos: Int) {
        when(tabPos) {
            0 -> {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, FixtureFragment.newInstance())
                    .addToBackStack(null)
                    .commit()
            }
            1 -> {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, LeaguesFragment.newInstance())
                    .addToBackStack(null)
                    .commit()
            }
            else -> {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, NewsFragment.newInstance())
                    .addToBackStack(null)
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