package com.escossio.agendafalante.bridge

import com.escossio.agendafalante.corecontract.ExperiencePackage

class IncomingCallSimulationBridge(
    private val dispatcher: EventDispatcher,
) {
    fun simulate(contactName: String, experiencePackage: ExperiencePackage): IncomingCallEvent {
        val event = IncomingCallEvent(
            eventId = "incoming-call-${experiencePackage.metadata.packageId}-${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis(),
            contactName = contactName,
            experiencePackageId = experiencePackage.metadata.packageId,
        )
        dispatcher.dispatch(event, experiencePackage)
        return event
    }
}
