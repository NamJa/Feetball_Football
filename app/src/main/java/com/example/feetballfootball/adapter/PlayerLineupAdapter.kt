package com.example.feetballfootball.adapter

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.example.feetballfootball.R
import com.example.feetballfootball.api.fixturedetail.PlayerData
import com.example.feetballfootball.api.fixturedetail.PlayersByTeamData
import com.example.feetballfootball.databinding.LineupPlayerRecyclerItemBinding
import com.example.feetballfootball.databinding.LineupSubstitutePlayerRecyclerItemBinding
import com.squareup.picasso.Picasso

class PlayerLineupAdapter(
    val context: Context,
    val rowLineup: List<PlayerData>,
    val teamRating: PlayersByTeamData,
    val isSubsitute: Boolean
): RecyclerView.Adapter<PlayerLineupAdapter.ColHolder>() {
    inner class ColHolder(
        itemView: View,
        val playerImageview: ImageView,
        val playerNameTextview: TextView,
        val playerRating: TextView,
        val playerNumberTextview: TextView
    ) : RecyclerView.ViewHolder(itemView) {
        fun bindPlayerImage(id: Int) {
            Picasso.get()
                .load("https://media.api-sports.io/football/players/${id}.png")
                .into(playerImageview)
            playerImageview.background = AppCompatResources.getDrawable(context,
                R.drawable.player_face_bg_circle
            )
            playerImageview.clipToOutline = true
        }
        fun bindPlayerRating(id: Int) {
            for(i in 0 until teamRating.players.size) {
                if (id == teamRating.players[i].player.id && teamRating.players[i].statistics[0].games.rating != null) {
                    val ratingFloat = teamRating.players[i].statistics[0].games.rating!!.toFloat()
                    playerRating.text = ratingFloat.toString()
                    if(ratingFloat >= 7.0f) {
                        val textBackground = playerRating.background as GradientDrawable
                        textBackground.color = context.resources.getColorStateList(R.color.player_rating_great, null)
                    } else {
                        val textBackground = playerRating.background as GradientDrawable
                        textBackground.color = context.resources.getColorStateList(R.color.player_rating_normal, null)
                    }
                }
                // 교체 명단에 들어 있지만, 경기를 뛰지 않아 평점이 발생하지 않은 경우
                else if(id == teamRating.players[i].player.id && teamRating.players[i].statistics[0].games.rating == null) {
                    val textBackground = playerRating.background as GradientDrawable
                    textBackground.color = context.resources.getColorStateList(android.R.color.transparent, null)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColHolder {
        val inflater = LayoutInflater.from(context)
        return if (isSubsitute) {
            val binding = LineupSubstitutePlayerRecyclerItemBinding.inflate(inflater, parent, false)
            ColHolder(binding.root, binding.playerImageview, binding.playerNameTextview, binding.playerRating, binding.playerNumberTextview)
        } else {
            val binding = LineupPlayerRecyclerItemBinding.inflate(inflater, parent, false)
            ColHolder(binding.root, binding.playerImageview, binding.playerNameTextview, binding.playerRating, binding.playerNumberTextview)
        }
    }

    override fun onBindViewHolder(holder: ColHolder, position: Int) {
        holder.playerNameTextview.text = rowLineup[position].player.name
        holder.bindPlayerImage(rowLineup[position].player.id)
        holder.playerNumberTextview.text = rowLineup[position].player.number.toString()
        holder.bindPlayerRating(rowLineup[position].player.id)
    }

    override fun getItemCount(): Int {
        return rowLineup.size
    }
}