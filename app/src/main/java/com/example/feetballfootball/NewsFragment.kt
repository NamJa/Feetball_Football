package com.example.feetballfootball

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat


private const val TAG = "NewsFragment"
class NewsFragment : Fragment() {

    private lateinit var mainContainer: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_news, container, false)
        initView(view)

//        // 상단바 색상 및 아이콘 색상 조절
//        val window: Window = requireActivity().window
//        WindowInsetsControllerCompat(window, mainContainer).isAppearanceLightStatusBars = true
//        window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.white)

        return view
    }

    private fun initView(view: View) {
        mainContainer = view.findViewById(R.id.news_fragment_main_container)
    }

    companion object {
        @JvmStatic
        fun newInstance() = NewsFragment()
    }
}