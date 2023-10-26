package app.revanced.manager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.ui.component.AppIcon
import app.revanced.manager.ui.component.AppInfo
import app.revanced.manager.ui.component.AppLabel
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.SegmentedButton
import app.revanced.manager.ui.viewmodel.InstalledAppInfoViewModel
import app.revanced.manager.util.PatchesSelection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstalledAppInfoScreen(
    onPatchClick: (packageName: String, patchesSelection: PatchesSelection) -> Unit,
    onBackClick: () -> Unit,
    viewModel: InstalledAppInfoViewModel
) {
    SideEffect {
        viewModel.onBackClick = onBackClick
    }

    var showUninstallDialog by rememberSaveable { mutableStateOf(false) }

    if (showUninstallDialog)
        UninstallDialog(
            onDismiss = { showUninstallDialog = false },
            onConfirm = { viewModel.uninstall() }
        )

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_info),
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            AppInfo(viewModel.appInfo)  {
                Text(viewModel.installedApp.version, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)

                if (viewModel.installedApp.installType == InstallType.ROOT) {
                    Text(
                        text = if (viewModel.rootInstaller.isAppMounted(viewModel.installedApp.currentPackageName)) {
                            stringResource(R.string.mounted)
                        } else {
                            stringResource(R.string.not_mounted)
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                SegmentedButton(
                    icon = Icons.Outlined.OpenInNew,
                    text = stringResource(R.string.open_app),
                    onClick = viewModel::launch
                )

                when (viewModel.installedApp.installType) {
                    InstallType.DEFAULT -> SegmentedButton(
                        icon = Icons.Outlined.Delete,
                        text = stringResource(R.string.uninstall),
                        onClick = viewModel::uninstall
                    )

                    InstallType.ROOT -> {
                        SegmentedButton(
                            icon = Icons.Outlined.SettingsBackupRestore,
                            text = stringResource(R.string.unpatch),
                            onClick = { showUninstallDialog = true },
                            enabled = viewModel.rootInstaller.hasRootAccess()
                        )

                        SegmentedButton(
                            icon = Icons.Outlined.Circle,
                            text = if (viewModel.isMounted) stringResource(R.string.unmount) else stringResource(R.string.mount),
                            onClick = viewModel::mountOrUnmount,
                            enabled = viewModel.rootInstaller.hasRootAccess()
                        )
                    }

                }

                SegmentedButton(
                    icon = Icons.Outlined.Update,
                    text = stringResource(R.string.repatch),
                    onClick = {
                        viewModel.appliedPatches?.let {
                            onPatchClick(viewModel.installedApp.originalPackageName, it)
                        }
                    },
                    enabled = viewModel.installedApp.installType != InstallType.ROOT || viewModel.rootInstaller.hasRootAccess()
                )
            }

            Column(
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                ListItem(
                    modifier = Modifier.clickable {  },
                    headlineContent = { Text(stringResource(R.string.applied_patches)) },
                    supportingContent = {
                        Text(
                            (viewModel.appliedPatches?.values?.sumOf { it.size } ?: 0).let {
                                pluralStringResource(
                                    id = R.plurals.applied_patches,
                                    it,
                                    it
                                )
                            }
                        )
                    },
                    trailingContent = { Icon(Icons.Filled.ArrowRight, contentDescription = stringResource(R.string.view_applied_patches)) }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.package_name)) },
                    supportingContent = { Text(viewModel.installedApp.currentPackageName) }
                )

                if (viewModel.installedApp.originalPackageName != viewModel.installedApp.currentPackageName) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.original_package_name)) },
                        supportingContent = { Text(viewModel.installedApp.originalPackageName) }
                    )
                }

                ListItem(
                    headlineContent = { Text(stringResource(R.string.install_type)) },
                    supportingContent = { Text(stringResource(viewModel.installedApp.installType.stringResource)) }
                )
            }

        }
    }
}

@Composable
fun UninstallDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) = AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.unpatch_app)) },
    text = { Text(stringResource(R.string.unpatch_description)) },
    confirmButton = {
        TextButton(
            onClick = {
                onConfirm()
                onDismiss()
            }
        ) {
            Text(stringResource(R.string.ok))
        }
    },
    dismissButton = {
        TextButton(
            onClick = onDismiss
        ) {
            Text(stringResource(R.string.cancel))
        }
    }
)