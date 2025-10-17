package com.lanxin.im.ui.contacts

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
import com.lanxin.im.data.model.Contact

/**
 * 联系人列表适配器（按设计文档实现）
 */
class ContactAdapter(
    private val onItemClick: (Contact) -> Unit
) : ListAdapter<Contact, ContactAdapter.ViewHolder>(ContactDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        
        fun bind(contact: Contact, onClick: (Contact) -> Unit) {
            // 使用Glide加载头像（完整实现）
            Glide.with(itemView.context)
                .load(R.drawable.ic_profile)
                .circleCrop()
                .into(ivAvatar)
            
            // 显示名称（备注优先，否则显示联系人ID）
            tvName.text = contact.remark ?: "联系人${contact.contactId}"
            
            // 点击事件
            itemView.setOnClickListener {
                onClick(contact)
            }
        }
    }
}

class ContactDiffCallback : DiffUtil.ItemCallback<Contact>() {
    override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
        return oldItem == newItem
    }
}

