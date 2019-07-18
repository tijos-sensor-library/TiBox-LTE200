
package tijos.framework.sensor.ec20.mqtt;

/**
 * Holds the set of options that control how the client connects to a server.
 */
public class MQTTConnectOptions {
	/**
	 * Mqtt Version 3.1.1
	 */
	public static final int MQTT_VERSION_3_1_1 = 4;
	/**
	 * the user name to connect the server
	 */
	private String userName;

	/**
	 * the password to connect the server
	 */
	private String password;

	/*
	 * if clean session or not when connec to the server
	 */
	private boolean cleanSession = true;

	/**
	 * the topic for LWT
	 */
	private String lwt_topic;
	private String lwt_payload;
	private int lwt_qos;
	private boolean lwt_retained;

	/**
	 * Constructs a new <code>MqttConnectOptions</code> object using the default
	 * values.
	 *
	 * The defaults are:
	 * <ul>
	 * <li>The keepalive interval is 60 seconds</li>
	 * <li>Clean Session is true</li>
	 * <li>The message delivery retry interval is 15 seconds</li>
	 * <li>The connection timeout period is 30 seconds</li>
	 * <li>No Will message is set</li>
	 * <li>A standard SocketFactory is used</li>
	 * </ul>
	 * More information about these values can be found in the setter methods.
	 */
	public MQTTConnectOptions() {
	}

	/**
	 * Returns the password to use for the connection.
	 * 
	 * @return the password to use for the connection.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Sets the password to use for the connection.
	 * 
	 * @param password A Char Array of the password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Returns the user name to use for the connection.
	 * 
	 * @return the user name to use for the connection.
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * Sets the user name to use for the connection.
	 * 
	 * @param userName The Username as a String
	 * @throws IllegalArgumentException if the user name is blank or only contains
	 *                                  whitespace characters.
	 */
	public void setUserName(String userName) {
		if ((userName != null) && (userName.trim().equals(""))) {
			throw new IllegalArgumentException();
		}
		this.userName = userName;
	}

	/**
	 * Sets the "Last Will and Testament" (LWT) for the connection. In the event
	 * that this client unexpectedly loses its connection to the server, the server
	 * will publish a message to itself using the supplied details.
	 *
	 * @param topic    the topic to publish to.
	 * @param payload  the string payload for the message.
	 * @param qos      the quality of service to publish the message at (0, 1 or 2).
	 * @param retained whether or not the message should be retained.
	 */
	public void setWill(String topic, String payload, int qos, boolean retained) {

		if ((topic == null) || (payload == null)) {
			throw new IllegalArgumentException("topic or payload is null.");
		}

		this.lwt_topic = topic;
		this.lwt_payload = payload;
		this.lwt_qos = qos;
		this.lwt_retained = retained;
	}

	public String getLWTTopic() {

		return this.lwt_topic;
	}

	public int getLWTQos() {
		return this.lwt_qos;
	}

	public boolean getLWTRetained() {
		return this.lwt_retained;
	}

	public String getLWTPayload() {
		return this.lwt_payload;
	}

	/**
	 * Returns whether the client and server should remember state for the client
	 * across reconnects.
	 * 
	 * @return the clean session flag
	 */
	public boolean isCleanSession() {
		return this.cleanSession;
	}

	/**
	 * Sets whether the client and server should remember state across restarts and
	 * reconnects.
	 * <ul>
	 * <li>If set to false both the client and server will maintain state across
	 * restarts of the client, the server and the connection. As state is
	 * maintained:
	 * <ul>
	 * <li>Message delivery will be reliable meeting the specified QOS even if the
	 * client, server or connection are restarted.
	 * <li>The server will treat a subscription as durable.
	 * </ul>
	 * <li>If set to true the client and server will not maintain state across
	 * restarts of the client, the server or the connection. This means
	 * <ul>
	 * <li>Message delivery to the specified QOS cannot be maintained if the client,
	 * server or connection are restarted
	 * <li>The server will treat a subscription as non-durable
	 * </ul>
	 * </ul>
	 * 
	 * @param cleanSession Set to True to enable cleanSession
	 */
	public void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}
}
