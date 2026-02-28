package com.example.feetballfootball.fragment.fixture

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.feetballfootball.R
import com.example.feetballfootball.adapter.FixtureRecyclerViewAdapter
import com.example.feetballfootball.api.FixtureResponse
import com.example.feetballfootball.databinding.FragmentFixtureBinding
import com.example.feetballfootball.databinding.LeagueFixtureBinding
import com.example.feetballfootball.viewModel.FeetballFootballViewModel
import org.threeten.bp.LocalDate

private const val TAG = "FixtureFragment"
class FixtureFragment : Fragment() {

    private lateinit var feetballfootballViewModel: FeetballFootballViewModel

    private lateinit var fixtureDataExecute: Array<MutableList<FixtureResponse>?>
    private lateinit var resultData: MutableLiveData<Int>

    private var _binding: FragmentFixtureBinding? = null
    private val binding get() = _binding!!

    var currentDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        feetballfootballViewModel =
            ViewModelProvider(this).get(FeetballFootballViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFixtureBinding.inflate(inflater, container, false)

//        // 상단바 색상 및 아이콘 색상 조절
//        val window: Window = requireActivity().window
//        WindowInsetsControllerCompat(window, binding.fixtureFragmentMainContainer).isAppearanceLightStatusBars = true
//        window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.white)

        binding.fixtureDate.text = currentDate.toString()


        binding.prevFixtureButton.setOnClickListener {
            currentDate = currentDate.minusDays(1)
            binding.fixtureDate.text = currentDate.toString()
            fixtureDataExecute = feetballfootballViewModel.fetchFixtureData(currentDate.toString())
            resultData = feetballfootballViewModel.resultData
        }
        binding.nextFixtureButton.setOnClickListener {
            currentDate = currentDate.plusDays(1)
            binding.fixtureDate.text = currentDate.toString()
            fixtureDataExecute = feetballfootballViewModel.fetchFixtureData(currentDate.toString())
            resultData = feetballfootballViewModel.resultData
        }
        //fixtureData = feetballfootballViewModel.fixtureData
        fixtureDataExecute = feetballfootballViewModel.fetchFixtureData(currentDate.toString())
        resultData = feetballfootballViewModel.resultData

        binding.leagueFixtureRecyclerview.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        binding.leagueFixtureRecyclerview.layoutManager = LinearLayoutManager(context)
        val dividerItemDecoration = com.example.feetballfootball.util.DividerItemDecoration(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.recyclerview_divider
            )!!
        )
        binding.leagueFixtureRecyclerview.addItemDecoration(dividerItemDecoration)
        return binding.root
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
                if (fixtureFinalData.size != 0) {
                    binding.noFixturesTextView.visibility = View.GONE
                    binding.leagueFixtureRecyclerview.visibility = View.VISIBLE
                    updateUI(fixtureFinalData)
                } else {
                    binding.noFixturesTextView.visibility = View.VISIBLE
                    binding.leagueFixtureRecyclerview.visibility = View.GONE
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateUI(fixtureData: MutableList<MutableList<FixtureResponse>>) {
        val adapter = AllFixtureRecyclerViewAdapter(fixtureData)
        binding.leagueFixtureRecyclerview.adapter = adapter
        binding.leagueFixtureRecyclerview.layoutManager = LinearLayoutManager(context)
    }

    private class LeagueFixtureHolder(val binding: LeagueFixtureBinding) : RecyclerView.ViewHolder(binding.root)

    private inner class AllFixtureRecyclerViewAdapter(
        var allFixtureData: MutableList<MutableList<FixtureResponse>>
    ) : RecyclerView.Adapter<LeagueFixtureHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeagueFixtureHolder {
            val binding = LeagueFixtureBinding.inflate(LayoutInflater.from(requireContext()), parent, false)
            return LeagueFixtureHolder(binding)
        }

        override fun onBindViewHolder(holder: LeagueFixtureHolder, position: Int) {
            holder.binding.leagueTitle.text = allFixtureData.get(position).get(0).league.name
            holder.binding.fixtureRecyclerview.adapter = FixtureRecyclerViewAdapter(requireContext(), allFixtureData[position])
            holder.binding.fixtureRecyclerview.layoutManager = LinearLayoutManager(context)
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