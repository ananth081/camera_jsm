package com.android.example.cameraxbasic.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.android.example.cameraxbasic.databinding.ItemDateBinding
import com.android.example.cameraxbasic.databinding.ItemImageBinding
import com.bumptech.glide.Glide
import java.io.File

@RequiresApi(Build.VERSION_CODES.Q)
class ImageRecyclerViewAdapter() :
    RecyclerView.Adapter<ImageRecyclerViewAdapter.ImageViewHolder>() {

    val typeDate = 100
    val typeMedia = 101
    var dataList: MutableList<Any> = emptyList<Any>().toMutableList()
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
        val mediaBinding = holder.itemBinding
        Glide.with(holder.dateBinding.root.context)
            .load((files.elementAt(position) as File).absolutePath)
            .into(mediaBinding.galleryImage)
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