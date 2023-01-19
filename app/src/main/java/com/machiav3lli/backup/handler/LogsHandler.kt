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
package com.machiav3lli.backup.handler

import android.content.Context
import android.content.Intent
import com.machiav3lli.backup.BACKUP_DATE_TIME_FORMATTER
import com.machiav3lli.backup.LOGS_FOLDER_NAME
import com.machiav3lli.backup.LOG_INSTANCE
import com.machiav3lli.backup.OABX
import com.machiav3lli.backup.R
import com.machiav3lli.backup.items.Log
import com.machiav3lli.backup.items.StorageFile
import com.machiav3lli.backup.pref_autoLogExceptions
import com.machiav3lli.backup.pref_maxLogCount
import com.machiav3lli.backup.preferences.onErrorInfo
import com.machiav3lli.backup.preferences.textLog
import com.machiav3lli.backup.utils.FileUtils.BackupLocationInAccessibleException
import com.machiav3lli.backup.utils.StorageLocationNotConfiguredException
import com.machiav3lli.backup.utils.getBackupRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

class LogsHandler {

    companion object {

        fun share(log: Log, asFile: Boolean = true) {
            MainScope().launch(Dispatchers.Default) {
                try {
                    getLogFile(log.logDate)?.let { log ->
                        val text = if (!asFile) log.readText() else ""
                        if (!asFile and text.isEmpty())
                            throw Exception("${log.name} is empty or cannot be read")
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/json"
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            putExtra(Intent.EXTRA_SUBJECT, "[NeoBackup] ${log.name}")
                            if (asFile)
                                putExtra(Intent.EXTRA_STREAM, log.uri)  // send as file
                            else
                                putExtra(Intent.EXTRA_TEXT, text)       // send as text
                        }
                        val shareIntent = Intent.createChooser(sendIntent, log.name)
                        OABX.activity?.startActivity(shareIntent)
                    }
                } catch (e: Throwable) {
                    unhandledException(e)
                }
            }
        }

        @Throws(IOException::class)
        fun writeToLogFile(logText: String): StorageFile? {
            runCatching {
                val backupRoot = OABX.context.getBackupRoot()
                val logsDirectory = backupRoot.ensureDirectory(LOGS_FOLDER_NAME)
                val date = LocalDateTime.now()
                val logItem = Log(logText, date)
                val logFileName = String.format(
                    LOG_INSTANCE,
                    BACKUP_DATE_TIME_FORMATTER.format(date)
                )
                logsDirectory.createFile(logFileName).let { logFile ->
                    BufferedOutputStream(logFile.outputStream()).use { logOut ->
                        logOut.write(
                            logItem.toJSON().toByteArray(StandardCharsets.UTF_8)
                        )
                        //traceDebug { "Wrote $logFile file for $logItem" }
                    }
                    housekeepingLogs()
                    return logFile
                }
            }
            return null
        }

        @Throws(IOException::class)
        fun readLogs(): MutableList<Log> {
            val logs = mutableListOf<Log>()
            val backupRoot = OABX.context.getBackupRoot()
            StorageFile.invalidateCache { it.contains(LOGS_FOLDER_NAME) }
            //val logsDirectory = StorageFile(backupRoot, LOG_FOLDER_NAME)
            backupRoot.findFile(LOGS_FOLDER_NAME)?.let { logsDir ->
                if (logsDir.isDirectory) {
                    logsDir.listFiles().forEach {
                        if (it.isFile) try {
                            logs.add(Log(it))   //TODO hg42 don't throw, but create a dummy log entry, so it can be deleted
                        } catch (e: Throwable) {
                            val message =
                                "incomplete log or wrong structure found in $it."
                            logException(e, it)
                            //no => recursion! logErrors(message)
                        }
                    }
                }
            }
            return logs
        }

