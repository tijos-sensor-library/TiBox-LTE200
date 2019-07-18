package tijos.framework.sensor.ec20;

import java.io.IOException;

public class GPSPosition {
	public String UTC;
	public double latitude;
	public double longitude;
	public double hdop;
	public double altitude;
	public int fix;
	public double cog;
	public double spkm;
	public double spkn;
	public String date;
	public int nsat;

	public void parse(String resp) throws IOException {

		int begin = resp.indexOf(':');
		if (begin < 0)
			throw new IOException("Wrong response");

		resp = resp.substring(begin + 2);
		String[] result = resp.split(",");

		if (result.length < 11)
			throw new IOException("Invalid format.");

		int pos = 0;
		this.UTC = result[pos++];
		this.latitude = RMC2Double(result[pos++]);
		pos++;
		this.longitude = RMC2Double(result[pos++]);
		pos++;
		this.hdop = Double.parseDouble(result[pos++]);
		this.altitude = Double.parseDouble(result[pos++]);
		this.fix = Integer.parseInt(result[pos++]);
		this.cog = Double.parseDouble(result[pos++]);
		this.spkm = Double.parseDouble(result[pos++]);
		this.spkn = Double.parseDouble(result[pos++]);
		this.date = result[pos++];
		this.nsat = Integer.parseInt(result[pos++]);

	}

//	+QGPSLOC: 093929.0,3959.807968,N,11619.997406,E,0.8,27.0,2,175.21,0.0,0.0,050719,07

	double RMC2Double(String rmc) {
		double temp = Double.parseDouble(rmc);
		int firstPart = ((int) temp) / 100;

		double result = firstPart + (temp - firstPart * 100) / 60.0d;

		return result;
	}

}
