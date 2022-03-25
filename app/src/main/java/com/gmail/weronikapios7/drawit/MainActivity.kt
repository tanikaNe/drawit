package com.gmail.weronikapios7.drawit

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.gmail.weronikapios7.drawit.databinding.ActivityMainBinding

//TODO modify dialogs to look nicer
//TODO add a selector to allow user change the size to whatever they want
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawingViewClass: DrawingView
    private lateinit var brushDialog: Dialog
    private var imageBtnCurrentPaint: ImageButton? = null
    private val idRegx by lazy { Regex("color_[a-z]*").toPattern() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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


}