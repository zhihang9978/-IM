package com.lanxin.im.ui.search

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lanxin.im.R
import com.lanxin.im.data.model.Message
import java.text.SimpleDateFormat
import java.util.*

/**
 * 搜索结果适配器
 */
class SearchResultAdapter(
    private val onItemClick: (Message) -> Unit
) : ListAdapter<Message, SearchResultAdapter.ViewHolder>(MessageDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        
        fun bind(message: Message, onClick: (Message) -> Unit) {
            tvName.text = "用户 ${message.senderId}"
            tvContent.text = message.content
            
            val time = Date(message.createdAt)
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            tvTime.text = timeFormat.format(time)
            
            Glide.with(itemView.context)
                .load(R.drawable.ic_profile)
                .circleCrop()
                .into(ivAvatar)
            
            itemView.setOnClickListener {
                onClick(message)
            }
        }
    }
}

class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem == newItem
    }
}

