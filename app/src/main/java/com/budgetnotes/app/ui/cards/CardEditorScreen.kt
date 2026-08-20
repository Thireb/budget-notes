package com.budgetnotes.app.ui.cards

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetnotes.app.BudgetNotesApplication
import com.budgetnotes.app.data.CardImageStore
import com.budgetnotes.app.data.CardType
import com.budgetnotes.app.ui.capture.InAppCardCamera
import com.budgetnotes.app.ui.theme.RedNegative
import com.budgetnotes.app.util.CardExpiry
import com.budgetnotes.app.util.ExpiryStatus
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditorScreen(
    viewModel: CardEditorViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val customFields by viewModel.customFields.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var imagePickSide by remember { mutableStateOf<CardImageStore.ImageSide?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showInAppCamera by remember { mutableStateOf(false) }
    var cameraOutputFile by remember { mutableStateOf<File?>(null) }
    var showAddFieldDialog by remember { mutableStateOf(false) }
    var newFieldName by remember { mutableStateOf("") }
    var checkoutJob by remember { mutableStateOf<Job?>(null) }

    val cardType = state.cardType
    val runOcr = cardType == CardType.PAYMENT || cardType == CardType.ID
    val imageStore = rememberImageStore(context)

    DisposableEffect(Unit) {
        onDispose { checkoutJob?.cancel() }
    }

    fun openInAppCamera(side: CardImageStore.ImageSide) {
        imagePickSide = side
        cameraOutputFile = viewModel.prepareCameraFile(side)
        showInAppCamera = true
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        val side = imagePickSide
        // Copies into app-private storage only — does not write back to the gallery.
        if (uri != null && side != null) {
            viewModel.onImagePicked(side, uri, runOcr = runOcr)
        }
        imagePickSide = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val side = imagePickSide
        if (granted && side != null) {
            openInAppCamera(side)
        } else {
            imagePickSide = null
            if (!granted) {
                scope.launch {
                    snackbarHostState.showSnackbar("Camera permission needed to take a photo")
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                CardEditorEvent.CardDeleted -> onBack()
                is CardEditorEvent.Copied -> {
                    snackbarHostState.showSnackbar("${event.label} copied")
                }
                is CardEditorEvent.Message -> {
                    snackbarHostState.showSnackbar(event.text)
                }
            }
        }
    }

    if (showInAppCamera) {
        val output = cameraOutputFile
        val side = imagePickSide
        if (output != null && side != null) {
            InAppCardCamera(
                outputFile = output,
                onCaptured = { file ->
                    showInAppCamera = false
                    cameraOutputFile = null
                    imagePickSide = null
                    viewModel.onCameraCaptured(side, file, runOcr = runOcr)
                },
                onCancel = {
                    showInAppCamera = false
                    cameraOutputFile = null
                    imagePickSide = null
                    output.delete()
                },
                onError = { message ->
                    showInAppCamera = false
                    cameraOutputFile = null
                    imagePickSide = null
                    scope.launch { snackbarHostState.showSnackbar(message) }
                },
            )
            return
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = state.labelDraft,
                        onValueChange = viewModel::onLabelChange,
                        singleLine = true,
                        placeholder = { Text("Label") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleRevealSecrets) {
                        Icon(
                            imageVector = if (state.revealSecrets) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (state.revealSecrets) "Hide secrets" else "Reveal secrets",
                        )
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete card") },
                            onClick = {
                                menuExpanded = false
                                confirmDelete = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (!state.loaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (state.missing || cardType == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Card not found")
            }
        } else {
            Box(modifier = Modifier.padding(padding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Photos", style = MaterialTheme.typography.titleMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        CardImageSlot(
                            title = "Front",
                            relativePath = state.frontImagePath,
                            imageStore = imageStore,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                imagePickSide = CardImageStore.ImageSide.FRONT
                                showImageSourceDialog = true
                            },
                        )
                        CardImageSlot(
                            title = "Back",
                            relativePath = state.backImagePath,
                            imageStore = imageStore,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                imagePickSide = CardImageStore.ImageSide.BACK
                                showImageSourceDialog = true
                            },
                        )
                    }

                    if (state.isOcrRunning) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Reading card…")
                        }
                    }

                    when (cardType) {
                        CardType.PAYMENT -> PaymentFields(
                            state = state,
                            viewModel = viewModel,
                            context = context,
                            onCopyForCheckout = {
                                checkoutJob?.cancel()
                                checkoutJob = scope.launch {
                                    runCheckoutCopy(
                                        context = context,
                                        state = state,
                                        snackbarHostState = snackbarHostState,
                                    )
                                }
                            },
                        )
                        CardType.ID -> IdFields(
                            state = state,
                            customFields = customFields,
                            viewModel = viewModel,
                            context = context,
                            onAddCustomField = {
                                newFieldName = ""
                                showAddFieldDialog = true
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = {
                showImageSourceDialog = false
                imagePickSide = null
            },
            title = { Text("Add photo") },
            text = {
                Text(
                    "Photos are stored only inside this app (not in your gallery). " +
                        "Camera captures in-app; Gallery only copies an existing photo into the app.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        val side = imagePickSide ?: return@TextButton
                        val hasCam = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCam) {
                            openInAppCamera(side)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                ) {
                    Text("Camera")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        galleryLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                ) {
                    Text("Gallery")
                }
            },
        )
    }

    if (showAddFieldDialog) {
        AlertDialog(
            onDismissRequest = { showAddFieldDialog = false },
            title = { Text("Custom field") },
            text = {
                OutlinedTextField(
                    value = newFieldName,
                    onValueChange = { newFieldName = it },
                    singleLine = true,
                    label = { Text("Field name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addCustomField(newFieldName)
                        showAddFieldDialog = false
                    },
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFieldDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete card?") },
            text = { Text("This permanently deletes the card and its photos.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.deleteCard()
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun rememberImageStore(context: Context): CardImageStore {
    return remember(context) {
        (context.applicationContext as com.budgetnotes.app.BudgetNotesApplication)
            .container.imageStore
    }
}

@Composable
private fun CardImageSlot(
    title: String,
    relativePath: String?,
    imageStore: CardImageStore,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    val sizePx = with(density) { 360.dp.roundToPx() }
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (relativePath != null) {
                EncryptedCardImage(
                    relativePath = relativePath,
                    imageStore = imageStore,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    sizePx = sizePx,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Text("Add", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private val ExpiringSoonAmber = Color(0xFFE65100)
private const val CHECKOUT_STEP_DELAY_MS = 8_000L

@Composable
private fun ExpiryStatusBanner(status: ExpiryStatus) {
    val label = CardExpiry.chipLabel(status) ?: return
    val color = when (status) {
        ExpiryStatus.EXPIRED -> RedNegative
        ExpiryStatus.EXPIRING_SOON -> ExpiringSoonAmber
        else -> return
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = when (status) {
                ExpiryStatus.EXPIRED -> "$label — update or replace this card"
                ExpiryStatus.EXPIRING_SOON -> "$label — expires within ${CardExpiry.WARN_WITHIN_DAYS} days"
                else -> label
            },
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

private suspend fun runCheckoutCopy(
    context: Context,
    state: CardEditorUiState,
    snackbarHostState: SnackbarHostState,
) = coroutineScope {
    val pan = state.cardNumber.filter { it.isDigit() }
    val expiry = listOf(state.expiryMonth, state.expiryYear.takeLast(2))
        .filter { it.isNotBlank() }
        .joinToString("/")
    val steps = buildList {
        if (pan.isNotBlank()) add("Number copied — paste now" to ("Card number" to pan))
        if (expiry.isNotBlank()) add("Expiry copied — paste now" to ("Expiry" to expiry))
        if (state.cvv.isNotBlank()) add("CVV copied — paste now" to ("CVV" to state.cvv))
    }
    if (steps.isEmpty()) {
        snackbarHostState.showSnackbar("Nothing to copy")
        return@coroutineScope
    }
    steps.forEachIndexed { index, (message, labeledValue) ->
        val (clipLabel, value) = labeledValue
        copyToClipboard(context, clipLabel, value)
        // Don't await snackbar dismiss — keep an 8s clipboard cadence.
        launch { snackbarHostState.showSnackbar(message) }
        if (index < steps.lastIndex) {
            delay(CHECKOUT_STEP_DELAY_MS)
        }
    }
}

@Composable
private fun PaymentFields(
    state: CardEditorUiState,
    viewModel: CardEditorViewModel,
    context: Context,
    onCopyForCheckout: () -> Unit,
) {
    val expiryStatus = CardExpiry.statusForPayment(state.expiryMonth, state.expiryYear)
    ExpiryStatusBanner(expiryStatus)
    Text("Payment details", style = MaterialTheme.typography.titleMedium)
    TextButton(onClick = onCopyForCheckout) {
        Icon(Icons.Default.ContentCopy, contentDescription = null)
        Text("  Copy for checkout")
    }
    CopyableField(
        label = "Card number",
        value = state.cardNumber,
        onValueChange = viewModel::onCardNumberChange,
        keyboardType = KeyboardType.Number,
        visualTransformation = when {
            state.cardNumber.isBlank() -> VisualTransformation.None
            state.revealSecrets -> PanGroupedTransformation(mask = false)
            else -> PanGroupedTransformation(mask = true)
        },
        onCopy = {
            copyToClipboard(context, "Card number", state.cardNumber.filter { it.isDigit() })
            viewModel.notifyCopied("Card number", state.cardNumber)
        },
    )
    CopyableField(
        label = "Cardholder name",
        value = state.cardholderName,
        onValueChange = viewModel::onCardholderNameChange,
        capitalization = KeyboardCapitalization.Characters,
        onCopy = {
            copyToClipboard(context, "Name", state.cardholderName)
            viewModel.notifyCopied("Name", state.cardholderName)
        },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CopyableField(
            label = "Exp month",
            value = state.expiryMonth,
            onValueChange = viewModel::onExpiryMonthChange,
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
            onCopy = {
                copyToClipboard(context, "Month", state.expiryMonth)
                viewModel.notifyCopied("Month", state.expiryMonth)
            },
        )
        CopyableField(
            label = "Exp year",
            value = state.expiryYear,
            onValueChange = viewModel::onExpiryYearChange,
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
            onCopy = {
                copyToClipboard(context, "Year", state.expiryYear)
                viewModel.notifyCopied("Year", state.expiryYear)
            },
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CopyableField(
            label = "CVV",
            value = state.cvv,
            onValueChange = viewModel::onCvvChange,
            keyboardType = KeyboardType.NumberPassword,
            visualTransformation = if (state.revealSecrets) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            modifier = Modifier.weight(1f),
            onCopy = {
                copyToClipboard(context, "CVV", state.cvv)
                viewModel.notifyCopied("CVV", state.cvv)
            },
        )
        CopyableField(
            label = "Brand",
            value = state.brand,
            onValueChange = viewModel::onBrandChange,
            modifier = Modifier.weight(1f),
            onCopy = {
                copyToClipboard(context, "Brand", state.brand)
                viewModel.notifyCopied("Brand", state.brand)
            },
        )
    }
    val expiryCombined = listOf(state.expiryMonth, state.expiryYear.takeLast(2))
        .filter { it.isNotBlank() }
        .joinToString("/")
    if (expiryCombined.isNotBlank()) {
        TextButton(
            onClick = {
                copyToClipboard(context, "Expiry", expiryCombined)
                viewModel.notifyCopied("Expiry", expiryCombined)
            },
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null)
            Text("  Copy expiry as MM/YY")
        }
    }
}

@Composable
private fun IdFields(
    state: CardEditorUiState,
    customFields: List<com.budgetnotes.app.data.CardCustomField>,
    viewModel: CardEditorViewModel,
    context: Context,
    onAddCustomField: () -> Unit,
) {
    val expiryStatus = CardExpiry.statusForIdDate(state.expiryDate)
    ExpiryStatusBanner(expiryStatus)
    Text("ID details", style = MaterialTheme.typography.titleMedium)
    CopyableField(
        label = "Full name",
        value = state.fullName,
        onValueChange = viewModel::onFullNameChange,
        capitalization = KeyboardCapitalization.Words,
        onCopy = {
            copyToClipboard(context, "Name", state.fullName)
            viewModel.notifyCopied("Name", state.fullName)
        },
    )
    CopyableField(
        label = "Document number",
        value = state.documentNumber,
        onValueChange = viewModel::onDocumentNumberChange,
        onCopy = {
            copyToClipboard(context, "Document number", state.documentNumber)
            viewModel.notifyCopied("Document number", state.documentNumber)
        },
    )
    CopyableField(
        label = "Issuer",
        value = state.issuer,
        onValueChange = viewModel::onIssuerChange,
        onCopy = {
            copyToClipboard(context, "Issuer", state.issuer)
            viewModel.notifyCopied("Issuer", state.issuer)
        },
    )
    CopyableField(
        label = "Expiry / valid until",
        value = state.expiryDate,
        onValueChange = viewModel::onExpiryDateChange,
        onCopy = {
            copyToClipboard(context, "Expiry", state.expiryDate)
            viewModel.notifyCopied("Expiry", state.expiryDate)
        },
    )

    Text("Custom fields", style = MaterialTheme.typography.titleMedium)
    customFields.forEach { field ->
        var name by remember(field.id) { mutableStateOf(field.name) }
        var value by remember(field.id) { mutableStateOf(field.value) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    viewModel.updateCustomField(field, it, value)
                },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.weight(0.4f),
            )
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                    viewModel.updateCustomField(field, name, it)
                },
                label = { Text("Value") },
                singleLine = true,
                modifier = Modifier.weight(0.5f),
            )
            IconButton(
                onClick = {
                    copyToClipboard(context, name, value)
                    viewModel.notifyCopied(name, value)
                },
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
            }
            IconButton(onClick = { viewModel.deleteCustomField(field.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete field")
            }
        }
    }
    TextButton(onClick = onAddCustomField) {
        Icon(Icons.Default.Add, contentDescription = null)
        Text("  Add custom field")
    }
}

@Composable
private fun CopyableField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
        ),
        visualTransformation = visualTransformation,
        trailingIcon = {
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy $label")
            }
        },
    )
}

/**
 * Shows the PAN in 4-digit groups. When [mask] is true every digit is '•'
 * (•••• •••• •••• ••••); when false digits stay visible (4111 1111 …).
 */
private class PanGroupedTransformation(
    private val mask: Boolean,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }
        val transformed = buildString(digits.length + digits.length / 4) {
            digits.forEachIndexed { index, ch ->
                if (index > 0 && index % 4 == 0) append(' ')
                append(if (mask) '•' else ch)
            }
        }
        return TransformedText(
            AnnotatedString(transformed),
            PanGroupOffsetMapping(digits.length),
        )
    }
}

/** Maps between digit-only original text and space-grouped transformed text. */
private class PanGroupOffsetMapping(
    private val digitCount: Int,
) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        val clamped = offset.coerceIn(0, digitCount)
        if (clamped == 0) return 0
        // Space is inserted before digits at indices 4, 8, 12… so first N digits
        // contain (N - 1) / 4 spaces.
        return clamped + (clamped - 1) / 4
    }

    override fun transformedToOriginal(offset: Int): Int {
        if (offset <= 0) return 0
        val maxTransformed = originalToTransformed(digitCount)
        val clamped = offset.coerceIn(0, maxTransformed)
        // Walk the same layout: 4 digits, space, 4 digits, space…
        var digits = 0
        var i = 0
        while (i < clamped && digits < digitCount) {
            if (digits > 0 && digits % 4 == 0) {
                // This transformed index is a space — consume it without a digit.
                i++
                continue
            }
            digits++
            i++
        }
        return digits
    }
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    if (value.isBlank()) return
    val app = context.applicationContext as BudgetNotesApplication
    app.container.clipboard.copy(label, value)
}
