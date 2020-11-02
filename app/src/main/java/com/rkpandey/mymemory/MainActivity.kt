package com.rkpandey.mymemory

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rkpandey.mymemory.models.BoardSize
import com.rkpandey.mymemory.utils.DEFAULT_ICONS

class MainActivity : AppCompatActivity() {
  
  private lateinit var rvBoard: RecyclerView
  private lateinit var tvNumMoves: TextView
  private lateinit var tvNumPairs: TextView

  private var boardSize = BoardSize.EASY

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    rvBoard = findViewById(R.id.rvBoard)
    tvNumMoves = findViewById(R.id.tvNumMoves)
    tvNumPairs = findViewById(R.id.tvNumPairs)

    val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
    val randomizedImages = (chosenImages + chosenImages).shuffled()

    rvBoard.adapter = MemoryBoardAdapter(this, boardSize, randomizedImages)
    rvBoard.setHasFixedSize(true)
    rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())
  }
}