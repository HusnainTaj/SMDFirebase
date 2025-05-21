package com.smd.l226786.smdfirebase.util

object DepartmentMapping {

    val DEPARTMENTS = mapOf(
        "Computer Science" to 1,
        "Software Engineering" to 2,
        "Artificial Intelligence" to 3,
        "Civil Engineering" to 4,
        "Electrical Engineering" to 5,
        "Mechanical Engineering" to 6,
        "Data Science" to 7
    )

    fun getDepartmentName(id: Int): String {
        return DEPARTMENTS.entries.firstOrNull { it.value == id }?.key ?: "Unknown"
    }

    fun getDepartmentId(name: String): Int {
        return DEPARTMENTS[name] ?: -1
    }

    fun getDepartmentNames(): List<String> {
        return DEPARTMENTS.keys.toList()
    }
}
