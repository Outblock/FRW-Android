package io.outblock.lilico.page.profile.subpage.wallet.childaccountdetail.presenter

import android.graphics.Color
import androidx.recyclerview.widget.LinearLayoutManager
import io.outblock.lilico.R
import io.outblock.lilico.base.presenter.BasePresenter
import io.outblock.lilico.databinding.ActivityChildAccountDetailBinding
import io.outblock.lilico.manager.childaccount.ChildAccount
import io.outblock.lilico.page.profile.subpage.wallet.childaccountdetail.ChildAccountDetailActivity
import io.outblock.lilico.page.profile.subpage.wallet.childaccountdetail.CoinData
import io.outblock.lilico.page.profile.subpage.wallet.childaccountdetail.CollectionData
import io.outblock.lilico.page.profile.subpage.wallet.childaccountdetail.adapter.AccessibleListAdapter
import io.outblock.lilico.page.profile.subpage.wallet.childaccountdetail.dialog.ChildAccountUnlinkDialog
import io.outblock.lilico.page.profile.subpage.wallet.childaccountdetail.model.ChildAccountDetailModel
import io.outblock.lilico.utils.extensions.dp2px
import io.outblock.lilico.utils.extensions.gone
import io.outblock.lilico.utils.extensions.setVisible
import io.outblock.lilico.utils.extensions.visible
import io.outblock.lilico.utils.loadAvatar
import io.outblock.lilico.utils.textToClipboard
import io.outblock.lilico.utils.toast
import io.outblock.lilico.widgets.itemdecoration.ColorDividerItemDecoration

class ChildAccountDetailPresenter(
    private val binding: ActivityChildAccountDetailBinding,
    private val activity: ChildAccountDetailActivity,
) : BasePresenter<ChildAccountDetailModel> {

    private val accessibleAdapter by lazy { AccessibleListAdapter() }
    private val nftCollections = mutableListOf<CollectionData>()
    private val coinList = mutableListOf<CoinData>()
    private var isShowEmptyCollection = false

    init {
        with(binding.accessibleListView) {
            adapter = accessibleAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(ColorDividerItemDecoration(Color.TRANSPARENT, 8.dp2px().toInt()))
        }
        binding.clHideEmpty.setOnClickListener {
            isShowEmptyCollection = isShowEmptyCollection.not()
            binding.switchEmpty.isChecked = isShowEmptyCollection
            with(binding.switchEmpty) {
                isChecked = isShowEmptyCollection
                jumpDrawablesToCurrentState()
            }
            updateNFTCollections()
        }
        binding.tvTabCollection.setOnClickListener { changeTabStatus(collectionSelected = true) }
        binding.tvTabCoin.setOnClickListener { changeTabStatus(collectionSelected = false) }
    }

    private fun updateNFTCollections() {
        accessibleAdapter.setNewDiffData(
            if (isShowEmptyCollection) nftCollections else nftCollections.filter { it.idList.isNotEmpty() }
        )
    }

    override fun bind(model: ChildAccountDetailModel) {
        model.account?.let { updateAccount(it) }
        model.nftCollections?.let { list ->
            nftCollections.clear()
            nftCollections.addAll(list.sortedByDescending { it.idList.size })
            changeTabStatus(collectionSelected = true)
        }
        model.coinList?.let {
            coinList.clear()
            coinList.addAll(it)
        }
    }

    private fun changeTabStatus(collectionSelected: Boolean) {
        binding.tvTabCollection.isSelected = collectionSelected
        binding.tvTabCoin.isSelected = collectionSelected.not()
        if (collectionSelected) {
            updateNFTCollections()
            binding.clHideEmpty.visible()
            binding.accessibleListView.visible()
            binding.tvCoinEmpty.gone()
        } else {
            accessibleAdapter.setNewDiffData(coinList)
            binding.clHideEmpty.gone()
            if (coinList.isEmpty()) {
                binding.tvCoinEmpty.visible()
                binding.accessibleListView.gone()
            } else {
                binding.accessibleListView.visible()
                binding.tvCoinEmpty.gone()
            }
        }
    }

    private fun updateAccount(account: ChildAccount) {
        with(binding) {
            logoView.loadAvatar(account.icon)
            nameView.text = account.name
            addressView.text = account.address

            descriptionView.text = account.description
            descriptionTitleView.setVisible(!account.description.isNullOrEmpty())

            addressCopyButton.setOnClickListener {
                textToClipboard(account.address)
                toast(msgRes = R.string.copy_address_toast)
            }

            unlinkButton.setOnClickListener {
                ChildAccountUnlinkDialog.show(
                    activity.supportFragmentManager,
                    account
                )
            }
        }
    }

}