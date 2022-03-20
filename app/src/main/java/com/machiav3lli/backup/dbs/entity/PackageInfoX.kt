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
package com.machiav3lli.backup.dbs.entity

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File

@Entity
open class PackageInfoX(
    @PrimaryKey
    var packageName: String,
    var packageLabel: String? = null,
    var versionName: String? = null,
    var versionCode: Int = 0,
    var profileId: Int = 0,
    var sourceDir: String? = null,
    var splitSourceDirs: Array<String> = arrayOf(),
    var isSystem: Boolean = false,
    var icon: Int = -1
) {
    open val isSpecial: Boolean
        get() = false

    constructor(context: Context, pi: PackageInfo) : this(
        packageName = pi.packageName,
        packageLabel = pi.applicationInfo.loadLabel(context.packageManager).toString(),
        versionName = pi.versionName,
        versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi.longVersionCode.toInt()
        else pi.versionCode,
        profileId = try {
            File(pi.applicationInfo.dataDir).parentFile?.name?.toInt() ?: -1
        } catch (e: NumberFormatException) {
            -1 // Android System "App" points to /data/system
        },
        sourceDir = pi.applicationInfo.sourceDir,
        splitSourceDirs = pi.applicationInfo.splitSourceDirs ?: arrayOf(),
        isSystem = pi.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM,
        icon = pi.applicationInfo.icon
    )
}