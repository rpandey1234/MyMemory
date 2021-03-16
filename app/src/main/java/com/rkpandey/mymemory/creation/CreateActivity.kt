package com.rkpandey.mymemory.creation

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.storage.ktx.storage
import com.rkpandey.mymemory.R
import com.rkpandey.mymemory.models.BoardSize
import com.rkpandey.mymemory.utils.BitmapScaler
import com.rkpandey.mymemory.utils.EXTRA_BOARD_SIZE
import com.rkpandey.mymemory.utils.EXTRA_GAME_NAME
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {

  companion object {
    private const val TAG = "CreateActivity"
    private const val PICK_PHOTO_CODE = 655
  }

  private lateinit var rvImagePicker: RecyclerView
  private lateinit var etGameName: EditText
  private lateinit var btnSave: Button
  private lateinit var pbUploading: ProgressBar

  private lateinit var imagePickerAdapter: ImagePickerAdapter
  private lateinit var boardSize: BoardSize
  private val chosenImageUris = mutableListOf<Uri>()
  private var numImagesRequired = -1
  private val firebaseAnalytics = Firebase.analytics
  private val remoteConfig = Firebase.remoteConfig
  private val storage = Firebase.storage
  private val db = Firebase.firestore

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_create)

    rvImagePicker = findViewById(R.id.rvImagePicker)
    etGameName = findViewById(R.id.etGameName)
    btnSave = findViewById(R.id.btnSave)
    pbUploading = findViewById(R.id.pbUploading)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
    numImagesRequired = boardSize.getNumPairs()
    supportActionBar?.title = "Choose pics (0 / $numImagesRequired)"

    btnSave.setOnClickListener {
      saveDataToFirebase()
    }
    etGameName.filters = arrayOf(InputFilter.LengthFilter(14))
    etGameName.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable?) {
        btnSave.isEnabled = shouldEnableSaveButton()
      }

      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
    imagePickerAdapter = ImagePickerAdapter(this, chosenImageUris, boardSize, object: ImagePickerAdapter.ImageClickListener {
      override fun onPlaceholderClicker() {
        launchIntentForPhotos()
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

  private fun saveDataToFirebase() {
    Log.i(TAG, "Going to save data to Firebase")
    val customGameName = etGameName.text.toString().trim()
    firebaseAnalytics.logEvent("creation_save_attempt") {
      param("game_name", customGameName)
    }
    btnSave.isEnabled = false
    db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
      if (document != null && document.data != null) {
        AlertDialog.Builder(this)
          .setTitle("Name taken")
          .setMessage("A game already exists with the name '$customGameName'. Please choose another")
          .setPositiveButton("OK", null)
          .show()
        btnSave.isEnabled = true
      } else {
        handleImageUploading(customGameName)
      }
    }.addOnFailureListener {exception ->
      Log.e(TAG, "Encountered error while saving memory game", exception)
      Toast.makeText(this, "Encountered error while saving memory game", Toast.LENGTH_SHORT).show()
    }
  }

  private fun handleImageUploading(gameName: String) {
    pbUploading.visibility = View.VISIBLE
    val uploadedImageUrls = mutableListOf<String>()
    var didEncounterError = false
    for ((index, photoUri) in chosenImageUris.withIndex()) {
      val imageByteArray = getImageByteArray(photoUri)
      val filePath = "images/$gameName/${System.currentTimeMillis()}-${index}.jpg"
      val photoReference = storage.reference.child(filePath)
      photoReference.putBytes(imageByteArray)
        .continueWithTask { photoUploadTask ->
          Log.i(TAG, "uploaded bytes: ${photoUploadTask.result?.bytesTransferred}")
          photoReference.downloadUrl
        }.addOnCompleteListener { downloadUrlTask ->
          if (!downloadUrlTask.isSuccessful) {
            Log.e(TAG, "Exception with Firebase storage", downloadUrlTask.exception)
            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
            didEncounterError = true
            return@addOnCompleteListener
          }
          if (didEncounterError) {
            pbUploading.visibility = View.GONE
            return@addOnCompleteListener
          }
          pbUploading.progress = uploadedImageUrls.size * 100 / chosenImageUris.size
          val downloadUrl = downloadUrlTask.result.toString()
          uploadedImageUrls.add(downloadUrl)
          Log.i(TAG, "Finished uploading $photoUri, Num uploaded: ${uploadedImageUrls.size}")
          if (uploadedImageUrls.size == chosenImageUris.size) {
            handleAllImagesUploaded(gameName, uploadedImageUrls)
          }
        }
    }
  }

  private fun handleAllImagesUploaded(gameName: String, imageUrls: MutableList<String>) {
    db.collection("games").document(gameName)
      .set(mapOf("images" to imageUrls))
      .addOnCompleteListener { gameCreationTask ->
        pbUploading.visibility = View.GONE
        if (!gameCreationTask.isSuccessful) {
          Log.e(TAG, "Exception with game creation", gameCreationTask.exception)
          Toast.makeText(this, "Failed game creation", Toast.LENGTH_SHORT).show()
          return@addOnCompleteListener
        }
        firebaseAnalytics.logEvent("creation_save_success") {
          param("game_name", gameName)
        }
        Log.i(TAG, "Successfully created game $gameName")
        AlertDialog.Builder(this)
          .setTitle("Upload complete! Let's play your game '$gameName'")
          .setPositiveButton("OK") { _, _ ->
            val resultData = Intent()
            resultData.putExtra(EXTRA_GAME_NAME, gameName)
            setResult(Activity.RESULT_OK, resultData)
            finish()
          }.show()
      }
  }

  private fun getImageByteArray(photoUri: Uri): ByteArray {
    val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val source = ImageDecoder.createSource(contentResolver, photoUri)
      ImageDecoder.decodeBitmap(source)
    } else {
      MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
    }
    Log.i(TAG, "Original width ${originalBitmap.width} and height ${originalBitmap.height}")
    val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap, remoteConfig.getLong("scaled_height").toInt())
    Log.i(TAG, "Scaled width ${scaledBitmap.width} and height ${scaledBitmap.height}")
    val byteOutputStream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, remoteConfig.getLong("compress_quality").toInt(), byteOutputStream)
    return byteOutputStream.toByteArray()
  }

  private fun shouldEnableSaveButton(): Boolean {
    if (chosenImageUris.size != numImagesRequired) {
      return false
    }
    if (etGameName.text.isBlank() || etGameName.text.length < 3) {
      return false
    }
    return true
  }

  private fun launchIntentForPhotos() {
    val intent = Intent(Intent.ACTION_PICK)
    intent.type = "image/*"
    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    startActivityForResult(Intent.createChooser(intent, "Choose pics"), PICK_PHOTO_CODE)
  }
}