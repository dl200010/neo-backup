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
package com.machiav3lli.backup.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.icu.util.Calendar
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.machiav3lli.backup.ISO_DATE_TIME_FORMAT
import com.machiav3lli.backup.ISO_DATE_TIME_FORMAT_MIN
import com.machiav3lli.backup.MODE_UNSET
import com.machiav3lli.backup.OABX
import com.machiav3lli.backup.OABX.Companion.getString
import com.machiav3lli.backup.R
import com.machiav3lli.backup.dbs.ODatabase
import com.machiav3lli.backup.dbs.dao.ScheduleDao
import com.machiav3lli.backup.dbs.entity.Schedule
import com.machiav3lli.backup.handler.ShellCommands
import com.machiav3lli.backup.preferences.pref_fakeScheduleMin
import com.machiav3lli.backup.preferences.pref_useAlarmClock
import com.machiav3lli.backup.preferences.pref_useExactAlarm
import com.machiav3lli.backup.services.AlarmReceiver
import com.machiav3lli.backup.services.ScheduleService
import com.machiav3lli.backup.traceSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlin.math.max

fun calculateTimeToRun(schedule: Schedule, now: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = schedule.timePlaced

    val limitIncrements = 366/schedule.interval
    val minTimeFromNow = TimeUnit.MINUTES.toMillis(1)

    val fakeMin = pref_fakeScheduleMin.value
    if (fakeMin > 0) {
        //c[Calendar.HOUR_OF_DAY] = schedule.timeHour
        c[Calendar.MINUTE] = (c[Calendar.MINUTE] / fakeMin + 1) * fakeMin % 60
        c[Calendar.SECOND] = 0
        c[Calendar.MILLISECOND] = 0
        repeat(limitIncrements) {
            if (c.timeInMillis > now + minTimeFromNow)
                return@repeat
            //traceSchedule { "increment $it * $fakeMin min" }
            c.add(Calendar.MINUTE, fakeMin)
        }
    } else {
        c[Calendar.HOUR_OF_DAY] = schedule.timeHour
        c[Calendar.MINUTE] = schedule.timeMinute
        c[Calendar.SECOND] = 0
        c[Calendar.MILLISECOND] = 0
        repeat(limitIncrements) {
            if (c.timeInMillis > now + minTimeFromNow)
                return@repeat
            traceSchedule { "increment $it * ${schedule.interval} days" }
            c.add(Calendar.DAY_OF_MONTH, schedule.interval)
        }
    }

    traceSchedule {
        "calculateTimeToRun: now: ${
            ISO_DATE_TIME_FORMAT.format(now)
        } placed: ${
            ISO_DATE_TIME_FORMAT.format(schedule.timePlaced)
        } interval: ${
            schedule.interval
        } next: ${
            ISO_DATE_TIME_FORMAT.format(c.timeInMillis)
        }"
    }
    return c.timeInMillis
}

val updateInterval = 1_000L
val useSeconds = true && updateInterval < 60_000

fun calcTimeLeft(schedule: Schedule): Pair<String, String> {
    var absTime = ""
    var relTime = ""
    val now = System.currentTimeMillis()
    val at = calculateTimeToRun(schedule, now)
    absTime = ISO_DATE_TIME_FORMAT_MIN.format(at)
    val timeDiff = max(at - now, 0)
    val days = TimeUnit.MILLISECONDS.toDays(timeDiff).toInt()
    if (days != 0) {
        relTime +=
            OABX.context.resources.getQuantityString(R.plurals.days_left, days, days) +
                    " + "
    }
    val hours = TimeUnit.MILLISECONDS.toHours(timeDiff).toInt() % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeDiff).toInt() % 60
    if (useSeconds) {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeDiff).toInt() % 60
        relTime += LocalTime.of(hours, minutes, seconds).toString()
    } else {
        relTime += LocalTime.of(hours, minutes).toString()
    }
    return Pair(absTime, relTime)
}

@Composable
fun timeLeft(
    schedule: Schedule,
    scope: CoroutineScope,
): MutableStateFlow<Pair<String, String>> {
    val state = MutableStateFlow(calcTimeLeft(schedule))

    LaunchedEffect(state) {
        delay(updateInterval)
        state.emit(calcTimeLeft(schedule))
    }

    //traceDebug { state.value.let { "⏳ ${it[0]}  🕒 ${it[1]}" } }

    return state
}


