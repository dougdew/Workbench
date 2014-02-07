package workbench.editor;

import javafx.beans.property.BooleanProperty;
import javafx.scene.Node;

import com.sforce.soap.metadata.Metadata;

public interface Editor {

	Node getRoot();
	
	Metadata getMetadata();
	void setMetadata(Metadata metadata);
	
	String getMetadataAsXml();
	void setMetadataAsXml(String xml);
	
	BooleanProperty dirty();
	
	// TODO: Fix this. Should probably copy the metadata
	// objects instead of locking their editors.
	void lock();
	void unlock();
}
