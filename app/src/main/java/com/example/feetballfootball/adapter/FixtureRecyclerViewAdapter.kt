package com.example.feetballfootball.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.Dimension
import androidx.recyclerview.widget.RecyclerView
import com.example.feetballfootball.R
import com.example.feetballfootball.api.Fixture
import com.example.feetballfootball.api.FixtureResponse
import com.example.feetballfootball.api.Teams
import com.example.feetballfootball.databinding.FixtureBinding
import com.squareup.picasso.Picasso

class FixtureRecyclerViewAdapter(var context: Context, var fixtureData: MutableList<FixtureResponse>): RecyclerView.Adapter<FixtureRecyclerViewAdapter.DataViewHolder>() {

    // Fragment 독립성을 유지하기 위한 콜백 인터페이스
    interface Callbacks{
        fun onFixtureSelected(fixtureID: Int)
    }
                                        //activity에서 구현하는 경우에만 context대입한다.
    private var callbacks: Callbacks = context as Callbacks
    // 여기에서 생성한 Callbacks 인터페이스를 FixtureFragment를 호스팅하는 MainActivity에서 구현한다.
    // FixtureFragment에서 인터페이스를 선언하는 편이 좀 더 좋았겠지만, 중첩 RecyclerView를 구현한 관계로 가독성을 위해서 여기에 선언함.
    // 하지만, 인자로 무거운 context 객체를 넘기느냐 vs
    //                          이 어댑터 클래스마저 FixtureFragment에서 구현하여 약간의 가독성을 포기하느냐는 나중에 테스트해볼 예정


    inner class DataViewHolder(val binding: FixtureBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private lateinit var fixture: Fixture

        init {
            binding.root.setOnClickListener(this)
        }

        fun bindLogo(teams: Teams) {
            Picasso.get()
                .load(teams.home.logoUrl)
                .into(binding.teamHomeLogo)
            Picasso.get()
                .load(teams.away.logoUrl)
                .into(binding.teamAwayLogo)
        }
        fun bind(fixture: Fixture) {
            this.fixture = fixture
        }

        override fun onClick(p0: View?) {
            if(fixture.status.short == "PST" || fixture.status.short == "NS" || fixture.status.short == "CANC") {
                Toast.makeText(context, R.string.toast_message_match_is_not_started, Toast.LENGTH_SHORT).show()
            } else {
                callbacks.onFixtureSelected(fixture.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder {
        val binding = FixtureBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DataViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DataViewHolder, position: Int) {
        when(fixtureData.get(position).fixture.status.short) {
            //https://www.api-football.com/documentation-v3#operation/get-fixtures 상태 코드 확인
            "PST" -> {
                holder.binding.elapsed.text = ""
                holder.binding.scoreOrTime.setTextSize(Dimension.SP, 15f);
                holder.binding.scoreOrTime.text = "연기됨"
            }
            "NS" -> {
                holder.binding.elapsed.text = ""
                var times = fixtureData.get(position).fixture.date.split("T")[1].split("+")[0].split(":")
                holder.binding.scoreOrTime.text = (times[0]+":"+times[1])
            }
            "CANC" -> {
                holder.binding.elapsed.text = ""
                holder.binding.scoreOrTime.text = "취소됨"
            }
            else -> {
                holder.binding.elapsed.text = fixtureData.get(position).fixture.status.elapsed.toString()+"'"
                var goals = fixtureData.get(position).goals
                holder.binding.scoreOrTime.text = (goals.home.toString() +"-"+goals.away.toString())
            }
        }
        holder.bind(fixtureData.get(position).fixture)
        holder.bindLogo(fixtureData.get(position).teams)
        holder.binding.teamHome.text = fixtureData.get(position).teams.home.name
        holder.binding.teamAway.text = fixtureData.get(position).teams.away.name

    }

    override fun getItemCount(): Int {
        return fixtureData.size
    }
}