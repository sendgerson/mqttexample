package br.com.sendgerson.mqttpoc;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "mqttpoc";
    private static final String SERVER_URI = "tcp://broker.hivemq.com:1883";
    private static final String CLIENT_ID = "sendgersontest";
    private static final String TOPIC_PUBLISH = "sendgersontopic2";
    private static final String TOPIC_SUBSCRIBE = "sendgersontopic1";
    private static final int QOS = 1;

    private MqttAndroidClient client;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createClient();
        findViewById(R.id.publish_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        publish();
                    }
                }
        );
    }

    private void createClient() {

        try {
            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setAutomaticReconnect(true);
            mqttConnectOptions.setCleanSession(false);

            client = new MqttAndroidClient(this.getApplicationContext(), SERVER_URI, CLIENT_ID);
            IMqttToken token = client.connect(mqttConnectOptions);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // Connect successfully
                    client.setBufferOpts(getDisconnectedBufferOptions());
                    Log.d(TAG, "onSuccess");
                    buildMqttCallback();
                    subscribeTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Error to connect. Something like connection timeout or firewall problems
                    Log.d(TAG, "onFailure");
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, "MqttException on connect", e);
        }
    }

    @NonNull
    private MqttConnectOptions getMqttConnectionOption() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setAutomaticReconnect(true);
        //mqttConnectOptions.setWill(Constants.PUBLISH_TOPIC, "I am going offline".getBytes(), 1, true);
        //mqttConnectOptions.setUserName("username");
        //mqttConnectOptions.setPassword("password".toCharArray());
        return mqttConnectOptions;
    }
    @NonNull
    private DisconnectedBufferOptions getDisconnectedBufferOptions() {
        // Offline opts
        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
        disconnectedBufferOptions.setBufferEnabled(true);
        disconnectedBufferOptions.setBufferSize(100);
        disconnectedBufferOptions.setPersistBuffer(false);
        disconnectedBufferOptions.setDeleteOldestMessages(false);
        return disconnectedBufferOptions;
    }

    private void publish() {
        try {
            String payload = "test publish message";
            byte[] encodedPayload = payload.getBytes(StandardCharsets.UTF_8);
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(TOPIC_PUBLISH, message);
        } catch (MqttException e) {
            Log.d(TAG, "Error to publish message", e);
        }
    }

    private void subscribeTopic() {
        try {
            IMqttToken subToken = client.subscribe(TOPIC_SUBSCRIBE, QOS);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // Topic subscribed with success
                    Log.d(TAG, "subscribe with success");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Fail to subscribe topic
                    Log.d(TAG, "Subscribe with error");
                }
            });
        } catch (MqttException e) {
            Log.d(TAG, "Exception to subscribe topic", e);
        }
    }

    private void buildMqttCallback() {
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    Log.d(TAG, "reconnected");
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeTopic();
                } else {
                    Log.d(TAG, "connected to");
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "Lost connection", cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d(TAG, "A message arrives from the server");
                showMessageToast("Received message: " + message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG, "Delivery complete");
            }
        });
    }

    private void showMessageToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

}
