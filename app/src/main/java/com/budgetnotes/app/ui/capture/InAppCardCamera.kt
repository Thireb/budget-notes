package com.budgetnotes.app.ui.capture

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

/**
 * In-app camera that writes only to [outputFile] (app-private). Never uses MediaStore / gallery.
 */
@Composable
fun InAppCardCamera(
    outputFile: File,
    onCaptured: (File) -> Unit,
    onCancel: () -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var capturing by remember { mutableStateOf(false) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var provider: ProcessCameraProvider? = null

        cameraProviderFuture.addListener(
            {
                try {
                    provider = cameraProviderFuture.get()
                    provider?.unbindAll()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = capture
                    provider?.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture,
                    )
                } catch (e: Exception) {
                    onError(e.message ?: "Camera failed")
                }
            },
            mainExecutor,
        )

        onDispose {
            try {
                provider?.unbindAll()
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Cancel",
                tint = Color.White,
            )
        }

        Text(
            text = "Photo stays in the app only",
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (capturing) {
                CircularProgressIndicator(color = Color.White)
            } else {
                IconButton(
                    onClick = {
                        val capture = imageCapture ?: return@IconButton
                        capturing = true
                        outputFile.parentFile?.mkdirs()
                        if (outputFile.exists()) outputFile.delete()
                        val options = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                        capture.takePicture(
                            options,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    capturing = false
                                    onCaptured(outputFile)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    capturing = false
                                    onError(exception.message ?: "Capture failed")
                                }
                            },
                        )
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White, CircleShape),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White, CircleShape),
                    )
                }
            }
        }
    }
}
