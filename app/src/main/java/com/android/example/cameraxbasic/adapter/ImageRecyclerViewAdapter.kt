package com.android.example.cameraxbasic.adapter

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.android.example.cameraxbasic.databinding.ItemDateBinding
import com.android.example.cameraxbasic.databinding.ItemImageBinding
import java.io.File

class ImageRecyclerViewAdapter(val imageList: List<File>, val dateList: List<String>) :
    RecyclerView.Adapter<ImageRecyclerViewAdapter.ImageViewHolder>() {

     val typeDate = 100
     val typeMedia = 101
    var getGalleryView: MutableSet<Any> = emptySet<Any>().toMutableSet()

    var items = listOf<Any>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    override fun getItemViewType(position: Int): Int {
        val listItem = getGalleryView.elementAt(position)
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
        val listItem = getGalleryView.elementAt(position)
        if (listItem is String) {
            bindDateView(holder, position, getGalleryView)
        } else {
            bindMediaView(holder, position, getGalleryView)
        }
    }

    private fun bindMediaView(
        holder: ImageRecyclerViewAdapter.ImageViewHolder,
        position: Int,
        files: MutableSet<Any>
    ) {
        val mediaBinding = holder.itemBinding
        mediaBinding.galleryImage.setImageURI(Uri.parse(files.elementAt(position).toString()))
    }

    private fun bindDateView(
        holder: ImageRecyclerViewAdapter.ImageViewHolder,
        position: Int,
        files: MutableSet<Any>
    ) {
        val dateBinding = holder.dateBinding
        dateBinding.date.text = files.elementAt(position).toString()
    }


    override fun getItemCount(): Int {
        Log.d("PRS","getItemCount"+getGalleryView.size)
        return getGalleryView.size
    }

    class ImageViewHolder(
        val itemBinding: ItemImageBinding,
        val dateBinding: ItemDateBinding,
        val itemView: View
    ) : RecyclerView.ViewHolder(itemView) {



    }
}