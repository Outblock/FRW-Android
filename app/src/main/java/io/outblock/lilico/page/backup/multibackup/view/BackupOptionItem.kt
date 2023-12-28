package io.outblock.lilico.page.backup.multibackup.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import io.outblock.lilico.R
import io.outblock.lilico.utils.extensions.setVisible


class BackupOptionItem : FrameLayout {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val array =
            context.obtainStyledAttributes(attrs, R.styleable.BackupOptionItem, defStyleAttr, 0)
        val image = array.getResourceId(R.styleable.BackupOptionItem_option_item_icon, 0)
        array.recycle()

        LayoutInflater.from(context).inflate(R.layout.layout_backup_option_item, this)
        val imageView = findViewById<ImageView>(R.id.iv_option_icon)

        imageView.setImageResource(image)
        changeItemStatus(false)
    }

    fun changeItemStatus(isSelected: Boolean) {
        findViewById<FrameLayout>(R.id.fl_option_background).let {
            setBackgroundResource(
                if (isSelected) {
                    R.drawable.bg_backup_option_selected
                } else {
                    R.drawable.bg_backup_option
                }
            )
        }
        findViewById<ImageView>(R.id.iv_selected).setVisible(isSelected)
    }
}