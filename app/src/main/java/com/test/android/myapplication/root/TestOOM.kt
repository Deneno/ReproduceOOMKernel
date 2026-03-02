package com.test.android.myapplication.root

import android.util.Log

object TestOOM {

    private val longtimeRootShell = RootShell()



    /** Checks the device's {@link RootState} */
    fun getRootState(): RootState {
        val rootCheckResult = executeCommand("su -v")

        if (rootCheckResult.exitCode == 0) return RootState.Rooted(RootState.RootType.Other)

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

    fun execAndGet(command: String): List<String> {
        if (getRootState() == RootState.NotRooted) return emptyList()

        return longtimeRootShell.exec(command).get()
    }



    private fun executeCommand(command: String): ExecutionResult {
        var exitCode = -1

        try {
            Log.d("TestOOM", "Executing command $command")
            val process = Runtime.getRuntime().exec("su -v")
            exitCode = process.waitFor()
        } catch (th: Throwable) {
            Log.d("TestOOM", "An error occurred while executing command '$command': $th")
        }

        return ExecutionResult(exitCode)
    }





    /** Result of command execution */
    private class ExecutionResult(val exitCode: Int)
}
