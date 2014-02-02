package workbench.editor;

import javafx.scene.Node;
import javafx.scene.control.TextArea;

import com.sforce.soap.metadata.Metadata;

public class DefaultEditor implements Editor {
	
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
			root.appendText(metadata.getClass().getName());
		}
		else {
			root.appendText("Null metadata");
		}
	}
}
