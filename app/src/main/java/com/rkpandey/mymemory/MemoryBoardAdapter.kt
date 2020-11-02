package com.rkpandey.mymemory

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.rkpandey.mymemory.models.BoardSize
import kotlin.math.min

class MemoryBoardAdapter(
  private val context: Context,
  private val boardSize: BoardSize,
  private val cardImages: List<Int>
) :
  RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

  companion object {
    private const val TAG = "MemoryBoardAdapter"
    private const val MARGIN_SIZE = 10
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val cardWidth = parent.width / 2 - 2 * MARGIN_SIZE
    val cardHeight = parent.height / 4 - 2 * MARGIN_SIZE
    val cardSideLength = min(cardWidth, cardHeight)
    val view = LayoutInflater.from(context).inflate(R.layout.memory_card, parent, false)
    val layoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
    layoutParams.width = cardSideLength
    layoutParams.height = cardSideLength
    layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
    // Set the appropriate width/height of the view
    return ViewHolder(view)
  }

  override fun getItemCount() = boardSize.numCards

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(position)
  }

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)

    fun bind(position: Int) {
      imageButton.setImageResource(cardImages[position])
      imageButton.setOnClickListener {
        Log.i(TAG, "Clicked on position $position")
      }
    }

  }
}
