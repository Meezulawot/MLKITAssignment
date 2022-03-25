package com.meezu.mlkitassignment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.CalendarContract
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files.size
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var REQUEST_GALLERY_CODE = 0
    private var REQUEST_CAMERA_CODE = 1
    private var imageUrl: String? = null

    private val permissions = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initListener()
    }

    private fun initListener(){
        btnImage.setOnClickListener(this)
        //check for permission
        if (!hasPermission()) {
            requestPermission()
        }

    }

    override fun onClick(p0: View?) {
        when(p0){
            btnImage->{
                loadPopUpMenu()
            }
        }
    }

    private fun requestPermission(){
        ActivityCompat.requestPermissions(
            this,
            permissions,
            1)
    }

    private fun hasPermission(): Boolean{
        var hasPermission = true
        for(permission in permissions){
            if(ActivityCompat.checkSelfPermission(
                    this,
                    permission
                )!= PackageManager.PERMISSION_GRANTED
            ){
                hasPermission = false
            }
        }
        return hasPermission
    }


//    private fun uploadImage() {
//        if (imageUrl != null) {
//            val file = File(imageUrl!!)
//            val reqFile =
//                RequestBody.create(MediaType.parse("image/jpeg"), file)
//
//        }
//    }

    private fun detectFaces(image: InputImage){
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

        val detector = FaceDetection.getClient(options)

        val result = detector.process(image)
            .addOnSuccessListener { faces ->
                processFaceList(faces)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun processFaceList(faces: List<Face>) {
        val paint = Paint()

        for (face in faces) {
            val bounds = face.boundingBox
            val rotX = face.headEulerAngleX
            val rotY = face.headEulerAngleY

            // Calculate positions.
            val left = rotX - (face.boundingBox.width() / 2.0f)
            val top = rotY - (face.boundingBox.height() / 2.0f)
            val right = rotX + (face.boundingBox.width() / 2.0f)
            val bottom = rotY + (face.boundingBox.height() / 2.0f)

            // Calculate width and height of label box
//            var textWidth = idPaints[colorID].measureText("ID: " + face.trackingId)
//            if (face.smilingProbability != null) {
//                yLabelOffset -= lineHeight
//                textWidth =
//                    max(
//                        textWidth,
//                        idPaints[colorID].measureText(
//                            String.format(Locale.US, "Happiness: %.2f", face.smilingProbability)
//                        )
//                    )
//            }

        }

    }

    @SuppressLint("SimpleDateFormat")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_GALLERY_CODE && data != null) {
                val selectedImage = data.data
                try{
                    val imageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
                    val file = bitmapToFile(imageBitmap, "$timeStamp.jpg")!!.absolutePath
                    imgView.setImageBitmap(BitmapFactory.decodeFile(file))
                    val image = InputImage.fromBitmap(imageBitmap,0)
                    detectFaces(image)

                }catch(e: IOException){
                    e.printStackTrace()
                }
            } else if (requestCode == REQUEST_CAMERA_CODE && data != null) {
                val imageBitmap = data.extras?.get("data") as Bitmap
                val file = bitmapToFile(imageBitmap, "$timeStamp.jpg")!!.absolutePath
                imgView.setImageBitmap(BitmapFactory.decodeFile(file))
                val image = InputImage.fromBitmap(imageBitmap, 0)
                detectFaces(image)
            }
        }
    }

    private fun bitmapToFile(
        bitmap: Bitmap,
        fileNameToSave: String
    ): File? {
        var file: File? = null
        return try {
            file = File(
                this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    .toString() + File.separator + fileNameToSave
            )
            file.createNewFile()
            //Convert bitmap to byte array
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos) // YOU can also save it in JPEG
            val bitMapData = bos.toByteArray()
            //write the bytes in file
            val fos = FileOutputStream(file)
            fos.write(bitMapData)
            fos.flush()
            fos.close()
            file
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            file // it will return null
        }
    }

    private fun loadPopUpMenu() {
        val popupMenu = PopupMenu(this, imgView)
        popupMenu.menuInflater.inflate(R.menu.gallery_camera, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuCamera ->
                    openCamera()
                R.id.menuGallery ->
                    openGallery()
            }
            true
        }
        popupMenu.show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_GALLERY_CODE)
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, REQUEST_CAMERA_CODE)
    }
}