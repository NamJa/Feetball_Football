package com.example.feetballfootball

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.recyclerview.widget.RecyclerView
import com.example.feetballfootball.api.FixtureResponse
import com.example.feetballfootball.api.Teams
import com.squareup.picasso.Picasso

class FixtureRecyclerViewAdapter(var fixtureData: MutableList<FixtureResponse>): RecyclerView.Adapter<FixtureRecyclerViewAdapter.DataViewHolder>() {
    inner class DataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val elapsedTextView : TextView
        val homeTeamTextView: TextView
        val scoreOrTimeTextView: TextView
        val awayTeamTextView: TextView
        val homeTeamLogo: ImageView
        val awayTeamLogo: ImageView

        init {
            elapsedTextView = itemView.findViewById(R.id.elapsed)
            homeTeamTextView = itemView.findViewById(R.id.team_home)
            scoreOrTimeTextView = itemView.findViewById(R.id.score_or_time)
            awayTeamTextView = itemView.findViewById(R.id.team_away)
            homeTeamLogo = itemView.findViewById(R.id.team_home_logo)
            awayTeamLogo = itemView.findViewById(R.id.team_away_logo)
        }

        fun bindLogo(teams: Teams) {
            Picasso.get()
                .load(teams.home.logoUrl)
                .resize(100,100)
                .into(homeTeamLogo)
            Picasso.get()
                .load(teams.away.logoUrl)
                .resize(100,100)
                .into(awayTeamLogo)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context).inflate(R.layout.fixture, parent, false)
        return DataViewHolder(layoutInflater)
    }

    override fun onBindViewHolder(holder: DataViewHolder, position: Int) {
        when(fixtureData.get(position).fixture.status.short) {
            //https://www.api-football.com/documentation-v3#operation/get-fixtures 상태 코드 확인
            "PST" -> {
                holder.elapsedTextView.text = ""
                holder.scoreOrTimeTextView.setTextSize(Dimension.SP, 15f);
                holder.scoreOrTimeTextView.text = "연기됨"
            }
            "NS" -> {
                holder.elapsedTextView.text = ""
                var times = fixtureData.get(position).fixture.date.split("T")[1].split("+")[0].split(":")
                holder.scoreOrTimeTextView.text = (times[0]+":"+times[1])
            }
            "CANC" -> {
                holder.elapsedTextView.text = ""
                holder.scoreOrTimeTextView.text = "취소됨"
            }
            else -> {
                holder.elapsedTextView.text = fixtureData.get(position).fixture.status.elapsed.toString()+"'"
                var goals = fixtureData.get(position).goals
                holder.scoreOrTimeTextView.text = (goals.home.toString() +"-"+goals.away.toString())
            }
        }
        holder.homeTeamTextView.text = fixtureData.get(position).teams.home.name
        holder.awayTeamTextView.text = fixtureData.get(position).teams.away.name
        holder.bindLogo(fixtureData.get(position).teams)

    }

    override fun getItemCount(): Int {
        return fixtureData.size
    }
}