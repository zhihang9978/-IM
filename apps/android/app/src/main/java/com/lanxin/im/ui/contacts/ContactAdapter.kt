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
 * 联系人列表Adapter (WildFire IM style)
 * 参考：WildFireChat 字母分组设计 (Apache 2.0)
 * 适配：蓝信IM
 * 
 * 功能:
 * - A-Z字母分组显示
 * - 40dp圆形头像
 * - 分组标题自动显示/隐藏
 * - 拼音首字母排序
 */
class ContactAdapter(
    private val onContactClick: (Contact) -> Unit
) : ListAdapter<ContactDisplayItem, ContactAdapter.ViewHolder>(ContactDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact_wildfire, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onContactClick)
    }
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val portraitImageView: ImageView = itemView.findViewById(R.id.portraitImageView)
        private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        private val sectionTextView: TextView = itemView.findViewById(R.id.sectionTextView)
        
        fun bind(item: ContactDisplayItem, onClick: (Contact) -> Unit) {
            // 加载头像 (WildFire IM: 40dp圆形头像)
            Glide.with(itemView.context)
                .load(item.avatar)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(portraitImageView)
            
            // 设置名称
            nameTextView.text = item.name
            
            // 显示分组标题 (WildFire IM style)
            if (item.showSection) {
                sectionTextView.visibility = View.VISIBLE
                sectionTextView.text = item.section
            } else {
                sectionTextView.visibility = View.GONE
            }
            
            // 点击事件
            itemView.setOnClickListener { onClick(item.contact) }
        }
    }
}

/**
 * 联系人显示项
 * 包含分组信息
 */
data class ContactDisplayItem(
    val contact: Contact,
    val avatar: String?,
    val name: String,
    val section: String,        // A-Z 或 # (特殊字符)
    val showSection: Boolean    // 是否显示分组标题
)

/**
 * DiffUtil回调
 */
class ContactDiffCallback : DiffUtil.ItemCallback<ContactDisplayItem>() {
    override fun areItemsTheSame(oldItem: ContactDisplayItem, newItem: ContactDisplayItem) = 
        oldItem.contact.id == newItem.contact.id
    
    override fun areContentsTheSame(oldItem: ContactDisplayItem, newItem: ContactDisplayItem) = 
        oldItem == newItem
}

/**
 * 联系人列表辅助工具
 */
object ContactListHelper {
    
    /**
     * 获取首字母（简化版）
     * WildFire IM使用拼音库，这里简化为只取英文首字母
     */
    fun getFirstLetter(name: String): String {
        if (name.isEmpty()) return "#"
        
        val firstChar = name[0].uppercaseChar()
        return if (firstChar in 'A'..'Z') {
            firstChar.toString()
        } else {
            "#"
        }
    }
    
    /**
     * 将联系人列表转换为显示项列表（带分组）
     */
    fun toDisplayItems(contacts: List<Contact>, contactsWithUsers: List<com.lanxin.im.data.remote.ContactItem>? = null): List<ContactDisplayItem> {
        if (contacts.isEmpty()) return emptyList()
        
        // 创建contactId到User的映射（如果有API数据）
        val userMap = contactsWithUsers?.associate { it.contact_id to it.user } ?: emptyMap()
        
        // 按首字母排序
        val sorted = contacts.sortedBy { getFirstLetter(it.remark ?: it.username) }
        
        // 转换为显示项，标记每个分组的第一项
        val result = mutableListOf<ContactDisplayItem>()
        var lastSection = ""
        
        for (contact in sorted) {
            val displayName = contact.remark ?: contact.username
            val section = getFirstLetter(displayName)
            val showSection = section != lastSection
            
            // 从映射获取用户信息
            val userInfo = userMap[contact.contactId]
            
            result.add(
                ContactDisplayItem(
                    contact = contact,
                    avatar = userInfo?.avatar,
                    name = displayName,
                    section = section,
                    showSection = showSection
                )
            )
            
            lastSection = section
        }
        
        return result
    }
}
