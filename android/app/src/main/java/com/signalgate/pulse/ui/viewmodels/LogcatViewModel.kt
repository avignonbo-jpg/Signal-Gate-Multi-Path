package com.signalgate.pulse.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the in-app Logcat viewer.
 * Dev builds only — never expose in release.
 */
class LogcatViewModel : ViewModel() {

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    fun captureLogcat() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val process = Runtime.getRuntime().exec("logcat -d -v time SignalGate:*")
                    process.inputStream.bufferedReader().use { it.readLines() }.takeLast(500)
                } catch (e: Exception) {
                    listOf("Error capturing logs: ${e.message}")
                }
            }
            _logs.value = result
        }
    }
}
