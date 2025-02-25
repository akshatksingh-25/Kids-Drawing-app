package com.example.kidsdrawing

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var customProgressDialog: Dialog? = null

    private var drawingView : DrawingView? = null
    private var mImageButtonCurrentPaint : ImageButton? = null

    val openGalleryLauncher : ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if (result.resultCode == RESULT_OK && result.data!=null){
                val imageBackground = findViewById<ImageView>(R.id.iv_background)
                //URI is like location on the device (C:\Users\aksha\Downloads\VIT Downloads\Syllabus) like this path
                // here we get result.data? i.e is location and then its data
                imageBackground.setImageURI(result.data?.data)
            }
        }


    val requestPermission : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions->
            permissions.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted){
                    Toast.makeText(this,"Permission Granted for $permissionName",Toast.LENGTH_LONG).show()

                    val pickIntent = Intent(
                        Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    )
                    openGalleryLauncher.launch(pickIntent)

                }else{
                    when (permissionName) {
                        Manifest.permission.READ_MEDIA_IMAGES ->
                            Toast.makeText(this, "Permission Images Denied", Toast.LENGTH_LONG).show()
                        Manifest.permission.READ_MEDIA_AUDIO ->
                            Toast.makeText(this, "Permission Audio Denied", Toast.LENGTH_LONG).show()
                        Manifest.permission.READ_MEDIA_VIDEO ->
                            Toast.makeText(this, "Permission Video Denied", Toast.LENGTH_LONG).show()
                        // TODO - Add more permissions as needed
                    }

                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(8.toFloat())

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)

        mImageButtonCurrentPaint = linearLayoutPaintColors[0] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
        )

        val ibBrush : ImageButton = findViewById(R.id.ib_brush)
        ibBrush.setOnClickListener{
            showBrushSizeChooserDialog()
        }

        val ibImagePick : ImageButton = findViewById(R.id.ib_image_pick)
        ibImagePick.setOnClickListener{
            requestStoragePermission()
        }

        val ibUndo =findViewById<ImageButton>(R.id.ib_image_undo)
        ibUndo.setOnClickListener{
            drawingView?.onClickUndo()
        }

        val ibRedo = findViewById<ImageButton>(R.id.ib_image_redo)
        ibRedo.setOnClickListener{
            drawingView?.onClickRedo()
        }

        val ibEraser = findViewById<ImageButton>(R.id.ib_image_erase)
        ibEraser.setOnClickListener{
            drawingView?.setEraser()
            drawingView?.setEraserSize(20.toFloat())
        }

        val ibSave = findViewById<ImageButton>(R.id.ib_image_save)
        ibSave.setOnClickListener{

            if(isReadStorageAllowed()){
                lifecycleScope.launch {
                    showProgressDialog()
                    val flDrawingView = findViewById<FrameLayout>(R.id.fl_drawing_view_container)
                    val myBitmap : Bitmap = getBitmapFromView(flDrawingView)
                    saveBitmapFile(myBitmap)
                }
            }
        }
    }



    private fun requestStoragePermission() {
        val permissions = arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO
            // TODO - Add writing external storage permission
        )

        if(permissions.any {
            ActivityCompat.shouldShowRequestPermissionRationale(this,it)
            }){
            showRationaleDialog("Kids Drawing App", "Kids Drawing App "+"needs to access your Permissions")
        }
        else{
            requestPermission.launch(permissions)
        }
    }


    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this@MainActivity)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("___Brush Size___")

        val smallButton = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        smallButton.setOnClickListener{
            drawingView?.setSizeForBrush(8.toFloat())
            brushDialog.dismiss()
        }

        val mediumButton = brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        mediumButton.setOnClickListener{
            drawingView?.setSizeForBrush(16.toFloat())
            brushDialog.dismiss()
        }

        val largeButton : ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeButton.setOnClickListener{
            drawingView?.setSizeForBrush(25.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }



    fun paintClicked(view : View){
//        Toast.makeText(this@MainActivity,"Clicked",Toast.LENGTH_SHORT).show()
        if(view !== mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
            )

            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )

            mImageButtonCurrentPaint = view
        }
    }


    private fun showRationaleDialog(title: String, message: String) {
        val builder : AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") {
                    dialog,_->
                dialog.dismiss()
            }
        builder.create().show()
    }

    // since what we are seeing is a view and we need bitmap to store the image
    private fun getBitmapFromView(view : View) : Bitmap{
        val returnedBitmap = Bitmap.createBitmap(
            view.width,
            view.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable!=null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }


    private suspend fun saveBitmapFile(mBitmap: Bitmap?) : String{
        var result=""
        withContext(Dispatchers.IO){
            if(mBitmap!=null){
                try{
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90, bytes)

                    val f = File(externalCacheDir?.absoluteFile.toString() + File.separator
                    + "KidDrawingApp_" + System.currentTimeMillis()/1000
                    + ".png"
                    )
                    val fo=FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath
                    runOnUiThread {
                        cancelProgressDialog()
                        if(result.isNotEmpty()){
                            Toast.makeText(
                                this@MainActivity,
                                "File saved Successfully : $result",
                                Toast.LENGTH_SHORT
                            ).show()
                            shareImage(result)
                        }else{
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong while saving the file",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                catch (e : Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }



    private fun isReadStorageAllowed(): Boolean {
        val readImagesPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        val readAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        val readVideoPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED

        return readImagesPermission && readAudioPermission && readVideoPermission
    }



    /**
     * Method is used to show the Custom Progress Dialog.
     */
    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@MainActivity)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        //Start the dialog and display it on screen.
        customProgressDialog?.show()
    }


    /**
     * This function is used to dismiss the progress dialog if it is visible to user.
     */
    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }


    // Here result is the path
    private fun shareImage(result : String){
        MediaScannerConnection.scanFile(this, arrayOf(result),null){
            path , uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent,"Share"))
        }
    }




}