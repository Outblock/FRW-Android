package io.outblock.lilico.page.profile.subpage.wallet.device

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.outblock.lilico.manager.account.DeviceInfoManager
import io.outblock.lilico.network.ApiService
import io.outblock.lilico.network.retrofit
import io.outblock.lilico.page.profile.subpage.wallet.device.model.DeviceTitle
import io.outblock.lilico.utils.uiScope
import io.outblock.lilico.utils.viewModelIOScope
import okhttp3.internal.filterList


class DevicesViewModel : ViewModel() {
    val devicesLiveData = MutableLiveData<List<Any>>()
    private val devices = mutableListOf<Any>()

    fun load() {
        viewModelIOScope(this) {
            val service = retrofit().create(ApiService::class.java)
            val response = service.getDeviceList()
            val deviceInfoList = response.data ?: emptyList()
            uiScope {
                if (deviceInfoList.isEmpty().not()) {
                    devices.clear()
                    val currentDevice =
                        deviceInfoList.filterList { DeviceInfoManager.isCurrentDevice(id) }
                    if (currentDevice.isNotEmpty()) {
                        devices.add(DeviceTitle("Current Device"))
                        devices.addAll(currentDevice)
                    }
                    val otherDevice = deviceInfoList.filterList {
                        DeviceInfoManager.isCurrentDevice(id).not()
                    }
                    if (otherDevice.isNotEmpty()) {
                        devices.add(DeviceTitle("Other Devices"))
                        devices.addAll(otherDevice)
                    }
                    devicesLiveData.postValue(devices)
                }
            }
        }
    }
}