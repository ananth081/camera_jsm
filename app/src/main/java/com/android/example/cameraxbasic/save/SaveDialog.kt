package com.android.example.cameraxbasic.save

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.camera.JsmGalleryActivity
import com.android.example.cameraxbasic.databinding.SaveDialogItemBinding

class SaveDialog : DialogFragment() {
    lateinit var binding: SaveDialogItemBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SaveDialogItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        fun newInstance(): SaveDialog {
            val args = Bundle()
            val fragment = SaveDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertOption = arrayOf("Publish Now", "Needs review")
        return activity?.let {
            val builder = AlertDialog.Builder(it, R.style.AlertDialogTheme)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            // builder.setView(inflater.inflate(R.layout.save_dialog_item, null))
            builder.setTitle("Do you want to publish this media now, or does it need to be reviewed")


            // Add action buttons
//                .setPositiveButton("Cancel"
//                ) { dialog, id ->
//                    // sign in the user ...
//                }
//                .setNegativeButton("Save") { dialog, id ->
//                    getDialog()?.cancel()
//                }

            builder.setSingleChoiceItems(
                alertOption,
                0
            ) { dialogInterface: DialogInterface, i: Int ->
                if (i == 0) {
                    binding.needsReviewRadioBt.isChecked = false
                } else {
                    binding.publishRadioBt.isChecked = false
                }

            }
            val intent = Intent(requireContext(), JsmGalleryActivity::class.java)
            builder.setPositiveButton(
                "Save"
            ) { dialog, id ->
                Log.d(
                    "PRS",
                    "id" + id + " binding.publishRadioBt.isChecked" + binding.publishRadioBt.isChecked
                )
                if (binding.publishRadioBt.isChecked) {
                    intent.putExtra("IS_PUBLISHED_SCREEN", true)
                } else {
                    intent.putExtra("IS_PUBLISHED_SCREEN", false)
                }
                startActivity(intent)
                getDialog()?.cancel()
            }.setNegativeButton("Cancel") { dialog, id ->
                getDialog()?.cancel()
//                    intent.putExtra("IS_PUBLISHED_SCREEN",true)
//                    startActivity(intent)
            }
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