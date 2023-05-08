package com.android.example.cameraxbasic.adapter

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.databinding.ItemDateBinding
import com.android.example.cameraxbasic.databinding.ItemImageBinding
import com.android.example.cameraxbasic.utils.BitmapCacheUtil
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
                val uriString =
                    Uri.parse((files.elementAt(position) as MediaStoreFile).file.absolutePath)
                listener?.onVideoItemClick(uriString.toString())
            }
        }
        if ((files.elementAt(position) as MediaStoreFile).fileType == FILE_TYPE_IMAGE) {
            loadImageThumbNail(context, files, position, mediaBinding)
        } else {
            loadVideoThumbNail(files, position, mediaBinding)
        }

    }

    private fun loadImageThumbNail(
        context: Context,
        files: MutableList<Any>,
        position: Int,
        mediaBinding: ItemImageBinding
    ) {
        try {
            val uri = (files.elementAt(position) as MediaStoreFile).uri
            val cachedBitmap = BitmapCacheUtil.getBitmap(uri.toString())
            if (cachedBitmap == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val bitmap = context.contentResolver?.loadThumbnail(
                        uri,
                        Size(100, 100),
                        null
                    )
                    if (bitmap != null) {
                        mediaBinding.galleryImage.setImageBitmap(bitmap)
                        BitmapCacheUtil.putBitmapInCache(uri.toString(), bitmap)
                    }
                    mediaBinding.videoPlaceHolder.visibility = View.GONE
                }
            } else {
                mediaBinding.galleryImage.setImageBitmap(cachedBitmap)
                mediaBinding.videoPlaceHolder.visibility = View.GONE
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun loadVideoThumbNail(
        files: MutableList<Any>,
        position: Int,
        mediaBinding: ItemImageBinding
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val file = (files.elementAt(position) as MediaStoreFile).file
                val cachedBitmap = BitmapCacheUtil.getBitmap(file.toString())
                if (cachedBitmap == null) {
                    val bitmap: Bitmap =
                        ThumbnailUtils.createVideoThumbnail(
                            file,
                            Size(100, 100),
                            null
                        )
                    mediaBinding.galleryImage.setImageBitmap(bitmap)
                    BitmapCacheUtil.putBitmapInCache(file.toString(), bitmap)
                } else {
                    mediaBinding.galleryImage.setImageBitmap(cachedBitmap)
                }
                mediaBinding.videoPlaceHolder.visibility = View.VISIBLE
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
                mediaBinding.videoPlaceHolder.visibility = View.GONE
                mediaBinding.galleryImage.setImageResource(R.drawable.baseline_play_circle_filled_24)
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