package workbench.editor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.xml.namespace.QName;

import javafx.scene.Node;
import javafx.scene.control.TextArea;

import com.sforce.soap.metadata.Metadata;
import com.sforce.ws.bind.TypeMapper;
import com.sforce.ws.parser.XmlOutputStream;
import com.sforce.ws.wsdl.Constants;

import workbench.Main;

public class DefaultEditor implements Editor {
	
	private static String METADATA_XML_ELEMENT_NAME = "Metadata";
	
	private Metadata metadata;
	private TextArea root;
	
	public DefaultEditor() {
		createGraph();
	}

	public Node getRoot() {
		return root;
	}
	
	public Metadata getMetadata() {
		return metadata;
	}
	
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
		setUIFromMetadata();
	}
	
	private void createGraph() {
		
		root = new TextArea();
		root.setEditable(false);
	}
	
	private void setUIFromMetadata() {
		root.clear();
		if (metadata != null) {
			
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				XmlOutputStream xout = new XmlOutputStream(baos, "    ");
		        xout.startDocument();
		        xout.writeText("\n");
		        
		        xout.setPrefix("env", Constants.SOAP_ENVELOPE_NS);
		        xout.setPrefix("xsd", Constants.SCHEMA_NS);
		        xout.setPrefix("xsi", Constants.SCHEMA_INSTANCE_NS);
		        xout.setPrefix("", Main.getMetadataNamespace());
		        
		        metadata.write(new QName(Main.getMetadataNamespace(), METADATA_XML_ELEMENT_NAME), xout, new TypeMapper());
		        
		        xout.endDocument();
		        xout.close();
		        
		        String xml = new String(baos.toByteArray(), Charset.forName("UTF-8"));
		        
		        root.appendText(xml);
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
		
	}
}