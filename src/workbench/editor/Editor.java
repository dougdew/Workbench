package workbench.editor;

import javafx.scene.Node;

import com.sforce.soap.metadata.Metadata;

public interface Editor {

	Node getRoot();
	
	Metadata getMetadata();
	void setMetadata(Metadata metadata);
}
