package com.escossio.agendafalante

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.escossio.agendafalante.bridge.EventDispatcher
import com.escossio.agendafalante.bridge.IncomingCallEvent
import com.escossio.agendafalante.bridge.IncomingCallSimulationBridge
import com.escossio.agendafalante.corecontract.ExperiencePackage
import com.escossio.agendafalante.corecontract.ExperiencePackageReader
import com.escossio.agendafalante.corecontract.ExperiencePackageValidation
import com.escossio.agendafalante.corecontract.ExperiencePackageValidator
import com.escossio.agendafalante.playback.DemoExperienceSelector
import com.escossio.agendafalante.playback.ExperiencePlaybackController
import com.escossio.agendafalante.playback.ExperiencePlaybackState
import com.escossio.agendafalante.telephony.TelephonyBridge
import com.escossio.agendafalante.telephony.TelephonyStatus
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BootstrapScreen()
                }
            }
        }
    }
}

@Composable
private fun BootstrapScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playbackController = remember { ExperiencePlaybackController(context) }
    val demoExperienceSelector = remember { DemoExperienceSelector() }
    val diagnosticsState = remember { DiagnosticsState() }
    val hasPhoneStatePermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_PHONE_STATE,
    ) == PackageManager.PERMISSION_GRANTED
    var telephonyPermissionGranted by remember { mutableStateOf(hasPhoneStatePermission) }
    var telephonyStatus by remember { mutableStateOf(if (hasPhoneStatePermission) "Monitoring" else "Permission Required") }
    var lastTelephonyEvent by remember { mutableStateOf<IncomingCallEvent?>(null) }
    var packageSnapshot by remember { mutableStateOf<PackageSnapshot?>(null) }
    var playbackStatus by remember { mutableStateOf("Experience Playback: Pending") }
    var lastEventState by remember { mutableStateOf("Idle") }
    var lastEventContact by remember { mutableStateOf("-") }
    var lastEventName by remember { mutableStateOf("None") }
    var lastEventStatus by remember { mutableStateOf("Idle") }
    val packageDir = context.filesDir.resolve("demo-package")

    val requestPhoneStatePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        telephonyPermissionGranted = granted
        telephonyStatus = if (granted) "Monitoring" else "Permission Required"
        diagnosticsState.telephonyState = telephonyStatus
    }

    val selectedSegments = packageSnapshot?.let { snapshot ->
        if (snapshot.validation.valid) {
            demoExperienceSelector.select(packageDir, snapshot.experiencePackage)
        } else {
            emptyList()
        }
    }.orEmpty()
    val playableSegmentsCount = selectedSegments.size

    val simulationBridge = remember {
        IncomingCallSimulationBridge(
            dispatcher = EventDispatcher(
                demoExperienceSelector = demoExperienceSelector,
                playbackController = playbackController,
                callbacks = object : EventDispatcher.Callbacks {
                    override fun packageDirectory(): File = packageDir

                    override fun onDispatching(event: IncomingCallEvent) {
                        diagnosticsState.bridgeState = "Dispatching"
                        lastEventState = "Dispatching"
                        lastEventName = "Incoming Call"
                        lastEventContact = event.contactName
                        lastEventStatus = "Dispatching"
                        playbackStatus = "Dispatching"
                    }

                    override fun onDispatched(event: IncomingCallEvent) {
                        diagnosticsState.bridgeState = "Ready"
                        lastEventState = "Dispatched"
                        lastEventName = "Incoming Call"
                        lastEventContact = event.contactName
                        lastEventStatus = "Dispatched"
                    }

                    override fun onDispatchFailed(event: IncomingCallEvent, reason: String) {
                        diagnosticsState.bridgeState = "Failed"
                        diagnosticsState.playbackState = "Failed"
                        lastEventState = "Failed"
                        lastEventName = "Incoming Call"
                        lastEventContact = event.contactName
                        lastEventStatus = reason
                        playbackStatus = reason
                    }

                    override fun onPlaybackStateChanged(state: ExperiencePlaybackState) {
                        diagnosticsState.playbackState = when (state) {
                            ExperiencePlaybackState.Idle -> "Idle"
                            ExperiencePlaybackState.Playing -> "Playing"
                            ExperiencePlaybackState.Completed -> "Completed"
                            ExperiencePlaybackState.Failed -> "Failed"
                        }
                        playbackStatus = "Experience Playback: ${diagnosticsState.playbackState}"
                        if (state == ExperiencePlaybackState.Completed) {
                            lastEventState = "Completed"
                            lastEventStatus = "Completed"
                        }
                    }
                },
            ),
        )
    }

    val telephonyBridge = remember {
        TelephonyBridge(
            context = context,
            experiencePackageIdProvider = {
                packageSnapshot?.packageId.takeUnless { it.isNullOrBlank() } ?: "demo-package"
            },
            callbacks = object : TelephonyBridge.Callbacks {
                override fun onStatusChanged(status: TelephonyStatus) {
                    telephonyStatus = when (status) {
                        TelephonyStatus.Monitoring -> "Monitoring"
                        TelephonyStatus.PermissionRequired -> "Permission Required"
                        is TelephonyStatus.Disabled -> "Disabled"
                    }
                    diagnosticsState.telephonyState = telephonyStatus
                }

                override fun onIncomingCallEvent(event: IncomingCallEvent) {
                    lastTelephonyEvent = event
                    diagnosticsState.lastTelephonyEvent = "eventId=${event.eventId}, timestamp=${event.timestamp}, contactName=${event.contactName}, experiencePackageId=${event.experiencePackageId}, type=${event.type}"
                    lastEventName = "Incoming Call"
                    lastEventContact = event.contactName
                    lastEventState = "Received"
                    lastEventStatus = "Telephony event published."
                    diagnosticsState.bridgeState = "Ready"
                }
            },
        )
    }

    DisposableEffect(telephonyPermissionGranted) {
        if (telephonyPermissionGranted) {
            telephonyBridge.start()
        } else {
            telephonyStatus = "Permission Required"
            diagnosticsState.telephonyState = telephonyStatus
        }
        onDispose {
            telephonyBridge.stop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(text = "Agenda Falante", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(text = "Android Platform", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        if (DEVELOPER_MODE) {
            Text(text = "Developer Diagnostics", modifier = Modifier.padding(top = 24.dp), fontWeight = FontWeight.SemiBold)
            Text(text = "Platform: ${diagnosticsState.platformState}")
            Text(text = "Telephony: ${diagnosticsState.telephonyState}")
            Text(text = "Bridge: ${diagnosticsState.bridgeState}")
            Text(text = "Experience Package: ${diagnosticsState.experiencePackageState}")
            Text(text = "Playback: ${diagnosticsState.playbackState}")
            Text(text = "Last Event: $lastEventName")
            Text(text = "Contact: $lastEventContact")
            Text(text = "Event State: $lastEventState")
            Text(text = "Event Status: $lastEventStatus")
            Text(text = "Last Telephony Event: ${diagnosticsState.lastTelephonyEvent}")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Telephony", fontWeight = FontWeight.SemiBold)
        Text(text = "Status:")
        Text(text = telephonyStatus)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Último evento:", fontWeight = FontWeight.SemiBold)
        lastTelephonyEvent?.let { event ->
            Text(text = "Tipo:")
            Text(text = "Incoming Call")
            Text(text = "Contato:")
            Text(text = event.contactName)
            Text(text = "Horário:")
            Text(text = formatEventTime(event.timestamp))
            Text(text = "Estado:")
            Text(text = "Received")
        } ?: Text(text = "Nenhum")
        Spacer(modifier = Modifier.height(8.dp))
        if (telephonyPermissionGranted) {
            if (DEVELOPER_MODE) {
                Text(text = "READ_PHONE_STATE granted.")
            }
        } else {
            Text(text = "Permissão de telefone necessária para monitorar chamadas.")
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    requestPhoneStatePermission.launch(Manifest.permission.READ_PHONE_STATE)
                },
            ) {
                Text(text = "Permitir monitoramento")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Experience Package: ${if (packageSnapshot == null) "Not Loaded" else "Loaded"}")
        if (packageSnapshot?.validation?.valid == true) {
            Text(text = "Experience Segments: $playableSegmentsCount")
        }
        Text(text = playbackStatus)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    val loaded = withContext(Dispatchers.IO) {
                        loadDemoPackage(context, packageDir)
                    }
                    packageSnapshot = loaded.toSnapshot()
                    playbackStatus = "Experience Playback: Pending"
                    diagnosticsState.onPackageLoaded(loaded.toSnapshot())
                }
            },
        ) {
            Text(text = "Load Demo Package")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (packageSnapshot?.validation?.valid == true) {
            Column {
                Text(text = "Simulation")
                Text(text = "Contato: Leonardo")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val snapshot = packageSnapshot ?: return@Button
                    val experiencePackage = snapshot.experiencePackage ?: return@Button
                    if (selectedSegments.isEmpty()) {
                        playbackStatus = "No playable segments found."
                        diagnosticsState.playbackState = "Failed"
                        lastEventName = "Incoming Call"
                        lastEventContact = "Leonardo"
                        lastEventState = "Failed"
                        lastEventStatus = "No playable segments found."
                        return@Button
                    }
                    simulationBridge.simulate(
                        contactName = "Leonardo",
                        experiencePackage = experiencePackage,
                    )
                },
            ) {
                Text(text = "Simulate Incoming Call")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val firstSegment = selectedSegments.firstOrNull()
                    if (firstSegment == null) {
                        playbackStatus = "No playable segments found."
                        return@Button
                    }
                    playbackController.play(listOf(firstSegment.audioFile)) { state ->
                        playbackStatus = when (state) {
                            ExperiencePlaybackState.Idle -> "Idle"
                            ExperiencePlaybackState.Playing -> "Experience Playback: Playing"
                            ExperiencePlaybackState.Completed -> "Experience Playback: Completed"
                            ExperiencePlaybackState.Failed -> "Experience Playback: Failed"
                        }
                    }
                },
                enabled = selectedSegments.isNotEmpty(),
            ) {
                Text(text = "Play First Segment")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (selectedSegments.isEmpty()) {
                        playbackStatus = "No playable segments found."
                        return@Button
                    }
                    playbackController.play(selectedSegments.map { it.audioFile }) { state ->
                        playbackStatus = when (state) {
                            ExperiencePlaybackState.Idle -> "Idle"
                            ExperiencePlaybackState.Playing -> "Experience Playback: Playing"
                            ExperiencePlaybackState.Completed -> "Experience Playback: Completed"
                            ExperiencePlaybackState.Failed -> "Experience Playback: Failed"
                        }
                    }
                },
                enabled = selectedSegments.isNotEmpty(),
            ) {
                Text(text = "Play Demo Experience")
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (selectedSegments.isEmpty()) {
                Text(text = "No playable segments found.")
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        packageSnapshot?.let { snapshot ->
            val validationLabel = if (snapshot.validation.valid) "OK" else "Failed"
            Text(text = "Package ID: ${snapshot.packageId}")
            Text(text = "Version: ${snapshot.version}")
            Text(text = "Package Type: ${snapshot.packageType}")
            Text(text = "Segments: ${snapshot.segmentCount}")
            Text(text = "Validation: $validationLabel", modifier = Modifier.padding(top = 8.dp))
            if (snapshot.warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Warnings:")
                snapshot.warnings.forEach { warning ->
                    Text(text = "- $warning")
                }
            }
            if (snapshot.errors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Errors:")
                snapshot.errors.forEach { error ->
                    Text(text = "- $error")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (!snapshot.validation.valid) {
                playbackStatus = "Experience Playback: Failed"
            }
        } ?: run {
            Text(text = "Validation: Not Loaded")
        }
    }
}

private class DiagnosticsState {
    var platformState: String = "OK"
    var telephonyState: String = "Disabled"
    var bridgeState: String = "Pending"
    var experiencePackageState: String = "Not Loaded"
    var playbackState: String = "Idle"
    var lastTelephonyEvent: String = "None"

    fun onPackageLoaded(snapshot: PackageSnapshot) {
        experiencePackageState = if (snapshot.validation.valid) "Loaded" else "Failed"
        bridgeState = "Pending"
        playbackState = "Idle"
    }
}

private data class LoadedExperiencePackage(
    val packageValue: ExperiencePackage?,
    val validation: ExperiencePackageValidation,
)

private data class PackageSnapshot(
    val experiencePackage: ExperiencePackage?,
    val packageId: String,
    val version: String,
    val packageType: String,
    val segmentCount: Int,
    val validation: ExperiencePackageValidation,
    val warnings: List<String>,
    val errors: List<String>,
)

private fun formatEventTime(timestamp: Long): String {
    return EVENT_TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
}

private fun loadDemoPackage(context: Context, destination: File): LoadedExperiencePackage {
    if (destination.exists()) {
        destination.deleteRecursively()
    }
    destination.mkdirs()
    copyAssetDirectory(context, destination, "demo-package")
    val validation = ExperiencePackageValidator().validate(destination)
    val packageValue = if (validation.valid) {
        ExperiencePackageReader().read(destination)
    } else {
        null
    }
    return LoadedExperiencePackage(packageValue = packageValue, validation = validation)
}

private fun LoadedExperiencePackage.toSnapshot(): PackageSnapshot {
    val packageValue = packageValue
    return PackageSnapshot(
        experiencePackage = packageValue,
        packageId = packageValue?.metadata?.packageId.orEmpty(),
        version = packageValue?.metadata?.version.orEmpty(),
        packageType = packageValue?.metadata?.packageType.orEmpty(),
        segmentCount = packageValue?.manifest?.segments?.size ?: 0,
        validation = validation,
        warnings = validation.warnings,
        errors = validation.errors,
    )
}

private fun copyAssetDirectory(context: Context, destination: File, assetPath: String) {
    val assets = context.assets
    val entries = assets.list(assetPath).orEmpty()
    if (entries.isEmpty()) {
        assets.open(assetPath).use { input ->
            destination.parentFile?.mkdirs()
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        return
    }

    destination.mkdirs()
    entries.forEach { entry ->
        val childAssetPath = "$assetPath/$entry"
        val childDestination = File(destination, entry)
        val childEntries = assets.list(childAssetPath).orEmpty()
        if (childEntries.isEmpty()) {
            assets.open(childAssetPath).use { input ->
                childDestination.outputStream().use { output -> input.copyTo(output) }
            }
        } else {
            copyAssetDirectory(context, childDestination, childAssetPath)
        }
    }
}

private val EVENT_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private const val DEVELOPER_MODE = false
