package tijos.framework.tibox.lte200;

import tijos.framework.component.modbus.rtu.ModbusClient;
import tijos.framework.component.serialport.TiSerialPort;
import tijos.framework.devicecenter.TiUART;
import tijos.framework.sensor.ec20.mqtt.IMQTTEventListener;
import tijos.framework.sensor.ec20.mqtt.MQTTClient;
import tijos.framework.sensor.ec20.mqtt.MQTTConnectOptions;
import tijos.framework.util.Delay;

class MQTTEventListner implements IMQTTEventListener {

	public MQTTEventListner() {
	}

	@Override
	public void onPublishDataArrived(int msgId, String topic, String message) {
		System.out.println("Data arrived " + msgId + " topic " + topic + " message " + message);
	}

	@Override
	public void onLinkLost(int error) {
		System.out.println("Data onMQTTLinkLost " + " error " + error);
	}

}

/**
 * Hello world!
 */
public class TiLTE200Sample {
	public static void main(String[] args) {
		System.out.println("Hello World!");

		try {

			TiLTE200 lte200 = TiLTE200.getInstance();

			lte200.turnOnSysLED();
			lte200.turnOnUserLED(0);
			lte200.turnOnUserLED(1);

			lte200.networkWakeUp();
			MQTTClient mqtt = lte200.getMQTTClient("mqtt.tijcloud.com", 1833, MQTTClient.generateClientId());

			mqtt.setEventListener(new MQTTEventListner());

			MQTTConnectOptions options = new MQTTConnectOptions();
			mqtt.connect(options);

			TiSerialPort rs485 = lte200.getRS485(9600, 8, 1, TiUART.PARITY_NONE);
			ModbusClient modbusRtu = new ModbusClient(rs485);

			String subtopic = "/topic/TiKit/" + lte200.networkGetIMEI() + "/cmd";
			mqtt.subscribe(1, subtopic);

			System.out.println(" IMSI : " + lte200.networkGetIMSI());
			System.out.println(" IMEI : " + lte200.networkGetIMEI());
			System.out.println(" RSSI : " + lte200.networkGetRSSI());

			while (true) {
				MonitorProcess(modbusRtu, mqtt, lte200);

				Delay.msDelay(1000 * 60);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	/**
	 * 通过RS485基于MODBUS协议读取设备数据并通过NBIOT上传至云平台
	 *
	 * @param modbusRtu
	 * @param mqtt
	 * @param iotPlatform
	 */
	public static void MonitorProcess(ModbusClient modbusRtu, MQTTClient mqtt, TiLTE200 lte200) {
		try {
			// MODBUS Server 设备地址
			int serverId = 1;
			
			// Input Register 开始地址
			int startAddr = 0;

			// 主题topic
			String pubtopic = "/topic/TiKit/" + lte200.networkGetIMEI() + "/data";
			
			// Read 2 registers from start address 读取个数
			int count = 2;

			// 读取Holding Register
			modbusRtu.InitReadHoldingsRequest(serverId, startAddr, count);

			try {
				int result = modbusRtu.execRequest();

				if (result == ModbusClient.RESULT_OK) {

					double humdity = modbusRtu.getResponseRegister(modbusRtu.getResponseAddress(), false);
					double temperature = modbusRtu.getResponseRegister(modbusRtu.getResponseAddress() + 1, false);

					System.out.println("temp = " + temperature + " humdity = " + humdity);
					String str = "{\"Temperature\":" + temperature / 10 + ",\"Rssi\":" + lte200.networkGetRSSI()
							+ ",\"Humidity\":" + humdity / 10 + "}";
					// 上报平台
					mqtt.publish(pubtopic, str, 1, false);

				} else {
					System.out.println("Modbus Error: result = " + result);
				}

			} catch (Exception ex) {
				ex.printStackTrace();
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
