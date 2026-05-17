package com.largeevent.management.model

import java.io.*

class EventSession(val title: String?, val startTime: String?, val endTime: String?) :
    Serializable {
    val displayText: String
        get() {
            val builder = StringBuilder()
            builder.append(if (title == null) "" else title)
            if (startTime != null && endTime != null) {
                builder.append(" (").append(startTime).append(" ~ ").append(endTime).append(")")
            }
            return builder.toString()
        }
}


