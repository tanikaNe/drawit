package com.gmail.weronikapios7.drawit

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
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.gmail.weronikapios7.drawit.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

//TODO modify dialogs to look nicer
//TODO add a slider to allow user change the size to whatever they want
//TODO finish share image
//TODO add eraser functionality
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawingViewClass: DrawingView
    private lateinit var brushDialog: Dialog
    private var imageBtnCurrentPaint: ImageButton? = null
    private var customProgressDialog: Dialog? = null

    private val openGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                binding.ivBackground.setImageURI(result.data?.data)
            }
        }

    private val requestPerms: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value

                if (isGranted) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                    val pickIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                } else {
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        drawingViewClass = binding.drawingView
        brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.brush_size_dialog)
        binding.drawingView.setBrushSize(20F)

        onClickBrushSelector()
        onClickGallery()
        onClickUndo()
        onClickRedo()
        onSave()
    }

    private fun onClickBrushSelector() {
        binding.ibBrush.setOnClickListener {
            brushDialog.window?.setGravity(Gravity.BOTTOM)

            showBrushSelector(brushDialog, R.id.ib_small_brush, 5F)
            showBrushSelector(brushDialog, R.id.ib_medium_brush, 10F)
            showBrushSelector(brushDialog, R.id.ib_large_brush, 15F)
            showBrushSelector(brushDialog, R.id.ib_extra_large_brush, 25f)
            brushDialog.show()

            findColorView(brushDialog)
        }
    }

    private fun onClickGallery() {
        binding.ibGallery.setOnClickListener {
            requestStoragePerms()
        }
    }

    private fun onClickUndo() {
        binding.ibUndo.setOnClickListener {
            drawingViewClass.onClickUndo()
        }
    }

    private fun onClickRedo() {
        binding.ibRepeat.setOnClickListener {
            drawingViewClass.onClickRedo()
        }
    }

    private fun onSave() {
        binding.ibDownload.setOnClickListener {
            if (isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch {
                    saveBitmapFile(getBitmapFromView(binding.flDrawingViewContainer))
                }
            }
        }
    }

    private fun showBrushSelector(dialog: Dialog, brushButtonId: Int, brushSize: Float) {

        val btn = dialog.findViewById<ImageButton>(brushButtonId)
        btn.setOnClickListener {
            drawingViewClass.setBrushSize(brushSize)
            dialog.dismiss()
        }

    }

    private fun paintClicked(view: View, dialog: Dialog) {
        //if (view !== imageBtnCurrentPaint) {
        val imageButton = view as ImageButton
        imageButton.setOnClickListener {
            val colorTag = imageButton.tag.toString()
            drawingViewClass.setColor(colorTag)
            imageBtnCurrentPaint = view
            dialog.dismiss()
        }

        // }
    }

    private fun findColorView(dialog: Dialog) {
        val colorsButtons =
            brushDialog.findViewById<LinearLayout>(R.id.paints_row_1).touchables.plus(
                brushDialog.findViewById<LinearLayout>(R.id.paints_row_2).touchables
            )

        colorsButtons.forEach { view ->
            if (view.tag != null) {
                paintClicked(view, dialog)
            }
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            ViewCompat.getWindowInsetsController(window.decorView) ?: return
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }


    private fun showRationaleDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        builder.create().show()
    }

    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        )

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePerms() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            showRationaleDialog("DrawIt!", "Needs to Access Your External Storage")
        } else {
            requestPerms.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE

                )
            )
        }
    }

    private fun getBitmapFromView(view: View) : Bitmap{
        val createdBitmap = Bitmap.createBitmap(
            view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(createdBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)
        return createdBitmap
    }

    private suspend fun saveBitmapFile(bitmap: Bitmap?): String{
        var result = ""
        withContext(Dispatchers.IO){
            if(bitmap !=null){
                try {
                    val bytes = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val file = File(externalCacheDir?.absoluteFile.toString()
                            + File.separator + "DrawIt_" + System.currentTimeMillis() /1000 + ".png"
                    )

                    val fileOutput = FileOutputStream(file)
                    fileOutput.write(bytes.toByteArray())
                    fileOutput.close()

                    result = file.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity, "File saved successfully :$result", Toast.LENGTH_SHORT)
                                .show()
                            shareImage(result)
                        }else{
                            Toast.makeText(this@MainActivity, "Something went wrong saving the file. Try again", Toast.LENGTH_SHORT).show()

                        }
                    }
                } catch (e:Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }

        return  result
    }

private fun showProgressDialog(){
    customProgressDialog = Dialog(this)

    /*Set the screen content from a layout resource. The resource will be
    inflated, adding all top-level view to the screen
     */
    customProgressDialog?.setContentView(R.layout.custom_dialog_progress)
    customProgressDialog?.show()
}

    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
            path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

}