package com.app.mqtthardwareapp.ViewModels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.mqtthardwareapp.Data.Device
import com.app.mqtthardwareapp.DeviceData
import com.app.mqtthardwareapp.Events.DeviceEvent
import com.app.mqtthardwareapp.MqttManager
import com.app.mqtthardwareapp.Screens.DeviceWithData
import com.app.mqtthardwareapp.States.DeviceState
import com.app.mqtthardwareapp.Utils.PrefsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeviceViewModel(private val repo: DeviceRepository,

) : ViewModel() {
    private val _state = MutableStateFlow(DeviceState())
    val state: StateFlow<DeviceState> = _state.asStateFlow()


    fun onEvent(event: DeviceEvent) {
        when (event) {
            is DeviceEvent.AddDevice -> {
                viewModelScope.launch {
                    val device = Device(
                        id = event.id,
                        slot = event.slot,
                        deviceId = event.deviceId,
                        name = event.deviceName,
                        interval = event.interval,
                        enabled = event.enabled,
                        subscribedReadTopic = "${event.deviceId}READ",
                        subscribedWriteTopic = "${event.deviceId}WRITE"
                    )
                    repo.addDevice(device)
                }
            }

            is DeviceEvent.DeleteDevice -> {
                viewModelScope.launch {
                    val device = _state.value.devices.find { it.id == event.id }
                    device?.let { repo.deleteDevice(it) }
                }
            }

            is DeviceEvent.EnableDevice -> {
                viewModelScope.launch { repo.enableDevice(event.id) }
            }

            is DeviceEvent.DisableDevice -> {
                viewModelScope.launch { repo.disableDevice(event.id) }
            }

            is DeviceEvent.LoadDevices -> {
                viewModelScope.launch {
                    repo.getDevices().collect { devices ->
                        _state.value = _state.value.copy(devices = devices)
                    }
                }
            }
        }
    }

    fun saveDevice(id: Int,slot:Int ,deviceName: String, deviceId: String, interval: Long, enabled: Boolean) {
        onEvent(DeviceEvent.AddDevice(id, slot, deviceId, deviceName, interval, enabled))
    }
    fun addDeviceFromQr(scannedValue: String) {
        viewModelScope.launch {
            val currentInterval = repo.getGlobalInterval() ?: 0L
            val nextSlot = getNextAvailableSlot()  // <-- SLOT, not id

            val device = Device(
                id = 0, // let Room auto-generate
                slot = nextSlot, // use calculated slot
                deviceId = scannedValue,
                name = "Scanned-${randomName()}",
                interval = currentInterval,
                enabled = false, // user decides later
                subscribedReadTopic = "${scannedValue}READ",
                subscribedWriteTopic = "${scannedValue}WRITE"
            )

            repo.addDevice(device)
        }
    }

    private fun getNextAvailableSlot(): Int {
        val currentSlots = _state.value.devices.map { it.slot }
        for (i in 1..20) {
            if (!currentSlots.contains(i)) return i
        }
        return 20 // fallback if all are filled
    }
    fun randomName(): String {
        val chars = ('A'..'Z') + ('0'..'9')
        return (1..4).map { chars.random() }.joinToString("")
    }

    val enabledDevices: StateFlow<List<Device>> =
        repo.getEnabledDevices()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Keep the entire selected Device
    private val _selectedDevice = MutableStateFlow<Device?>(null)
    val selectedDevice: StateFlow<Device?> = _selectedDevice

    fun selectDevice(device: Device, context: Context) {
        _selectedDevice.value = device
        PrefsHelper.saveSelectedDevice(context, device.deviceId)
    }
    fun deleteSelectedDevice() {
        viewModelScope.launch {
            _selectedDevice.value?.let { device ->
                // delete the selected one
                repo.deleteDevice(device)
                _selectedDevice.value = null

                // get updated list from DB
                val updatedDevices = repo.getAllDevicesOnce()

                // shift slots: reassign sequentially (1 → N)
                updatedDevices.forEachIndexed { index, dev ->
                    val corrected = dev.copy(slot = index + 1)
                    repo.addDevice(corrected)
                }
            }
        }
    }


    private val _deviceDataMap = MutableStateFlow<Map<String, DeviceData>>(emptyMap())
    val deviceDataMap: StateFlow<Map<String, DeviceData>> = _deviceDataMap
    val enabledDevicesWithData: StateFlow<List<DeviceWithData>> =
        combine(enabledDevices, deviceDataMap) { devices, dataMap ->
            devices.map { device ->
                DeviceWithData(
                    device = device,
                    data = dataMap[device.deviceId] // match by deviceId
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /*fun updateDeviceData(deviceId: String, newData: DeviceData) {
        _deviceDataMap.value = _deviceDataMap.value.toMutableMap().apply {
            val oldData = this[deviceId]
            if (oldData != null) {
                // merge: only update non-null or changed fields
                put(deviceId, oldData.copy(
                    speedSetting = newData.speedSetting ?: oldData.speedSetting,
                    currentSpeed = newData.currentSpeed ?: oldData.currentSpeed,
                    remainingDistance = newData.remainingDistance ?: oldData.remainingDistance,
                    remainingTime = newData.remainingTime ?: oldData.remainingTime,
                    batteryVoltage = newData.batteryVoltage ?: oldData.batteryVoltage,
                    pressure = newData.pressure ?: oldData.pressure,
                    pressureLimit = newData.pressureLimit ?: oldData.pressureLimit,
                    automaticReportsEnable = newData.automaticReportsEnable ?: oldData.automaticReportsEnable,
                    mainValveStatus = newData.mainValveStatus ?: oldData.mainValveStatus,
                    finalFinishNotification = newData.finalFinishNotification?:oldData.finalFinishNotification,
                    lowPressureAlarm = newData.lowPressureAlarm?:oldData.lowPressureAlarm
                ))
            } else {
                // first time, just put it
                put(deviceId, newData)
            }
        }
    }*/
    fun updateDeviceData(context: Context, deviceId: String, newData: DeviceData) {
        _deviceDataMap.value = _deviceDataMap.value.toMutableMap().apply {
            val oldData = this[deviceId]
            val merged = if (oldData != null) {
                oldData.copy(
                    speedSetting = newData.speedSetting ?: oldData.speedSetting,
                    currentSpeed = newData.currentSpeed ?: oldData.currentSpeed,
                    remainingDistance = newData.remainingDistance ?: oldData.remainingDistance,
                    remainingTime = newData.remainingTime ?: oldData.remainingTime,
                    batteryVoltage = newData.batteryVoltage ?: oldData.batteryVoltage,
                    pressure = newData.pressure ?: oldData.pressure,
                    pressureLimit = newData.pressureLimit ?: oldData.pressureLimit,
                    automaticReportsEnable = newData.automaticReportsEnable ?: oldData.automaticReportsEnable,
                    mainValveStatus = newData.mainValveStatus ?: oldData.mainValveStatus,
                    finalFinishNotification = newData.finalFinishNotification ?: oldData.finalFinishNotification,
                    lowPressureAlarm = newData.lowPressureAlarm ?: oldData.lowPressureAlarm
                )
            } else newData

            put(deviceId, merged)

            // persist snapshot
            PrefsHelper.saveDeviceData(context, deviceId, merged)
        }
    }


    fun setIntervalForAll(intervalSec: Long) {
        viewModelScope.launch {
            val updated = state.value.devices.map { it.copy(interval = intervalSec * 1000) } // store ms
            updated.forEach { dev -> repo.addDevice(dev) } // or repo.updateDevice(dev)
            _state.value = _state.value.copy(devices = updated)
        }
    }
    fun restoreSelectedDevice(context: Context, devices: List<Device>) {
        val savedId = PrefsHelper.getSelectedDevice(context)
        if (!savedId.isNullOrEmpty()) {
            devices.firstOrNull { it.deviceId == savedId }?.let { dev ->
                _selectedDevice.value = dev


                PrefsHelper.getAllSavedDeviceData(context)[dev.deviceId]?.let { cached ->
                    _deviceDataMap.value = _deviceDataMap.value.toMutableMap().apply {
                        put(dev.deviceId, cached)
                    }
                }
            }
        }
    }
    private val _globalInterval = MutableStateFlow<Long>(10_000L) // default
    val globalInterval: StateFlow<Long> = _globalInterval

    init {
        viewModelScope.launch {
            val saved = repo.getGlobalInterval()
            if (saved != null) {
                _globalInterval.value = saved
            }
        }
    }






}

