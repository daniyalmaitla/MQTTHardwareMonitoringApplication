package com.app.mqtthardwareapp

// Your data class
data class DeviceData(
    val speedSetting: Double? = null,
    val currentSpeed: Double? = null,
    val remainingDistance: Double? = null,
    val remainingTime: Double? = null,
    val batteryVoltage: Double? = null,
    val pressure: Double? = null,
    val pressureLimit: Int? = null,
    val automaticReportsEnable: Int? = null,
    val finalFinishNotification: Int? = null,
    val beforeFinishNotification: Int? = null,
    val lowPressureAlarm: Int? = null,
    val lowSpeedAlarm: Int? = null,
    val mainValveStatus: Int? = null,
    val spare14: String? = null,
    val spare15: String? = null,
    val spare16: String? = null,
    val spare17: String? = null,
    val spare18: String? = null,
    val spare19: String? = null,
    val spare20: String? = null
)

// Parser function
/*fun parsePayload(payload: String): DeviceData {
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
}*/
fun parsePayload(payload: String): DeviceData {
    val values = payload.split(":")

    return DeviceData(
        speedSetting = values.getOrNull(0)?.toDoubleOrNull(),
        currentSpeed = values.getOrNull(1)?.toDoubleOrNull(),
        remainingDistance = values.getOrNull(2)?.toDoubleOrNull(),
        remainingTime = values.getOrNull(3)?.toDoubleOrNull(),
        batteryVoltage = values.getOrNull(4)?.toDoubleOrNull(),
        pressure = values.getOrNull(5)?.toDoubleOrNull(),
        pressureLimit = values.getOrNull(6)?.toIntOrNull(),
        automaticReportsEnable = values.getOrNull(7)?.toIntOrNull(),
        finalFinishNotification = values.getOrNull(8)?.toIntOrNull(),
        beforeFinishNotification = values.getOrNull(9)?.toIntOrNull(),
        lowPressureAlarm = values.getOrNull(10)?.toIntOrNull(),
        lowSpeedAlarm = values.getOrNull(11)?.toIntOrNull(),
        mainValveStatus = values.getOrNull(12)?.toIntOrNull(),
        spare14 = values.getOrNull(13),
        spare15 = values.getOrNull(14),
        spare16 = values.getOrNull(15),
        spare17 = values.getOrNull(16),
        spare18 = values.getOrNull(17),
        spare19 = values.getOrNull(18),
        spare20 = values.getOrNull(19)
    )
}

