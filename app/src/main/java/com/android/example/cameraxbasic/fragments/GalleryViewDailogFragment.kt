package com.android.example.cameraxbasic.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.databinding.GalleryViewBinding

class GalleryViewDailogFragment: DialogFragment() {
    interface selectedView{
        fun getSelectedView(selectedView: String)
    }

    var viewSelected = ""
    lateinit var binding:GalleryViewBinding
    var listener:selectedView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = GalleryViewBinding.inflate(inflater,container,false)
        binding.toolbar.navigationIcon = resources.getDrawable(R.drawable.ic_back_arrow)
        binding.toolbar.title = "Views"
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.checkbox1.setOnClickListener {
            viewSelected = "Day"
            binding.checkbox3.isChecked = false
            binding.checkbox2.isChecked = false

        }

        binding.checkbox2.setOnClickListener {
            viewSelected = "Week"
            binding.checkbox1.isChecked = false
            binding.checkbox3.isChecked = false

        }

        binding.checkbox3.setOnClickListener {
            viewSelected = "Month"
            binding.checkbox1.isChecked = false
            binding.checkbox2.isChecked = false

        }

        binding.save.setOnClickListener {
            if(viewSelected.isNotEmpty()){
                listener?.getSelectedView(viewSelected)
                dismiss()
            }
        }
        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }
    }

    fun initialiseObject(obj:selectedView){
        this.listener = obj
    }


}