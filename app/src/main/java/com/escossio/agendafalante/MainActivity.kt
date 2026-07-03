package com.escossio.agendafalante

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.escossio.agendafalante.corecontract.ExperiencePackage
import com.escossio.agendafalante.corecontract.ExperiencePackageReader
import com.escossio.agendafalante.corecontract.ExperiencePackageValidation
import com.escossio.agendafalante.corecontract.ExperiencePackageValidator
import com.escossio.agendafalante.playback.AudioPlaybackController
import com.escossio.agendafalante.playback.firstAvailableSegment
import java.io.File
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
    val playbackController = remember { AudioPlaybackController(context) }
    var packageSnapshot by remember { mutableStateOf<PackageSnapshot?>(null) }
    var playbackStatus by remember { mutableStateOf("Pending") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(text = "Agenda Falante", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(text = "Android Platform", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text(text = "Platform: OK", modifier = Modifier.padding(top = 24.dp))
        Text(text = "Bridge: Pending")
        Text(text = "Experience Package: ${if (packageSnapshot == null) "Not Loaded" else "Loaded"}")
        Text(text = "Playback: $playbackStatus")
        Text(text = "Status: Ready", modifier = Modifier.padding(top = 24.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    val loaded = withContext(Dispatchers.IO) {
                        loadDemoPackage(context, context.filesDir.resolve("demo-package"))
                    }
                    packageSnapshot = loaded.toSnapshot()
                    playbackStatus = "Pending"
                }
            },
        ) {
            Text(text = "Load Demo Package")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (packageSnapshot?.validation?.valid == true) {
            Button(
                onClick = {
                    val pkgDir = context.filesDir.resolve("demo-package")
                    val selected = firstAvailableSegment(pkgDir)
                    if (selected == null || !selected.audioFile.isFile) {
                        playbackStatus = "Failed"
                        return@Button
                    }
                    playbackStatus = "Playing"
                    playbackController.play(
                        wavFile = selected.audioFile,
                        onPlaying = { playbackStatus = "Playing" },
                        onCompleted = { playbackStatus = "Completed" },
                        onFailed = { playbackStatus = "Failed" },
                    )
                },
            ) {
                Text(text = "Play First Segment")
            }
            Spacer(modifier = Modifier.height(16.dp))
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
                playbackStatus = "Failed"
            }
        } ?: run {
            Text(text = "Validation: Not Loaded")
        }
    }
}

private data class LoadedExperiencePackage(
    val packageValue: ExperiencePackage?,
    val validation: ExperiencePackageValidation,
)

private data class PackageSnapshot(
    val packageId: String,
    val version: String,
    val packageType: String,
    val segmentCount: Int,
    val validation: ExperiencePackageValidation,
    val warnings: List<String>,
    val errors: List<String>,
)

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
