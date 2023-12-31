package io.outblock.lilico.page.guide.presenter

import android.view.View
import com.bumptech.glide.Glide
import io.outblock.lilico.base.presenter.BasePresenter
import io.outblock.lilico.base.recyclerview.BaseViewHolder
import io.outblock.lilico.databinding.ItemGuidePageBinding
import io.outblock.lilico.page.guide.model.GuideItemModel

class GuideItemPresenter(
    private val view: View,
) : BaseViewHolder(view), BasePresenter<GuideItemModel> {

    private val binding by lazy { ItemGuidePageBinding.bind(view) }

    override fun bind(model: GuideItemModel) {
        with(binding) {
            Glide.with(coverView).load(model.cover).into(coverView)
            titleView.setText(model.title)
            descView.setText(model.desc)
        }
    }
}