package com.android.example.cameraxbasic.fragments


import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.android.example.cameraxbasic.adapter.ImageRecyclerViewAdapter
import com.android.example.cameraxbasic.databinding.FragmentPublishedBinding
import java.io.File

class PublishedFragment : Fragment() {
    val TAG = "PublishedFragment"
    lateinit var binding: FragmentPublishedBinding

    companion object {
        fun newInstance() = PublishedFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPublishedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        readImageFileFromStorage()
    }

    private fun readImageFileFromStorage() {
        val filePath = File(Environment.getExternalStorageDirectory().path + File.separator + "Download" + File.separator)

        val imagePath: MutableList<File>? = null
        val fileList = filePath.listFiles()?.toList()
        if(fileList!=null){
            for(i in 0..fileList.size){
                imagePath?.add(fileList[i])
            }
        }
//        for (i in 0..files.size) {
//           // if (files[i].name.endsWith(".jpg")) {
//                imagePath?.add(files[i])
//          //  }
//        }

            binding.galleryImage.layoutManager = GridLayoutManager(requireContext(), 4)
            val adapter = ImageRecyclerViewAdapter(fileList!!)
            binding.galleryImage.adapter = adapter



//        } else {
//            Log.d(TAG, "readImageFileFromStorage: " + "no images found")
//        }
    }
}