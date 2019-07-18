package tijos.framework.sensor.ec20.mqtt;

import java.io.IOException;

import tijos.framework.sensor.ec20.IEC20MqttEventListener;
import tijos.framework.sensor.ec20.TiEC20;
import tijos.framework.util.Delay;

public class MQTTClient implements IEC20MqttEventListener {

	private int client_idx = 0;

	/*
	 * client id
	 */
	private String _clientId;

	/*
	 * MQTT Server
	 */
	private String _server;

	private int _port;

	private TiEC20 _ec20;

	private int _msgId;

	private IMQTTEventListener _mqttEventListener = null;

	/**
	 * Initialize mqtt client with ec20 and server options
	 * 
	 * @param ec20
	 * @param server   mqtt server
	 * @param port     mqtt port
	 * @param clientId cilent id
	 */
	public MQTTClient(TiEC20 ec20, String server, int port, String clientId) {
		this._ec20 = ec20;

		this._server = server;
		this._port = port;
		this._clientId = clientId;
	}

	/*
	 * get current client id
	 */
	public String getClientId() {
		return _clientId;
	}

	/**
	 * Event listener for data arrived from remote node
	 *
	 * @param listener
	 */
	public void setEventListener(IMQTTEventListener listener) {
		_mqttEventListener = listener;
		this._ec20.setEventListener(this);
	}

	/**
	 * Returns a randomly generated client identifier based on the the fixed prefix
	 * (tijos) and the system time.
	 * <p>
	 * When cleanSession is set to false, an application must ensure it uses the
	 * same client identifier when it reconnects to the server to resume state and
	 * maintain assured message delivery.
	 * </p>
	 * 
	 * @return a generated client identifier
	 */
	public static String generateClientId() {
		return "tijos" + System.currentTimeMillis();
	}

	/**
	 * Connect to the server
	 * 
	 * @param options MQTT options
	 * @throws IOException If error occurs
	 */
	public void connect(MQTTConnectOptions options) throws IOException {

		if(_ec20.isMqttConnected())
			_ec20.mqttDisconnect(this.client_idx);

		String user = null;
		String password = null;

		if (options != null) {
			if (options.getLWTTopic() != null) {
				_ec20.mqttConfigWillTopic(this.client_idx, options.getLWTQos(), options.getLWTRetained() ? 1 : 0,
						options.getLWTTopic(), options.getLWTPayload());
			}

			_ec20.mqttConfigSessionType(this.client_idx, options.isCleanSession() ? 1 : 0);

			user = options.getUserName();
			password = options.getPassword();
		}

		try {
			_ec20.mqttConnect(this.client_idx, this._server, this._port, this._clientId, user, password);
		} catch (IOException ex) {
			ex.printStackTrace();
			_ec20.mqttDisconnect(this.client_idx);
			Delay.msDelay(2000);
			_ec20.mqttConnect(this.client_idx, this._server, this._port, this._clientId, user, password);
		}

	}

	/**
	 * Disconnect the server
	 * 
	 * @throws MqttException If a MQTT error occurs
	 */
	public void disconnect() throws IOException {

		_ec20.mqttDisconnect(this.client_idx);
	}

	/**
	 * subscribe a topic from the server
	 * 
	 * @param qos   the qos to subscribe
	 * @param topic the topic name to subscribe
	 * @return the message id for the current subscribe, it is used in the
	 *         subscribeCompleted event
	 * @throws IOException If error occurs
	 */
	public int subscribe(int qos, String... topics) throws IOException {

		this._msgId++;
		_ec20.mqttSubscribe(this.client_idx, this._msgId, qos, topics);

		return this._msgId;
	}

	/**
	 * publish topic to the server
	 * 
	 * @param topic    the topic name
	 * @param message  the message to publish
	 * @param qos      the qos
	 * @param retained if retained in server
	 * @return the message id for the current publish
	 * @throws IOException error occurs
	 */
	public int publish(String topic, String message, int qos, boolean retained) throws IOException {
		this._msgId++;

		_ec20.mqttPublish(this.client_idx, this._msgId, qos, retained ? 1 : 0, topic, message);

		return this._msgId;
	}

	@Override
	public void onMQTTPublishDataArrived(int client_idx, int msgId, String topic, String message) {

		if (this._mqttEventListener != null) {
			this._mqttEventListener.onPublishDataArrived(msgId, topic, message);
		}
	}

	@Override
	public void onMQTTLinkLost(int client_idx, int error) {

		if (this._mqttEventListener != null) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					_mqttEventListener.onLinkLost(error);
				}
			}).start();
		}
	}

}
