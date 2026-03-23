package com.example.gasonow.utils

import java.util.Calendar

object ScheduleHelper {

    /** Returns true=open, false=closed, null=unknown */
    fun isOpen(horario: String): Boolean? {
        if (horario.isBlank()) return null
        val h = horario.uppercase()
            .replace("Á", "A").replace("É", "E").replace("Í", "I")
            .replace("Ó", "O").replace("Ú", "U").trim()

        // Detect 24h all-week patterns
        val is24h = h.contains("24H") || h.contains("24 H")
        val isAllWeek = h.contains("L-D") || h.contains("LUN-DOM") ||
                h.contains("LUNES A DOMINGO") || h.contains("TODOS LOS DIAS") ||
                (!h.contains(";") && !h.contains(":"))
        if (is24h && isAllWeek) return true

        val calendar = Calendar.getInstance()
        val todayIndex = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        // Pattern: "DAYS: HH:MM-HH:MM" optionally with "24H"
        val segmentPattern = Regex(
            """([A-Z](?:-[A-Z])?(?:/[A-Z](?:-[A-Z])?)*)\s*:\s*(?:(\d{1,2}:\d{2})-(\d{1,2}:\d{2})|24H?)"""
        )
        val matches = segmentPattern.findAll(h).toList()
        if (matches.isEmpty()) return null

        for (match in matches) {
            val dayStr = match.groupValues[1]
            if (!dayApplies(dayStr, todayIndex)) continue

            // If this day segment is 24H
            if (match.groupValues[2].isEmpty()) return true

            val openMin = timeToMinutes(match.groupValues[2]) ?: continue
            val closeMin = timeToMinutes(match.groupValues[3]) ?: continue

            return if (closeMin > openMin) {
                nowMinutes in openMin until closeMin
            } else {
                nowMinutes >= openMin || nowMinutes < closeMin
            }
        }
        return null
    }

    private fun dayApplies(dayStr: String, todayIndex: Int): Boolean {
        val dayMap = mapOf(
            "L" to 0, "LU" to 0, "LUN" to 0,
            "M" to 1, "MA" to 1, "MAR" to 1,
            "X" to 2, "MI" to 2, "MIE" to 2,
            "J" to 3, "JU" to 3, "JUE" to 3,
            "V" to 4, "VI" to 4, "VIE" to 4,
            "S" to 5, "SA" to 5, "SAB" to 5,
            "D" to 6, "DO" to 6, "DOM" to 6
        )
        for (part in dayStr.split("/")) {
            val seg = part.trim()
            if (seg.contains("-")) {
                val sides = seg.split("-")
                val start = dayMap[sides[0].trim()] ?: continue
                val end = dayMap[sides[1].trim()] ?: continue
                if (end >= start) {
                    if (todayIndex in start..end) return true
                } else {
                    if (todayIndex >= start || todayIndex <= end) return true
                }
            } else {
                if (dayMap[seg.trim()] == todayIndex) return true
            }
        }
        return false
    }

    private fun timeToMinutes(time: String): Int? {
        val parts = time.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h * 60 + m
    }
}
