package workbench;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.sforce.ws.MessageHandler;

import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;

public class LogController {
	
	public static class LogHandler implements MessageHandler {
		
		String title;
		String url;
		String message;
		
		public void setTitle(String title) {
			this.title = title;
		}
		public String getTitle() {
			return title;
		}
		
		public void setUrl(String url) {
			this.url = url;
		}
		public String getUrl() {
			return url;
		}
		
		public void setMessage(String message) {
			this.message = message;
		}
		public String getMessage() {
			return message;
		}

		@Override
		public void handleRequest(URL endpoint, byte[] request) {
			// TODO Auto-generated method stub		
		}

		@Override
		public void handleResponse(URL endpoint, byte[] response) {
			url = endpoint.toString();
			message = new String(response);
		}
	}
	
	private Main application;
	
	private AnchorPane root;
	private TextArea textArea;
	
	public LogController(Main application) {
		this.application = application;
		createGraph();
		application.metadataConnection().addListener((o, oldValue, newValue) -> handleMetadataConnectionChanged());
	}
	
	public Node getRoot() {
		return root;
	}
	
	public void log(LogHandler message) {
		if (message.getTitle() != null) {
			textArea.appendText(message.getTitle() + "\n");
		}
		textArea.appendText(message.getUrl() + "\n");
		textArea.appendText(prettyFormat(message.getMessage()));
		textArea.appendText("\n\n\n\n\n");
	}
	
	private void createGraph() {
		
		root = new AnchorPane();
		
		textArea = new TextArea();
		AnchorPane.setTopAnchor(textArea, 0.0);
		AnchorPane.setBottomAnchor(textArea, 0.0);
		AnchorPane.setLeftAnchor(textArea, 0.0);
		AnchorPane.setRightAnchor(textArea, 0.0);
		root.getChildren().add(textArea);
	}
	
	private void handleMetadataConnectionChanged() {
		
		if (application.metadataConnection().get() != null) {
			
		}
		else {
			
		}
	}
	
	private String prettyFormat(String input) {
	    return prettyFormat(input, 2);
	}
	
	private String prettyFormat(String input, int indent) {
	    try {
	        Source xmlInput = new StreamSource(new StringReader(input));
	        StringWriter stringWriter = new StringWriter();
	        StreamResult xmlOutput = new StreamResult(stringWriter);
	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        transformerFactory.setAttribute("indent-number", indent);
	        Transformer transformer = transformerFactory.newTransformer(); 
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.transform(xmlInput, xmlOutput);
	        return xmlOutput.getWriter().toString();
	    } catch (Exception e) {
	        throw new RuntimeException(e); // simple exception handling, please review it
	    }
	}

	
}
