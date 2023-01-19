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
package com.machiav3lli.backup.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState
import com.machiav3lli.backup.OABX
import com.machiav3lli.backup.R
import com.machiav3lli.backup.dbs.ODatabase
import com.machiav3lli.backup.fragments.HelpSheet
import com.machiav3lli.backup.ui.compose.icons.Phosphor
import com.machiav3lli.backup.ui.compose.icons.phosphor.Info
import com.machiav3lli.backup.ui.compose.item.RoundButton
import com.machiav3lli.backup.ui.compose.item.TopBar
import com.machiav3lli.backup.ui.compose.navigation.NavItem
import com.machiav3lli.backup.ui.compose.navigation.PagerNavBar
import com.machiav3lli.backup.ui.compose.navigation.PrefsNavHost
import com.machiav3lli.backup.ui.compose.theme.AppTheme
import com.machiav3lli.backup.utils.destinationToItem
import com.machiav3lli.backup.utils.setCustomTheme
import com.machiav3lli.backup.viewmodels.ExportsViewModel
import com.machiav3lli.backup.viewmodels.LogViewModel

class PrefsActivityX : BaseActivity() {
    private var helpSheet: HelpSheet? = null
    private val exportsViewModel: ExportsViewModel by viewModels {
        ExportsViewModel.Factory(ODatabase.getInstance(applicationContext).scheduleDao, application)
    }
    private val logsViewModel: LogViewModel by viewModels {
        LogViewModel.Factory(application)
    }

    @OptIn(
        ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
        ExperimentalPagerApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        OABX.activity = this
        setCustomTheme()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val pagerState = rememberPagerState()
                val navController = rememberAnimatedNavController()
                val pages = listOf(
                    NavItem.UserPrefs,
                    NavItem.ServicePrefs,
                    NavItem.AdvancedPrefs,
                    NavItem.ToolsPrefs,
                )
                val currentPage by remember(pagerState.currentPage) { mutableStateOf(pages[pagerState.currentPage]) }
                var barVisible by remember { mutableStateOf(true) }

                navController.addOnDestinationChangedListener { _, destination, _ ->
                    barVisible = destination.route == NavItem.Settings.destination
                }

                Scaffold(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    topBar = {
                        Column {
                            TopBar(
                                title = stringResource(
                                    id = if (barVisible) currentPage.title
                                    else navController.currentDestination?.destinationToItem()?.title
                                        ?: NavItem.Settings.title
                                )
                            ) {
                                RoundButton(
                                    icon = Phosphor.Info,
                                    description = stringResource(id = R.string.help),
                                ) {
                                    if (helpSheet != null && helpSheet!!.isVisible) helpSheet?.dismissAllowingStateLoss()
                                    helpSheet = HelpSheet()
                                    helpSheet!!.showNow(supportFragmentManager, "HELPSHEET")
                                }
                            }
                        }
                    },
                    bottomBar = {
                        AnimatedVisibility(
                            barVisible,
                            enter = slideInVertically { height -> height },
                            exit = slideOutVertically { height -> height },
                        ) {
                            PagerNavBar(pageItems = pages, pagerState = pagerState)
                        }
                    }
                ) { paddingValues ->
                    PrefsNavHost(
                        modifier = Modifier.padding(paddingValues),
                        navController = navController,
                        pagerState = pagerState,
                        pages = pages,
                        viewModels = listOf(
                            exportsViewModel,
                            logsViewModel,
                        )
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        OABX.activity = this
    }
}
