package com.escossio.agendafalante.bridge

data class IncomingCallEvent(
    val eventId: String,
    val timestamp: Long,
    val contactName: String,
    val experiencePackageId: String,
    val type: String = "incoming_call",
)
