package com.zekid.contactnotifier.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Topic
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.zekid.contactnotifier.data.AppSettings
import com.zekid.contactnotifier.ui.theme.NtfyFlowTheme
import kotlinx.coroutines.flow.collectLatest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.PermissionStatus
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

data class PermissionUiState(
    val permission: String,
    val label: String,
    val description: String,
    val isGranted: Boolean,
    val shouldShowRationale: Boolean
)

private data class RequiredAppPermission(
    val permission: String,
    val label: String,
    val description: String
)

private val REQUIRED_APP_PERMISSIONS = listOf(
    RequiredAppPermission(
        permission = android.Manifest.permission.RECEIVE_SMS,
        label = "SMS",
        description = "Required to detect incoming SMS"
    ),
    RequiredAppPermission(
        permission = android.Manifest.permission.READ_PHONE_STATE,
        label = "Phone state",
        description = "Required to detect incoming calls"
    ),
    RequiredAppPermission(
        permission = android.Manifest.permission.READ_CALL_LOG,
        label = "Call log",
        description = "Required to show the caller number"
    ),
    RequiredAppPermission(
        permission = android.Manifest.permission.READ_CONTACTS,
        label = "Contacts",
        description = "Required to match sender names"
    )
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = REQUIRED_APP_PERMISSIONS.map { it.permission }
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveEvent.collectLatest {
            snackbarHostState.showSnackbar("Settings saved successfully")
        }
    }

    val permissionItems = REQUIRED_APP_PERMISSIONS.map { required ->
        val state = permissionsState.permissions.find { it.permission == required.permission }
        val denied = state?.status as? PermissionStatus.Denied
        PermissionUiState(
            permission = required.permission,
            label = required.label,
            description = required.description,
            isGranted = state != null && denied == null,
            shouldShowRationale = denied?.shouldShowRationale == true
        )
    }

    var notificationAccessGranted by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccessGranted = NotificationManagerCompat
                    .getEnabledListenerPackages(context)
                    .contains(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SettingsScreenContent(
        settings = settings,
        editableUrl = viewModel.editableServerUrl,
        editableTopic = viewModel.editableTopic,
        hasUnsavedChanges = viewModel.hasUnsavedChanges,
        onUrlChange = { viewModel.updateEditableUrl(it) },
        onTopicChange = { viewModel.updateEditableTopic(it) },
        onSaveClick = { viewModel.saveSettings() },
        onCallToggle = { viewModel.updateCallNotificationsEnabled(it) },
        onSmsToggle = { viewModel.updateSmsNotificationsEnabled(it) },
        onNotificationListenerToggle = { viewModel.updateNotificationListenerEnabled(it) },
        notificationAccessGranted = notificationAccessGranted,
        onOpenNotificationAccessClick = {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        },
        snackbarHostState = snackbarHostState,
        permissionItems = permissionItems,
        allPermissionsGranted = permissionsState.allPermissionsGranted,
        onGrantPermissionsClick = { permissionsState.launchMultiplePermissionRequest() },
        onOpenAppSettingsClick = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            )
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    settings: AppSettings,
    editableUrl: String,
    editableTopic: String,
    hasUnsavedChanges: Boolean,
    onUrlChange: (String) -> Unit,
    onTopicChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onCallToggle: (Boolean) -> Unit,
    onSmsToggle: (Boolean) -> Unit,
    onNotificationListenerToggle: (Boolean) -> Unit,
    notificationAccessGranted: Boolean,
    onOpenNotificationAccessClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    permissionItems: List<PermissionUiState>,
    allPermissionsGranted: Boolean,
    onGrantPermissionsClick: () -> Unit,
    onOpenAppSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text("NtfyFlow Settings") },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                PermissionStatusCard(
                    permissionItems = permissionItems,
                    allPermissionsGranted = allPermissionsGranted,
                    onGrantPermissionsClick = onGrantPermissionsClick,
                    onOpenAppSettingsClick = onOpenAppSettingsClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsSectionTitle(title = "Server Configuration", icon = Icons.Default.Settings)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editableUrl,
                            onValueChange = onUrlChange,
                            label = { Text("Ntfy Server URL") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            placeholder = { Text("https://ntfy.sh") },
                            leadingIcon = { Icon(Icons.Rounded.Public, contentDescription = null) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )

                        OutlinedTextField(
                            value = editableTopic,
                            onValueChange = onTopicChange,
                            label = { Text("Ntfy Topic") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            placeholder = { Text("my_secret_topic") },
                            leadingIcon = { Icon(Icons.Rounded.Topic, contentDescription = null) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = onSaveClick,
                            modifier = Modifier.align(Alignment.End),
                            shape = MaterialTheme.shapes.medium,
                            enabled = hasUnsavedChanges
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            Text(if (hasUnsavedChanges) "Save Config" else "Saved")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SettingsSectionTitle(title = "Notification Toggles", icon = Icons.Default.Notifications)
                        }
                        
                        SettingsSwitchItem(
                            title = "Call Notifications",
                            description = "Forward incoming call events to ntfy",
                            icon = Icons.Rounded.Call,
                            checked = settings.callNotificationsEnabled,
                            onCheckedChange = onCallToggle
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        SettingsSwitchItem(
                            title = "SMS Notifications",
                            description = "Forward incoming SMS messages to ntfy",
                            icon = Icons.Rounded.Message,
                            checked = settings.smsNotificationsEnabled,
                            onCheckedChange = onSmsToggle
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        SettingsSwitchItem(
                            title = "Chat+ / RCS Fallback",
                            description = "Forward message notifications, works with Chat+ (RCS) on",
                            icon = Icons.Default.Notifications,
                            checked = settings.notificationListenerEnabled,
                            onCheckedChange = onNotificationListenerToggle
                        )

                        if (settings.notificationListenerEnabled && !notificationAccessGranted) {
                            Text(
                                text = "Notification access is off — enable it so message alerts can be read.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            TextButton(
                                onClick = onOpenNotificationAccessClick,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text("Open notification access")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "NtfyFlow monitors telephony events in the background and sends them to your configured ntfy server.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    permissionItems: List<PermissionUiState>,
    allPermissionsGranted: Boolean,
    onGrantPermissionsClick: () -> Unit,
    onOpenAppSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsSectionTitle(title = "App Permissions", icon = Icons.Filled.Lock)
            if (allPermissionsGranted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = "All permissions granted. SMS and call detection is active.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    text = "Without these permissions Android never delivers SMS or call events to the app — the receivers stay silent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                permissionItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.isGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (item.isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Column {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (item.isGranted) "${item.description} — granted"
                                else if (item.shouldShowRationale) "${item.description} — denied, tap Grant below"
                                else "${item.description} — denied",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onGrantPermissionsClick) {
                        Text("Grant permissions")
                    }
                    TextButton(onClick = onOpenAppSettingsClick) {
                        Text("Open app settings")
                    }
                }
                Text(
                    text = "If Grant does nothing, the permission was denied permanently — use Open app settings instead.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { 
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            ) 
        },
        supportingContent = { Text(description) },
        leadingContent = { 
            Icon(
                imageVector = icon, 
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            ) 
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun SettingsScreenPreview() {
    NtfyFlowTheme {
        SettingsScreenContent(
            settings = AppSettings(
                ntfyServerUrl = "https://ntfy.sh",
                ntfyTopic = "my_topic",
                callNotificationsEnabled = true,
                smsNotificationsEnabled = false,
                notificationListenerEnabled = true
            ),
            editableUrl = "https://ntfy.sh",
            editableTopic = "my_topic",
            hasUnsavedChanges = true,
            onUrlChange = {},
            onTopicChange = {},
            onSaveClick = {},
            onCallToggle = {},
            onSmsToggle = {},
            onNotificationListenerToggle = {},
            notificationAccessGranted = false,
            onOpenNotificationAccessClick = {},
            snackbarHostState = remember { SnackbarHostState() },
            permissionItems = listOf(
                PermissionUiState("", "SMS", "Required to detect incoming SMS", true, false),
                PermissionUiState("", "Phone state", "Required to detect incoming calls", false, true),
                PermissionUiState("", "Call log", "Required to show the caller number", false, false),
                PermissionUiState("", "Contacts", "Required to match sender names", true, false)
            ),
            allPermissionsGranted = false,
            onGrantPermissionsClick = {},
            onOpenAppSettingsClick = {}
        )
    }
}
