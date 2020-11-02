package com.rkpandey.mymemory.creation

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rkpandey.mymemory.R
import com.rkpandey.mymemory.models.BoardSize
import com.rkpandey.mymemory.utils.EXTRA_BOARD_SIZE

class CreateActivity : AppCompatActivity() {

  private lateinit var rvImagePicker: RecyclerView
  private lateinit var etGameName: EditText
  private lateinit var btnSave: Button

  private lateinit var imagePickerAdapter: ImagePickerAdapter
  private lateinit var boardSize: BoardSize
  private val chosenImageUris = mutableListOf<Uri>()
  private var numImagesRequired = -1

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_create)

    rvImagePicker = findViewById(R.id.rvImagePicker)
    etGameName = findViewById(R.id.etGameName)
    btnSave = findViewById(R.id.btnSave)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
    numImagesRequired = boardSize.getNumPairs()
    supportActionBar?.title = "Choose pics (0 / $numImagesRequired)"

    imagePickerAdapter = ImagePickerAdapter(this, chosenImageUris, boardSize)
    rvImagePicker.adapter = imagePickerAdapter
    rvImagePicker.setHasFixedSize(true)
    rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
      return true
    }
    return super.onOptionsItemSelected(item)
  }
}