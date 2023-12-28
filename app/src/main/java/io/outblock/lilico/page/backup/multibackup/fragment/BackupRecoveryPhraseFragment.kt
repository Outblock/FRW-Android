package io.outblock.lilico.page.backup.multibackup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import io.outblock.lilico.databinding.FragmentBackupRecoveryPhraseBinding
import io.outblock.lilico.manager.transaction.OnTransactionStateChange
import io.outblock.lilico.manager.transaction.TransactionState
import io.outblock.lilico.manager.transaction.TransactionStateManager
import io.outblock.lilico.page.backup.multibackup.viewmodel.BackupRecoveryPhraseViewModel
import io.outblock.lilico.page.backup.multibackup.viewmodel.MultiBackupViewModel
import io.outblock.lilico.page.walletcreate.fragments.mnemonic.MnemonicAdapter
import io.outblock.lilico.utils.extensions.setVisible
import io.outblock.lilico.widgets.itemdecoration.GridSpaceItemDecoration


class BackupRecoveryPhraseFragment : Fragment(), OnTransactionStateChange {

    private lateinit var binding: FragmentBackupRecoveryPhraseBinding
    private lateinit var viewModel: BackupRecoveryPhraseViewModel
    private val adapter by lazy { MnemonicAdapter() }

    private val backupViewModel by lazy {
        ViewModelProvider(this.requireActivity())[MultiBackupViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBackupRecoveryPhraseBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[BackupRecoveryPhraseViewModel::class.java].apply {
            createBackupCallbackLiveData.observe(viewLifecycleOwner) { isSuccess ->
                if (isSuccess) {
                    backupViewModel.toNext()
                }
            }
            mnemonicListLiveData.observe(viewLifecycleOwner) { list ->
                adapter.setNewDiffData(list)
            }

        }
        TransactionStateManager.addOnTransactionStateChange(this)
        initPhrases()
    }

    private fun initPhrases() {
        with(binding.mnemonicContainer) {
            setVisible()
            adapter = this.adapter
            layoutManager = GridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false)
            addItemDecoration(GridSpaceItemDecoration(vertical = 16.0))
        }
        binding.stringContainer.setVisible(false)
        binding.copyButton.setOnClickListener {
            viewModel.copyMnemonic()
        }
        with(binding.btnNext) {
            setProgressVisible(true)
            setOnClickListener {
                viewModel.uploadToChainAndSync()
            }
        }
        viewModel.loadMnemonic()
    }

    override fun onTransactionStateChange() {
        val transactionList = TransactionStateManager.getTransactionStateList()
        val transaction =
            transactionList.firstOrNull { it.type == TransactionState.TYPE_ADD_PUBLIC_KEY }
        transaction?.let { state ->
            if (state.isSealed()) {
                viewModel.syncKeyInfo()
            }
        }
    }
}