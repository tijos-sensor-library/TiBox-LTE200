package tijos.framework.sensor.ec20.io;

public class ATResponse {

	private String response = "";
	private String keywords = "";

	public void reset() {
		this.response = "";
		this.keywords = "";
	}

	public void setExpectedRsp(String keywords) {
		this.keywords = keywords;
	}

	public void setResponse(String resp) {

		this.response += resp;
	}

	public String getResponse() {
		return this.response;
	}

	public String getKeywords() {
		return this.keywords;
	}

}
