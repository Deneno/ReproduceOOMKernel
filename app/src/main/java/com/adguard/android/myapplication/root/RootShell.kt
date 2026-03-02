package com.adguard.android.myapplication.root

import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.nio.CharBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Shell under root which uses for running of commands
 */
class RootShell {

    companion object {
        private const val TAG = "RootShell"
    }



    /* We use this pseudo-command to separate commands.
     * One more idea to separate is to run "echo "commandDelimiter $?".
     * We can get exit code of command using this variant. */
    private val commandDelimiter = "[]///[]"

    private val nextShellNumber = AtomicInteger(0)

    private var process: Process? = null
    private var shellInput: PrintWriter? = null
    private var handler: Handler? = null
    private val outputs = ConcurrentLinkedQueue<ShellResult<*>>()



    /**
     * Queues command and gets Future with result and state
     *
     * @param command shell command for running
     * @return Future object with result and state
     * @throws IOException IO error
     */
    @Throws(IOException::class)
    fun exec(command: String): ShellResult.List {
        return execPrivate(command, ShellResult.List(null))
    }

    /**
     * Queues command and gets Future with result and state
     *
     * @param command shell command for running
     * @param buffer buffer for shell output
     * @return Future object with result and state
     * @throws IOException IO error
     */
    @Throws(IOException::class)
    fun exec(command: String, buffer: CharBuffer): ShellResult.Buffer {
        return execPrivate(command, ShellResult.Buffer(buffer))
    }

    /**
     * Queues command and gets Future with result and state
     *
     * @param command shell command for running
     * @param listener listener for pass of intermediate output
     * @return Future object with result and state
     * @throws IOException IO error
     */
    @Throws(IOException::class)
    fun exec(command: String, listener: ShellResult.Listener): ShellResult.List {
        return execPrivate(command, ShellResult.List(listener))
    }



    @Throws(IOException::class)
    private fun <T : ShellResult<*>> execPrivate(command: String, instance: T): T {
        synchronized(this) {
            if (process == null) {
                val process = ProcessBuilder().command("su").redirectErrorStream(true).start()
                this.process = process
                shellInput = PrintWriter(process.outputStream, true)
            }

            if (handler == null) {
                handler = Handler(nextShellNumber.incrementAndGet())
                handler?.start()
                Log.w(TAG, "Root shell \"${handler?.name}\" started.")
            }

            outputs.add(instance)
            shellInput?.println(command)
            shellInput?.println(commandDelimiter)
            return instance
        }
    }

    private fun addLine(text: String, forceLog: Boolean) {
        var result: ShellResult<*>
        synchronized (this) {
            result = outputs.peek()
        }
        if (result != null) {
            result.add(text)
            if (forceLog) {
                Log.w(TAG, "Text added to result: " + text)
            }
        } else {
            Log.w(TAG, "Text didn't add to result: " + text)
        }
    }

    private fun cancellAllQueue() {
        outputs.forEach {
            outputs.poll()?.cancel(true)
        }
    }

    private fun reset() {
        synchronized(this) {
            cancellAllQueue()
            process?.destroy()
            process = null
            handler = null
        }
    }





    /**
     * Handler-thread which reads shell output
     */
    private inner class Handler(shellNumber: Int) : Thread("ShellHandler-$shellNumber") {

        init {
            setDaemon(true)
        }

        override fun run() {
            try {
                processShellOutputWhileOpened()
            } catch (e: Exception) {
                addLine("Root shell destroyed: " + e, true)
            } finally {
                Log.w(TAG, "Root shell \"" + getName() + "\" closed")
                reset()
            }
        }



        @Throws(Exception::class)
        private fun processShellOutputWhileOpened() {
            try {
                val reader = BufferedReader(InputStreamReader(process?.inputStream))
                reader.forEachLine { processLine(it) }
            } catch (e: Exception) {
                if (e.message?.contains("Stream closed") != true) {
                    // Anyway we will catch IOException with message "Stream closed", when the stream will be closed
                    // It's expected behavior and we shouldn't care about it.
                    // Another way, if exception don't contains this message we must throw this exception in the outer try-catch block.
                    throw e
                }
            }
            val exitStatus = process?.waitFor()
            if (exitStatus != 0) {
                addLine("Root shell exited with non-zero exit status: " + exitStatus, true)
            }
        }

        private fun processLine(line: String) {
            if (line.isNotEmpty()) {
                if (line.contains(commandDelimiter)) {
                    var result: ShellResult<*>? = null
                    synchronized(this) {
                         result = outputs.poll()
                    }
                    result?.setupDone()
                } else {
                    addLine(line, false)
                }
            }
        }
    }
}
