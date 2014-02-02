package workbench.editor;

import javafx.scene.Node;
import javafx.scene.control.TextArea;

import com.sforce.soap.metadata.CustomObject;
import com.sforce.soap.metadata.Metadata;

public class CustomObjectEditor implements Editor {

	private static final String TYPE = "CustomObject";
	private static final String FILE_EXTENSION = "object";
	
	public static String getType() {
		return TYPE;
	}
	
	public static String getFileExtension() {
		return FILE_EXTENSION;
	}
	
	private Metadata metadata;
	private TextArea root;
	
	public CustomObjectEditor() {
		createGraph();
	}
	
	public Node getRoot() {
		return root;
	}
	
	public Metadata getMetadata() {
		return metadata;
	}
	
	public void setMetadata(Metadata metadata) {
		if (metadata != null && !(metadata instanceof CustomObject)) {
			return;
		}
		this.metadata = metadata;
		loadMetadata();
	}
	
	private void createGraph() {
		root = new TextArea();
		root.setEditable(false);
	}
	
	private void loadMetadata() {
		root.clear();
		if (metadata != null) {
			root.appendText("Loaded CustomObject from Metadata");
		}
		else {
			root.appendText("Null CustomObject");
		}
	}
}
