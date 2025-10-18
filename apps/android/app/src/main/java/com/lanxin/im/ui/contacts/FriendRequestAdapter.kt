package com.lanxin.im.ui.contacts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lanxin.im.R
import com.lanxin.im.data.remote.FriendRequestItem
import java.text.SimpleDateFormat
import java.util.*

class FriendRequestAdapter(
    private val onAccept: (Long) -> Unit,
    private val onReject: (Long) -> Unit
) : ListAdapter<FriendRequestItem, FriendRequestAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatarView: ImageView = itemView.findViewById(R.id.avatar)
        private val nameView: TextView = itemView.findViewById(R.id.name)
        private val messageView: TextView = itemView.findViewById(R.id.message)
        private val timeView: TextView = itemView.findViewById(R.id.time)
        private val acceptButton: Button = itemView.findViewById(R.id.btn_accept)
        private val rejectButton: Button = itemView.findViewById(R.id.btn_reject)
        private val statusView: TextView = itemView.findViewById(R.id.status)
        private val buttonsLayout: View = itemView.findViewById(R.id.buttons_layout)

        fun bind(item: FriendRequestItem) {
            val sender = item.sender
            
            nameView.text = sender?.username ?: "未知用户"
            messageView.text = item.message ?: "请求添加你为好友"
            timeView.text = formatTime(item.created_at)

            if (!sender?.avatar.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(sender?.avatar)
                    .placeholder(R.drawable.ic_default_avatar)
                    .into(avatarView)
            } else {
                avatarView.setImageResource(R.drawable.ic_default_avatar)
            }

            when (item.status) {
                "pending" -> {
                    buttonsLayout.visibility = View.VISIBLE
                    statusView.visibility = View.GONE
                    
                    acceptButton.setOnClickListener {
                        onAccept(item.id)
                    }
                    
                    rejectButton.setOnClickListener {
                        onReject(item.id)
                    }
                }
                "accepted" -> {
                    buttonsLayout.visibility = View.GONE
                    statusView.visibility = View.VISIBLE
                    statusView.text = "已接受"
                }
                "rejected" -> {
                    buttonsLayout.visibility = View.GONE
                    statusView.visibility = View.VISIBLE
                    statusView.text = "已拒绝"
                }
                else -> {
                    buttonsLayout.visibility = View.GONE
                    statusView.visibility = View.GONE
                }
            }
        }

        private fun formatTime(timestamp: Long): String {
            val date = Date(timestamp * 1000)
            val now = Date()
            val diff = now.time - date.time

            return when {
                diff < 60 * 1000 -> "刚刚"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
                else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<FriendRequestItem>() {
        override fun areItemsTheSame(oldItem: FriendRequestItem, newItem: FriendRequestItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FriendRequestItem, newItem: FriendRequestItem): Boolean {
            return oldItem == newItem
        }
    }
}
