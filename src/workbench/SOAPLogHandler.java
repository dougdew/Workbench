package workbench;

import java.net.URL;

import workbench.LogController.LogMessage;

import com.sforce.ws.MessageHandler;

public class SOAPLogHandler implements LogMessage, MessageHandler {
	
	String title;
	String url;
	String message;
	
	public SOAPLogHandler(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getMessage() {
		return message;
	}

	public void handleRequest(URL endpoint, byte[] request) {
		// TODO Auto-generated method stub		
	}

	public void handleResponse(URL endpoint, byte[] response) {
		url = endpoint.toString();
		message = new String(response);
	}
}
