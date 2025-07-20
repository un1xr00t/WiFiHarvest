package com.app.wifiharvest

/**
 * Simple singleton to ensure the same ViewModel instance is used
 * across Activity, Fragments, and Service
 */
object WifiViewModelHolder {
    private var _viewModel: SharedWifiViewModel? = null

    fun getViewModel(): SharedWifiViewModel {
        if (_viewModel == null) {
            _viewModel = SharedWifiViewModel()
        }
        return _viewModel!!
    }

    fun setViewModel(viewModel: SharedWifiViewModel) {
        _viewModel = viewModel
    }
}