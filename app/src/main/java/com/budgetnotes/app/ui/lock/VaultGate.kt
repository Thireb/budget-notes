package com.budgetnotes.app.ui.lock

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.budgetnotes.app.BudgetNotesApplication
import com.budgetnotes.app.security.VaultLockManager
import javax.crypto.Cipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PIN_LENGTH = 6

@Composable
fun VaultGate(
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as BudgetNotesApplication
    val lockManager = app.container.lockManager
    val scope = rememberCoroutineScope()
    val activity = context as? FragmentActivity

    val systemAuth = remember { lockManager.prefersBiometricGate() }
    var usePinUi by remember {
        mutableStateOf(!systemAuth && (!lockManager.isSetup || lockManager.hasPinFallback))
    }
    var setupMode by remember {
        mutableStateOf(!lockManager.isSetup && !systemAuth)
    }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var autoPrompted by remember { mutableStateOf(false) }

    fun finishUnlock() {
        scope.launch {
            busy = true
            error = null
            try {
                withContext(Dispatchers.IO) {
                    app.container.unlockWithSessionKey()
                }
                onUnlocked()
            } catch (e: Exception) {
                app.container.lock()
                error = e.message ?: "Could not unlock vault"
                busy = false
            }
        }
    }

    fun runSystemAuth() {
        val host = activity ?: return
        if (busy) return
        val isFirstSetup = !lockManager.isSetup
        val prepared: Cipher = if (isFirstSetup) {
            try {
                lockManager.createBiometricCipherForEncrypt()
            } catch (e: Exception) {
                error = e.message ?: "Biometrics unavailable"
                usePinUi = true
                setupMode = true
                return
            }
        } else {
            val decryptCipher = lockManager.createBiometricCipherForDecrypt()
            if (decryptCipher == null) {
                error = "Unlock key missing — set a PIN or reinstall"
                if (lockManager.hasPinFallback) usePinUi = true
                return
            }
            decryptCipher
        }

        busy = true
        promptSystemAuth(
            activity = host,
            cipher = prepared,
            title = if (isFirstSetup) "Set up Budget Notes" else "Unlock Budget Notes",
            subtitle = if (isFirstSetup) {
                "Confirm fingerprint or device lock to encrypt your vault"
            } else {
                "Confirm fingerprint or device lock"
            },
            onResult = { okCipher ->
                if (okCipher == null) {
                    busy = false
                    error = "Authentication canceled"
                    return@promptSystemAuth
                }
                val ok = if (isFirstSetup) {
                    lockManager.setupWithBiometricCipher(okCipher)
                } else {
                    lockManager.unlockWithBiometricCipher(okCipher)
                }
                if (ok) {
                    finishUnlock()
                } else {
                    busy = false
                    error = "Could not open vault"
                }
            },
        )
    }

    // Wallet-style: prompt fingerprint immediately on open when system auth is available.
    LaunchedEffect(Unit) {
        if (systemAuth && !autoPrompted && !usePinUi) {
            autoPrompted = true
            runSystemAuth()
        }
    }

    fun submitPin(value: String) {
        if (busy || value.length != PIN_LENGTH) return
        if (setupMode) {
            if (confirmPin == null) {
                confirmPin = value
                pin = ""
                error = null
                return
            }
            if (value != confirmPin) {
                error = "PINs do not match"
                confirmPin = null
                pin = ""
                return
            }
            busy = true
            scope.launch {
                try {
                    withContext(Dispatchers.Default) {
                        lockManager.setupPin(value.toCharArray())
                    }
                    finishUnlock()
                } catch (e: Exception) {
                    error = e.message ?: "Setup failed"
                    busy = false
                    confirmPin = null
                    pin = ""
                }
            }
        } else {
            busy = true
            scope.launch {
                val ok = withContext(Dispatchers.Default) {
                    lockManager.unlockWithPin(value.toCharArray())
                }
                if (ok) finishUnlock()
                else {
                    error = "Wrong PIN"
                    pin = ""
                    busy = false
                }
            }
        }
    }

    LaunchedEffect(pin) {
        if (usePinUi && !busy && pin.length == PIN_LENGTH) {
            submitPin(pin)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when {
                usePinUi && setupMode && confirmPin == null -> "Create a PIN"
                usePinUi && setupMode -> "Confirm PIN"
                usePinUi -> "Enter PIN"
                !lockManager.isSetup -> "Protect your vault"
                else -> "Unlock Budget Notes"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                usePinUi && setupMode && confirmPin == null ->
                    "This device has no fingerprint/face lock. Set a 6-digit app PIN instead."
                usePinUi && setupMode -> "Enter the same PIN again."
                usePinUi -> "Enter your app PIN."
                !lockManager.isSetup ->
                    "Use your fingerprint (or device lock) to encrypt notes and cards on this phone."
                else ->
                    "Use your fingerprint or device lock to open the vault."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (busy) {
            CircularProgressIndicator()
        } else if (!usePinUi) {
            Button(onClick = { runSystemAuth() }) {
                Text(if (lockManager.isSetup) "Unlock with fingerprint" else "Set up with fingerprint")
            }
            if (lockManager.hasPinFallback) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { usePinUi = true; error = null }) {
                    Text("Use app PIN instead")
                }
            }
        } else {
            PinDots(length = pin.length, max = PIN_LENGTH)
            Spacer(modifier = Modifier.height(16.dp))
            PinPad(
                onDigit = { d ->
                    if (pin.length < PIN_LENGTH) {
                        pin += d
                        error = null
                    }
                },
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
            )
            if (systemAuth) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        usePinUi = false
                        pin = ""
                        error = null
                        runSystemAuth()
                    },
                ) {
                    Text("Use fingerprint instead")
                }
            }
        }
    }
}

@Composable
private fun PinDots(length: Int, max: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(max) { index ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < length) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}

@Composable
private fun PinPad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { d ->
                    TextButton(onClick = { onDigit(d) }, modifier = Modifier.size(72.dp)) {
                        Text(d.toString(), style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.size(72.dp))
            TextButton(onClick = { onDigit('0') }, modifier = Modifier.size(72.dp)) {
                Text("0", style = MaterialTheme.typography.headlineMedium)
            }
            IconButton(onClick = onBackspace, modifier = Modifier.size(72.dp)) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete")
            }
        }
    }
}

private fun promptSystemAuth(
    activity: FragmentActivity,
    cipher: Cipher,
    title: String,
    subtitle: String,
    onResult: (Cipher?) -> Unit,
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(result.cryptoObject?.cipher)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult(null)
            }

            override fun onAuthenticationFailed() {
                // Keep dialog open for another try
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(VaultLockManager.SYSTEM_AUTHENTICATORS)
        .build()
    prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
}
