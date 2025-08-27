package com.app.mqtthardwareapp.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DeviceViewModelFactory(
    private val repo: DeviceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceViewModel::class.java)) {
            return DeviceViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}