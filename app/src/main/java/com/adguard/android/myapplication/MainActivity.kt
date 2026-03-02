package com.adguard.android.myapplication

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.adguard.android.myapplication.root.Scp343
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var countLayout: TextInputLayout
    private lateinit var countInput: TextInputEditText
    private lateinit var commandLayout: TextInputLayout
    private lateinit var commandInput: TextInputEditText
    private lateinit var executeButton: MaterialButton
    private lateinit var outputText: TextView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupListeners()
    }



    private fun initViews() {
        countLayout = findViewById(R.id.countLayout)
        countInput = findViewById(R.id.countInput)
        commandLayout = findViewById(R.id.commandLayout)
        commandInput = findViewById(R.id.commandInput)
        executeButton = findViewById(R.id.executeButton)
        outputText = findViewById(R.id.outputText)
        outputText.movementMethod = ScrollingMovementMethod()
    }

    private fun setupListeners() {
        executeButton.setOnClickListener {
            executeRootCommand()
        }
    }

    private fun executeRootCommand() {
        val countText = countInput.text?.toString()?.trim()
        val commandText = commandInput.text?.toString()?.trim()

        val count = countText?.toIntOrNull()?.takeIf { it > 0 }
        if (count == null) {
            countLayout.error = "Алло, количество пожалуйста"
            return
        }
        countLayout.error = null

        if (commandText.isNullOrBlank()) {
            commandLayout.error = "Алло, команду пожалуйста"
            return
        }
        commandLayout.error = null

        executeButton.isEnabled = false
        outputText.text = "Выполняем...\n"

        thread {
            try {
                val results = StringBuilder()
                repeat(count) { index ->
                    Log.e("RootTest", "Root test with command: $commandText, try: $index/$count")
                    val output = Scp343.execAndGet(commandText)
                    results.append("[$index] ${output.joinToString("\n")}\n")

                    runOnUiThread {
                        outputText.text = results.toString()
                    }
                }

                runOnUiThread {
                    executeButton.isEnabled = true
                    Snackbar.make(outputText, "Закончили упражнение", Snackbar.LENGTH_SHORT).show()
                }
            } catch (th: Throwable) {
                runOnUiThread {
                    executeButton.isEnabled = true
                    outputText.append("\nОшибка: ${th.message}")
                    Snackbar.make(outputText, "Ошибка какая-то: ${th.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}