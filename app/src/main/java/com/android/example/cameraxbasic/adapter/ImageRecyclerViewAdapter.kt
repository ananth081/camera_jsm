package com.android.example.cameraxbasic.adapter

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.example.cameraxbasic.databinding.ItemDateBinding
import com.android.example.cameraxbasic.databinding.ItemImageBinding
import com.android.example.cameraxbasic.utils.FILE_TYPE_IMAGE
import com.android.example.cameraxbasic.utils.MediaStoreFile

class ImageRecyclerViewAdapter() :
    RecyclerView.Adapter<ImageRecyclerViewAdapter.ImageViewHolder>() {

    interface ItemClickListenr {
        fun onImageItemClick(uri: String)
        fun onVideoItemClick(uri: String)
    }

    val typeDate = 100
    val typeMedia = 101
    var dataList: MutableList<Any> = emptyList<Any>().toMutableList()
    var listener: ItemClickListenr? = null


    fun setClickListner(clickListenr: ItemClickListenr) {
        this.listener = clickListenr

    }

    override fun getItemViewType(position: Int): Int {
        val listItem = dataList.elementAt(position)
        return when (listItem) {
            is String -> typeDate
            else -> typeMedia
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val dateBinding =
            ItemDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return if (viewType == typeDate) {
            ImageViewHolder(binding, dateBinding, dateBinding.root)
        } else {
            ImageViewHolder(binding, dateBinding, binding.root)
        }
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        //holder.view.galleryImage.setImageURI(imageList[position]?.toUri())
        val listItem = dataList.elementAt(position)
        if (listItem is String) {
            bindDateView(holder, position, dataList)
        } else {
            bindMediaView(holder, position, dataList)
        }
    }


    private fun bindMediaView(
        holder: ImageRecyclerViewAdapter.ImageViewHolder,
        position: Int,
        files: MutableList<Any>
    ) {
        val context = holder.dateBinding.root.context
        val mediaBinding = holder.itemBinding
//        Glide.with(holder.dateBinding.root.context)
//            .load((files.elementAt(position) as File).absolutePath)
//            .into(mediaBinding.galleryImage)
        holder.itemBinding.root.setOnClickListener {
            if ((files.elementAt(position) as MediaStoreFile).fileType == FILE_TYPE_IMAGE) {
                listener?.onImageItemClick((files.elementAt(position) as MediaStoreFile).uri.toString())
            } else {
                listener?.onVideoItemClick((files.elementAt(position) as MediaStoreFile).uri.toString())
            }
        }
        if ((files.elementAt(position) as MediaStoreFile).fileType == FILE_TYPE_IMAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val bitmap = context?.contentResolver?.loadThumbnail(
                    (files.elementAt(position) as MediaStoreFile).uri,
                    Size(100, 100),
                    null
                )
                mediaBinding.galleryImage.setImageBitmap(bitmap)
                mediaBinding.videoPlaceHolder.visibility = View.GONE
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val bitmap: Bitmap =
                    ThumbnailUtils.createVideoThumbnail(
                        (files.elementAt(position) as MediaStoreFile).file,
                        Size(100, 100),
                        null
                    )
                mediaBinding.galleryImage.setImageBitmap(bitmap)
                mediaBinding.videoPlaceHolder.visibility = View.VISIBLE

            }
        }

    }

    private fun bindDateView(
        holder: ImageRecyclerViewAdapter.ImageViewHolder,
        position: Int,
        files: MutableList<Any>
    ) {
        val dateBinding = holder.dateBinding
        dateBinding.date.text = files.elementAt(position).toString()
    }


    override fun getItemCount(): Int {
        return dataList.size
    }

    class ImageViewHolder(
        val itemBinding: ItemImageBinding,
        val dateBinding: ItemDateBinding,
        val itemView: View
    ) : RecyclerView.ViewHolder(itemView) {


    }
}