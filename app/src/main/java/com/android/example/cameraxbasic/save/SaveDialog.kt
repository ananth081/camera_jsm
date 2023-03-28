package com.android.example.cameraxbasic.save

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.android.example.cameraxbasic.R

class SaveDialog : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    companion object {
        fun newInstance(): SaveDialog{
            val args = Bundle()
            val fragment = SaveDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(inflater.inflate(R.layout.save_dialog_item, null))
                // Add action buttons
//                .setPositiveButton("Cancel"
//                ) { dialog, id ->
//                    // sign in the user ...
//                }
//                .setNegativeButton("Save") { dialog, id ->
//                    getDialog()?.cancel()
//                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
//        if (dialog != null) {
//            val width = getWidth()
//            val height = getHeight()
//            dialog.window!!.setLayout(width, height)
//        }
    }


//    override fun getWidth(): Int {
//        return if (!isTablet()) {
//            ViewGroup.LayoutParams.MATCH_PARENT
//        } else {
//            activity?.getWidthInPixel(resources.getInteger(R.integer.dialog_width))
//                ?: ViewGroup.LayoutParams.MATCH_PARENT
//        }
//    }
//
//    override fun getHeight(): Int {
//        return if (!isTablet()) {
//            ViewGroup.LayoutParams.MATCH_PARENT
//        } else {
//            return activity?.getHeightInPixel(resources.getInteger(R.integer.dialog_height))
//                ?: ViewGroup.LayoutParams.MATCH_PARENT
//        }
//    }

}