package com.lanxin.im.widget

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.lanxin.im.R

/**
 * OptionItemView - 通用设置选项视图
 * 参考：WildFireChat OptionItemView (Apache 2.0)
 */
class OptionItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val startImageView: ImageView
    private val endImageView: ImageView
    private val arrowIndicator: ImageView
    private val titleTextView: TextView
    private val descTextView: TextView
    private val badgeTextView: TextView
    private val dividerView: View

    init {
        View.inflate(context, R.layout.widget_option_item, this)
        
        startImageView = findViewById(R.id.leftImageView)
        endImageView = findViewById(R.id.rightImageView)
        arrowIndicator = findViewById(R.id.arrowImageView)
        titleTextView = findViewById(R.id.titleTextView)
        descTextView = findViewById(R.id.descTextView)
        badgeTextView = findViewById(R.id.badgeTextView)
        dividerView = findViewById(R.id.dividerLine)

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.OptionItemView)
            
            try {
                // 左侧图标
                val startResId = typedArray.getResourceId(R.styleable.OptionItemView_start_src, 0)
                if (startResId != 0) {
                    startImageView.visibility = VISIBLE
                    startImageView.setImageResource(startResId)
                }
                
                // 左侧图标着色
                val startTint = typedArray.getColor(R.styleable.OptionItemView_start_tint, 0)
                if (startTint != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startImageView.imageTintList = ColorStateList.valueOf(startTint)
                }
                
                // 标题
                typedArray.getString(R.styleable.OptionItemView_title)?.let { title ->
                    titleTextView.text = title
                }
                
                // 描述
                typedArray.getString(R.styleable.OptionItemView_desc)?.let { desc ->
                    if (desc.isNotEmpty()) {
                        descTextView.visibility = VISIBLE
                        descTextView.text = desc
                    }
                }
                
                // 徽章计数
                val badgeCount = typedArray.getInt(R.styleable.OptionItemView_badge_count, 0)
                if (badgeCount > 0) {
                    badgeTextView.visibility = VISIBLE
                    badgeTextView.text = badgeCount.coerceAtMost(99).toString()
                }
                
                // 右侧图标
                val endResId = typedArray.getResourceId(R.styleable.OptionItemView_end_src, 0)
                if (endResId != 0) {
                    endImageView.visibility = VISIBLE
                    endImageView.setImageResource(endResId)
                }
                
                // 箭头指示器
                val showArrow = typedArray.getBoolean(R.styleable.OptionItemView_show_arrow_indicator, false)
                arrowIndicator.visibility = if (showArrow) VISIBLE else GONE
                
                // 分割线
                val showDivider = typedArray.getBoolean(R.styleable.OptionItemView_show_divider, true)
                dividerView.visibility = if (showDivider) VISIBLE else GONE
                
            } finally {
                typedArray.recycle()
            }
        }
    }

    fun setTitle(title: String) {
        titleTextView.text = title
    }

    fun setDesc(desc: String?) {
        if (desc.isNullOrEmpty()) {
            descTextView.visibility = GONE
        } else {
            descTextView.visibility = VISIBLE
            descTextView.text = desc
        }
    }

    fun setBadgeCount(count: Int) {
        if (count > 0) {
            badgeTextView.visibility = VISIBLE
            badgeTextView.text = count.coerceAtMost(99).toString()
        } else {
            badgeTextView.visibility = GONE
        }
    }

    fun setDividerVisibility(visibility: Int) {
        dividerView.visibility = visibility
    }
}
