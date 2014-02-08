package workbench;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

import com.sforce.soap.enterprise.GetUserInfoResult;
import com.sforce.soap.metadata.DescribeMetadataObject;
import com.sforce.soap.metadata.FileProperties;

public class PropertiesController {
	
	public static class MetadataProperty {
		
		public MetadataProperty(String n, String v) {
			setName(n);
			setValue(v);
		}
		
		private StringProperty name;
		public StringProperty nameProperty() {
			if (name == null) {
				name = new SimpleStringProperty(this, "name");
			}
			return name;
		}
		public String getName() {
			return nameProperty().get();
		}
		public void setName(String n) {
			nameProperty().set(n);
		}
		
		private StringProperty value;
		public StringProperty valueProperty() {
			if (value == null) {
				value = new SimpleStringProperty(this, "value");
			}
			return value;
		}
		public String getValue() {
			return valueProperty().get();
		}
		public void setValue(String v) {
			valueProperty().set(v);
		}
	}

	private Main application;
	
	private AnchorPane root;
	private TableView<MetadataProperty> tableView;
	private TableColumn<MetadataProperty, String> nameCol;
	private TableColumn<MetadataProperty, String> valueCol;
	
	public PropertiesController(Main application) {
		this.application = application;
		createGraph();
		showPropertiesForUserInfo(null);
	}
	
	public Node getRoot() {
		return root;
	}
	
	public void showPropertiesForUserInfo(GetUserInfoResult uir) {
		
		if (uir != null) {
			ObservableList<MetadataProperty> data = FXCollections.observableArrayList(
					new MetadataProperty("Org ID", uir.getOrganizationId()),
					new MetadataProperty("Org Name", uir.getOrganizationName()),
					new MetadataProperty("User Name", uir.getUserName()),
					new MetadataProperty("User Full Name", uir.getUserFullName()),
					new MetadataProperty("User ID", uir.getUserId()),
					new MetadataProperty("Profile ID", uir.getProfileId()),
					new MetadataProperty("Role ID", uir.getRoleId()),
					new MetadataProperty("Profile ID", uir.getProfileId()),
					new MetadataProperty("Email", uir.getUserEmail()),
					new MetadataProperty("Currency Symbol", uir.getCurrencySymbol()),
					new MetadataProperty("Org Currency Code", uir.getOrgDefaultCurrencyIsoCode()),
					new MetadataProperty("User Currency Code", uir.getUserDefaultCurrencyIsoCode()),
					new MetadataProperty("User Language", uir.getUserLanguage()),
					new MetadataProperty("User Locale", uir.getUserLocale()),
					new MetadataProperty("User Time Zone", uir.getUserTimeZone()),
					new MetadataProperty("User Type", uir.getUserType()),
					new MetadataProperty("User UI Skin", uir.getUserUiSkin()));						
			tableView.setItems(data);
		}
		else {
			tableView.setItems(null);
		}
	}
	
	public void showPropertiesForType(DescribeMetadataObject dmo) {
		
		if (dmo != null) {
			ObservableList<MetadataProperty> data = FXCollections.observableArrayList(
					new MetadataProperty("Child XML Names", dmo.getChildXmlNames().toString()),
					new MetadataProperty("Directory Name", dmo.getDirectoryName()),
					new MetadataProperty("Suffix", dmo.getSuffix()),
					new MetadataProperty("XML Name", dmo.getXmlName()),
					new MetadataProperty("Is In Folder", Boolean.toString(dmo.isInFolder())),
					new MetadataProperty("Is Metafile", Boolean.toString(dmo.isMetaFile())),
					new MetadataProperty("Has Metafile", Boolean.toString(dmo.getMetaFile())));
			tableView.setItems(data);
		}
		else {
			tableView.setItems(null);
		}
	}
	
	public void showPropertiesForFile(FileProperties fp) {
		
		if (fp != null) {
			ObservableList<MetadataProperty> data = FXCollections.observableArrayList(
					new MetadataProperty("Created By Id", fp.getCreatedById()),
					new MetadataProperty("Created By Name", fp.getCreatedByName()),
					new MetadataProperty("File Name", fp.getFileName()),
					new MetadataProperty("Full Name", fp.getFullName()),
					new MetadataProperty("Id", fp.getId()),
					new MetadataProperty("Last Modified By Id", fp.getLastModifiedById()),
					new MetadataProperty("Last Modified By Name", fp.getLastModifiedByName()),
					new MetadataProperty("Namespace Prefix", fp.getNamespacePrefix()),
					new MetadataProperty("Type", fp.getType()),
					new MetadataProperty("Created Date", fp.getCreatedDate().toString()),
					new MetadataProperty("Last Modified Date", fp.getLastModifiedDate().toString())/* TODO,
					new MetadataProperty("Manageable State", fp.getManageableState().toString())*/);
			tableView.setItems(data);
		}
		else {
			tableView.setItems(null);
		}
	}
	
	private void createGraph() {
		
		root = new AnchorPane();
		
		tableView = new TableView<>();
		tableView.setPlaceholder(new Label(""));
		tableView.setEditable(true);
		AnchorPane.setTopAnchor(tableView, 0.0);
		AnchorPane.setBottomAnchor(tableView, 0.0);
		AnchorPane.setLeftAnchor(tableView, 0.0);
		AnchorPane.setRightAnchor(tableView, 0.0);		
		root.getChildren().add(tableView);
		
		nameCol = new TableColumn<>();
		nameCol.setText("Name");
		nameCol.setPrefWidth(150.0);
		nameCol.setCellValueFactory(new PropertyValueFactory<MetadataProperty, String>("name"));
		tableView.getColumns().add(nameCol);
		
		valueCol = new TableColumn<>();
		valueCol.setText("Value");
		valueCol.setPrefWidth(260.0);
		valueCol.setCellValueFactory(new PropertyValueFactory<MetadataProperty, String>("value"));
		valueCol.setEditable(true);
		tableView.getColumns().add(valueCol);
	}
}
