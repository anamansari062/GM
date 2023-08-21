package com.example.gm.presentation.ui.dashboard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gm.R
import com.example.gm.presentation.utils.Nft

class NftAdapter(private val context: Context, private val dataList: List<Nft>) : RecyclerView.Adapter<NftAdapter.ViewHolder>() {
    private var listener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.nft_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val image = dataList[position].image
        Glide.with(context)
            .load(image) // Replace with your image URL
            .into(holder.imageView)

        holder.itemView.setOnClickListener {
            if (listener != null) {
                listener!!.onItemClick(dataList[position])
            }
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    interface OnItemClickListener {
        fun onItemClick(nft: Nft)
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        this.listener = listener
    }
}
