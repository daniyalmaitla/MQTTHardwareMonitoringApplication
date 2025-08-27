package com.app.mqtthardwareapp.States

import com.app.mqtthardwareapp.Data.Device

data class DeviceState(
    val devices: List<Device> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)