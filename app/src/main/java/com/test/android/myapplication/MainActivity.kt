package com.test.android.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import com.test.android.myapplication.root.TestOOM
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val countLayout = findViewById<TextInputLayout>(R.id.countLayout)
        val countInput = findViewById<TextInputEditText>(R.id.countInput)
        val commandLayout = findViewById<TextInputLayout>(R.id.commandLayout)
        val commandInput = findViewById<TextInputEditText>(R.id.commandInput)
        val executeButton = findViewById<MaterialButton>(R.id.executeButton)
        commandInput.setText("su -v")
        commandInput.addTextChangedListener { commandLayout.error = null }
        countInput.addTextChangedListener { countLayout.error = null }

        executeButton.setOnClickListener { executeRootCommand(countLayout, countInput, commandLayout, commandInput, executeButton) }
    }

    private fun executeRootCommand(
        countLayout: TextInputLayout,
        countInput: TextInputEditText,
        commandLayout: TextInputLayout,
        commandInput: TextInputEditText,
        executeButton: MaterialButton,
    ) {
        val count = countInput.text
            ?.toString()
            ?.trim()
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
            ?: return Unit.also { countLayout.error = "Add count, please" }

        countLayout.error = null

        val commandText = commandInput.text
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return Unit.also { commandLayout.error = "Add command, please" }

        commandLayout.error = null

        executeButton.isEnabled = false

        thread {
            try {
                val results = StringBuilder()
                repeat(count) { index ->
                    val index = index + 1
                    Log.e("RootTest", "Root test with command: $commandText, try: $index/$count")
                    val output = TestOOM.execAndGet(commandText)
                    results.append("[$index] ${output.joinToString("\n")}\n")
                }

                runOnUiThread {
                    executeButton.isEnabled = true
                    Snackbar.make(findViewById(R.id.main), "Finish", Snackbar.LENGTH_SHORT).show()
                }
            } catch (th: Throwable) {
                runOnUiThread {
                    executeButton.isEnabled = true
                    Snackbar.make(findViewById(R.id.main), "Error: ${th.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}