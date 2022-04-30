package tests

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.machiav3lli.backup.PREFS_ENCRYPTION
import com.machiav3lli.backup.actions.BackupAppAction
import com.machiav3lli.backup.actions.RestoreAppAction
import com.machiav3lli.backup.handler.ShellHandler
import com.machiav3lli.backup.handler.ShellHandler.Companion.runAsRoot
import com.machiav3lli.backup.handler.ShellHandler.Companion.utilBoxQ
import com.machiav3lli.backup.items.RootFile
import com.machiav3lli.backup.items.StorageFile
import com.machiav3lli.backup.utils.getPrivateSharedPrefs
import com.topjohnwu.superuser.ShellUtils.fastCmd
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class Test_BackupRestore {

    companion object {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        //val context = Utils.getDeContext(Utils.getContext())
        //val context = Utils.getContext()

        lateinit var tempDir: RootFile
        lateinit var testDir: RootFile

        var backupCreated = false
        var contentLs = listOf<String>()
        var contentCount = 0

        @BeforeClass @JvmStatic
        fun setupClass() {
            //tempDir = RootFile(context.cacheDir, "test_backup_restore")
            tempDir = RootFile(context.dataDir, "test_backup_restore")
            //tempDir = RootFile("/data/local/tmp/test_backup_restore")
            tempDir.deleteRecursive()
            tempDir.mkdirs()
            testDir = prepareDirectory()
        }

        @AfterClass @JvmStatic
        fun cleanupClass() {
            tempDir.deleteRecursive()
        }

        fun listContent(dir: RootFile): List<String> {
            //return runAsRoot("ls -bAlZ ${dir.quoted} | grep -v total | sort").out.joinToString("\n")
            return runAsRoot(
                        "cd ${dir.quoted} ; stat -c '%y %A %f %F %U %G %s %n' .* * | grep -v '\\.$'"
            ).out
                .filterNotNull()
                .map { it.replace(Regex(""":\d\d\.\d{9}\b"""), "") }
                .map { it.replace(Regex(""" link +\w+ +\w+"""), " link") }
                .sorted()
        }

        var checks = mutableMapOf<String, (file: RootFile) -> Boolean>()

        private fun prepareDirectory(): RootFile {

            contentCount = 0

            tempDir.setWritable(true, false)

            var dir: RootFile
            var files: RootFile

            run {
                val name = "test_data"
                dir = RootFile(tempDir, name)
                dir.mkdirs()
            }

            run {
                val name = "files"
                files = RootFile(dir, name)
                files.mkdirs()
                checks["$name/type"] = { it.isDirectory }
                contentCount++
            }

            run {
                val name = "regular"
                val regularContent = "TEST"
                val regularTime = "2013-12-21T12:34:56"
                val regular = RootFile(dir, name)
                fastCmd("$utilBoxQ echo -n '$regularContent' >${regular.quoted}")
                fastCmd("$utilBoxQ touch -d $regularTime ${regular.quoted}")
                checks["$name/type"] = { it.isFile }
                checks["$name/content"] = { it.readText() == regularContent }
                checks["#$name/time"] = {
                    SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss",
                        Locale.getDefault()
                    ).format(it.lastModified()) == regularTime
                }
                contentCount++
            }

            run {
                val name = "regular_system"
                val systemContent = "SYSTEM"
                val system = RootFile(dir, name)
                fastCmd("$utilBoxQ echo -n '$systemContent' >${system.quoted}")
                fastCmd("$utilBoxQ chown system:system ${system.quoted}")
                checks["$name/type"] = { it.isFile }
                checks["$name/content"] = { it.readText() == systemContent }
                contentCount++
            }

            run {
                val name = "fifo"
                val fifo = RootFile(dir, name)
                fastCmd("$utilBoxQ mkfifo ${fifo.quoted}")
                contentCount++
            }

            run {
                val name = "link_sym_dir_rel"
                val symLinkDirRel = RootFile(dir, name)
                fastCmd("cd ${dir.quoted} && $utilBoxQ ln -s ${files.name} ${symLinkDirRel.name}")
                checks["$name/type"] = { it.isSymlink() }
                contentCount++
            }

            run {
                val name = "link_sym_dir_abs"
                val symLinkDirAbs = RootFile(dir, name)
                fastCmd("$utilBoxQ ln -s ${files.absolutePath} ${symLinkDirAbs.quoted}")
                checks["$name/type"] = { it.isSymlink() }
                contentCount++
            }

            /*
            run {
                val name = "..dir"
                val dotdotdir = RootFile(dir, name)
                fastCmd("$utilBoxQ mkdir -p ${dotdotdir.quoted}")
                checks["$name/type"] = { it.isDirectory }
                contentCount++
            }
            */

            contentLs = listContent(dir)

            return dir
        }
    }

    //val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivityX::class.java)
    //    .putExtra("title", "Testing rules!")

    //@get:Rule
    //val activityRule = activityScenarioRule<MainActivityX>( /* intent */ )
    //var activityRule = ActivityScenarioRule(MainActivityX::class.java)

    lateinit var tempStorage : StorageFile
    lateinit var shellHandler : ShellHandler
    lateinit var backupDirTarApi : StorageFile
    lateinit var backupDirTarCmd : StorageFile
    lateinit var archiveTarApi : StorageFile
    lateinit var archiveTarCmd : StorageFile
    lateinit var restoreCache : File

    fun archiveTarApi(compress: Boolean) = StorageFile(backupDirTarApi, "data.tar${if (compress) ".gz" else ""}")
    fun archiveTarCmd(compress: Boolean) = StorageFile(backupDirTarCmd, "data.tar${if (compress) ".gz" else ""}")

    @Before
    fun init() {
        tempStorage = StorageFile(tempDir)
        shellHandler = ShellHandler()
        backupDirTarApi = tempStorage.createDirectory("backup_tarapi")
        backupDirTarCmd = tempStorage.createDirectory("backup_tarcmd")
        //restoreCache = RootFile(tempDir, "restore").also { it.mkdirs() }
        //restoreCache = RootFile(context.cacheDir)
        restoreCache = File(context.cacheDir, "restore").also { it.mkdirs() }
    }

    @After
    fun cleanup() {
        RootFile(restoreCache).deleteRecursive()
    }

    fun checkDirectory(dir: RootFile) {
        val want = contentLs
        val real = listContent(dir)
        assertTrue(want.isNotEmpty())
        assertTrue(real.isNotEmpty())
        checks.forEach { (name, check) ->
            if(name.startsWith("#"))
                return
            val fileName = name.split("/")[0]
            val result = check(RootFile(dir, fileName))
            if(!result)
                Log.w("checkDirectory", name)
            assertTrue("check failed for $name", result)
        }
        assertEquals(want, real)
    }

    @Test
    fun test_sourceContents() {
        assertTrue(contentLs.size == contentCount)
        checkDirectory(testDir)
    }

    fun backup(compress: Boolean) {
        if(!backupCreated) {
            context.applicationContext.getPrivateSharedPrefs().edit().putBoolean(PREFS_ENCRYPTION, false)
            val iv = null     //initIv(CIPHER_ALGORITHM)
            val backupAction = BackupAppAction(context, null, shellHandler)
            val fromDir = StorageFile(testDir)
            backupAction.genericBackupDataTarApi(
                "data", backupDirTarApi, fromDir.toString(),
                compress, iv
            )
            backupAction.genericBackupDataTarCmd(
                "data", backupDirTarCmd, fromDir.toString(),
                compress, iv
            )
            backupCreated = true
        }
    }

    /*
    fun listTar(archive: StorageFile): String {
        val result = runAsRoot(
            // toybox doesn't have awk
            //"tar -tvzf ${quote(archive.file!!.absolutePath)} | awk '{if($8) {print $1,$2,$3,$6,$7,$8} else {print $1,$2,$3,$6}}'"
            "tar -tvzf ${quote(archive.file!!.absolutePath)}"
        ).out
            .filterNotNull()
            .map { it.replace(Regex("""/$"""), "") }
            .map { it.replace(Regex(""" \./"""), " ") }
            .sorted()
            .joinToString("\n")
        return result
    }
    */

    fun checkRestore(toType: String, fromType: String) {
        val compress = false

        backup(compress)

        val archive = if(fromType == "tarapi")
                            archiveTarApi(compress)
                        else
                            archiveTarCmd(compress)
        val restoreAction = RestoreAppAction(context, null, shellHandler)
        val restoreDir = RootFile(tempDir, "restore_" + toType)
        restoreDir.mkdirs()
        if(toType == "tarapi")
            restoreAction.genericRestoreFromArchiveTarApi(
                "data", archive, restoreDir.toString(),
                compress, false, null, restoreCache
            )
        else
            restoreAction.genericRestoreFromArchiveTarCmd(
                "data", archive, restoreDir.toString(),
                compress, false, null
            )
        checkDirectory(restoreDir)
    }

    @Test fun test_restore_tarapi_from_tarapi() { checkRestore("tarapi", "tarapi") }
    @Test fun test_restore_tarcmd_from_tarapi() { checkRestore("tarcmd", "tarapi") }
    @Test fun test_restore_tarapi_from_tarcmd() { checkRestore("tarapi", "tarcmd") }
    @Test fun test_restore_tarcmd_from_tarcmd() { checkRestore("tarcmd", "tarcmd") }
}
