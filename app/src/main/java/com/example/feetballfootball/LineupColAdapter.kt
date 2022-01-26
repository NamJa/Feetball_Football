package com.example.feetballfootball

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.feetballfootball.api.fixturedetail.PlayerData
import com.example.feetballfootball.api.fixturedetail.PlayerRatingData
import com.example.feetballfootball.api.fixturedetail.PlayersByTeamData
import com.squareup.picasso.Picasso

class LineupColAdapter(val context: Context, val rowLineup: List<PlayerData>, val teamRating: PlayersByTeamData): RecyclerView.Adapter<LineupColAdapter.ColHolder>() {
    inner class ColHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playerImage: ImageView
        val playerName: TextView
        val playerRating: TextView
        val playerNumber: TextView
        init {
            playerImage = itemView.findViewById(R.id.player_imageview)
            playerName = itemView.findViewById(R.id.player_name_textview)
            playerRating = itemView.findViewById(R.id.player_rating)
            playerNumber = itemView.findViewById(R.id.player_number_textview)
        }
        fun bindPlayerImage(id: Int) {
            Picasso.get()
                .load("https://media.api-sports.io/football/players/${id}.png")
                .into(playerImage)
            playerImage.background = AppCompatResources.getDrawable(context, R.drawable.player_face_bg_circle)
            playerImage.clipToOutline = true
        }
        fun bindPlayerRating(id: Int) {
            for(i in 0 until teamRating.players.size) {
                if (id == teamRating.players[i].player.id) {
                    val ratingFloat = teamRating.players[i].statistics[0].games.rating!!.toFloat()
                    playerRating.text = ratingFloat.toString()
                    if(ratingFloat >= 7.0f) {
                        Log.d("LineColAdapter", ratingFloat.toString())
                        val textBackground = playerRating.background as GradientDrawable
                        textBackground.color = context.resources.getColorStateList(R.color.player_rating_great, null)
                    } else {
                        val textBackground = playerRating.background as GradientDrawable
                        textBackground.color = context.resources.getColorStateList(R.color.player_rating_normal, null)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.lineup_col_recycler_item, parent, false)
        return ColHolder(view)
    }

    override fun onBindViewHolder(holder: ColHolder, position: Int) {
        holder.playerName.text = rowLineup[position].player.name
        holder.bindPlayerImage(rowLineup[position].player.id)
        holder.playerNumber.text = rowLineup[position].player.number.toString()
        holder.bindPlayerRating(rowLineup[position].player.id)
    }

    override fun getItemCount(): Int {
        return rowLineup.size
    }
}