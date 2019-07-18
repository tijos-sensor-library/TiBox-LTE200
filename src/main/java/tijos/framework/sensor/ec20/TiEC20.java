package tijos.framework.sensor.ec20;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

import tijos.framework.devicecenter.TiUART;
import tijos.framework.sensor.ec20.io.ATResponse;
import tijos.framework.sensor.ec20.io.TiUartInputStream;
import tijos.framework.sensor.ec20.io.TiUartOutputStream;
import tijos.framework.sensor.ec20.mqtt.MQTTClient;
import tijos.framework.util.Delay;
import tijos.framework.util.logging.*;

/**
 * Quectel EC20 4G module driver for TiJOS
 */
public class TiEC20 extends Thread {

	// IO stream for UART
	InputStream input;
	OutputStream output;

	TiUART uart;

	// Keep the UART read thread running
	private boolean keeprunning = true;

	private ATResponse atResp = new ATResponse();

	private IEC20MqttEventListener eventListener;

	/**
	 * Initialize IO stream for UART
	 *
	 * @param uart TiUART object
	 */
	public TiEC20(TiUART uart) {
		this.uart = uart;
		this.input = new BufferedInputStream(new TiUartInputStream(uart), 256);
		this.output = new TiUartOutputStream(uart);

		this.setDaemon(true);
		this.start();
	}

