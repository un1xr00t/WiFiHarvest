package com.app.wifiharvest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class SharedWifiViewModel : ViewModel() {
    private val _networks = MutableLiveData<MutableList<WifiNetwork>>(mutableListOf())
    val networks: LiveData<MutableList<WifiNetwork>> = _networks

    fun addNetwork(network: WifiNetwork) {
        val currentList = _networks.value ?: mutableListOf()
        if (currentList.none { it.bssid == network.bssid }) {
            currentList.add(network)
            _networks.postValue(currentList)
        }
    }

    fun clearNetworks() {
        _networks.value = mutableListOf()
    }
}
