package com.example.feetballfootball

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

private const val TAG = "FixtureDetailFragment"
private const val ARG_FIXTURE_ID = "fixture_id"

class FixtureDetailFragment : Fragment() {
    private var fixtureID: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fixtureID = arguments?.getInt(ARG_FIXTURE_ID) ?: 0
        Log.d(TAG, "Received fixture Id: $fixtureID")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fixture_detail, container, false)

        val textView = view.findViewById<TextView>(R.id.IDtest_textview)
        textView.text = "수신한 경기 일정 ID: " + fixtureID.toString()
        return view
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