	@Override
	public void run() {
		while (keeprunning) {
			try {
				String resp = readLine();
				if (resp.length() == 0) {
					continue;
				}

				logMsg(resp);

				if (resp.equals("ERROR") || resp.startsWith("+CME ERROR:")) {
					this.atResp.setResponse(resp);
					synchronized (this.atResp) {
						this.atResp.notifyAll();
					}
					continue;
				}

				if (atResp.getKeywords().length() > 0) {
					if (resp.contains(atResp.getKeywords())) {
						this.atResp.setResponse(resp);
						synchronized (this.atResp) {
							this.atResp.notifyAll();
						}
						continue;
					}
				} else {
					if (resp.equals("OK")) {
						synchronized (this.atResp) {
							this.atResp.notifyAll();
						}
						continue;
					}
				}

				if (resp.contains("+QMTSTAT:")) // there is a change in the state of MQTT link layer
				{
					this.mqttLinkChanged(resp);

				} else if (resp.contains("+QMTRECV:")) // Notify the Host to Read MQTT Packet Data
				{
					this.mqttDataArrived(resp);
				} else {
					this.atResp.setResponse(resp);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * Event listener for data arrived from remote node
	 *
	 * @param listener
	 */
	public void setEventListener(IEC20MqttEventListener listener) {
		this.eventListener = listener;
	}

	/**
	 * 检查模块是否连接
	 * 
	 * @return
	 * @throws IOException
	 */
	public boolean isReady() throws IOException {
		String resp = sendCommand("AT");
		if(resp == null)
			return false;
		
		if(resp.equals("AT") || resp.length() == 0)
			return true;

		return false;
	}
	
	/**
   * 打开模块射频功能
   *
   * @throws IOException
   */
  public void turnOnMT() throws IOException {
    String resp = sendCommand2("AT+CFUN=1", "+CFUN:");

  }

	/**
	 * 查询模块射频功能状态
	 *
	 * @return true 射频已打开 false 射频未打开
	 * @throws IOException
	 */
	public boolean isMTOn() throws IOException {

		String resp = sendCommand("AT+CFUN?");
		if (resp.equals("+CFUN: 1"))
			return true;

		return false;
	}

	public void echoOff() throws IOException {
		String resp = sendCommand("ATE0");
	}

	/**
	 * 关闭模块射频功能
	 *
	 * @throws IOException
	 */
	public void turnOffMT() throws IOException {
		String resp = sendCommand("AT+CFUN=0");
		if(!resp.isEmpty())
			throw new IOException(resp);

	}

	/**
	 * Request TA Model Identification 
	 * @return
	 * @throws IOException
	 */
	public String getModel() throws IOException {
		
		String resp = sendCommand("AT+GMM");
		return resp;
	}
	
	/**
	 * 查询 IMSI 码 IMSI 是国际移动用户识别码，International Mobile Subscriber Identification
	 * Number 的缩写
	 *
	 * @return IMSI
	 * @throws IOException
	 */
	public String getIMSI() throws IOException {
		String resp = sendCommand("AT+CIMI");
		return resp;
	}

	/**
	 * 查询模块 IMEI, 国际移动设备识别码International Mobile Equipment Identity
	 *
	 * @return
	 * @throws IOException
	 */
	public String getIMEI() throws IOException {
		String resp = sendCommand("AT+GSN");
		return resp;
	}

	/**
	 * 获取SIM卡CCID
	 * 
	 * @return
	 * @throws IOException
	 */
	public String getICCID() throws IOException {
		String resp = sendCommand("AT+QCCID");
		return resp;
	}

	/**
	 * 查询模块信号
	 *
	 * @return 0 - 表示网络未知，或者网络未附着
	 * @throws IOException
	 */
	public int getRSSI() throws IOException {
		String resp = sendCommand("AT+CSQ");

		int begin = resp.indexOf(':');
		int end = resp.lastIndexOf(',');

		if (begin < 0 || end < 0 || begin >= end)
			throw new IOException("Wrong response");

		String rssi = resp.substring(begin + 2, end);

		int r = Integer.parseInt(rssi);
		if (r == 99) {// no signl
			r = 0;
		}

		return r;
	}

	/**
	 * 注网
	 *
	 * @throws IOException
	 */
	public void attachNetwork() throws IOException {
		String resp = sendCommand("AT+CGATT=1");
		if(!resp.isEmpty())
			throw new IOException(resp);

	}

	/**
	 * 查询是否已注网
	 *
	 * @return
	 * @throws IOException
	 */
	public boolean isNetworkAttached() throws IOException {
		String resp = sendCommand("AT+CGATT?");
		if (resp.equals("+CGATT: 1"))
			return true;
		return false;

	}

	/**
	 * Active PDP context
	 * 
	 * @throws IOException
	 */
	public void activePDPContext() throws IOException {
		String resp = sendCommand("AT+CGACT=1,1");
		if(!resp.isEmpty())
			throw new IOException(resp);

	}

	/**
	 * SIM卡检测
	 * 
	 * @return
	 * @throws IOException
	 */
	public boolean checkSIMCard() throws IOException {
		String resp = sendCommand2("AT+CPIN?" , "+CPIN");

		if (resp.equals("+CPIN: READY"))
			return true;

		return false;
	}

	/**
	 * 查询网络是否注册
	 *
	 * @return
	 * @throws IOException
	 */
	public boolean isNetworkRegistred() throws IOException {
		sendCommand("AT+CEREG=2"); // 允许返回CI等信息

		String resp = sendCommand("AT+CEREG?");

		int begin = resp.indexOf(':');
		if (begin < 0)
			throw new IOException("Wrong response");

		resp = resp.substring(begin + 2);
		String[] result = resp.split(",");

		int s = Integer.parseInt(result[1]);
		return s > 0 ? true : false;
	}

	/**
	 * 获取设备IP地址
	 *
	 * @return
	 * @throws IOException
	 */
	public String getIPAddress() throws IOException {
		String resp = sendCommand("AT+CGPADDR=1");

		if (!resp.startsWith("+CGPADDR"))
			throw new IOException("Failed to get IP address");

		int pos = resp.lastIndexOf(',');
		if (pos < 0)
			return "";

		return resp.substring(pos + 1);
	}

	/**
	 * Get date time from the network
	 * 
	 * @return
	 * @throws IOException
	 */
	public Calendar getDateTime() throws IOException {
		String data = sendCommand("AT+CCLK?");

		if (data.length() < 10)
			return null;

		int begin = data.indexOf(':');

		int yearPos = data.indexOf('/', begin + 1);
		int year = Integer.parseInt(data.substring(begin + 3, yearPos));

		int monPos = data.indexOf('/', yearPos + 1);
		int month = Integer.parseInt(data.substring(yearPos + 1, monPos));

		int dayPos = data.indexOf(',', yearPos + 1);
		int day = Integer.parseInt(data.substring(monPos + 1, dayPos));

		int hourPos = data.indexOf(':', dayPos + 1);
		int hours = Integer.parseInt(data.substring(dayPos + 1, hourPos));

		int minPos = data.indexOf(':', hourPos + 1);
		int minutes = Integer.parseInt(data.substring(hourPos + 1, minPos));

		int secondPos = data.indexOf('+', minPos + 1);
		int seconds = Integer.parseInt(data.substring(minPos + 1, secondPos));

		Calendar cal = Calendar.getInstance();
		cal.set(year + 100, month - 1, day, hours, minutes, seconds);

		return cal;

	}

	/**
	 * Create mqtt client instance
	 * 
	 * @param server
	 * @param port
	 * @param clientId
	 * @return
	 */
	public MQTTClient getMqttClient(String server, int port, String clientId) {

		MQTTClient client = new MQTTClient(this, server, port, clientId);

		return client;

	}

	/**
	 * Will topic
	 * 
	 * @param id
	 * @param willQos
	 * @param willRetain
	 * @param willTopic
	 * @param willMsg
	 * @throws IOException
	 */
	public void mqttConfigWillTopic(int id, int willQos, int willRetain, String willTopic, String willMsg)
			throws IOException {
		String cmd = "AT+QMTCFG=\"will\"," + id + ",0," + willQos + "," + willRetain + "," + "\"" + willTopic + "\",\""
				+ willMsg + "\"";
		String resp = sendCommand(cmd);
		if(!resp.isEmpty())
			throw new IOException(resp);

	}

	/**
	 * Session type
	 * 
	 * @param id
	 * @param cleanSession
	 * @throws IOException
	 */
	public void mqttConfigSessionType(int id, int cleanSession) throws IOException {
		String cmd = "AT+QMTCFG=\"session\"," + id + "," + cleanSession;
		String resp = sendCommand(cmd);
		if(!resp.isEmpty())
			throw new IOException(resp);

	}

	/**
	 * 
	 * @param id
	 * @param productKey
	 * @param deviceName
	 * @param deviceSecret
	 * @throws IOException
	 */
	public void mqttConfigAliAuth(int id, String productKey, String deviceName, String deviceSecret)
			throws IOException {
		String cmd = "AT+QMTCFG=\"aliauth\"," + id + "," + "\"" + productKey + "\",\"" + deviceName + "\",\""
				+ deviceSecret + "\"";
		String resp = sendCommand(cmd);
		if(!resp.isEmpty())
			throw new IOException(resp);

	}

	/**
	 * Connect to MQTT Server
	 * 
	 * @param id       client id (0-5)
	 * @param host     mqtt server
	 * @param port     mqtt port
	 * @param clientId client id
	 * @param userName
	 * @param password
	 * @throws IOException
	 */
	public void mqttConnect(int id, String host, int port, String clientId, String userName, String password)
			throws IOException {

		String cmd = "AT+QMTCFG=\"recv/mode\"," + id + ",0,1";
		sendCommand(cmd);

		cmd = "AT+QMTOPEN=" + id + ",\"" + host + "\"," + port;
		String resp = sendCommand2(cmd, "+QMTOPEN");
		if (resp.contains("+QMTOPEN")) {
			int pos = resp.lastIndexOf(',');
			String result = resp.substring(pos + 1);
			int ret = Integer.parseInt(result);
			if (ret != 0) {
				throw new IOException("Failed to open connection, err " + ret);
			}
		} else {
			throw new IOException(resp);
		}

		cmd = "AT+QMTCONN=" + id + ",\"" + clientId + "\"";
		if (userName != null && password != null) {
			cmd += ",";
			cmd += "\"" + userName + "\",";
			cmd += "\"" + password + "\"";
		}
		resp = sendCommand2(cmd, "+QMTCONN");

		int pos = resp.lastIndexOf(',');
		String result = resp.substring(pos + 1);
		int ret = Integer.parseInt(result);
		if (ret != 0) {
			throw new IOException("Failed to connnect server, err " + ret);
		}

	}

	/**
	 * Disconnect from server
	 * 
	 * @param id
	 * @throws IOException
	 */
	public void mqttDisconnect(int id) throws IOException {
		String cmd = "AT+QMTDISC=" + id;
		sendCommand2(cmd, "+QMTDISC");

		cmd = "AT+QMTCLOSE=" + id;
		this.sendCommand2(cmd, "+QMTCLOSE");
	}
	
	/**
	 * Is mqtt server connected or not
	 * @return
	 * @throws IOException
	 */
	public boolean isMqttConnected() throws IOException{
		
		String resp = sendCommand("AT+QMTCONN?");
		if(resp != null && resp.length() > 5)
			return true;
		
		return false;
		
	}

	/**
	 * publish topic message
	 * 
	 * @param id
	 * @param msgId
	 * @param qos
	 * @param retain
	 * @param topic
	 * @param message
	 * @throws IOException
	 */
	public void mqttPublish(int id, int msgId, int qos, int retain, String topic, String message) throws IOException {

		String cmd = "AT+QMTPUBEX=" + id + "," + msgId + "," + qos + "," + retain + ",\"" + topic + "\","
				+ message.length();
		String resp = sendCommand2(cmd, ">");
		if (resp.contains("ERROR"))
			throw new IOException(resp);

		resp = sendCommand2(message, "+QMTPUBEX:");

		int ret = 0;
		while (resp != null) {
			int begin = resp.indexOf(':');
			if (begin < 0)
				throw new IOException("Wrong response");

			resp = resp.substring(begin + 2);
			String[] result = resp.split(",");
			ret = Integer.parseInt(result[2]);
			if (ret != 1)
				break;

			resp = this.waitResponse("+QMTPUBEX:", 5000);
		}

		if (ret == 2)
			throw new IOException("Failed to send packet");

	}

	/**
	 * subscribe topic
	 * 
	 * @param id
	 * @param msgId
	 * @param qos
	 * @param topics
	 * @throws IOException
	 */
	public void mqttSubscribe(int id, int msgId, int qos, String... topics) throws IOException {
		String cmd = "AT+QMTSUB=" + id + "," + msgId;
		for (String topic : topics) {
			cmd += ",";
			cmd += "\"" + topic + "\"," + qos;
		}

		String resp = this.sendCommand2(cmd, "+QMTSUB:");
		if (resp.contains("+CME ERROR"))
			throw new IOException(resp);

		int begin = resp.indexOf(':');
		if (begin < 0)
			throw new IOException("Wrong response");

		resp = resp.substring(begin + 2);
		String[] result = resp.split(",");

		int ret = Integer.parseInt(result[2]);

		if (ret != 0)
			throw new IOException("Subscription error " + ret);

	}

	/**
	 * unsubscribe msg
	 * 
	 * @param id
	 * @param msgId
	 * @param topics
	 * @throws IOException
	 */
	public void mqttUnsubscribe(int id, int msgId, String[] topics) throws IOException {
		String cmd = "AT+QMTUNS=" + id + "," + msgId;
		for (String topic : topics) {
			cmd += ",";
			cmd += "\"" + topic + "\"";
		}

		String resp = this.sendCommand2(cmd, "+QMTUNS");
		if (resp.contains("+CME ERROR"))
			throw new IOException(resp);

		int begin = resp.indexOf(':');
		if (begin < 0)
			throw new IOException("Wrong response");

		resp = resp.substring(begin + 2);
		String[] result = resp.split(",");

		int ret = Integer.parseInt(result[2]);

		if (ret != 0)
			throw new IOException("Unsubscription error " + ret);
	}

	/**
	 * Get Last error
	 * 
	 * @return
	 * @throws IOException
	 */
	public String getLastError() throws IOException {
		String error = sendCommand("AT+QIGETERROR");

		return error;
	}

	/**
	 * 启动模组 检查模组状态、检查SIM卡、打开射频、注网
	 * 
	 * @throws IOException
	 */
	public void startup() throws IOException {

		int loop = 10;
		while(!this.isReady() && loop -- > 0) {
			Delay.msDelay(1000);
		};
		
		if(!this.isReady()) 
			throw new IOException("EC20 is not ready.");

		this.echoOff();

		if(!this.checkSIMCard())
			throw new IOException("SIM Card Error");

		this.turnOnMT();

		// 查询模块射频功能状态
		loop = 10;
		while (!isMTOn() && loop-- > 0) {
			turnOnMT();
			Delay.msDelay(2000);
		}

		this.attachNetwork();
		loop = 10;
		while (!this.isNetworkAttached() && loop-- > 0) {
			this.attachNetwork();
			Delay.msDelay(2000);
		}

		this.activePDPContext();
	}

	/**
	 * Enable/Disable GNSS to Run Automatically
	 * 
	 * @param enable
	 * @throws IOException
	 */
	public void autoGPS(boolean enable) throws IOException {
		String cmd = "+QGPSCFG:\"autogps\"," + (enable ? 1 : 0);
		String resp = this.sendCommand(cmd);
		if (resp.contains("+CME ERROR"))
			throw new IOException(resp);

	}

	/**
	 * Turn ON GPS
	 * 
	 * @throws IOException
	 */
	public void tunOnGPS() throws IOException {
		String resp = this.sendCommand("AT+QGPS=1");
		if (resp.contains("+CME ERROR")) {
			if (!resp.equals("+CME ERROR: 504"))
				throw new IOException(resp);
		}
	}

	/**
	 * Turn OFF GPS
	 * 
	 * @throws IOException
	 */
	public void tunOffGPS() throws IOException {
		String resp = this.sendCommand("AT+QGPS=0");
		if (resp.contains("+CME ERROR"))
			throw new IOException(resp);
	}

	/**
	 * Get GPS location
	 * 
	 * @return gps location, null means location is not ready 
	 * @throws IOException
	 */
	public GPSPosition getGPSPosition() throws IOException {
		String resp = this.sendCommand("AT+QGPSLOC=1");
		// not ready
		if (resp.contains("+CME ERROR")) {
			return null;
		}

		GPSPosition gps = new GPSPosition();
		gps.parse(resp);

		return gps;
	}

//	public void sendSMS(String target, String message) throws IOException {
//		
//		this.sendCommand("AT+CMGF=1");
//		this.sendCommand("AT+CSCS=\"GSM\"");
//		
//		this.sendCommand2("AT+CMGS=\"" + target +"\"", ">");
//		this.sendCommand2(message, ">");
//		this.output.write(0x1A);
//		
//	}
	
	
	/**
	 * Send AT command to device
	 *
	 * @param cmd
	 * @throws IOException
	 */
	private String sendCommand(String cmd) throws IOException {

		logMsg(cmd);

		synchronized (this.atResp) {
			try {
				this.atResp.reset();
				output.write((cmd + "\r\n").getBytes());

				this.atResp.wait(5000);
				//this.atResp.wait();

				return atResp.getResponse();

			} catch (Exception ie) {

			}
		}
		return null;
	}

	private String sendCommand2(String cmd, String expKeyWords) throws IOException {

		logMsg(cmd);
		synchronized (this.atResp) {
			try {
				this.atResp.reset();
				this.atResp.setExpectedRsp(expKeyWords);

				output.write((cmd + "\r\n").getBytes());

				this.atResp.wait(5000);
				//this.atResp.wait();

				return atResp.getResponse();

			} catch (Exception ie) {

			}
		}

		return null;
	}

	private String waitResponse(String expKeyWords, int timeOut) throws IOException {
		synchronized (this.atResp) {
			try {
				this.atResp.reset();
				this.atResp.setExpectedRsp(expKeyWords);

				this.atResp.wait(timeOut);

				return atResp.getResponse();

			} catch (Exception ie) {

			}
		}

		return null;
	}

	private String readLine() throws IOException {
		StringBuilder sb = new StringBuilder(32);

		int timeout = 4000;
		while ((timeout -= 50) > 0) {
			if (input.available() < 2) {
				Delay.msDelay(50);
				continue;
			}

			int val = input.read();
			if (val == 0x0D) {
				val = input.read(); // 0x0a
				break;
			}

			// only for character
			if (val >= 0x20)
				sb.append((char) val);
		}
		return sb.toString();
	}

	private void mqttLinkChanged(String resp) throws IOException {
		System.out.println("mqttLinkChanged " + resp);

		int begin = resp.indexOf(':');
		if (begin < 0)
			throw new IOException("Wrong response");

		resp = resp.substring(begin + 2);
		String[] result = resp.split(",");

		int id = Integer.parseInt(result[0]);
		int error = Integer.parseInt(result[1]);

		if (this.eventListener != null) {
			this.eventListener.onMQTTLinkLost(id, error);
		}
	}

	private void mqttDataArrived(String resp) throws IOException {
		System.out.println("mqttDataArrived " + resp);

		int begin = resp.indexOf(':');
		if (begin < 0)
			throw new IOException("Wrong response");

		resp = resp.substring(begin + 2);
		String[] result = resp.split(",");

		if (result.length == 5) {

			int id = Integer.parseInt(result[0]);
			int msgId = Integer.parseInt(result[1]);
			String topic = result[2];

			String payload = result[4];

			if (this.eventListener != null) {
				this.eventListener.onMQTTPublishDataArrived(id, msgId, topic, payload);
			}

		} else {
			String cmd = "AT+QMTRECV=" + result[0] + result[1];
			output.write((cmd + "\r\n").getBytes());
		}

	}
	
	private void logMsg(String msg)
	{
		//Logger.info("EC20", msg);
		System.out.println(msg);
	}

}
