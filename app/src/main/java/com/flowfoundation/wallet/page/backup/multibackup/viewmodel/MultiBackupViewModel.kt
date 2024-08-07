package com.flowfoundation.wallet.page.backup.multibackup.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.flowfoundation.wallet.page.backup.BackupListManager
import com.flowfoundation.wallet.page.backup.multibackup.model.BackupOption
import com.flowfoundation.wallet.page.backup.multibackup.model.BackupOptionModel


class MultiBackupViewModel : ViewModel() {

    val optionChangeLiveData = MutableLiveData<BackupOptionModel>()

    private val optionList = mutableListOf<BackupOption>()
    private var currentOption = BackupOption.BACKUP_START
    private var currentIndex = -1

    fun getBackupOptionList(): List<BackupOption> {
        return optionList
    }

    fun getCurrentIndex(): Int {
        return currentIndex
    }

    fun changeOption(option: BackupOption, index: Int) {
        this.currentOption = option
        this.currentIndex = index
        optionChangeLiveData.postValue(BackupOptionModel(option, index))
    }

    fun isBackupValid(): Boolean {
        return optionList.size + BackupListManager.backupCount() >= 2
    }

    fun startBackup() {
        addCompleteOption()
        changeOption(optionList[0], 0)
    }

    fun toNext() {
        ++currentIndex
        changeOption(optionList[currentIndex], currentIndex)
    }

    fun addCompleteOption() {
        if (optionList.lastOrNull() != BackupOption.BACKUP_COMPLETED) {
            optionList.remove(BackupOption.BACKUP_COMPLETED)
            optionList.add(BackupOption.BACKUP_COMPLETED)
        }
    }

    fun selectOption(option: BackupOption, callback: (isSelected: Boolean) -> Unit) {
        if (optionList.contains(option)) {
            optionList.remove(option)
            callback.invoke(false)
        } else {
            optionList.add(option)
            callback.invoke(true)
        }
    }
}