package com.app.mqtthardwareapp.Utils

import android.content.Context
import android.content.SharedPreferences
import com.app.mqtthardwareapp.DeviceData
import org.json.JSONObject

object PrefsHelper {
    private const val PREFS_NAME = "mqtt_prefs"
    private const val KEY_CLIENT_ID = "client_id"
    private const val KEY_SELECTED_DEVICE = "selected_device"
    private const val KEY_SAVED_IDS = "saved_device_ids"
    private const val KEY_SERVER_DETAILS = "server_details"


    const val DEFAULT_SERVER_JSON = """
        {
            "serverUri": "tcp://ce47707f.ala.dedicated.gcp.emqxcloud.com:1883",
            "username": "positron",
            "password": "positron"
        }
    """

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveServerDetails(context: Context, json: String) {
        prefs(context).edit().putString(KEY_SERVER_DETAILS, json).apply()
    }

    fun getServerDetails(context: Context): String {
        return prefs(context).getString(KEY_SERVER_DETAILS, DEFAULT_SERVER_JSON)!!
    }


    fun saveClientId(context: Context, clientId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CLIENT_ID, clientId).apply()
    }

    fun getClientId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CLIENT_ID, "") ?: ""
    }


    fun saveSelectedDevice(context: Context, deviceId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_DEVICE, deviceId).apply()
    }
    fun saveDeviceData(context: Context, deviceId: String, data: DeviceData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = deviceDataToJson(data)
        prefs.edit().putString("device_data_$deviceId", json).apply()

        // maintain list of saved ids
        val savedIds = prefs.getString(KEY_SAVED_IDS, "") ?: ""
        val setIds = savedIds.split(",").filter { it.isNotBlank() }.toMutableSet()
        setIds.add(deviceId)
        prefs.edit().putString(KEY_SAVED_IDS, setIds.joinToString(",")).apply()
    }

    fun getSelectedDevice(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_DEVICE, null)
    }
    fun getAllSavedDeviceData(context: Context): Map<String, DeviceData> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedIds = prefs.getString(KEY_SAVED_IDS, "") ?: ""
        val ids = savedIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val out = mutableMapOf<String, DeviceData>()
        ids.forEach { id ->
            try {
                prefs.getString("device_data_$id", null)?.let { json ->
                    jsonToDeviceData(json)?.let { dd -> out[id] = dd }
                }
            } catch (_: Exception) { /* ignore malformed */ }
        }
        return out
    }

    fun removeDeviceData(context: Context, deviceId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("device_data_$deviceId").apply()
        val savedIds = prefs.getString(KEY_SAVED_IDS, "") ?: ""
        val setIds = savedIds.split(",").filter { it.isNotBlank() }.toMutableSet()
        setIds.remove(deviceId)
        prefs.edit().putString(KEY_SAVED_IDS, setIds.joinToString(",")).apply()
    }

    // --------------------- JSON helpers ---------------------

    private fun deviceDataToJson(d: DeviceData): String {
        val j = JSONObject()
        // only write non-null fields so JSON stays compact
        d.speedSetting?.let { j.put("speedSetting", it) }
        d.currentSpeed?.let { j.put("currentSpeed", it) }
        d.remainingDistance?.let { j.put("remainingDistance", it) }
        d.remainingTime?.let { j.put("remainingTime", it) }
        d.batteryVoltage?.let { j.put("batteryVoltage", it) }
        d.pressure?.let { j.put("pressure", it) }
        d.pressureLimit?.let { j.put("pressureLimit", it) }
        d.automaticReportsEnable?.let { j.put("automaticReportsEnable", it) }
        d.finalFinishNotification?.let { j.put("finalFinishNotification", it) }
        d.beforeFinishNotification?.let { j.put("beforeFinishNotification", it) }
        d.lowPressureAlarm?.let { j.put("lowPressureAlarm", it) }
        d.lowSpeedAlarm?.let { j.put("lowSpeedAlarm", it) }
        d.mainValveStatus?.let { j.put("mainValveStatus", it) }
        // add other fields if you need them...
        return j.toString()
    }

    private fun jsonToDeviceData(json: String): DeviceData? {
        return try {
            val j = JSONObject(json)

            fun doubleIfPresent(key: String): Double? =
                if (j.has(key) && !j.isNull(key)) j.getDouble(key) else null

            fun intIfPresent(key: String): Int? =
                if (j.has(key) && !j.isNull(key)) j.getInt(key) else null

            DeviceData(
                speedSetting = doubleIfPresent("speedSetting"),
                currentSpeed = doubleIfPresent("currentSpeed"),
                remainingDistance = doubleIfPresent("remainingDistance"),
                remainingTime = doubleIfPresent("remainingTime"),
                batteryVoltage = doubleIfPresent("batteryVoltage"),
                pressure = doubleIfPresent("pressure"),
                pressureLimit = intIfPresent("pressureLimit"),
                automaticReportsEnable = intIfPresent("automaticReportsEnable"),
                finalFinishNotification = intIfPresent("finalFinishNotification"),
                beforeFinishNotification = intIfPresent("beforeFinishNotification"),
                lowPressureAlarm = intIfPresent("lowPressureAlarm"),
                lowSpeedAlarm = intIfPresent("lowSpeedAlarm"),
                mainValveStatus = intIfPresent("mainValveStatus")
            )
        } catch (e: Exception) {
            null
        }
    }

}