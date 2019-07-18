package tijos.framework.component.serialport;

import java.io.IOException;

import tijos.framework.devicecenter.TiUART;
import tijos.framework.util.Delay;
import tijos.framework.util.Formatter;
import tijos.framework.util.logging.Logger;

/**
 * Serial Port based on UART for TiJOS, support RS232 and RS485
 *
 * @author TiJOS
 */
public class TiSerialPort {

    private TiUART uart;

    /**
     * Initialize TiSerialPort with UART and GPIO
     *
     * @param uartPort UART port id
     * @param gpioPort GPIO port id, GPIO should be specified for RS485
     * @param gpioPin  GPIO pin id
     * @throws IOException
     */
    public TiSerialPort(int uartPort) throws IOException {

        // RS485使用UART1 根据外设进行初始化
        uart = TiUART.open(uartPort);
    }

    /**
     * Open with communication parameters
     *
     * @param baudRate
     * @param dataBitNum
     * @param stopBitNum
     * @param parity
     * @throws IOException
     */
    public void open(int baudRate, int dataBitNum, int stopBitNum, int parity) throws IOException {

        // UART通讯参数
        uart.setWorkParameters(dataBitNum, stopBitNum, parity, baudRate);
    }

    /**
     * Close
     *
     * @throws IOException
     */
    public void close() throws IOException {
        this.uart.close();
    }

    /**
     * Clear UART buffer
     *
     * @throws IOException
     */
    public void clearInput() throws IOException {

        this.uart.clear(TiUART.BUFF_READ);
    }

    /**
     * Write data to the uart
     *
     * @param buffer
     * @param start
     * @param length
     * @throws IOException
     */
    public void write(byte[] buffer, int start, int length) throws IOException {
        this.uart.write(buffer, start, length);
    }

    /**
     * Read data from uart
     *
     * @return data or null
     * @throws IOException
     */
    public byte[] read() throws IOException {
        int avail = this.uart.available();
        if (avail <= 0)
            return null;

        byte[] buffer = new byte[avail];
        this.uart.read(buffer, 0, avail);

        return buffer;
    }

    /**
     * Read data from uart
     *
     * @param msec read all data within the time interval if there are data
     * @return
     * @throws IOException
     */
    public byte[] read(int msec) throws IOException {

        int avail = this.uart.available();
        if (avail <= 0)
            return null;

        byte[] buffer = new byte[512];

        int num = 0;
        int left = buffer.length;

        //read ms to get all data
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < msec) {
            int len = this.uart.read(buffer, num, left);
            num += len;
            left -= len;

            if (left == 0)
                break;
        }

        byte[] newBuff = new byte[num];
        System.arraycopy(buffer, 0, newBuff, 0, num);

        return newBuff;
    }

    /**
     * Read data into buffer from the UART
     *
     * @param start
     * @param length
     * @param modbusClient
     * @return
     * @throws IOException
     */
    public boolean readToBuffer(byte[] buffer, int start, int length, int timeOut) throws IOException {

        long now = System.currentTimeMillis();
        long deadline = now + timeOut;
        int offset = start;
        int bytesToRead = length;
        int res;
        while ((now < deadline) && (bytesToRead > 0)) {
            res = this.uart.read(buffer, offset, bytesToRead);
            if (res <= 0) {
                Delay.msDelay(10);
                now = System.currentTimeMillis();
                continue;
            }

            offset += res;
            bytesToRead -= res;
            if (bytesToRead > 0) // only to avoid redundant call of System.currentTimeMillis()
                now = System.currentTimeMillis();
        }
        res = length - bytesToRead; // total bytes read
        if (res < length) {
            Logger.info("TiRS485",
                    "Read timeout(incomplete): " + Formatter.toHexString(buffer, offset, start + res, ""));

            return false;
        } else
            return true;
    }


}
