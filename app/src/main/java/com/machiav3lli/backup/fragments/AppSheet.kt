/*
 * OAndBackupX: open-source apps backup and restore app.
 * Copyright (C) 2020  Antonios Hazim
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.machiav3lli.backup.fragments

import android.app.ActivityManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.machiav3lli.backup.ActionListener
import com.machiav3lli.backup.BUNDLE_USERS
import com.machiav3lli.backup.OABX
import com.machiav3lli.backup.R
import com.machiav3lli.backup.dbs.ODatabase
import com.machiav3lli.backup.dbs.entity.Backup
import com.machiav3lli.backup.dialogs.BackupDialogFragment
import com.machiav3lli.backup.dialogs.RestoreDialogFragment
import com.machiav3lli.backup.exodusUrl
import com.machiav3lli.backup.handler.BackupRestoreHelper.ActionType
import com.machiav3lli.backup.handler.ShellCommands
import com.machiav3lli.backup.handler.ShellHandler
import com.machiav3lli.backup.items.Package
import com.machiav3lli.backup.preferences.pref_useWorkManagerForSingleManualJob
import com.machiav3lli.backup.tasks.BackupActionTask
import com.machiav3lli.backup.tasks.RestoreActionTask
import com.machiav3lli.backup.ui.compose.icons.Icon
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.icon.Exodus
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArchiveTray
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArrowSquareOut
import com.machiav3lli.backup.ui.compose.icons.phosphor.CaretDown
import com.machiav3lli.backup.ui.compose.icons.phosphor.Info
import com.machiav3lli.backup.ui.compose.icons.phosphor.Leaf
import com.machiav3lli.backup.ui.compose.icons.phosphor.Prohibit
import com.machiav3lli.backup.ui.compose.icons.phosphor.ProhibitInset
import com.machiav3lli.backup.ui.compose.icons.phosphor.TrashSimple
import com.machiav3lli.backup.ui.compose.icons.phosphor.Warning
import com.machiav3lli.backup.ui.compose.item.BackupItem
import com.machiav3lli.backup.ui.compose.item.CardButton
import com.machiav3lli.backup.ui.compose.item.ElevatedActionButton
import com.machiav3lli.backup.ui.compose.item.MorphableTextField
import com.machiav3lli.backup.ui.compose.item.PackageIcon
import com.machiav3lli.backup.ui.compose.item.RoundButton
import com.machiav3lli.backup.ui.compose.item.TagsBlock
import com.machiav3lli.backup.ui.compose.item.TitleText
import com.machiav3lli.backup.ui.compose.recycler.InfoChipsBlock
import com.machiav3lli.backup.ui.compose.theme.AppTheme
import com.machiav3lli.backup.ui.compose.theme.LocalShapes
import com.machiav3lli.backup.utils.infoChips
import com.machiav3lli.backup.utils.show
import com.machiav3lli.backup.utils.showError
import com.machiav3lli.backup.viewmodels.AppSheetViewModel
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber

class AppSheet(val appInfo: Package) : BaseSheet(), ActionListener {
    private lateinit var viewModel: AppSheetViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val database = ODatabase.getInstance(requireContext())
        val users =
            if (savedInstanceState != null) savedInstanceState.getStringArrayList(BUNDLE_USERS) else ArrayList()
        val shellCommands = ShellCommands(users)
        val viewModelFactory =
            AppSheetViewModel.Factory(
                appInfo,
                database,
                shellCommands,
                requireActivity().application
            )
        viewModel = ViewModelProvider(this, viewModelFactory)[AppSheetViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent { AppPage() }
        }
    }

    fun updateApp(app: Package) {
        viewModel.thePackage.value = app
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    @Composable
    fun AppPage() {
        val thePackage by viewModel.thePackage.observeAsState()
        val snackbarText by viewModel.snackbarText.observeAsState()
        val appExtras by viewModel.appExtras.observeAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val nestedScrollConnection = rememberNestedScrollInteropConnection()
        val coroutineScope = rememberCoroutineScope()

        thePackage?.let { packageInfo ->
            val imageData by remember(packageInfo) {
                mutableStateOf(
                    if (packageInfo.isSpecial) packageInfo.packageInfo.icon
                    else "android.resource://${packageInfo.packageName}/${packageInfo.packageInfo.icon}"
                )
            }
            if (viewModel.refreshNow) {
                requireMainActivity().updatePackage(packageInfo.packageName ?: "")
                viewModel.refreshNow = false
            }


            AppTheme {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    bottomBar = {

                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { paddingValues ->
                    LazyColumn(
                        modifier = Modifier
                            .padding(paddingValues)
                            .nestedScroll(nestedScrollConnection)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        item {
                            OutlinedCard(
                                modifier = Modifier.padding(top = 8.dp),
                                shape = RoundedCornerShape(LocalShapes.current.medium),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PackageIcon(item = packageInfo, imageData = imageData)

                                    Column(
                                        modifier = Modifier
                                            .wrapContentHeight()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = packageInfo.packageLabel,
                                            softWrap = true,
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = packageInfo.packageName,
                                            softWrap = true,
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    AnimatedVisibility(visible = packageInfo.isInstalled && !packageInfo.isSpecial) {
                                        RoundButton(
                                            icon = Phosphor.Info,
                                            modifier = Modifier.fillMaxHeight()
                                        ) {
                                            val intent =
                                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                            intent.data =
                                                Uri.fromParts(
                                                    "package",
                                                    packageInfo.packageName,
                                                    null
                                                )
                                            startActivity(intent)
                                        }
                                    }
                                    RoundButton(
                                        icon = Phosphor.CaretDown,
                                        modifier = Modifier.fillMaxHeight()
                                    ) {
                                        dismissAllowingStateLoss()
                                    }
                                }
                            }
                        }
                        item {
                            AnimatedVisibility(visible = !snackbarText.isNullOrEmpty()) {
                                Text(
                                    text = snackbarText.toString(),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        item {
                            InfoChipsBlock(list = packageInfo.infoChips())
                        }
                        item {
                            AnimatedVisibility(
                                modifier = Modifier.fillMaxWidth(),
                                visible = !packageInfo.isSpecial
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // TODO Add enabled state
                                    CardButton(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(1f),
                                        icon = Icon.Exodus,
                                        tint = colorResource(id = R.color.ic_exodus),
                                        description = stringResource(id = R.string.exodus_report)
                                    ) {
                                        requireContext().startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(exodusUrl(packageInfo.packageName))
                                            )
                                        )
                                    }
                                    AnimatedVisibility(
                                        visible = true, //appInfo.isInstalled && ! appInfo.isDisabled,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        CardButton(
                                            enabled = packageInfo.isInstalled && !packageInfo.isDisabled,
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(1f),
                                            icon = Phosphor.ArrowSquareOut,
                                            tint = colorResource(id = R.color.ic_obb),
                                            description = stringResource(id = R.string.launch_app)
                                        ) {
                                            requireContext().packageManager.getLaunchIntentForPackage(
                                                packageInfo.packageName
                                            )?.let {
                                                startActivity(it)
                                            }
                                        }
                                    }
                                    AnimatedVisibility(
                                        visible = packageInfo.isInstalled,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        CardButton(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(1f),
                                            icon = if (packageInfo.isDisabled) Phosphor.Leaf
                                            else Phosphor.ProhibitInset,
                                            tint = if (packageInfo.isDisabled) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.tertiaryContainer,
                                            description = stringResource(
                                                id = if (packageInfo.isDisabled) R.string.enablePackage
                                                else R.string.disablePackage
                                            ),
                                            onClick = { showEnableDisableDialog(packageInfo.isDisabled) }
                                        )
                                    }
                                    AnimatedVisibility(
                                        visible = packageInfo.isInstalled && !packageInfo.isSystem,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        CardButton(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(1f),
                                            icon = Phosphor.TrashSimple,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            description = stringResource(id = R.string.uninstall),
                                            onClick = {
                                                snackbarHostState.showUninstallDialog(
                                                    packageInfo,
                                                    coroutineScope
                                                )
                                            }
                                        )
                                    }
                                    CardButton(
                                        modifier = Modifier
                                            .weight(1f),
                                        icon = Phosphor.Prohibit,
                                        tint = colorResource(id = R.color.ic_updated),
                                        description = stringResource(id = R.string.global_blocklist_add)
                                    ) {
                                        requireMainActivity().viewModel.addToBlocklist(packageInfo.packageName)
                                    }
                                }
                            }
                        }
                        item {
                            TitleText(textId = R.string.title_tags)
                            TagsBlock(
                                tags = appExtras?.customTags ?: mutableSetOf(),
                                onRemove = {
                                    viewModel.setExtras(appExtras?.apply {
                                        customTags.remove(it)
                                    })
                                },
                                onAdd = {
                                    viewModel.setExtras(appExtras?.apply {
                                        if (customTags.isNotEmpty())
                                            customTags.add(it)
                                        else
                                            customTags = mutableSetOf(it)
                                    })
                                }
                            )
                        }
                        item {
                            TitleText(textId = R.string.title_note)
                            MorphableTextField(
                                text = appExtras?.note,
                                onCancel = {
                                },
                                onSave = {
                                    viewModel.setExtras(appExtras?.apply { note = it })
                                }
                            )
                        }
                        item {
                            TitleText(textId = R.string.available_actions)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AnimatedVisibility(visible = packageInfo.isInstalled || packageInfo.isSpecial) {
                                    ElevatedActionButton(
                                        icon = Phosphor.ArchiveTray,
                                        text = stringResource(id = R.string.backup),
                                        fullWidth = true,
                                        enabled = snackbarText.isNullOrEmpty(),
                                        onClick = { showBackupDialog(packageInfo) }
                                    )
                                }
                                AnimatedVisibility(visible = packageInfo.hasBackups) {
                                    ElevatedActionButton(
                                        icon = Phosphor.TrashSimple,
                                        text = stringResource(id = R.string.delete_all_backups),
                                        fullWidth = true,
                                        positive = false,
                                        enabled = snackbarText.isNullOrEmpty(),
                                        onClick = {
                                            snackbarHostState.showDeleteAllBackupsDialog(
                                                packageInfo,
                                                coroutineScope
                                            )
                                        }
                                    )
                                }
                                AnimatedVisibility(visible = packageInfo.isInstalled && !packageInfo.isSpecial) {
                                    ElevatedActionButton(
                                        icon = Phosphor.Warning,
                                        text = stringResource(id = R.string.forceKill),
                                        fullWidth = true,
                                        colored = false,
                                        onClick = { showForceKillDialog(packageInfo) }
                                    )
                                }
                                AnimatedVisibility(
                                    visible = packageInfo.isInstalled && !packageInfo.isSpecial && ((packageInfo.storageStats?.dataBytes
                                        ?: 0L) >= 0L)
                                ) {
                                    ElevatedActionButton(
                                        icon = Phosphor.TrashSimple,
                                        text = stringResource(id = R.string.clear_cache),
                                        fullWidth = true,
                                        colored = false,
                                        onClick = { showClearCacheDialog(packageInfo) }
                                    )
                                }
                            }
                        }
                        items(items = packageInfo.backupsNewestFirst) {
                            BackupItem(
                                it,
                                onRestore = { item ->
                                    packageInfo.let { app ->
                                        if (!app.isSpecial && !app.isInstalled
                                            && !item.hasApk && item.hasAppData
                                        ) {
                                            snackbarHostState.show(
                                                coroutineScope = coroutineScope,
                                                message = getString(R.string.notInstalledModeDataWarning)
                                            )
                                        } else {
                                            RestoreDialogFragment(
                                                app,
                                                item,
                                                this@AppSheet
                                            )
                                                .show(
                                                    requireActivity().supportFragmentManager,
                                                    "restoreDialog"
                                                )
                                        }
                                    }
                                },
                                onDelete = { item ->
                                    packageInfo.let { app ->
                                        AlertDialog.Builder(requireContext())
                                            .setTitle(app.packageLabel)
                                            .setMessage(R.string.deleteBackupDialogMessage)
                                            .setPositiveButton(R.string.dialogYes) { dialog: DialogInterface?, _: Int ->
                                                snackbarHostState.show(
                                                    coroutineScope = coroutineScope,
                                                    message = "${app.packageLabel}: ${
                                                        getString(
                                                            R.string.deleteBackup
                                                        )
                                                    }"
                                                )
                                                if (!app.hasBackups) {
                                                    Timber.w("UI Issue! Tried to delete backups for app without backups.")
                                                    dialog?.dismiss()
                                                }
                                                viewModel.deleteBackup(item)
                                            }
                                            .setNegativeButton(R.string.dialogNo, null)
                                            .show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onActionCalled(
        actionType: ActionType?,
        mode: Int,
        backup: Backup?
    ) {
        viewModel.thePackage.value?.let { p ->
            when {
                actionType === ActionType.BACKUP -> {
                    if (pref_useWorkManagerForSingleManualJob.value) {
                        OABX.main?.startBatchAction(
                            true,
                            listOf(this.appInfo.packageName),
                            listOf(mode)
                        ) {
                            //viewModel.refreshNow.value = true
                            // TODO refresh only the influenced packages
                            it.removeObserver(this)
                        }
                    } else {
                        BackupActionTask(
                            p, requireMainActivity(), OABX.shellHandlerInstance!!, mode,
                            this
                        ).execute()
                    }
                }
                actionType === ActionType.RESTORE -> {
                    if (pref_useWorkManagerForSingleManualJob.value) {
                        OABX.main?.startBatchAction(
                            false,
                            listOf(this.appInfo.packageName),
                            listOf(mode)
                        ) {
                            //viewModel.refreshNow.value = true
                            // TODO refresh only the influenced packages
                            it.removeObserver(this)
                        }
                    } else {
                        backup?.let { backupProps: Backup ->
                            val backupDir =
                                backupProps.getBackupInstanceFolder(p.getAppBackupRoot())
                            RestoreActionTask(
                                p, requireMainActivity(), OABX.shellHandlerInstance!!, mode,
                                backupProps, backupDir!!, this
                            ).execute()
                        }
                    }
                }
                else -> {
                    Timber.e("unhandled actionType: $actionType")
                }
            }
        }
    }

    private fun showEnableDisableDialog(enable: Boolean) {
        val title =
            if (enable) getString(R.string.enablePackageTitle) else getString(R.string.disablePackageTitle)
        try {
            val userList = viewModel.getUsers()
            val selectedUsers = mutableListOf<String>()
            if (userList.size == 1) {
                selectedUsers.add(userList[0])
                viewModel.enableDisableApp(selectedUsers, enable)
                return
            }
            AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMultiChoiceItems(
                    userList,
                    null
                ) { _: DialogInterface?, chosen: Int, checked: Boolean ->
                    if (checked) {
                        selectedUsers.add(userList[chosen])
                    } else selectedUsers.remove(userList[chosen])
                }
                .setPositiveButton(R.string.dialogOK) { _: DialogInterface?, _: Int ->
                    try {
                        viewModel.enableDisableApp(selectedUsers, enable)
                    } catch (e: ShellCommands.ShellActionFailedException) {
                        requireActivity().showError(e.message)
                    }
                }
                .setNegativeButton(R.string.dialogCancel) { _: DialogInterface?, _: Int -> }
                .show()
        } catch (e: ShellCommands.ShellActionFailedException) {
            requireActivity().showError(e.message)
        }
    }

    private fun SnackbarHostState.showUninstallDialog(
        app: Package,
        coroutineScope: CoroutineScope
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle(app.packageLabel)
            .setMessage(R.string.uninstallDialogMessage)
            .setPositiveButton(R.string.dialogYes) { _: DialogInterface?, _: Int ->
                viewModel.uninstallApp()
                this.show(
                    coroutineScope = coroutineScope,
                    message = "${app.packageLabel}: ${getString(R.string.uninstallProgress)}"
                )
            }
            .setNegativeButton(R.string.dialogNo, null)
            .show()
    }

    fun showBackupDialog(app: Package) {
        BackupDialogFragment(app, this)
            .show(requireActivity().supportFragmentManager, "backupDialog")
    }

    fun SnackbarHostState.showDeleteAllBackupsDialog(
        app: Package,
        coroutineScope: CoroutineScope
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle(app.packageLabel)
            .setMessage(R.string.deleteBackupDialogMessage)
            .setPositiveButton(R.string.dialogYes) { _: DialogInterface?, _: Int ->
                viewModel.deleteAllBackups()
                this.show(
                    coroutineScope = coroutineScope,
                    message = "${app.packageLabel}: ${getString(R.string.delete_all_backups)}"
                )
            }
            .setNegativeButton(R.string.dialogNo, null)
            .show()
    }

    // TODO hg42 force-stop, force-close, ... ? I think these are different ones, and I don't know which
    private fun showForceKillDialog(app: Package) {
        AlertDialog.Builder(requireContext())
            .setTitle(app.packageLabel)
            .setMessage(R.string.forceKillMessage)
            .setPositiveButton(R.string.dialogYes) { _: DialogInterface?, _: Int ->
                (requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                    .killBackgroundProcesses(app.packageName)
            }
            .setNegativeButton(R.string.dialogNo, null)
            .show()
    }

    private fun showClearCacheDialog(app: Package) {
        try {
            Timber.i("${app.packageLabel}: Wiping cache")
            ShellCommands.wipeCache(requireContext(), app)
            viewModel.refreshNow = true
        } catch (e: ShellCommands.ShellActionFailedException) {
            // Not a critical issue
            val errorMessage: String =
                when (val cause = e.cause) {
                    is ShellHandler.ShellCommandFailedException -> {
                        cause.shellResult.err.joinToString(
                            " "
                        )
                    }
                    else -> {
                        cause?.message ?: "unknown error"
                    }
                }
            Timber.w("Cache couldn't be deleted: $errorMessage")
        }
    }

    fun showSnackBar(message: String) {
        viewModel.snackbarText.value = message
    }

    fun dismissSnackBar() {
        viewModel.snackbarText.value = ""
    }
}
