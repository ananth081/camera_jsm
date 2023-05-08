package com.android.example.cameraxbasic.fragments


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.android.example.cameraxbasic.ImageDetailActivity
import com.android.example.cameraxbasic.adapter.ImageRecyclerViewAdapter
import com.android.example.cameraxbasic.camera.JsmGalleryActivity
import com.android.example.cameraxbasic.camera.VideoActivity
import com.android.example.cameraxbasic.databinding.FragmentPublishedBinding
import com.android.example.cameraxbasic.utils.IMAGE_URI_STRING_KEY
import com.android.example.cameraxbasic.utils.MEDIA_TYPE_KEY
import com.android.example.cameraxbasic.utils.MediaStoreFile
import com.android.example.cameraxbasic.viewmodels.DRAFT
import com.android.example.cameraxbasic.viewmodels.GalleryViewModel
import java.io.File
import kotlin.math.roundToInt


class PublishedFragment : Fragment(), ImageRecyclerViewAdapter.ItemClickListenr {
    val TAG = "PublishedFragment"
    lateinit var binding: FragmentPublishedBinding
    var adapter: ImageRecyclerViewAdapter? = null
    var fileList: List<File>? = null
    val typeDate = 100
    val typeMedia = 101
    val dataViewModel: GalleryViewModel by viewModels()

    companion object {
        fun newInstance(type: String): Fragment {
            val fragment = PublishedFragment()
            val bundle = Bundle()
            bundle.putString(MEDIA_TYPE_KEY, type)
            fragment.arguments = bundle
            return fragment
        }
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
        extractArguments()
        readImageFileFromStorage()
        dataViewModel.communicator.observe(viewLifecycleOwner) { list ->
            if (type == DRAFT) {
                val draftItemSize = list.filterIsInstance<MediaStoreFile>().toList().size
                (activity as JsmGalleryActivity).updateText(draftItemSize)
            }

            adapter = ImageRecyclerViewAdapter()
            adapter?.setClickListner(this)
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
            adapter?.dataList?.addAll(list!!)
//            adapter?.dataList?.add("March 28, 2023")
//            adapter?.dataList?.addAll(list)
            binding.galleryImage.adapter = adapter
        }
    }

    lateinit var type: String
    private fun extractArguments() {
        type = arguments?.getString(MEDIA_TYPE_KEY)!!
    }


    private fun readImageFileFromStorage() {
        dataViewModel.loadImages(requireContext(), type)

    }

    override fun onImageItemClick(uri: String) {
        val intent = Intent(requireContext(), ImageDetailActivity::class.java)
        intent.putExtra(IMAGE_URI_STRING_KEY, uri)
        startActivity(intent)
    }

    override fun onVideoItemClick(uri: String) {
        val intent = Intent(requireContext(), VideoActivity::class.java)
        intent.putExtra("video_uri", Uri.parse(uri))
        startActivity(intent)
    }

    fun refresh(filterType: Int) {
        dataViewModel.loadImages(requireContext(), type, filterType)
    }

//    fun onMediaViewSelected(viewSelected: String) {
//        val filePath =
//            File(Environment.getExternalStorageDirectory().path + File.separator + DIRECTORY_NAME)
//        fileList = filePath.listFiles()?.toList()
//        if (fileList?.size!! > 0) {
//            when (viewSelected) {
//                "Day" -> {
//                    adapter?.dataList?.clear()
//                    adapter?.dataList?.add("March 26, 2023")
//                    adapter?.dataList?.addAll(fileList!!)
//                    adapter?.dataList?.add("March 28, 2023")
//                    adapter?.dataList?.addAll(fileList!!)
//                    adapter?.notifyDataSetChanged()
//                }
//                "Week" -> {
//                    adapter?.dataList?.clear()
//                    adapter?.dataList?.add("Mar 14 - 21, 2022")
//                    adapter?.dataList?.addAll(fileList!!)
//                    adapter?.dataList?.add("Mar 22 - 29, 2022")
//                    adapter?.dataList?.addAll(fileList!!)
//                    adapter?.notifyDataSetChanged()
//                }
//                "Month" -> {
//                    adapter?.dataList?.clear()
//                    adapter?.dataList?.add("Feburary 2023")
//                    adapter?.dataList?.addAll(fileList!!)
//                    adapter?.dataList?.add("March 2023")
//                    adapter?.dataList?.addAll(fileList!!)
//                    adapter?.notifyDataSetChanged()
//                }
//            }
//        }
//    }
}