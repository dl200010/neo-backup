/*
 * Neo Backup: open-source apps backup and restore app.
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
package com.machiav3lli.backup.pages

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.machiav3lli.backup.R
import com.machiav3lli.backup.activities.IntroActivityX
import com.machiav3lli.backup.preferences.persist_ignoreBatteryOptimization
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.ArrowRight
import com.machiav3lli.backup.ui.compose.item.ElevatedActionButton
import com.machiav3lli.backup.ui.compose.item.PermissionItem
import com.machiav3lli.backup.ui.item.Permission
import com.machiav3lli.backup.utils.checkBatteryOptimization
import com.machiav3lli.backup.utils.checkCallLogsPermission
import com.machiav3lli.backup.utils.checkContactsPermission
import com.machiav3lli.backup.utils.checkSMSMMSPermission
import com.machiav3lli.backup.utils.checkUsageStatsPermission
import com.machiav3lli.backup.utils.getStoragePermission
import com.machiav3lli.backup.utils.hasStoragePermissions
import com.machiav3lli.backup.utils.isStorageDirSetAndOk
import com.machiav3lli.backup.utils.requireCallLogsPermission
import com.machiav3lli.backup.utils.requireContactsPermission
import com.machiav3lli.backup.utils.requireSMSMMSPermission
import com.machiav3lli.backup.utils.requireStorageLocation
import com.machiav3lli.backup.utils.setBackupDir
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsPage() {
    val context = LocalContext.current
    val introActivityX = context as IntroActivityX
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val permissionsList = remember {
        mutableStateListOf<Pair<Permission, () -> Unit>>()
    }

    val askForDirectory =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.data != null && result.resultCode == Activity.RESULT_OK) {
                result.data?.let {
                    val uri = it.data ?: return@rememberLauncherForActivityResult
                    val flags = it.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                    context.setBackupDir(uri)
                }
            }
        }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner, effect = {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionsList.clear()
                permissionsList.addAll(buildList {
                    if (!context.hasStoragePermissions)
                        add(Pair(Permission.StorageAccess) { introActivityX.getStoragePermission() })
                    if (!context.isStorageDirSetAndOk)
                        add(Pair(Permission.StorageLocation) {
                            introActivityX.requireStorageLocation(askForDirectory)
                        })
                    if (!context.checkBatteryOptimization(powerManager))
                        add(Pair(Permission.BatteryOptimization) {
                            introActivityX.showBatteryOptimizationDialog(powerManager)
                        })
                    if (!context.checkUsageStatsPermission)
                        add(Pair(Permission.UsageStats) { introActivityX.usageStatsPermission })
                    if (!context.checkSMSMMSPermission)
                        add(Pair(Permission.SMSMMS) { introActivityX.smsmmsPermission })
                    if (!context.checkCallLogsPermission)
                        add(Pair(Permission.CallLogs) { introActivityX.callLogsPermission })
                    if (!context.checkContactsPermission)
                        add(Pair(Permission.Contacts) { introActivityX.contactsPermission })
                })
                if (permissionsList.isEmpty()) introActivityX.moveTo(3)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    })

    Scaffold(
        bottomBar = {
            AnimatedVisibility(visible = permissionsList.isEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ElevatedActionButton(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(id = R.string.dialog_start),
                        icon = Phosphor.ArrowRight,
                    ) {
                        introActivityX.moveTo(3)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(permissionsList) {
                PermissionItem(it.first, it.second)
            }
        }
    }
}

val AppCompatActivity.usageStatsPermission: Unit
    get() {
        AlertDialog.Builder(this)
            .setTitle(R.string.grant_usage_access_title)
            .setMessage(R.string.grant_usage_access_message)
            .setPositiveButton(R.string.dialog_approve) { _: DialogInterface?, _: Int ->
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            .setNeutralButton(getString(R.string.dialog_refuse)) { _: DialogInterface?, _: Int -> }
            .setCancelable(false)
            .show()
    }

val AppCompatActivity.smsmmsPermission: Unit
    get() {
        AlertDialog.Builder(this)
            .setTitle(R.string.smsmms_permission_title)
            .setMessage(R.string.grant_smsmms_message)
            .setPositiveButton(R.string.dialog_approve) { _: DialogInterface?, _: Int ->
                requireSMSMMSPermission()
            }
            .setNeutralButton(getString(R.string.dialog_refuse)) { _: DialogInterface?, _: Int -> }
            .setCancelable(false)
            .show()
    }

val AppCompatActivity.callLogsPermission: Unit
    get() {
        AlertDialog.Builder(this)
            .setTitle(R.string.calllogs_permission_title)
            .setMessage(R.string.grant_calllogs_message)
            .setPositiveButton(R.string.dialog_approve) { _: DialogInterface?, _: Int ->
                this.requireCallLogsPermission()
            }
            .setNeutralButton(getString(R.string.dialog_refuse)) { _: DialogInterface?, _: Int -> }
            .setCancelable(false)
            .show()
    }

val AppCompatActivity.contactsPermission: Unit
    get() {
        AlertDialog.Builder(this)
            .setTitle(R.string.contacts_permission_title)
            .setMessage(R.string.grant_contacts_message)
            .setPositiveButton(R.string.dialog_approve) { _: DialogInterface?, _: Int ->
                this.requireContactsPermission()
            }
            .setNeutralButton(getString(R.string.dialog_refuse)) { _: DialogInterface?, _: Int -> }
            .setCancelable(false)
            .show()
    }

fun AppCompatActivity.showBatteryOptimizationDialog(powerManager: PowerManager?) {
    AlertDialog.Builder(this)
        .setTitle(R.string.ignore_battery_optimization_title)
        .setMessage(R.string.ignore_battery_optimization_message)
        .setPositiveButton(R.string.dialog_approve) { _: DialogInterface?, _: Int ->
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:" + packageName)
            try {
                startActivity(intent)
                persist_ignoreBatteryOptimization.value =
                    powerManager?.isIgnoringBatteryOptimizations(packageName) == true
            } catch (e: ActivityNotFoundException) {
                Timber.w(e, "Ignore battery optimizations not supported")
                Toast.makeText(
                    this,
                    R.string.ignore_battery_optimization_not_supported,
                    Toast.LENGTH_LONG
                ).show()
                persist_ignoreBatteryOptimization.value = true
            }
        }
        .setNeutralButton(R.string.dialog_refuse) { _: DialogInterface?, _: Int ->
            persist_ignoreBatteryOptimization.value = true
        }
        .setCancelable(false)
        .show()
}
