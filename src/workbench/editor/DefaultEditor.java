package workbench.editor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.xml.namespace.QName;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.TextArea;

import com.sforce.soap.metadata.Metadata;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.bind.TypeMapper;
import com.sforce.ws.parser.PullParserException;
import com.sforce.ws.parser.XmlInputStream;
import com.sforce.ws.parser.XmlOutputStream;
import com.sforce.ws.wsdl.Constants;

import workbench.Main;

public class DefaultEditor implements Editor {
	
	private static String METADATA_XML_ELEMENT_NAME = "Metadata";
	
	private Metadata metadata;
	private TextArea root;
	private BooleanProperty dirtyProperty = new SimpleBooleanProperty();
	
	public DefaultEditor() {
		createGraph();
	}

	public Node getRoot() {
		return root;
	}
	
	public Metadata getMetadata() {
		if (dirtyProperty.get()) {
			setMetadataFromUI();
		}
		return metadata;
	}
	
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
		setUIFromMetadata();
		dirtyProperty.set(false);
	}
	
	public BooleanProperty dirty() {
		return dirtyProperty;
	}
	
	public void lock() {
		root.setDisable(true);
	}
	
	public void unlock() {
		root.setDisable(false);
	}
	
	private void createGraph() {
		
		root = new TextArea();
		root.setEditable(true);
		root.setOnKeyTyped(e -> dirtyProperty.set(true));
	}
	
	private void setUIFromMetadata() {
		root.clear();
		if (metadata != null) {
			
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				XmlOutputStream xout = new XmlOutputStream(baos, "    ");
		        
		        xout.setPrefix("env", Constants.SOAP_ENVELOPE_NS);
		        xout.setPrefix("xsd", Constants.SCHEMA_NS);
		        xout.setPrefix("xsi", Constants.SCHEMA_INSTANCE_NS);
		        xout.setPrefix("", Main.getMetadataNamespace());
		        
		        metadata.write(new QName(Main.getMetadataNamespace(), METADATA_XML_ELEMENT_NAME), xout, new TypeMapper());
		        
		        xout.close();
		        
		        String xml = new String(baos.toByteArray(), Charset.forName("UTF-8"));
		        
		        root.appendText(xml);
		        root.setScrollTop(0.0);
			}
			catch (IOException e) {
				// TODO: Fix this
				e.printStackTrace();
			}
		}
		else {
			root.appendText("Null metadata");
		}
	}
	
	private void setMetadataFromUI() {
		
		if (metadata != null) {
			 
			try {
				String xml = root.getText();
				
				XmlInputStream xin = new XmlInputStream();
				xin.setInput(new ByteArrayInputStream(xml.getBytes(Charset.forName("UTF-8"))), "UTF-8");
				
				metadata.load(xin, new TypeMapper());
			}
			catch (PullParserException e) {
				e.printStackTrace();
			}
			catch (ConnectionException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			} 	
		}
	}
}