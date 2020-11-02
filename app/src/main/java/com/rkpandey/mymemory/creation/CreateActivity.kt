package com.rkpandey.mymemory.creation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rkpandey.mymemory.R
import com.rkpandey.mymemory.models.BoardSize
import com.rkpandey.mymemory.utils.EXTRA_BOARD_SIZE
import com.rkpandey.mymemory.utils.isPermissionGranted
import com.rkpandey.mymemory.utils.requestPermission

class CreateActivity : AppCompatActivity() {

  companion object {
    private const val TAG = "CreateActivity"
    private const val PICK_PHOTO_CODE = 655
    private const val READ_EXTERNAL_PHOTOS_CODE = 248
    private const val READ_PHOTOS_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
  }

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

    imagePickerAdapter = ImagePickerAdapter(this, chosenImageUris, boardSize, object: ImagePickerAdapter.ImageClickListener {
      override fun onPlaceholderClicker() {
        // Need READ_EXTERNAL_FILES permission
        if (isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)) {
          launchIntentForPhotos()
        } else {
          requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTOS_CODE)
        }
      }
    })
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

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    if (requestCode == READ_EXTERNAL_PHOTOS_CODE) {
      if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        launchIntentForPhotos()
      } else {
        Toast.makeText(this, "In order to create a custom game, you need to provide access to your photos", Toast.LENGTH_LONG).show()
      }
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null) {
      Log.w(TAG, "Did not get data back from the launched activity, user likely canceled flow")
      return
    }
    Log.i(TAG, "onActivityResult")
    val selectedUri = data.data
    val clipData = data.clipData
    if (clipData != null) {
      Log.i(TAG, "clipData numImages ${clipData.itemCount}: $clipData")
      for (i in 0 until clipData.itemCount) {
        val clipItem = clipData.getItemAt(i)
        if (chosenImageUris.size < numImagesRequired) {
          chosenImageUris.add(clipItem.uri)
        }
      }
    } else if (selectedUri != null) {
      Log.i(TAG, "data: $selectedUri")
      chosenImageUris.add(selectedUri)
    }
    imagePickerAdapter.notifyDataSetChanged()
    supportActionBar?.title = "Choose pics (${chosenImageUris.size} / $numImagesRequired)"
    btnSave.isEnabled = shouldEnableSaveButton()
  }

  private fun shouldEnableSaveButton(): Boolean {
    // Check if we should enable save button or not
    return true
  }

  private fun launchIntentForPhotos() {
    val intent = Intent(Intent.ACTION_PICK)
    intent.type = "image/*"
    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    startActivityForResult(Intent.createChooser(intent, "Choose pics"), PICK_PHOTO_CODE)
  }
}