package com.chase1st.feetballfootball.fragment.fixture

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chase1st.feetballfootball.R
import com.chase1st.feetballfootball.api.fixturedetail.Events
import com.chase1st.feetballfootball.api.fixturedetail.FixtureDetailResponse
import com.chase1st.feetballfootball.viewModel.FixtureDetailViewModel

private const val TAG = "FixtureDetailEventsFragment"
private const val ARG_FIXTURE_ID = "fixture_id"

class FixtureDetailEventsFragment : Fragment() {
    private var fixtureID = 0
    private lateinit var fixtureDetailViewModel: FixtureDetailViewModel
    private lateinit var fixtureDetailLiveData: LiveData<List<FixtureDetailResponse>>

    private lateinit var eventsRecyclerView: RecyclerView

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
        val view = inflater.inflate(R.layout.fragment_fixture_detail_events, container, false)
        initView(view)

        fixtureDetailLiveData.observe(
            viewLifecycleOwner,
            Observer {
                val eventList = it[0].events
                eventsRecyclerView.adapter = EventsRecyclerViewAdapter(eventList, it[0].teams.home.id, it[0].teams.away.id)
                eventsRecyclerView.layoutManager = LinearLayoutManager(context)
            }
        )

        return view
    }

    fun initView(view: View) {
        eventsRecyclerView = view.findViewById(R.id.events_recyclerview)
    }

    private inner class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val homeElapsed: TextView = view.findViewById(R.id.home_elapsed)
        val homeMainPlayer: TextView = view.findViewById(R.id.home_main_player)
        val homeAssistPlayer: TextView = view.findViewById(R.id.home_assist_player)
        val awayElapsed: TextView = view.findViewById(R.id.away_elapsed)
        val awayMainPlayer: TextView = view.findViewById(R.id.away_main_player)
        val awayAssistPlayer: TextView = view.findViewById(R.id.away_assist_player)

        val substitutionICON: LinearLayout = view.findViewById(R.id.substitutionICON)
        val goalSign: ImageView = view.findViewById(R.id.goal_sign_imageview)
        val penaltySign: ImageView = view.findViewById(R.id.penaltykick_imageview)
        val penaltyMissedSign: ImageView = view.findViewById(R.id.penaltykick_missed_imageview)
        val yellowCard: ImageView = view.findViewById(R.id.yellowcard_imageview)
        val redCard: ImageView = view.findViewById(R.id.redcard_imageview)
        val varAwardedSign: ImageView = view.findViewById(R.id.var_awarded_sign_imageview)
        val varCancelled: ImageView = view.findViewById(R.id.var_cancelled_imageview)

        val eventSignList: List<View> = listOf(substitutionICON, goalSign, penaltySign, penaltyMissedSign, yellowCard, redCard, varAwardedSign, varCancelled)

        fun bind(event: Events, home: Int, away: Int) {
            when(event.type) {
                "Goal" -> {
                    if(event.detail == "Normal Goal") {
                        setEventsOfPlayer(event, home, 1, isSubstitute = false)
                    } else if(event.detail == "Own Goal") {
                        setEventsOfPlayer(event, home, 1, isSubstitute = false)
                    } else if(event.detail == "Penalty") {
                        setEventsOfPlayer(event, home, 2, isSubstitute = false)
                    } else if(event.detail == "Missed Penalty") {
                        setEventsOfPlayer(event, home, 3, isSubstitute = false)
                    }
                }
                "subst" -> {
                    homeMainPlayer.setTextColor(resources.getColor(R.color.events_subst_player_in, null))
                    awayMainPlayer.setTextColor(resources.getColor(R.color.events_subst_player_in, null))
                    homeAssistPlayer.setTextColor(resources.getColor(R.color.events_subst_player_out, null))
                    awayAssistPlayer.setTextColor(resources.getColor(R.color.events_subst_player_out, null))
                    setEventsOfPlayer(event, home, 0, true)
                }
                "Card" -> {
                    if (event.detail == "Yellow Card") {
                        setEventsOfPlayer(event, home, 4, isSubstitute = false)
                    } else if (event.detail == "Second Yellow Card") {
                        setEventsOfPlayer(event, home, 4, isSubstitute = false)
                    } else { // Red Card
                        setEventsOfPlayer(event, home, 5, isSubstitute = false)
                    }
                }
                "Var" -> {
                    if(event.detail == "Penalty awarded") {
                        setEventsOfPlayer(event, home, 6, isSubstitute = false)
                    } else { // VAR check Goal Cancelled
                        setEventsOfPlayer(event, home, 7, isSubstitute = false)
                    }
                }
            }
        }
        fun setEventsOfPlayer(event: Events, home: Int, visibleViewIndex: Int, isSubstitute: Boolean) {
            val elapsed = if(event.team.id == home) { homeElapsed } else { awayElapsed }
            val mainPlayer = if(event.team.id == home) { homeMainPlayer } else { awayMainPlayer }
            val assistPlayer = if(event.team.id == home) { homeAssistPlayer } else { awayAssistPlayer }
            val extra = if(event.time.extra != null) { "+${event.time.extra}" } else { "" }
            elapsed.text = event.time.elapsed.toString()+extra+"'"
            if (isSubstitute == false) {
                mainPlayer.text = event.player.name
                assistPlayer.text = if (event.assist.name != "null") { event.assist.name } else { "" }
            } else {
                // 선수 교체 이벤트의 경우에는 assist가 교체 대상 선수이므로 mainPlayer 자리에 assist.name을 할당한다.
                // 즉, mainPlayer와 assistPlayer에 할당하는 데이터를 바꾸는것
                mainPlayer.text = if (event.assist.name != "null") { event.assist.name } else { "" }
                assistPlayer.text = event.player.name
            }
            eventSignVisible(visibleViewIndex)
        }
        fun eventSignVisible(signIndex: Int) {
            // 0: substitutionICON, 1: goalSign, 2: Penalty Kick, 3: Penalty Kick Missed, 4: yellowCard, 5: redCard, 6: varAwardedSign, 7: varCancelled
            for (i in eventSignList.indices) {
                if (i == signIndex) { // 요청받은 인덱스의 View는 출력
                    eventSignList[i].visibility = View.VISIBLE
                } else { // 나머지는 다 비활성화
                    eventSignList[i].visibility = View.GONE
                }
            }
        }
    }

    private inner class EventsRecyclerViewAdapter(
        val eventsList: List<Events>,
        val home: Int,
        val away: Int
    ): RecyclerView.Adapter<EventViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
            val view = layoutInflater.inflate(R.layout.events_recycler_item, parent, false)
            return EventViewHolder(view)
        }

        override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
            holder.bind(eventsList[position], home, away)
        }

        override fun getItemCount(): Int {
            return eventsList.size
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(fixtureID: Int): FixtureDetailEventsFragment {
            val args = Bundle().apply {
                putInt(ARG_FIXTURE_ID, fixtureID)
            }
            return FixtureDetailEventsFragment().apply {
                arguments = args
            }
        }
    }
}