package com.example.feetballfootball

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.feetballfootball.api.fixturedetail.PlayerData
import com.squareup.picasso.Picasso

class LineupColAdapter(val context: Context, val rowLineup: List<PlayerData>): RecyclerView.Adapter<LineupColAdapter.ColHolder>() {
    inner class ColHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playerImage: ImageView
        val playerName: TextView
        init {
            playerImage = itemView.findViewById(R.id.player_imageview)
            playerName = itemView.findViewById(R.id.player_name_textview)
        }
        fun bindPlayerImage(id: Int) {
            Picasso.get()
                .load("https://media.api-sports.io/football/players/${id}.png")
                .into(playerImage)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.lineup_col_recycler_item, parent, false)
        return ColHolder(view)
    }

    override fun onBindViewHolder(holder: ColHolder, position: Int) {
        holder.playerName.text = rowLineup[position].player.name
        holder.bindPlayerImage(rowLineup[position].player.id)
    }

    override fun getItemCount(): Int {
        return rowLineup.size
    }
}