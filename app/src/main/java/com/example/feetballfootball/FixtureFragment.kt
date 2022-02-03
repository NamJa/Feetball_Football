package com.example.feetballfootball

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.feetballfootball.api.FixtureResponse
import org.threeten.bp.LocalDate

private const val TAG = "FixtureFragment"
class FixtureFragment : Fragment() {

    private lateinit var feetballfootballViewModel: FeetballFootballViewModel

    private lateinit var fixtureData: MutableLiveData<Array<MutableList<FixtureResponse>?>>
    private lateinit var fixtureDataExecute: Array<MutableList<FixtureResponse>?>
    private lateinit var resultData: MutableLiveData<Int>
    private lateinit var progressBar: ProgressBar
    private lateinit var fixtureDateTextView: TextView
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button
    private lateinit var allLeaugeFixtureRecyclerView: RecyclerView

    var currentDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        feetballfootballViewModel =
            ViewModelProvider(this).get(FeetballFootballViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fixture, container, false)
        Log.d(TAG, currentDate.toString())
        fixtureDateTextView = view.findViewById(R.id.fixture_date)
        prevButton = view.findViewById(R.id.prev_fixture_button)
        nextButton = view.findViewById(R.id.next_fixture_button)

        fixtureDateTextView.text = currentDate.toString()


        prevButton.setOnClickListener {
            currentDate = currentDate.minusDays(1)
            fixtureDateTextView.text = currentDate.toString()
            fixtureDataExecute = feetballfootballViewModel.fetchFixtureData(currentDate.toString())
            resultData = feetballfootballViewModel.resultData
        }
        nextButton.setOnClickListener {
            currentDate = currentDate.plusDays(1)
            fixtureDateTextView.text = currentDate.toString()
            fixtureDataExecute = feetballfootballViewModel.fetchFixtureData(currentDate.toString())
            resultData = feetballfootballViewModel.resultData
        }
        //fixtureData = feetballfootballViewModel.fixtureData
        fixtureDataExecute = feetballfootballViewModel.fetchFixtureData(currentDate.toString())
        resultData = feetballfootballViewModel.resultData
        allLeaugeFixtureRecyclerView = view.findViewById(R.id.league_fixture_recyclerview) as RecyclerView
        allLeaugeFixtureRecyclerView.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        allLeaugeFixtureRecyclerView.layoutManager = LinearLayoutManager(context)


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        /* 비동기 실행 방식 */
//        fixtureData.observe(
//            viewLifecycleOwner,
//            Observer {
//                val fixtureFinalData: MutableList<MutableList<FixtureResponse>> = mutableListOf()
//                // arrayOfNull로 처리한 함수를 여기에서 null값 없이 재처리
//                for (i in 0 until fixtureDataExecute.size) {
//                    if(fixtureDataExecute[i] == null) {
//                        continue
//                    } else {
//                        fixtureFinalData.add(fixtureDataExecute[i]!!)
//                    }
//                }
//                updateUI(fixtureFinalData)
//            }
//        )
        resultData.observe(
            viewLifecycleOwner,
            Observer {
                Log.d(TAG, "feetballfootballViewModel.currentDate: ${feetballfootballViewModel.currentDate.toString()}")
                val fixtureFinalData: MutableList<MutableList<FixtureResponse>> = mutableListOf()
                // arrayOfNull로 처리한 함수를 여기에서 null값 없이 재처리
                for (i in 0 until fixtureDataExecute.size) {
                    if(fixtureDataExecute[i] == null) {
                        continue
                    } else {
                        fixtureFinalData.add(fixtureDataExecute[i]!!)
                    }
                }
                updateUI(fixtureFinalData)
            }
        )
    }
    private fun updateUI(fixtureData: MutableList<MutableList<FixtureResponse>>) {
        val adapter = AllFixtureRecyclerViewAdapter(fixtureData)
        allLeaugeFixtureRecyclerView.adapter = adapter
        allLeaugeFixtureRecyclerView.layoutManager = LinearLayoutManager(context)
    }



    private class LeagueFixtureHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val leagueTitleTextView: TextView
        val allFixturesRecyclerView: RecyclerView

        init {
            leagueTitleTextView = itemView.findViewById(R.id.league_title)
            allFixturesRecyclerView = itemView.findViewById(R.id.fixture_recyclerview)
        }

        fun bind() {

        }
    }
    private inner class AllFixtureRecyclerViewAdapter(
        var allFixtureData: MutableList<MutableList<FixtureResponse>>
    ) : RecyclerView.Adapter<LeagueFixtureHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeagueFixtureHolder {
            val layoutInflater =
                LayoutInflater.from(requireContext()).inflate(R.layout.league_fixture, parent, false)
            return LeagueFixtureHolder(layoutInflater)
        }

        override fun onBindViewHolder(holder: LeagueFixtureHolder, position: Int) {
            holder.leagueTitleTextView.text = allFixtureData.get(position).get(0).league.name
            holder.allFixturesRecyclerView.adapter = FixtureRecyclerViewAdapter(requireContext(), allFixtureData[position])
            holder.allFixturesRecyclerView.layoutManager = LinearLayoutManager(context)
        }

        override fun getItemCount(): Int {
            return allFixtureData.size
        }
    }


    companion object {
        @JvmStatic
        fun newInstance() = FixtureFragment()
    }
}