package io.outblock.lilico.page.nft.nftlist.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import io.outblock.lilico.R
import io.outblock.lilico.base.presenter.BasePresenter
import io.outblock.lilico.base.recyclerview.BaseAdapter
import io.outblock.lilico.base.recyclerview.BaseViewHolder
import io.outblock.lilico.databinding.ItemNftListCollectionTabBinding
import io.outblock.lilico.page.nft.nftlist.NftViewModel
import io.outblock.lilico.page.nft.nftlist.model.CollectionItemModel
import io.outblock.lilico.page.profile.subpage.wallet.ChildAccountCollectionManager
import io.outblock.lilico.utils.extensions.dp2px
import io.outblock.lilico.utils.extensions.res2color
import io.outblock.lilico.utils.extensions.setVisible
import io.outblock.lilico.utils.findActivity

class CollectionTabsAdapter : BaseAdapter<CollectionItemModel>(diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return TabsViewHolder(parent.inflate(R.layout.item_nft_list_collection_tab))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as TabsViewHolder).bind(getItem(position))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
    }
}

private class TabsViewHolder(
    private val view: View,
) : BaseViewHolder(view), BasePresenter<CollectionItemModel> {
    private val binding by lazy { ItemNftListCollectionTabBinding.bind(view) }

    private val corners by lazy { 12.dp2px().toInt() }

    private val activity by lazy { findActivity(view) as FragmentActivity }

    private val viewModel by lazy { ViewModelProvider(activity)[NftViewModel::class.java] }

    private var model: CollectionItemModel? = null

    init {
        view.setOnClickListener {
            model?.collection?.contractName?.let { viewModel.selectCollection(it) }
        }
    }

    override fun bind(model: CollectionItemModel) {
        val config = model.collection
        with(binding) {
            if (this@TabsViewHolder.model != model) {
                Glide.with(coverView).load(config.logo).transform(CenterCrop(), RoundedCorners(corners)).into(coverView)
                nameView.text = config.name
                countView.text = view.context.getString(R.string.collectibles_count, model.count)
            }
            root.strokeColor = if (model.isSelected) R.color.neutrals4.res2color() else Color.TRANSPARENT
        }
        this.model = model
        bindAccessible(model)
    }

    private fun bindAccessible(model: CollectionItemModel) {
        val accessible = ChildAccountCollectionManager.isNFTCollectionAccessible(model.collection.id)
        binding.countView.setVisible(accessible)
        binding.tvInaccessibleTag.setVisible(accessible.not())
    }
}

val diffCallback = object : DiffUtil.ItemCallback<CollectionItemModel>() {
    override fun areItemsTheSame(oldItem: CollectionItemModel, newItem: CollectionItemModel): Boolean {
        return oldItem.collection.contractName == newItem.collection.contractName
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: CollectionItemModel, newItem: CollectionItemModel): Boolean {
        return oldItem == newItem
    }
}