package com.android.example.cameraxbasic.fragments


import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.adapter.ImageRecyclerViewAdapter
import com.android.example.cameraxbasic.camera.JsmGalleryActivity
import com.android.example.cameraxbasic.databinding.FragmentPublishedBinding
import java.io.File
import kotlin.math.roundToInt

class PublishedFragment : Fragment() {
    val TAG = "PublishedFragment"
    val DIRECTORY_NAME = "/Pictures/JSM Analysis"
    lateinit var binding: FragmentPublishedBinding
    var adapter:ImageRecyclerViewAdapter? =null
    var fileList:List<File>? = null
    val typeDate = 100
    val typeMedia = 101

    companion object {
        fun newInstance() = PublishedFragment()
    }

    enum class GalleryList {
        DATE,
        MEDIA
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

        val filePath =
            File(Environment.getExternalStorageDirectory().path + File.separator + DIRECTORY_NAME)
         fileList = filePath.listFiles()?.toList()
        if(fileList?.size!!>0) {

            adapter = ImageRecyclerViewAdapter()
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels / displayMetrics.density
            val noOfColumns = (screenWidth / 100 + 0.9).roundToInt() - 1
            val glm = GridLayoutManager(
                context, noOfColumns
            )
            glm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (adapter?.getItemViewType(position) == typeDate) {
                        noOfColumns
                    } else 1
                }
            }
            binding.galleryImage.layoutManager = glm
            adapter?.getGalleryView?.add("March 26, 2023")
            adapter?.getGalleryView?.addAll(fileList!!)
            adapter?.getGalleryView?.add("March 28, 2023")
            adapter?.getGalleryView?.addAll(fileList!!)
            binding.galleryImage.adapter = adapter
        }

    }

    fun onMediaViewSelected(viewSelected:String){
        val filePath = File(Environment.getExternalStorageDirectory().path + File.separator + DIRECTORY_NAME)
        fileList = filePath.listFiles()?.toList()
        if(fileList?.size!!>0) {
            when (viewSelected) {
                "Day" -> {
                    adapter?.getGalleryView?.clear()
                    adapter?.getGalleryView?.add("March 26, 2023")
                    adapter?.getGalleryView?.addAll(fileList!!)
                    adapter?.getGalleryView?.add("March 28, 2023")
                    adapter?.getGalleryView?.addAll(fileList!!)
                    adapter?.notifyDataSetChanged()
                }
                "Week" -> {
                    adapter?.getGalleryView?.clear()
                    adapter?.getGalleryView?.add("Mar 14 - 21, 2022")
                    adapter?.getGalleryView?.addAll(fileList!!)
                    adapter?.getGalleryView?.add("Mar 22 - 29, 2022")
                    adapter?.getGalleryView?.addAll(fileList!!)
                    adapter?.notifyDataSetChanged()
                }
                "Month" -> {
                    adapter?.getGalleryView?.clear()
                    adapter?.getGalleryView?.add("Feburary 2023")
                    adapter?.getGalleryView?.addAll(fileList!!)
                    adapter?.getGalleryView?.add("March 2023")
                    adapter?.getGalleryView?.addAll(fileList!!)
                    adapter?.notifyDataSetChanged()
                }
            }
        }
    }
}