fun scheduleAlarm(context: Context, scheduleId: Long, rescheduleBoolean: Boolean) {
    if (scheduleId >= 0) {
        Thread {
            val scheduleDao = ODatabase.getInstance(context).scheduleDao
            var schedule = scheduleDao.getSchedule(scheduleId)
            if (schedule?.enabled == true) {

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                val now = System.currentTimeMillis()
                val timeToRun = calculateTimeToRun(schedule, now)
                val timeLeft = timeToRun - now

                if (rescheduleBoolean) {
                    schedule = schedule.copy(
                        timePlaced = now,
                        timeToRun = timeToRun
                    )
                    traceSchedule { "re-scheduling $schedule" }
                    scheduleDao.update(schedule)
                } else {
                    if (timeLeft <= TimeUnit.MINUTES.toMillis(1)) {
                        schedule = schedule.copy(
                            timeToRun = now + TimeUnit.MINUTES.toMillis(1)
                        )
                        traceSchedule { "!!!!!!!!!! timeLeft < 1 min -> set schedule $schedule" }
                        scheduleDao.update(schedule)
                    }
                }

                val hasPermission: Boolean =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        alarmManager.canScheduleExactAlarms()
                    } else {
                        true
                    }
                val pendingIntent = createPendingIntent(context, scheduleId)
                if (hasPermission && pref_useAlarmClock.value) {
                    traceSchedule { "alarmManager.setAlarmClock $schedule" }
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(schedule.timeToRun, null),
                        pendingIntent
                    )
                } else {
                    if (hasPermission && pref_useExactAlarm.value) {
                        traceSchedule { "alarmManager.setExactAndAllowWhileIdle $schedule" }
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            schedule.timeToRun,
                            pendingIntent
                        )
                    } else {
                        traceSchedule { "alarmManager.setAndAllowWhileIdle $schedule" }
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            schedule.timeToRun,
                            pendingIntent
                        )
                    }
                }
                traceSchedule {
                    "schedule starting in: ${
                        TimeUnit.MILLISECONDS.toMinutes(schedule.timeToRun - System.currentTimeMillis())
                    } minutes"
                }
            } else
                traceSchedule { "schedule is disabled. Nothing to schedule!" }
        }.start()
    } else {
        Timber.e("got id: $scheduleId from $context")
    }
}

fun cancelAlarm(context: Context, scheduleId: Long) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = createPendingIntent(context, scheduleId)
    alarmManager.cancel(pendingIntent)
    pendingIntent.cancel()
    traceSchedule { "cancelled schedule with id: $scheduleId" }
}

fun scheduleAlarms() {
    CoroutineScope(Dispatchers.Default).launch {
        val scheduleDao = ODatabase.getInstance(OABX.context).scheduleDao
        traceSchedule { "schedule alarms" }
        scheduleDao.all
            .forEach {
                if (it.enabled)
                    scheduleAlarm(OABX.context, it.id, false)
                else
                    cancelAlarm(OABX.context, it.id)
            }
    }
}

fun createPendingIntent(context: Context, scheduleId: Long): PendingIntent {
    val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
        action = "schedule"
        putExtra("scheduleId", scheduleId)
        addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
    }
    return PendingIntent.getBroadcast(
        context,
        scheduleId.toInt(),
        alarmIntent,
        PendingIntent.FLAG_IMMUTABLE
    )
}


fun startSchedule(schedule: Schedule) {

    OABX.main?.let {

        val database = ODatabase.getInstance(OABX.context)

        val message = StringBuilder()

        message.append(
            "\n${getString(R.string.sched_mode)} ${
                modesToString(
                    OABX.context,
                    modeToModes(schedule.mode)
                )
            }"
        )
        message.append(
            "\n${getString(R.string.backup_filters)} ${
                filterToString(
                    OABX.context,
                    schedule.filter
                )
            }"
        )
        message.append(
            "\n${getString(R.string.other_filters_options)} ${
                specialFilterToString(
                    OABX.context,
                    schedule.specialFilter
                )
            }"
        )
        // TODO list the CL packages
        message.append(
            "\n${getString(R.string.customListTitle)}: ${
                if (schedule.customList.isNotEmpty()) getString(
                    R.string.dialogYes
                ) else getString(R.string.dialogNo)
            }"
        )
        // TODO list the BL packages
        message.append(
            "\n${getString(R.string.sched_blocklist)}: ${
                if (schedule.blockList.isNotEmpty()) getString(
                    R.string.dialogYes
                ) else getString(R.string.dialogNo)
            }"
        )
        AlertDialog.Builder(it)
            .setTitle("${schedule.name}: ${getString(R.string.sched_activateButton)}?")
            .setMessage(message)
            .setPositiveButton(R.string.dialogOK) { _: DialogInterface?, _: Int ->
                if (schedule.mode != MODE_UNSET)
                    StartSchedule(OABX.context, database.scheduleDao, schedule.id).execute()
            }
            .setNegativeButton(R.string.dialogCancel) { _: DialogInterface?, _: Int -> }
            .show()
    }
}

internal class StartSchedule(
    val context: Context,
    val scheduleDao: ScheduleDao,
    private val scheduleId: Long
) :
    ShellCommands.Command {

    override fun execute() {
        Thread {
            val now = System.currentTimeMillis()
            val serviceIntent = Intent(context, ScheduleService::class.java)
            scheduleDao.getSchedule(scheduleId)?.let { schedule ->
                serviceIntent.putExtra("scheduleId", scheduleId)
                serviceIntent.putExtra("name", schedule.getBatchName(now))
                context.startService(serviceIntent)
            }
        }.start()
    }
}

