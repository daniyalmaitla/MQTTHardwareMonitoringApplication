package com.app.mqtthardwareapp

// Your data class
data class DeviceData(
    val speedSetting: Double,
    val currentSpeed: Double,
    val remainingDistance: Double,
    val remainingTime: Double,
    val batteryVoltage: Double,
    val pressure: Double,
    val pressureLimit: Int,
    val automaticReportsEnable: Int,
    val finalFinishNotification: Int,
    val beforeFinishNotification: Int,
    val lowPressureAlarm: Int,
    val lowSpeedAlarm: Int,
    val mainValveStatus: Int,
    val spare14: String,
    val spare15: String,
    val spare16: String,
    val spare17: String,
    val spare18: String,
    val spare19: String,
    val spare20: String
)

// Parser function
fun parsePayload(payload: String): DeviceData {
    val values = payload.split(":")

    return DeviceData(
        speedSetting = values.getOrNull(0)?.toDoubleOrNull() ?: 0.0,
        currentSpeed = values.getOrNull(1)?.toDoubleOrNull() ?: 0.0,
        remainingDistance = values.getOrNull(2)?.toDoubleOrNull() ?: 0.0,
        remainingTime = values.getOrNull(3)?.toDoubleOrNull() ?: 0.0,
        batteryVoltage = values.getOrNull(4)?.toDoubleOrNull() ?: 0.0,
        pressure = values.getOrNull(5)?.toDoubleOrNull() ?: 0.0,
        pressureLimit = values.getOrNull(6)?.toIntOrNull() ?: 0,
        automaticReportsEnable = values.getOrNull(7)?.toIntOrNull() ?: 0,
        finalFinishNotification = values.getOrNull(8)?.toIntOrNull() ?: 0,
        beforeFinishNotification = values.getOrNull(9)?.toIntOrNull() ?: 0,
        lowPressureAlarm = values.getOrNull(10)?.toIntOrNull() ?: 0,
        lowSpeedAlarm = values.getOrNull(11)?.toIntOrNull() ?: 0,
        mainValveStatus = values.getOrNull(12)?.toIntOrNull() ?: 0,
        spare14 = values.getOrNull(13) ?: "",
        spare15 = values.getOrNull(14) ?: "",
        spare16 = values.getOrNull(15) ?: "",
        spare17 = values.getOrNull(16) ?: "",
        spare18 = values.getOrNull(17) ?: "",
        spare19 = values.getOrNull(18) ?: "",
        spare20 = values.getOrNull(19) ?: ""
    )
}
