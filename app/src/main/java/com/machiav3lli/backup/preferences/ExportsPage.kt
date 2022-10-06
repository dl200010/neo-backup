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
package com.machiav3lli.backup.preferences

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.machiav3lli.backup.R
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.CalendarPlus
import com.machiav3lli.backup.ui.compose.recycler.ExportedScheduleRecycler
import com.machiav3lli.backup.ui.compose.theme.AppTheme
import com.machiav3lli.backup.viewmodels.ExportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportsPage(viewModel: ExportsViewModel) {
    val exports by viewModel.exportsList.observeAsState()

    SideEffect {
        viewModel.refreshList()
    }

    AppTheme {
        Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(onClick = viewModel::exportSchedules) {
                    Icon(
                        imageVector = Phosphor.CalendarPlus,
                        contentDescription = stringResource(id = R.string.dialog_export_schedules)
                    )
                    Text(text = stringResource(id = R.string.dialog_export_schedules))
                }
            }
        ) { paddingValues ->
            ExportedScheduleRecycler(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                productsList = exports,
                onImport = { viewModel.importSchedule(it) },
                onDelete = { viewModel.deleteExport(it) }
            )
        }
    }
}