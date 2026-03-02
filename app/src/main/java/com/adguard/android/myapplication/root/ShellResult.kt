package com.adguard.android.myapplication.root

import androidx.annotation.CallSuper
import java.nio.CharBuffer
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Future shell result with output and state.
 * You can pass listener for listening of intermediate shell output.
 *
 * @param <T> Class of shell output
 */
open class ShellResult<T>(
    val output: T,
    private val listener: Listener?,
) : Future<T> {

    @Volatile private var canceled = false
    @Volatile private var done = false



    override fun cancel(b: Boolean): Boolean {
        synchronized (this) {
            this.canceled = b
            (this as? Object)?.notifyAll()
        }
        return true
    }

    override fun isCancelled(): Boolean {
        return canceled
    }

    override fun isDone(): Boolean {
        return done
    }

    override fun get(): T {
        synchronized(this) {
            waitFinishing()
        }
        return output
    }

    override fun get(l: Long, timeUnit: TimeUnit): T? {
        return null
    }

    @CallSuper
    open fun setupDone() {
        synchronized (this) {
            this.done = true
            (this as? Object)?.notifyAll()
        }
    }

    @CallSuper
    open fun add(result: String) {
        listener?.onOutput(result)
    }



    private fun waitFinishing() {
        while (!isDone && !isCancelled) {
            try {
                (this as? Object)?.wait()
            } catch (_: InterruptedException) {
                /* Do nothing */
            }
        }
    }





    /** Future shell result that represents result as list of strings */
    class List(listener: Listener?) : ShellResult<MutableList<String>>(ArrayList(), listener) {

        override fun add(result: String) {
            output.add(result)
            super.add(result)
        }
    }

    /** Future shell result that represents result as char buffer */
    class Buffer(output: CharBuffer) : ShellResult<CharBuffer>(output, null) {
        private var read: Int = 0

        override fun add(result: String) {
            if (isCancelled) {
                return
            } else if (read + result.length <= output.length) {
                output.put(result)
                read += result.length
            } else {
                val sizePart = output.length - read
                if (sizePart > 0) {
                    output.put(result.substring(0, output.length - read))
                    read += sizePart
                }
                cancel(true)
            }
            super.add(result)
        }

        override fun setupDone() {
            output.flip().limit(read)
            super.setupDone()
        }
    }



    /** Async listener for shell's output */
    fun interface Listener {

        /** A payload to be invoked on part of shell's output */
        fun onOutput(output: String)
    }
}
