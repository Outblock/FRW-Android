package com.flowfoundation.wallet.page.nft.nftdetail.presenter

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.zackratos.ultimatebarx.ultimatebarx.addStatusBarTopPadding
import com.flowfoundation.wallet.R
import com.flowfoundation.wallet.base.presenter.BasePresenter
import com.flowfoundation.wallet.databinding.ActivityNftDetailBinding
import com.flowfoundation.wallet.manager.config.AppConfig
import com.flowfoundation.wallet.manager.config.NftCollectionConfig
import com.flowfoundation.wallet.manager.wallet.WalletManager
import com.flowfoundation.wallet.network.model.Nft
import com.flowfoundation.wallet.page.collection.CollectionActivity
import com.flowfoundation.wallet.page.nft.nftdetail.model.NftDetailModel
import com.flowfoundation.wallet.page.nft.nftdetail.shareNft
import com.flowfoundation.wallet.page.nft.nftdetail.widget.NftMorePopupMenu
import com.flowfoundation.wallet.page.nft.nftlist.*
import com.flowfoundation.wallet.page.nft.nftlist.utils.NftFavoriteManager
import com.flowfoundation.wallet.page.profile.subpage.wallet.ChildAccountCollectionManager
import com.flowfoundation.wallet.page.send.nft.NftSendAddressDialog
import com.flowfoundation.wallet.utils.*
import com.flowfoundation.wallet.utils.exoplayer.createExoPlayer
import com.flowfoundation.wallet.utils.extensions.gone
import com.flowfoundation.wallet.utils.extensions.res2String
import com.flowfoundation.wallet.utils.extensions.res2color
import com.flowfoundation.wallet.utils.extensions.setVisible
import com.flowfoundation.wallet.utils.extensions.visible
import com.flowfoundation.wallet.widgets.ProgressDialog
import com.flowfoundation.wallet.widgets.likebutton.LikeButton
import com.flowfoundation.wallet.widgets.likebutton.OnLikeListener
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlin.math.min

private val filterMetadata by lazy { listOf("uri", "img", "description") }

