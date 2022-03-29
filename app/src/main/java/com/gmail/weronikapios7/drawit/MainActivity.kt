package com.gmail.weronikapios7.drawit

import android.Manifest
import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
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
import com.gmail.weronikapios7.drawit.databinding.ActivityMainBinding

//TODO modify dialogs to look nicer
//TODO add a selector to allow user change the size to whatever they want
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawingViewClass: DrawingView
    private lateinit var brushDialog: Dialog
    private var imageBtnCurrentPaint: ImageButton? = null

    private val openGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result ->
        if(result.resultCode == RESULT_OK && result.data!=null){
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


        binding.ibBrush.setOnClickListener {
            brushDialog.window?.setGravity(Gravity.BOTTOM)

            showBrushSelector(brushDialog, R.id.ib_small_brush, 5F)
            showBrushSelector(brushDialog, R.id.ib_medium_brush, 10F)
            showBrushSelector(brushDialog, R.id.ib_large_brush, 15F)
            showBrushSelector(brushDialog, R.id.ib_extra_large_brush, 25f)

            brushDialog.show()

            findColorView(brushDialog)
        }

        binding.ibGallery.setOnClickListener {
            requestStoragePerms()
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
                    Manifest.permission.READ_EXTERNAL_STORAGE
                    //TODO add writing external storage permission
                )
            )
        }
    }


}