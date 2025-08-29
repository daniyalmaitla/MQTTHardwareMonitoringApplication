package com.app.mqtthardwareapp.Data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceId: String,
    val slot: Int,
    val name: String,
    val interval: Long,
    val enabled: Boolean,
    val subscribedReadTopic: String,
    val subscribedWriteTopic: String
)