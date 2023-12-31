package io.outblock.lilico.page.send.transaction

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import com.zackratos.ultimatebarx.ultimatebarx.UltimateBarX
import com.zackratos.ultimatebarx.ultimatebarx.addStatusBarTopPadding
import io.outblock.lilico.R
import io.outblock.lilico.base.activity.BaseActivity
import io.outblock.lilico.databinding.ActivityTransactionSendBinding
import io.outblock.lilico.manager.coin.FlowCoin
import io.outblock.lilico.page.address.AddressBookFragment
import io.outblock.lilico.page.address.AddressBookViewModel
import io.outblock.lilico.page.send.transaction.model.TransactionSendModel
import io.outblock.lilico.page.send.transaction.presenter.TransactionSendPresenter
import io.outblock.lilico.utils.extensions.res2String
import io.outblock.lilico.utils.extensions.res2color
import io.outblock.lilico.utils.isNightMode
import io.outblock.lilico.utils.launch
import io.outblock.lilico.utils.registerBarcodeLauncher

class TransactionSendActivity : BaseActivity() {

    private lateinit var binding: ActivityTransactionSendBinding
    private lateinit var presenter: TransactionSendPresenter
    private lateinit var viewModel: SelectSendAddressViewModel

    private val coinSymbol by lazy { intent.getStringExtra(COIN_SYMBOL)!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionSendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        UltimateBarX.with(this).fitWindow(false).light(!isNightMode(this)).applyStatusBar()
        UltimateBarX.with(this).fitWindow(true).light(!isNightMode(this)).applyNavigationBar()

        supportFragmentManager.beginTransaction().replace(R.id.search_container, AddressBookFragment()).commit()

        binding.root.addStatusBarTopPadding()
        presenter = TransactionSendPresenter(supportFragmentManager, binding.addressContent, coinSymbol)
        viewModel = ViewModelProvider(this)[SelectSendAddressViewModel::class.java].apply {
            onAddressSelectedLiveData.observe(this@TransactionSendActivity) { presenter.bind(TransactionSendModel(selectedAddress = it)) }
        }
        ViewModelProvider(this)[AddressBookViewModel::class.java].apply {
            clearEditTextFocusLiveData.observe(this@TransactionSendActivity) { presenter.bind(TransactionSendModel(isClearInputFocus = it)) }
        }

        setupToolbar()
        val barcodeLauncher = registerBarcodeLauncher { presenter.bind(TransactionSendModel(qrcode = it)) }
        binding.scanButton.setOnClickListener { barcodeLauncher.launch() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun setupToolbar() {
        binding.toolbar.navigationIcon?.mutate()?.setTint(R.color.neutrals1.res2color())
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        title = R.string.send_to.res2String()
    }

    companion object {
        private const val COIN_SYMBOL = "COIN_SYMBOL"
        fun launch(context: Context, coinSymbol: String = FlowCoin.SYMBOL_FLOW) {
            context.startActivity(Intent(context, TransactionSendActivity::class.java).apply {
                putExtra(COIN_SYMBOL, coinSymbol)
            })
        }
    }
}