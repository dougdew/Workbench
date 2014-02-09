package workbench;

import java.net.URL;

import workbench.LogController.LogMessage;

import com.sforce.ws.MessageHandler;

public class SOAPLogHandler implements LogMessage, MessageHandler {
	
	String title;
	String url;
	String summary;
	String details;
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
	
	public String getDetails() {
		return details;
	}
	public void setDetails(String details) {
		this.details = details;
	}

	public void handleRequest(URL endpoint, byte[] request) {
		// TODO Auto-generated method stub		
	}

	public void handleResponse(URL endpoint, byte[] response) {
		url = endpoint.toString();
		details = new String(response);
	}
}
