package `in`.planckstudio.dethping

import android.os.AsyncTask
import android.widget.Toast
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

public class UDPSenderTask(
    private val ipAddress: String,
    private val port: Int,
    private val numPackets: Int,
    private val numTimes: Int,
    private val timeInSeconds: Int = 30
) : AsyncTask<Void, Void, Void>() {

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg params: Void?): Void? {
        val startTime = System.currentTimeMillis()
        val maxPayloadSize = 1472
        try {
            val address = InetAddress.getByName(ipAddress)
            val socket = DatagramSocket()

            for (i in 0 until numPackets) {
                if (isCancelled || hasReachedTimeLimit(startTime)) break
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
        //
    }

    override fun onCancelled() {
        //
    }

    private fun hasReachedTimeLimit(startTime: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = (currentTime - startTime) / 1000
        return elapsedTime >= timeInSeconds
    }

    public fun generateRandomMessage(maxSize: Int): String {
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