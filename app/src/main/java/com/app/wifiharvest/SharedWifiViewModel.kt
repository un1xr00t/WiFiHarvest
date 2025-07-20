package com.app.wifiharvest

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng

class SharedWifiViewModel : ViewModel() {
    private val _networks = MutableLiveData<MutableList<WifiNetwork>>(mutableListOf())
    val networks: LiveData<MutableList<WifiNetwork>> = _networks
    private val _lastKnownLocation = MutableLiveData<LatLng>()
    val lastKnownLocation: LiveData<LatLng> = _lastKnownLocation

    fun addNetwork(network: WifiNetwork) {
        val currentList = _networks.value ?: mutableListOf()
        if (currentList.none { it.bssid == network.bssid }) {
            currentList.add(network)
            _networks.postValue(currentList)
            Log.d("SharedWifiViewModel", "✓ Added network: ${network.ssid} (${network.bssid}). Total: ${currentList.size}")
        } else {
            Log.d("SharedWifiViewModel", "Network already exists: ${network.ssid} (${network.bssid})")
        }
    }

    fun pushLastNetworkToMap() {
        val last = _networks.value?.lastOrNull() ?: return
        _lastKnownLocation.postValue(LatLng(last.lat, last.lng))
        Log.d("SharedWifiViewModel", "Pushed location to map: ${last.lat}, ${last.lng}")
    }

    fun clearNetworks() {
        _networks.value = mutableListOf()
        Log.d("SharedWifiViewModel", "Cleared all networks")
    }

    // Debug method to check current state
    fun getDebugInfo(): String {
        val networkCount = _networks.value?.size ?: 0
        val lastNetwork = _networks.value?.lastOrNull()
        return "ViewModel ${hashCode()}: $networkCount networks, last: ${lastNetwork?.ssid ?: "none"}"
    }
}