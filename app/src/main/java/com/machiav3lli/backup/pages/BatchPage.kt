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
package com.machiav3lli.backup.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.ALT_MODE_APK
import com.machiav3lli.backup.ALT_MODE_BOTH
import com.machiav3lli.backup.ALT_MODE_DATA
import com.machiav3lli.backup.OABX
import com.machiav3lli.backup.R
import com.machiav3lli.backup.dialogs.BatchDialogFragment
import com.machiav3lli.backup.items.Package
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.DiamondsFour
import com.machiav3lli.backup.ui.compose.icons.phosphor.HardDrives
import com.machiav3lli.backup.ui.compose.icons.phosphor.Nut
import com.machiav3lli.backup.ui.compose.item.ActionButton
import com.machiav3lli.backup.ui.compose.item.RoundButton
import com.machiav3lli.backup.ui.compose.item.StateChip
import com.machiav3lli.backup.ui.compose.recycler.BatchPackageRecycler
import com.machiav3lli.backup.ui.compose.theme.ColorAPK
import com.machiav3lli.backup.ui.compose.theme.ColorData
import com.machiav3lli.backup.viewmodels.BatchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchPage(viewModel: BatchViewModel, backupBoolean: Boolean) {
    val main = OABX.main!!
    val filteredList by main.viewModel.filteredList.collectAsState(emptyList())

    val filterPredicate = { item: Package ->
        if (backupBoolean) item.isInstalled else item.hasBackups
    }
    val workList = filteredList.filter(filterPredicate)

    var allApkChecked by remember(workList) {
        mutableStateOf(
            viewModel.apkCheckedList.size ==
                    workList
                        .filter { !it.isSpecial && (backupBoolean || it.hasApk) }
                        .size
        )
    }
    var allDataChecked by remember(workList) {
        mutableStateOf(
            viewModel.dataCheckedList.size ==
                    workList
                        .filter { backupBoolean || it.hasData }
                        .size
        )
    }

    val batchConfirmListener = object : BatchDialogFragment.ConfirmListener {
        override fun onConfirmed(selectedPackages: List<String?>, selectedModes: List<Int>) {
            main.startBatchAction(backupBoolean, selectedPackages, selectedModes) {
                it.removeObserver(this)
            }
        }
    }

    val selection = remember { mutableStateMapOf<Package, Boolean>() }
    filteredList.forEach {
        selection.putIfAbsent(it, false)
    }

    Scaffold(containerColor = Color.Transparent) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            BatchPackageRecycler(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                productsList = workList,
                restore = !backupBoolean,
                apkCheckedList = viewModel.apkCheckedList,
                dataCheckedList = viewModel.dataCheckedList,
                onApkClick = { item: Package, b: Boolean ->
                    if (b) viewModel.apkCheckedList.add(item.packageName)
                    else viewModel.apkCheckedList.remove(item.packageName)
                    allApkChecked =
                        viewModel.apkCheckedList.size == workList.filter { ai -> !ai.isSpecial && (backupBoolean || ai.hasApk) }.size
                }, onDataClick = { item: Package, b: Boolean ->
                    if (b) viewModel.dataCheckedList.add(item.packageName)
                    else viewModel.dataCheckedList.remove(item.packageName)
                    allDataChecked =
                        viewModel.dataCheckedList.size == workList
                            .filter { ai -> backupBoolean || ai.hasData }.size
                }) { item, checkApk, checkData ->
                when (checkApk) {
                    true -> viewModel.apkCheckedList.add(item.packageName)
                    else -> viewModel.apkCheckedList.remove(item.packageName)
                }
                when (checkData) {
                    true -> viewModel.dataCheckedList.add(item.packageName)
                    else -> viewModel.dataCheckedList.remove(item.packageName)
                }
                allApkChecked =
                    viewModel.apkCheckedList.size ==
                            (workList.filter { ai -> !ai.isSpecial && (backupBoolean || ai.hasApk) }.size)
                allDataChecked =
                    viewModel.dataCheckedList.size ==
                            (workList.filter { ai -> backupBoolean || ai.hasData }.size)
            }
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StateChip(
                    icon = Phosphor.DiamondsFour,
                    text = stringResource(id = R.string.all_apk),
                    checked = allApkChecked,
                    color = ColorAPK
                ) {
                    val checkBoolean = !allApkChecked
                    allApkChecked = checkBoolean
                    if (checkBoolean)
                        viewModel.apkCheckedList.addAll(
                            workList
                                .filter { ai -> !ai.isSpecial && (backupBoolean || ai.hasApk) }
                                .mapNotNull(Package::packageName)
                        )
                    else
                        viewModel.apkCheckedList.clear()
                }
                Spacer(modifier = Modifier.width(0.1.dp))
                StateChip(
                    icon = Phosphor.HardDrives,
                    text = stringResource(id = R.string.all_data),
                    checked = allDataChecked,
                    color = ColorData
                ) {
                    val checkBoolean = !allDataChecked
                    allDataChecked = checkBoolean
                    if (checkBoolean)
                        viewModel.dataCheckedList.addAll(
                            workList
                                .filter { ai -> backupBoolean || ai.hasData }
                                .mapNotNull(Package::packageName)
                        )
                    else
                        viewModel.dataCheckedList.clear()
                }
                RoundButton(icon = Phosphor.Nut) {
                    main.showBatchPrefsSheet(backupBoolean)
                }
                ActionButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(id = if (backupBoolean) R.string.backup else R.string.restore),
                    positive = true
                ) {
                    val checkedPackages = filteredList
                        .filter { it.packageName in viewModel.apkCheckedList.union(viewModel.dataCheckedList) }
                    val selectedList =
                        checkedPackages.map(Package::packageInfo).toCollection(ArrayList())
                    val selectedListModes = checkedPackages
                        .map {
                            when (it.packageName) {
                                in viewModel.apkCheckedList.intersect(viewModel.dataCheckedList) -> ALT_MODE_BOTH
                                in viewModel.apkCheckedList -> ALT_MODE_APK
                                else -> ALT_MODE_DATA
                            }
                        }
                        .toCollection(ArrayList())
                    if (selectedList.isNotEmpty()) {
                        BatchDialogFragment(
                            backupBoolean,
                            selectedList,
                            selectedListModes,
                            batchConfirmListener
                        )
                            .show(
                                main.supportFragmentManager,
                                "DialogFragment"
                            )
                    }
                }
            }
        }
    }
}