class NftDetailPresenter(
    private val activity: AppCompatActivity,
    private val binding: ActivityNftDetailBinding,
) : BasePresenter<NftDetailModel> {

    private var nft: Nft? = null

    private var pageColor: Int = R.color.text_sub.res2color()

    private val screenHeight by lazy { ScreenUtils.getScreenHeight() }

    private val videoPlayer by lazy { createExoPlayer(activity) }

    private var coverRatio = "1:1"

    init {
        setupToolbar()
        with(binding) {
            toolbar.addStatusBarTopPadding()

            collectButton.setOnLikeListener(object : OnLikeListener {
                override fun liked(likeButton: LikeButton?) {
                    val nft = nft ?: return
                    toggleNftSelection(nft)
                }

                override fun unLiked(likeButton: LikeButton?) {
                    val nft = nft ?: return
                    toggleNftSelection(nft)
                }
            })

            backgroundImage.layoutParams.height = ScreenUtils.getScreenHeight()
            backgroundGradient.layoutParams.height = ScreenUtils.getScreenHeight()

            scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
                updateToolbarColor(scrollY)
            })

            moreButton.setOnClickListener {
                nft?.let { NftMorePopupMenu(it, moreButton, pageColor).show() }
            }
            collectButton.setLikeDrawableTint(R.color.colorSecondary.res2color())
            shareButton.setOnClickListener { showShareNft() }
            sendButton.setOnClickListener {
                val uniqueId = nft?.uniqueId() ?: return@setOnClickListener
                NftSendAddressDialog.newInstance(uniqueId).show(activity.supportFragmentManager, "")
            }
            sendButton.isEnabled = !WalletManager.isChildAccountSelected()
        }
    }

    private fun showShareNft() {
        nft?.let {
            val dialog = ProgressDialog(activity)
            dialog.show()
            shareNft(binding.shareScreenshotWrapper, it) { file ->
                dialog.dismiss()
                activity.shareFile(file, title = "Nft share", text = it.name().orEmpty(), type = "image/*")
            }
        }
    }

    override fun bind(model: NftDetailModel) {
        model.nft?.let { safeRun { bindData(it) } }
        if (!nft?.video().isNullOrBlank()) {
            model.onPause?.let { safeRun { videoPlayer.pause() } }
            model.onRestart?.let { safeRun { videoPlayer.play() } }
            model.onDestroy?.let { safeRun { videoPlayer.release() } }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindData(nft: Nft) {
        this.nft = nft
        with(binding) {
            val config = NftCollectionConfig.get(nft.collectionAddress)
            val name = config?.name ?: nft.contractName()
            val title = "$name #${nft.id}"
            toolbar.title = title

            ioScope { updateSelectionState(NftFavoriteManager.isFavoriteNft(nft)) }

            bindCover(nft)
            Glide.with(backgroundImage).load(nft.cover())
                .transition(DrawableTransitionOptions.withCrossFade(100))
                .transform(BlurTransformation(15, 30))
                .into(backgroundImage)

            titleView.text = title
            subtitleView.text = name
            Glide.with(collectionIcon).load(config?.logo).transform(CenterCrop(), CircleCrop()).into(collectionIcon)
            descView.text = nft.desc()

            purchaseDate.text = "01.01.2022"
            purchasePrice.text = "1239.22"

            bindTags(nft)

            bindVideo(nft)

            bindAccessible(title, nft)

            collectionWrapper.setOnClickListener {
                CollectionActivity.launch(activity, nft.collectionContractName)
            }

            ioScope { updateSelectionState(NftFavoriteManager.isFavoriteNft(nft)) }

            sendButton.setVisible(!nft.isDomain() && AppConfig.showNFTTransfer())
        }
    }

    private fun bindAccessible(title: String, nft: Nft) {
        if (ChildAccountCollectionManager.isNFTAccessible(nft.id)) {
            binding.inaccessibleTip.gone()
            return
        }
        val accountName = WalletManager.childAccount(WalletManager.selectedWalletAddress())?.name ?: R.string.default_child_account_name.res2String()
        binding.tvInaccessibleTip.text = activity.getString(R.string.inaccessible_token_tip, title, accountName)
        binding.inaccessibleTip.visible()
    }

    private fun bindVideo(nft: Nft) {
        val uri = nft.video()
        if (uri.isNullOrBlank()) {
            return
        }
        with(videoPlayer) {
            setVideoTextureView(binding.videoView)
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            volume = 0f
            prepare()
            play()
        }
    }

    private fun bindCover(nft: Nft) {
        Glide.with(binding.coverView)
            .asBitmap()
            .load(nft.cover())
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    ioScope {
                        coverRatio = "${resource.width}:${resource.height}"
                        val color = Palette.from(resource).generate().getDominantColor(R.color.text_sub.res2color())
                        uiScope {
                            updatePageColor(color)
                            binding.coverView.setImageBitmap(resource)
                        }
                    }
                }
            })
    }

    private fun updatePageColor(color: Int) {
        pageColor = color
        with(binding) {
            with(mediaWrapper.layoutParams as ConstraintLayout.LayoutParams) {
                dimensionRatio = coverRatio
                mediaWrapper.layoutParams = this
            }

            shareButton.setColorFilter(color)
            ioScope { updateSelectionState(NftFavoriteManager.isFavoriteNft(nft!!)) }
            tags.children.forEach { tag ->
                tag.background.setTint(color)
                tag.findViewById<TextView>(R.id.title_view).setTextColor(color)
            }

            sendButton.iconTint = ColorStateList.valueOf(color)
            moreButton.iconTint = ColorStateList.valueOf(color)
        }
    }

    private fun bindTags(nft: Nft) {
        val tags = nft.traits ?: return
        with(binding.tags) {
            tags.filter { !filterMetadata.contains(it.name.lowercase()) && it.value.isNotBlank() && !it.value.startsWith("https://") }
                .forEach { metadata ->
                    val tagView = (LayoutInflater.from(activity).inflate(R.layout.item_nft_tag, this, false) as ViewGroup)
                    tagView.background.setTint(pageColor)
                    tagView.background.alpha = (0.4f * 255).toInt()
                    val titleView = tagView.findViewById<TextView>(R.id.title_view)
                    val contentView = tagView.findViewById<TextView>(R.id.content_view)

                    titleView.setTextColor(pageColor)
                    titleView.text = metadata.name.uppercase()
                    contentView.text = metadata.value
                    addView(tagView)
                }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.navigationIcon?.mutate()?.setTint(R.color.neutrals1.res2color())
        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activity.supportActionBar?.setDisplayShowHomeEnabled(true)
        activity.title = ""
    }

    private fun updateSelectionState(isSelected: Boolean) {
        uiScope {
            with(binding.collectButton) {
                isLiked = isSelected
                setUnLikeDrawableTint(pageColor)
                setCircleStartColorInt(pageColor)
            }
        }
    }

    private fun toggleNftSelection(nft: Nft) {
        ioScope {
            if (NftFavoriteManager.isFavoriteNft(nft)) {
                NftFavoriteManager.removeFavorite(nft.contractName(), nft.tokenId())
                updateSelectionState(false)
            } else {
                NftFavoriteManager.addFavorite(nft)
                updateSelectionState(true)
            }
        }
    }

    private fun ActivityNftDetailBinding.updateToolbarColor(scrollY: Int) {
        val progress = min(scrollY / (screenHeight * 0.25f), 1.0f)
        toolbar.setBackgroundColor(
            ArgbEvaluator().evaluate(
                progress,
                R.color.transparent.res2color(),
                R.color.background.res2color(),
            ) as Int
        )
        toolbar.setTitleTextColor(
            ArgbEvaluator().evaluate(
                progress,
                R.color.transparent.res2color(),
                R.color.text.res2color(),
            ) as Int
        )
    }
}