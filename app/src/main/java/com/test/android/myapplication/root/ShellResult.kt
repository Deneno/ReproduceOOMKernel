package com.test.android.myapplication.root

import androidx.annotation.CallSuper
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



    /** Async listener for shell's output */
    fun interface Listener {

        /** A payload to be invoked on part of shell's output */
        fun onOutput(output: String)
    }
}