        @Throws(IOException::class)
        fun housekeepingLogs() {
            try {
                val backupRoot = OABX.context.getBackupRoot()
                StorageFile.invalidateCache { it.contains(LOGS_FOLDER_NAME) }
                //val logsDirectory = StorageFile(backupRoot, LOG_FOLDER_NAME)
                backupRoot.findFile(LOGS_FOLDER_NAME)?.let { logsDir ->
                    if (logsDir.isDirectory) {
                        // must be ISO time format with sane sorted fields yyyy-mm-dd hh:mm:ss
                        val logs = logsDir.listFiles().sortedByDescending { it.name }
                        //traceDebug { "logs ${logs.map { it.name ?: "?" }.joinToString(" ")}" }
                        if (logs.size > pref_maxLogCount.value)
                            logs.subList(pref_maxLogCount.value, logs.size)
                                .forEach {
                                    try {
                                        //traceDebug { "delete ${it.path}" }
                                        it.delete()
                                    } catch (e: Throwable) {
                                        val message =
                                            "cannot delete log '${it.path}'"
                                        logException(e, message)    // only log -> no recursion!
                                    }
                                }
                    }
                }
            } catch (e: Throwable) {
                val message =
                    "housekeepingLogs failed"
                logException(e, message)    // only log -> no recursion!
            }
        }

        fun getLogFile(date: LocalDateTime): StorageFile? {
            val backupRoot = OABX.context.getBackupRoot()
            backupRoot.findFile(LOGS_FOLDER_NAME)?.let { logsDir ->
                val logFileName = String.format(
                    LOG_INSTANCE,
                    BACKUP_DATE_TIME_FORMATTER.format(date)
                )
                val file = logsDir.findFile(logFileName)
                return if (file?.exists() == true) file else null
            }
            return null
        }

        fun logErrors(errors: String) {
            try {
                val logText = errors + "\n\n" + onErrorInfo().joinToString("\n")
                writeToLogFile(logText)
            } catch (e: IOException) {
                logException(e, backTrace = true)
            } catch (e: StorageLocationNotConfiguredException) {
                logException(e, backTrace = true)
            } catch (e: BackupLocationInAccessibleException) {
                logException(e, backTrace = true)
            }
        }

        fun stackTrace(e: Throwable) = e.stackTrace.joinToString("\nat ", "at ")
        fun message(e: Throwable, backTrace: Boolean = false) =
            "${e::class.simpleName}${
                if (e.message != null)
                    "\n${e.message}"
                else
                    ""
            }${
                if (e.cause != null)
                    "\n${e.cause!!::class.simpleName}\ncause: ${e.cause!!.message}"
                else
                    ""
            }${
                if (backTrace)
                    "\n${stackTrace(e)}"
                else
                    ""
            }${
                if (backTrace && e.cause != null)
                    "\n${stackTrace(e.cause!!)}"
                else
                    ""
            }"

        fun logException(
            e: Throwable,
            what: Any? = null,
            backTrace: Boolean = false,
            prefix: String? = null,
            unhandled: Boolean = false,
        ) {
            var whatStr = ""
            if (what != null) {
                whatStr = what.toString()
                whatStr = if (whatStr.contains("\n") || whatStr.length > 20)
                    "{\n$whatStr\n}\n"
                else
                    "$whatStr : "
            }
            Timber.e("$prefix$whatStr\n${message(e, backTrace)}")
            if (unhandled && pref_autoLogExceptions.value) {
                textLog(
                    listOf(
                        "$whatStr\n${message(e, backTrace)}"
                    ) + onErrorInfo()
                )
            }
        }

        fun unhandledException(e: Throwable, what: Any? = null) {
            logException(e, what, backTrace = true, prefix = "unexpected: ", unhandled = true)
        }

        fun handleErrorMessages(context: Context, errorText: String?): String? {
            return when {
                errorText?.contains("bytes specified in the header were written")
                    ?: false -> context.getString(R.string.error_datachanged)
                errorText?.contains("Input is not in the .gz format")
                    ?: false -> context.getString(R.string.error_encryptionpassword)
                else         -> errorText
            }
        }
    }
}
