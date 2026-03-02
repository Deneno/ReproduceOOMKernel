package com.adguard.android.myapplication.root

import android.util.Log
import java.io.IOException
import java.nio.CharBuffer
import java.nio.charset.Charset

/**
 * SCP343 has obvious omnipotence.
 *
 * -[WARNING SL-4 or higher needed for further access]-
 *
 * <b>Document #343-1a:</b> "[DATA LOST]…as of [DATA EXPUNGED] 'visitors' of SCP-343 are to be
 * questioned as to their intent and convers…[DATA LOST]…uestions pertaining to other SCP are to be put
 * forth…[DATA LOST]… orders of User-Programmer ████████████ ███████████"
 */
object Scp343 {

    private const val TAG = "Scp343"
    private val MAGISK_ROOT_NAME = "MAGISK"

    private val longtimeRootShell = RootShell()



    /** Checks the device's {@link RootState} */
    fun getRootState(): RootState {
        val rootCheckResult = executeCommand("su -v")

        if (rootCheckResult.output != null && rootCheckResult.output.contains(MAGISK_ROOT_NAME, ignoreCase = true)) {
            return RootState.Rooted(RootState.RootType.Magisk)
        } else if (rootCheckResult.exitCode == 0) {
            return RootState.Rooted(RootState.RootType.Other)
        }

        val suCheckCommands = listOf(
            "/system/xbin/which su",
            "/system/bin/which su",
            "which su",
        )

        suCheckCommands.forEach { command ->
            if (executeCommand(command).exitCode == 0) {
                return RootState.Rooted(RootState.RootType.Other)
            }
        }

        return RootState.NotRooted
    }

    /** Requests and checks that user is allowed to run programs as root and store su version */
    fun requestAndCheckRootAccess(): Boolean {
        if (getRootState() == RootState.NotRooted) return false

        try {
            val result = longtimeRootShell.exec("id")
            val list = result.get()
            if (result.isCancelled) {
                throw IOException("Command id canceled")
            }
            list.forEach { line ->
                if (line.contains("uid=0")) return true
            }
        } catch (th: Throwable) {
            Log.w(TAG, "Root check failed: $th")
        }

        return false
    }

    /**
     * Executes specified command with root rights
     *
     * @param command command
     * @return future with command result and state
     * @throws IOException IO error
     */
    @Throws(IOException::class)
    fun exec(command: String): ShellResult.List {
        if (getRootState() == RootState.NotRooted) return ShellResult.List(null)

        return longtimeRootShell.exec(command)
    }

    /**
     * Executes command with root rights and using result listener
     *
     * @param command command
     * @param buffer buffer for output
     * @throws IOException IO error
     */
    @Throws(IOException::class)
    fun exec(command: String, buffer: CharBuffer): ShellResult.Buffer {
        buffer.clear()

        if (getRootState() == RootState.NotRooted) return ShellResult.Buffer(buffer)

        return longtimeRootShell.exec(command, buffer)
    }

    /**
     * Executes specified command with root rights and gets calculated result
     *
     * @param command command
     * @return output
     * @throws IOException IO error
     */
    @Throws(IOException::class)
    fun execAndGet(command: String): List<String> {
        if (getRootState() == RootState.NotRooted) return emptyList()

        return longtimeRootShell.exec(command).get()
    }

    /**
     * Executes specified commands with root rights
     *
     * @param commandsArray array of commands
     * @param sleepTime sleep time
     * @return list of results
     * @throws IOException IO error
     * @throws InterruptedException Operation interrupted
     */
    @Throws(IOException::class, InterruptedException::class)
    fun execAndGet(commandsArray: Array<String>, sleepTime: Long): List<String> {
        if (getRootState() == RootState.NotRooted) return emptyList()

        val output = ArrayList<String>()
        commandsArray.forEach { command ->
            val result = longtimeRootShell.exec(command)
            Thread.sleep(sleepTime)
            output.addAll(result.get())
        }

        return output
    }

    /**
     * Executes specified commands with root rights using result listener
     *
     * @param commandsArray array of commands
     * @param listener listener that invoked at each line
     * @throws IOException IO error
     */
    @Throws(IOException::class)
    fun execAndWaitFinish(commandsArray: Array<String>, listener: ShellResult.Listener) {
        if (getRootState() == RootState.NotRooted) return

        var result: ShellResult<*>? = null
        commandsArray.forEach { command ->
            result = longtimeRootShell.exec(command, listener)
        }

        result?.get() // Waiting for only last command
    }

    /**
     * Check that command output contains string
     *
     * @param command       Command to run
     * @param stringToCheck String to check
     * @return True if output contains the given string
     */
    @SuppressWarnings("SameParameterValue")
    fun commandOutputContains(command: String, stringToCheck: String): Boolean {
        if (getRootState() == RootState.NotRooted) return false

        try {
            val result = longtimeRootShell.exec(command)
            return "${result.get()}\n".contains(stringToCheck)
        } catch (e: IOException) {
            return false
        }
    }



    private fun executeCommand(command: String): ExecutionResult {
        var output: String? = null
        var exitCode = -1

        try {
            Log.d(TAG, "Executing command $command")
            val process = Runtime.getRuntime().exec("su -v")
            exitCode = process.waitFor()

            output = process.inputStream.bufferedReader(Charset.defaultCharset()).readText()
        } catch (th: Throwable) {
            Log.d(TAG, "An error occurred while executing command '$command': $th")
        }

        return ExecutionResult(exitCode, output)
    }





    /** Result of command execution */
    private class ExecutionResult(val exitCode: Int, val output: String?)
}
