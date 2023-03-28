package com.android.example.cameraxbasic.fragments


import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.adapter.ImageRecyclerViewAdapter
import com.android.example.cameraxbasic.databinding.FragmentPublishedBinding
import java.io.File

class PublishedFragment : Fragment() {
    val TAG = "PublishedFragment"
    lateinit var binding: FragmentPublishedBinding
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

        // add images to Download and Download2 folder
        val appName = requireActivity().resources.getString(R.string.app_name)
        val DIRECTORY_NAME = "%Pictures/$appName%"

        val filePath = File(Environment.getExternalStorageDirectory().path + File.separator + DIRECTORY_NAME + File.separator)

        val imagePath: MutableList<File>? = null
        val fileList = filePath.listFiles()?.toList()
        if (fileList != null) {
            for (i in 0..fileList.size) {
                imagePath?.add(fileList[i])
            }
        }
        val filePath2 = File(Environment.getExternalStorageDirectory().path + File.separator + DIRECTORY_NAME + File.separator)
        val fileList2 = filePath2.listFiles()?.toList()

        val hashMap = HashMap<String, List<File>?>()


        // val adapter = ImageRecyclerViewAdapter(fileList!!)
        val dateList = listOf<String>("23 Oct", "24 Oct")

        val adapter = ImageRecyclerViewAdapter(fileList!!, dateList)
        val glm = GridLayoutManager(
            context, 4
        )
        glm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter.getItemViewType(position) == typeDate) {
                    4
                } else 1
            }
        }
        binding.galleryImage.layoutManager = glm
        adapter.getGalleryView.add("26 March 2023")
        adapter.getGalleryView.addAll(fileList)
        adapter.getGalleryView.add("28 March 2023")
        adapter.getGalleryView.addAll(fileList2!!)
        // adapter.items = fileList
        binding.galleryImage.adapter = adapter


//        } else {
//            Log.d(TAG, "readImageFileFromStorage: " + "no images found")
//        }
    }
}