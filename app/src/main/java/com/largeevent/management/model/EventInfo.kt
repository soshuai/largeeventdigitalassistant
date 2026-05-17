package com.largeevent.management.model

import java.io.*
import java.util.*

class EventInfo(
    name: String?,
    sessions: MutableList<EventSession?>?,
    venuePermissions: MutableList<String?>?,
    areaPermissions: MutableList<String?>?
) : Serializable {
    @JvmField
    val name: String
    private val sessions: MutableList<EventSession?>
    private val venuePermissions: MutableList<String?>
    private val areaPermissions: MutableList<String?>

    init {
        this.name = if (name == null) "未设置活动" else name
        this.sessions =
            if (sessions == null) ArrayList<EventSession?>() else ArrayList<EventSession?>(sessions)
        this.venuePermissions = if (venuePermissions == null) ArrayList<String?>()
        else ArrayList<String?>(venuePermissions)
        this.areaPermissions = if (areaPermissions == null) ArrayList<String?>()
        else ArrayList<String?>(areaPermissions)
    }

    fun getSessions(): MutableList<EventSession?> {
        return Collections.unmodifiableList<EventSession?>(sessions)
    }

    fun getVenuePermissions(): MutableList<String?> {
        return Collections.unmodifiableList<String?>(venuePermissions)
    }

    fun getAreaPermissions(): MutableList<String?> {
        return Collections.unmodifiableList<String?>(areaPermissions)
    }
}


