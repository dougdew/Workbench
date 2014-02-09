package workbench;

import java.util.Calendar;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.AnchorPane;

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
	private TreeTableView<MetadataProperty> treeTableView;
	TreeItem<MetadataProperty> treeTableViewRoot;
	private TreeTableColumn<MetadataProperty, String> nameColumn;
	private TreeTableColumn<MetadataProperty, String> valueColumn;
	
	public PropertiesController(Main application) {
		this.application = application;
		createGraph();
		showPropertiesForUserInfo(null);
	}
	
	public Node getRoot() {
		return root;
	}
	
	public void showPropertiesForUserInfo(GetUserInfoResult uir) {
		
		treeTableViewRoot.getChildren().clear();
		
		if (uir != null) {
			
			TreeItem<MetadataProperty> orgId = new TreeItem<>(new MetadataProperty("Org Id", uir.getOrganizationId()));
			treeTableViewRoot.getChildren().add(orgId);
			
			TreeItem<MetadataProperty> orgName = new TreeItem<>(new MetadataProperty("Org Name", uir.getOrganizationName()));
			treeTableViewRoot.getChildren().add(orgName);
			
			TreeItem<MetadataProperty> userName = new TreeItem<>(new MetadataProperty("User Name", uir.getUserName()));
			treeTableViewRoot.getChildren().add(userName);
			
			TreeItem<MetadataProperty> userFullName = new TreeItem<>(new MetadataProperty("User Full Name", uir.getUserFullName()));
			treeTableViewRoot.getChildren().add(userFullName);
			
			TreeItem<MetadataProperty> userId = new TreeItem<>(new MetadataProperty("User Id", uir.getUserId()));
			treeTableViewRoot.getChildren().add(userId);
			
			TreeItem<MetadataProperty> profileId = new TreeItem<>(new MetadataProperty("Profile Id", uir.getProfileId()));
			treeTableViewRoot.getChildren().add(profileId);
			
			TreeItem<MetadataProperty> roleId = new TreeItem<>(new MetadataProperty("Role Id", uir.getRoleId()));
			treeTableViewRoot.getChildren().add(roleId);
			
			TreeItem<MetadataProperty> email = new TreeItem<>(new MetadataProperty("Email", uir.getUserEmail()));
			treeTableViewRoot.getChildren().add(email);
			
			TreeItem<MetadataProperty> currencySymbol = new TreeItem<>(new MetadataProperty("Currency Symbol", uir.getCurrencySymbol()));
			treeTableViewRoot.getChildren().add(currencySymbol);
			
			TreeItem<MetadataProperty> orgCurrencyCode = new TreeItem<>(new MetadataProperty("Org Currency Code", uir.getOrgDefaultCurrencyIsoCode()));
			treeTableViewRoot.getChildren().add(orgCurrencyCode);
			
			TreeItem<MetadataProperty> userCurrencyCode = new TreeItem<>(new MetadataProperty("User Currency Code", uir.getUserDefaultCurrencyIsoCode()));
			treeTableViewRoot.getChildren().add(userCurrencyCode);
			
			TreeItem<MetadataProperty> userLanguage = new TreeItem<>(new MetadataProperty("User Language", uir.getUserLanguage()));
			treeTableViewRoot.getChildren().add(userLanguage);
			
			TreeItem<MetadataProperty> userLocale = new TreeItem<>(new MetadataProperty("User Locale", uir.getUserLocale()));
			treeTableViewRoot.getChildren().add(userLocale);
			
			TreeItem<MetadataProperty> userTimeZone = new TreeItem<>(new MetadataProperty("User Time Zone", uir.getUserTimeZone()));
			treeTableViewRoot.getChildren().add(userTimeZone);
			
			TreeItem<MetadataProperty> userType = new TreeItem<>(new MetadataProperty("User Type", uir.getUserType()));
			treeTableViewRoot.getChildren().add(userType);
			
			TreeItem<MetadataProperty> userUiSkin = new TreeItem<>(new MetadataProperty("User UI Skin", uir.getUserUiSkin()));
			treeTableViewRoot.getChildren().add(userUiSkin);
		}
	}
	
	public void showPropertiesForType(DescribeMetadataObject dmo) {
		
		treeTableViewRoot.getChildren().clear();
		
		if (dmo != null) {
			
			String childXmlNamesCount = dmo.getChildXmlNames().length == 0 ? "No children" : String.format("%d children", dmo.getChildXmlNames().length);
			TreeItem<MetadataProperty> childXmlNames = new TreeItem<>(new MetadataProperty("Child XML Names", childXmlNamesCount));
			treeTableViewRoot.getChildren().add(childXmlNames);
			for (String name : dmo.getChildXmlNames()) {
				TreeItem<MetadataProperty> childXmlName = new TreeItem<>(new MetadataProperty("", name));
				childXmlNames.getChildren().add(childXmlName);
			}
			
			TreeItem<MetadataProperty> directoryName = new TreeItem<>(new MetadataProperty("Directory Name", dmo.getDirectoryName()));
			treeTableViewRoot.getChildren().add(directoryName);
			
			TreeItem<MetadataProperty> suffix = new TreeItem<>(new MetadataProperty("Suffix", dmo.getSuffix()));
			treeTableViewRoot.getChildren().add(suffix);
			
			TreeItem<MetadataProperty> xmlName = new TreeItem<>(new MetadataProperty("Xml Name", dmo.getXmlName()));
			treeTableViewRoot.getChildren().add(xmlName);
			
			TreeItem<MetadataProperty> isInFolder = new TreeItem<>(new MetadataProperty("Is In Folder", Boolean.toString(dmo.isInFolder())));
			treeTableViewRoot.getChildren().add(isInFolder);
			
			TreeItem<MetadataProperty> isMetafile = new TreeItem<>(new MetadataProperty("Is Metafile", Boolean.toString(dmo.isMetaFile())));
			treeTableViewRoot.getChildren().add(isMetafile);
			
			TreeItem<MetadataProperty> hasMetafile = new TreeItem<>(new MetadataProperty("Has Metafile", Boolean.toString(dmo.getMetaFile())));
			treeTableViewRoot.getChildren().add(hasMetafile);
		}
	}
	
	public void showPropertiesForFile(FileProperties fp) {
	
		treeTableViewRoot.getChildren().clear();
		
		if (fp != null) {
			TreeItem<MetadataProperty> createdById = new TreeItem<>(new MetadataProperty("Created By Id", fp.getCreatedById()));
			treeTableViewRoot.getChildren().add(createdById);
			
			TreeItem<MetadataProperty> createdByName = new TreeItem<>(new MetadataProperty("Created By Name", fp.getCreatedByName()));
			treeTableViewRoot.getChildren().add(createdByName);
			
			TreeItem<MetadataProperty> fileName = new TreeItem<>(new MetadataProperty("File Name", fp.getFileName()));
			treeTableViewRoot.getChildren().add(fileName);
			
			TreeItem<MetadataProperty> fullName = new TreeItem<>(new MetadataProperty("Full Name", fp.getFullName()));
			treeTableViewRoot.getChildren().add(fullName);
			
			TreeItem<MetadataProperty> id = new TreeItem<>(new MetadataProperty("Id", fp.getId()));
			treeTableViewRoot.getChildren().add(id);
			
			TreeItem<MetadataProperty> lastModifiedById = new TreeItem<>(new MetadataProperty("Last Modified By Id", fp.getLastModifiedById()));
			treeTableViewRoot.getChildren().add(lastModifiedById);
			
			TreeItem<MetadataProperty> lastModifiedByName = new TreeItem<>(new MetadataProperty("Last Modified By Name", fp.getLastModifiedByName()));
			treeTableViewRoot.getChildren().add(lastModifiedByName);
			
			TreeItem<MetadataProperty> namespacePrefix = new TreeItem<>(new MetadataProperty("Namespace Prefix", fp.getNamespacePrefix()));
			treeTableViewRoot.getChildren().add(namespacePrefix);
			
			TreeItem<MetadataProperty> type = new TreeItem<>(new MetadataProperty("Type", fp.getType()));
			treeTableViewRoot.getChildren().add(type);
			
			TreeItem<MetadataProperty> createdDate = new TreeItem<>(new MetadataProperty("Created Date", createDateString(fp.getCreatedDate())));
			treeTableViewRoot.getChildren().add(createdDate);
			
			TreeItem<MetadataProperty> lastModifiedDate = new TreeItem<>(new MetadataProperty("Last Modified Date", createDateString(fp.getLastModifiedDate())));
			treeTableViewRoot.getChildren().add(lastModifiedDate);
		}
	}
	
	private String createDateString(Calendar date) {
		// TODO: Add time zone information
		return String.format("%d/%d/%d %d:%d", 
				             date.get(Calendar.DAY_OF_MONTH), 
				             date.get(Calendar.MONTH) + 1, 
				             date.get(Calendar.YEAR),
				             date.get(Calendar.HOUR_OF_DAY),
				             date.get(Calendar.MINUTE));
	}
	
	private void createGraph() {
		
		root = new AnchorPane();
		
		treeTableView = new TreeTableView<>();
		treeTableView.setShowRoot(false);
		AnchorPane.setTopAnchor(treeTableView, 0.0);
		AnchorPane.setBottomAnchor(treeTableView, 0.0);
		AnchorPane.setLeftAnchor(treeTableView, 0.0);
		AnchorPane.setRightAnchor(treeTableView, 0.0);
		root.getChildren().add(treeTableView);
		
		nameColumn = new TreeTableColumn<>("Name");
		nameColumn.setPrefWidth(150.);
		nameColumn.setCellValueFactory(new TreeItemPropertyValueFactory<MetadataProperty, String>("name"));
		treeTableView.getColumns().add(nameColumn);
		
		valueColumn = new TreeTableColumn<>("Value");
		valueColumn.setPrefWidth(260.0);
		valueColumn.setCellValueFactory(new TreeItemPropertyValueFactory<MetadataProperty, String>("value"));
		treeTableView.getColumns().add(valueColumn);
		
		treeTableViewRoot = new TreeItem<>(new MetadataProperty("ROOT", "ROOT"));
		treeTableView.setRoot(treeTableViewRoot);
	}
}
