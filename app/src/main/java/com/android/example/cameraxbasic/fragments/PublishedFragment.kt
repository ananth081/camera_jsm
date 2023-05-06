package com.android.example.cameraxbasic.fragments


import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.android.example.cameraxbasic.adapter.ImageRecyclerViewAdapter
import com.android.example.cameraxbasic.databinding.FragmentPublishedBinding
import com.android.example.cameraxbasic.viewmodel.DIRECTORY_NAME
import com.android.example.cameraxbasic.viewmodel.DataViewModel
import java.io.File
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.Q)

class PublishedFragment : Fragment() {
    val TAG = "PublishedFragment"
    lateinit var binding: FragmentPublishedBinding
    var adapter: ImageRecyclerViewAdapter? = null
    var fileList: List<File>? = null
    val typeDate = 100
    val typeMedia = 101
//    val dataViewModel: DataViewModel by activityViewModels()

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
        Thread().run {
            val filePath =
                File(Environment.getExternalStorageDirectory().path + File.separator + DIRECTORY_NAME)
            fileList = filePath.listFiles()?.toList()
            if (fileList?.size!! > 0) {

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
                adapter?.dataList?.add("March 26, 2023")
                adapter?.dataList?.addAll(fileList!!)
                adapter?.dataList?.add("March 28, 2023")
                adapter?.dataList?.addAll(fileList!!)
                Handler(Looper.getMainLooper()).post {
                    binding.galleryImage.adapter = adapter
                }
            }

        }

    }

    fun onMediaViewSelected(viewSelected: String) {
        val filePath =
            File(Environment.getExternalStorageDirectory().path + File.separator + DIRECTORY_NAME)
        fileList = filePath.listFiles()?.toList()
        if (fileList?.size!! > 0) {
            when (viewSelected) {
                "Day" -> {
                    adapter?.dataList?.clear()
                    adapter?.dataList?.add("March 26, 2023")
                    adapter?.dataList?.addAll(fileList!!)
                    adapter?.dataList?.add("March 28, 2023")
                    adapter?.dataList?.addAll(fileList!!)
                    adapter?.notifyDataSetChanged()
                }
                "Week" -> {
                    adapter?.dataList?.clear()
                    adapter?.dataList?.add("Mar 14 - 21, 2022")
                    adapter?.dataList?.addAll(fileList!!)
                    adapter?.dataList?.add("Mar 22 - 29, 2022")
                    adapter?.dataList?.addAll(fileList!!)
                    adapter?.notifyDataSetChanged()
                }
                "Month" -> {
                    adapter?.dataList?.clear()
                    adapter?.dataList?.add("Feburary 2023")
                    adapter?.dataList?.addAll(fileList!!)
                    adapter?.dataList?.add("March 2023")
                    adapter?.dataList?.addAll(fileList!!)
                    adapter?.notifyDataSetChanged()
                }
            }
        }
    }
}