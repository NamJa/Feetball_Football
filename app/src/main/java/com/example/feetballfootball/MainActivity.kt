package com.example.feetballfootball

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.tabs.TabItem
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {
    private lateinit var tabLayout: TabLayout

    val footballDataFetchr = FootballDataFetchr()
    private lateinit var tabItem1: TabItem
    private lateinit var tabItem2: TabItem
    private lateinit var tabItem3: TabItem
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tabLayout = findViewById(R.id.bottom_tab_layout)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        //footballDataFetchr.fetchFootballFixtures("2021-12-27", 39, 2021)
        // fragment 생성 예정
    }
}