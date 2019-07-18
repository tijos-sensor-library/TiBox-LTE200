package tijos.framework.tibox.lte200;

import java.io.IOException;

import tijos.framework.component.serialport.TiSerialPort;
import tijos.framework.devicecenter.TiGPIO;
import tijos.framework.devicecenter.TiUART;
import tijos.framework.platform.peripheral.TiLight;
import tijos.framework.sensor.ec20.TiEC20;
import tijos.framework.sensor.ec20.mqtt.MQTTClient;
import tijos.framework.util.Delay;

/**
 * TiLte200 driver based on TiJOS
 */
public class TiLTE200 {

	TiGPIO gpioLED;
	TiGPIO gpioEC20;

	TiEC20 ec20;
	TiSerialPort rs485;

	private static TiLTE200 lte200Instance;
	
	private TiLTE200() {

	}

	/**
	 * Get the single instance
	 * @return
	 * @throws IOException
	 */
	public static TiLTE200 getInstance() throws IOException {
		if( lte200Instance == null) {
			lte200Instance = new TiLTE200();
			lte200Instance.initialize();
		}
		
		return lte200Instance;
	}
	
	/**
	 * Initialize
	 * 
	 * @throws IOException
	 */
	private void initialize() throws IOException {
		gpioLED = TiGPIO.open(0, 5, 6);
		// D3
		gpioLED.setWorkMode(5, TiGPIO.OUTPUT_PP);
		// D4
		gpioLED.setWorkMode(6, TiGPIO.OUTPUT_PP);

		gpioEC20 = TiGPIO.open(2, 7, 8);
		gpioEC20.setWorkMode(7, TiGPIO.OUTPUT_PP);
		gpioEC20.setWorkMode(8, TiGPIO.OUTPUT_PP);

		TiUART uart = TiUART.open(2);
		uart.setWorkParameters(8, 1, TiUART.PARITY_NONE, 115200);

		ec20 = new TiEC20(uart);
	}

	/**
	 * Turn on system LED
	 * 
	 * @throws IOException
	 */
	public void turnOnSysLED() throws IOException {
		TiLight.getInstance().turnOn(0);
	}

	/**
	 * Turn off system LED
	 * 
	 * @throws IOException
	 */
	public void turnOffSysLED() throws IOException {
		TiLight.getInstance().turnOff(0);

	}

	/**
	 * Turn on user LED
	 * 
	 * @param id 0 or 1
	 * @throws IOException
	 */
	public void turnOnUserLED(int id) throws IOException {
		if (id != 0 && id != 0)
			throw new IOException("Invalid id");

		gpioLED.writePin(5 + id, 0);
	}

	/**
	 * Turn off user LED
	 * 
	 * @param id
	 * @throws IOException
	 */
	public void turnOffUserLED(int id) throws IOException {
		if (id != 0 && id != 0)
			throw new IOException("Invalid id");

		gpioLED.writePin(5 + id, 1);
	}

	/**
	 * Wakeup LTE module
	 * 
	 * @throws IOException
	 */
	public void networkWakeUp() throws IOException {
		gpioEC20.writePin(7, 1);
		Delay.msDelay(1000);
		gpioEC20.writePin(7, 0);
		Delay.msDelay(15000);
	}

	/**
	 * Reset LTE module
	 * 
	 * @throws IOException
	 */
	public void networkReset() throws IOException {
		gpioEC20.writePin(8, 1);
		Delay.msDelay(1000);
		gpioEC20.writePin(8, 0);
		Delay.msDelay(15000);
	}

	/**
	 * Startup LTE network
	 * 
	 * @throws IOException
	 */
	public void networkStartup() throws IOException {
		this.ec20.startup();
	}

	/**
	 * Get IMEI of the LTE module
	 * 
	 * @return
	 * @throws IOException
	 */
	public String networkGetIMEI() throws IOException {
		return this.ec20.getIMEI();
	}

	/**
	 * Get IMSI of the SIM Card
	 * 
	 * @return
	 * @throws IOException
	 */
	public String networkGetIMSI() throws IOException {
		return this.ec20.getIMSI();
	}

	/**
	 * Get signal strength
	 * 
	 * @return
	 * @throws IOException
	 */
	public int networkGetRSSI() throws IOException {
		return this.ec20.getRSSI();
	}

	/**
	 * Get LTE module
	 */
	public TiEC20 getNetworkService() {
		return this.ec20;
	}

	/**
	 * Get RS485 port of the device
	 *
	 * @return
	 * @throws IOException
	 */
	public TiSerialPort getRS485(int baudRate, int dataBitNum, int stopBitNum, int parity) throws IOException {

		if (rs485 == null) {
			// 485端口 - UART 1
			rs485 = new TiSerialPort(1);
			rs485.open(baudRate, dataBitNum, stopBitNum, parity);
		}

		return rs485;
	}

	/**
	 * Get MQTT client
	 * 
	 * @param server
	 * @param port
	 * @param clientId
	 * @return
	 */
	MQTTClient getMQTTClient(String server, int port, String clientId) {
		MQTTClient mqtt = ec20.getMqttClient(server, port, clientId);
		return mqtt;
	}

}
