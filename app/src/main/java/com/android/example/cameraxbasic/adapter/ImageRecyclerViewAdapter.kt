package com.android.example.cameraxbasic.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.android.example.cameraxbasic.databinding.ItemDateBinding
import com.android.example.cameraxbasic.databinding.ItemImageBinding
import java.io.File

class ImageRecyclerViewAdapter(val imageList:List<File?>):
    RecyclerView.Adapter<ImageRecyclerViewAdapter.ImageViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.view.galleryImage.setImageURI(imageList[position]?.toUri())
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    class ImageViewHolder(val view:ItemImageBinding):RecyclerView.ViewHolder(view.root){

    }
}