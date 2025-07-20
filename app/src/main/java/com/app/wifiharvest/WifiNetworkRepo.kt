package com.app.wifiharvest

import androidx.lifecycle.MutableLiveData

object WifiNetworkRepo {
    val networks = MutableLiveData<MutableList<WifiNetwork>>(mutableListOf())

    fun add(network: WifiNetwork) {
        val current = networks.value ?: mutableListOf()
        if (current.none { it.bssid == network.bssid }) {
            current.add(network)
            networks.postValue(current)
        }
    }

    fun clear() {
        networks.postValue(mutableListOf())
    }
}
