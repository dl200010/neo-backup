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

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec
import com.machiav3lli.backup.MAIN_FILTER_DEFAULT
import com.machiav3lli.backup.MAIN_FILTER_DEFAULT_WITHOUT_SPECIAL
import com.machiav3lli.backup.MODE_APK
import com.machiav3lli.backup.SPECIAL_FILTER_ALL
import com.machiav3lli.backup.handler.LogsHandler
import com.machiav3lli.backup.handler.WorkHandler
import com.machiav3lli.backup.items.StorageFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import java.io.IOException

@Entity
@Serializable
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val enabled: Boolean = false,
    val name: String = "New Schedule",
    val timeHour: Int = 0,
    val timeMinute: Int = 0,
    val interval: Int = 1,
    val timePlaced: Long = System.currentTimeMillis(),

    val filter: Int = MAIN_FILTER_DEFAULT,
    val mode: Int = MODE_APK,
    val specialFilter: Int = SPECIAL_FILTER_ALL,

    val timeToRun: Long = 0,        //TODO should this be in hashCode and equals ???

    val customList: Set<String> = setOf(),

    val blockList: Set<String> = setOf(),
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val schedule = other as Schedule
        return id == schedule.id
                && name == schedule.name
                && enabled == schedule.enabled
                && timeHour == schedule.timeHour
                && timeMinute == schedule.timeMinute
                && interval == schedule.interval
                && timePlaced == schedule.timePlaced
                && filter == schedule.filter
                && mode == schedule.mode
                && specialFilter == schedule.specialFilter
                && customList == schedule.customList
                && blockList == schedule.blockList
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 31 * hash + id.toInt()
        hash = 31 * hash + name.hashCode()
        hash = 31 * hash + if (enabled) 1 else 0
        hash = 31 * hash + timeHour
        hash = 31 * hash + timeMinute
        hash = 31 * hash + interval
        hash = 31 * hash + timePlaced.toInt()
        hash = 31 * hash + filter.hashCode()
        hash = 31 * hash + mode.hashCode()
        hash = 31 * hash + specialFilter.hashCode()
        hash = 31 * hash + customList.hashCode()
        hash = 31 * hash + blockList.hashCode()
        return hash
    }

    override fun toString(): String {
        return "Schedule{" +
                "id=" + id +
                ", name=" + name +
                ", enabled=" + enabled +
                ", timeHour=" + timeHour +
                ", timeMinute=" + timeMinute +
                ", interval=" + interval +
                ", timePlaced=" + timePlaced +
                ", timeToRun=" + timeToRun +
                ", filter=" + filter +
                ", mode=" + mode +
                ", specialFilter=" + specialFilter +
                ", customList=" + customList +
                ", blockList=" + blockList +
                '}'
    }

    fun getBatchName(startTime: Long): String =
        WorkHandler.getBatchName(this.name, startTime)

    class Builder() {
        var schedule: Schedule = Schedule()

        constructor(exportFile: StorageFile) : this() {
            try {
                exportFile.inputStream()!!.use { inputStream ->
                    val item = fromJson(inputStream.reader().readText())
                    schedule = item.copy(
                        enabled = false,
                        timePlaced = System.currentTimeMillis(),
                        timeToRun = 0,
                    )
                }
            } catch (e: FileNotFoundException) {
                throw Backup.BrokenBackupException(
                    "Cannot open ${exportFile.name} at ${exportFile.path}",
                    e
                )
            } catch (e: IOException) {
                throw Backup.BrokenBackupException(
                    "Cannot read ${exportFile.name} at ${exportFile.path}",
                    e
                )
            } catch (e: Throwable) {
                LogsHandler.unhandledException(e, exportFile.path)
                throw Backup.BrokenBackupException("Unable to process ${exportFile.name} at ${exportFile.path}. [${e.javaClass.canonicalName}] $e")
            }
        }

        fun withId(id: Int): Builder {
            schedule = schedule.copy(id = id.toLong())
            return this
        }

        fun withSpecial(with: Boolean = true): Builder {
            schedule = schedule.copy(
                filter = if (with) MAIN_FILTER_DEFAULT
                else MAIN_FILTER_DEFAULT_WITHOUT_SPECIAL
            )
            return this
        }

        fun import(export: Schedule): Builder {
            schedule = export
                .copy(
                    id = schedule.id,
                    enabled = false,
                    timePlaced = System.currentTimeMillis(),
                    timeToRun = 0,
                )
            return this
        }

        fun build(): Schedule {
            return schedule
        }
    }

    fun toJSON() = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String) = Json.decodeFromString<Schedule>(json)

        @RenameColumn(
            tableName = "Schedule",
            fromColumnName = "timeUntilNextEvent",
            toColumnName = "timeToRun"
        )
        class AutoMigration : AutoMigrationSpec {}
    }
}