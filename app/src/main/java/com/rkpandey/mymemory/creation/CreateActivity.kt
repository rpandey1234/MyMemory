package com.rkpandey.mymemory.creation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.rkpandey.mymemory.R
import com.rkpandey.mymemory.models.BoardSize
import com.rkpandey.mymemory.utils.EXTRA_BOARD_SIZE

class CreateActivity : AppCompatActivity() {

  private lateinit var boardSize: BoardSize
  private var numImagesRequired = -1

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_create)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
    numImagesRequired = boardSize.getNumPairs()
    supportActionBar?.title = "Choose pics (0 / $numImagesRequired)"
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
      return true
    }
    return super.onOptionsItemSelected(item)
  }
}