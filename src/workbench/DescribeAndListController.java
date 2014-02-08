package workbench;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;

import com.sforce.soap.metadata.DeleteResult;
import com.sforce.soap.metadata.DescribeMetadataObject;
import com.sforce.soap.metadata.DescribeMetadataResult;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.ws.ConnectionException;

public class DescribeAndListController {
	
	private static class DescribeWorkerResults {
		
		private SOAPLogHandler logHandler;
		private SortedMap<String, DescribeMetadataObject> description;
		
		public void setLogHandler(SOAPLogHandler logHandler) {
			this.logHandler = logHandler;
		}
		public SOAPLogHandler getLogHandler() {
			return logHandler;
		}
		
		public void setDescription(SortedMap<String, DescribeMetadataObject> description) {
			this.description = description;
		}
		public SortedMap<String, DescribeMetadataObject> getDescription() {
			return description;
		}
	}
	
	private static class DescribeWorker extends Task<DescribeWorkerResults> {
		
		private MetadataConnection connection;
		private double apiVersion;
		
		public DescribeWorker(MetadataConnection connection, double apiVersion) {
			this.connection = connection;
			this.apiVersion = apiVersion;
		}
		
		@Override
		protected DescribeWorkerResults call() throws Exception {
			
			DescribeWorkerResults workerResults = new DescribeWorkerResults();
			
			try {
				SOAPLogHandler logHandler = new SOAPLogHandler("DESCRIBE");
				connection.getConfig().addMessageHandler(logHandler);
				workerResults.setLogHandler(logHandler);
				
				DescribeMetadataResult mdapiDescribe = connection.describeMetadata(apiVersion);
				
				SortedMap<String, DescribeMetadataObject> describeResult = new TreeMap<>();
				for (DescribeMetadataObject dmo : mdapiDescribe.getMetadataObjects()) {
					describeResult.put(dmo.getXmlName(), dmo);
				}
				workerResults.setDescription(describeResult);
				
				connection.getConfig().clearMessageHandlers();
			}
			catch (ConnectionException e) {
				// TODO: Fix this
				e.printStackTrace();
			}
			return workerResults;
		}
	}
	
	private static class ListWorkerResults {
		
		private SOAPLogHandler logHandler;
		private SortedMap<String, FileProperties> list;
		
		public void setLogHandler(SOAPLogHandler logHandler) {
			this.logHandler = logHandler;
		}
		public SOAPLogHandler getLogHandler() {
			return logHandler;
		}
		
		public void setList(SortedMap<String, FileProperties> list) {
			this.list = list;
		}
		public SortedMap<String, FileProperties> getList() {
			return list;
		}
	}
	
	private static class ListWorker extends Task<ListWorkerResults> {
		
		private MetadataConnection connection;
		private double apiVersion;
		private String typeName;
		
		public ListWorker(MetadataConnection connection, double apiVersion, String typeName) {
			this.connection = connection;
			this.apiVersion = apiVersion;
			this.typeName = typeName;
		}
		
		@Override
		protected ListWorkerResults call() throws Exception {
			
			ListWorkerResults workerResults = new ListWorkerResults();
			
			try {
				SOAPLogHandler logHandler = new SOAPLogHandler("LIST");
				connection.getConfig().addMessageHandler(logHandler);
				workerResults.setLogHandler(logHandler);
				
				SortedMap<String, FileProperties> listResult = new TreeMap<>();
				ListMetadataQuery query = new ListMetadataQuery();
				query.setType(typeName);
				
				FileProperties[] mdapiList = connection.listMetadata(new ListMetadataQuery[]{query}, apiVersion);
				
				for (FileProperties fp : mdapiList) {
					listResult.put(fp.getFileName(), fp);
				}
				workerResults.setList(listResult);
				
				connection.getConfig().clearMessageHandlers();
			}
			catch (ConnectionException e) {
				// TODO: Fix this
				e.printStackTrace();
			}
			
			return workerResults;
		}
	}
	
	private static class DeleteWorkerResults {
		
		private SOAPLogHandler logHandler;
		private boolean success;
		
		public void setLogHandler(SOAPLogHandler logHandler) {
			this.logHandler = logHandler;
		}
		public SOAPLogHandler getLogHandler() {
			return logHandler;
		}
		
		public void setSuccess(boolean success) {
			this.success = success;
		}
		public boolean getSuccess() {
			return success;
		}
	}
	
	private static class DeleteWorker extends Task<DeleteWorkerResults> {
		
		private MetadataConnection connection;
		private String typeName;
		private String fullName;
		
		public DeleteWorker(MetadataConnection connection, String typeName, String fullName) {
			this.connection = connection;
			this.typeName = typeName;
			this.fullName = fullName;
		}
		
		@Override
		protected DeleteWorkerResults call() throws Exception {
			
			DeleteWorkerResults workerResults = new DeleteWorkerResults();
			
			try {
				SOAPLogHandler logHandler = new SOAPLogHandler("DELETE");
				connection.getConfig().addMessageHandler(logHandler);
				workerResults.setLogHandler(logHandler);
				
				DeleteResult[] mdapiDelete = connection.deleteMetadata(typeName, new String[]{fullName});
				if (mdapiDelete != null && mdapiDelete.length == 1 && mdapiDelete[0].getSuccess()) {
					workerResults.setSuccess(true);
				} 
				
				connection.getConfig().clearMessageHandlers();
			}
			catch (ConnectionException e) {
				// TODO: Fix this
				e.printStackTrace();
			}
			
			return workerResults;
		}
	}
	
	private Main application;
	
	private SortedMap<String, DescribeMetadataObject> metadataDescription;
	private Map<String, SortedMap<String, FileProperties>> metadataLists = new TreeMap<>();
	
	private AnchorPane root;
	private HBox treeOperationsBar;
	private TreeView<String> descriptionAndListsTree;
	private TreeItem<String> descriptionAndListsTreeRoot;
	private Button describeButton;
	private Button listButton;
	private Button readButton;
	private Button deleteButton;
	private Button cancelButton;
	
	public DescribeAndListController(Main application) {
		this.application = application;
		createGraph();
		application.metadataConnection().addListener((o, oldValue, newValue) -> handleMetadataConnectionChanged());
		application.orgName().addListener((o, oldValue, newValue) -> handleOrgNameChanged(oldValue, newValue));
	}
	
	public Node getRoot() {
		return root;
	}
	
	private void createGraph() {
		
		root = new AnchorPane();
		
		treeOperationsBar = new HBox();
		treeOperationsBar.setAlignment(Pos.BASELINE_CENTER);
		AnchorPane.setTopAnchor(treeOperationsBar, 6.0);
		AnchorPane.setLeftAnchor(treeOperationsBar, 0.0);
		AnchorPane.setRightAnchor(treeOperationsBar, 0.0);
		root.getChildren().add(treeOperationsBar);
			
		describeButton = new Button("Describe");
		describeButton.setDisable(true);
		describeButton.setOnAction(e -> handleDescribeButtonClicked(e));
		treeOperationsBar.getChildren().add(describeButton);
		
		listButton = new Button("List");
		listButton.setDisable(true);
		listButton.setOnAction(e -> handleListButtonClicked(e));
		treeOperationsBar.getChildren().add(listButton);
		
		readButton = new Button("Read");
		readButton.setDisable(true);
		readButton.setOnAction(e -> handleReadButtonClicked(e));
		treeOperationsBar.getChildren().add(readButton);
		
		deleteButton = new Button("Delete");
		deleteButton.setDisable(true);
		deleteButton.setOnAction(e -> handleDeleteButtonClicked(e));
		treeOperationsBar.getChildren().add(deleteButton);
		
		cancelButton = new Button("Cancel");
		cancelButton.setDisable(true);
		treeOperationsBar.getChildren().add(cancelButton);
		
		descriptionAndListsTree = new TreeView<>();
		descriptionAndListsTree.setOnMouseClicked(e -> handleTreeItemClicked(e));
		AnchorPane.setTopAnchor(descriptionAndListsTree, 40.0);
		AnchorPane.setBottomAnchor(descriptionAndListsTree, 0.0);
		AnchorPane.setLeftAnchor(descriptionAndListsTree, 0.0);
		AnchorPane.setRightAnchor(descriptionAndListsTree, 0.0);
		root.getChildren().add(descriptionAndListsTree);
		
		descriptionAndListsTreeRoot = new TreeItem<>("");
		descriptionAndListsTree.setRoot(descriptionAndListsTreeRoot);
		descriptionAndListsTreeRoot.setExpanded(true);
	}
	
	private void handleMetadataConnectionChanged() {
		if (application.metadataConnection().get() != null) {
			setDisablesForTreeSelection();
		}
		else {
			describeButton.setDisable(true);
			listButton.setDisable(true);
			readButton.setDisable(true);
			deleteButton.setDisable(true);
		}
	}
	
	private void handleOrgNameChanged(String oldOrgName, String newOrgName) {
		descriptionAndListsTree.getRoot().getChildren().clear();
		if (metadataDescription != null) {
			metadataDescription.clear();
		}
		metadataLists.clear();
		
		descriptionAndListsTreeRoot.setValue(newOrgName);
		descriptionAndListsTree.getSelectionModel().select(descriptionAndListsTreeRoot);
		
		setDisablesForTreeSelection();
	}
	
	private void handleTreeItemClicked(MouseEvent e) {
		
		if (e.getClickCount() == 1) {		
			setDisablesForTreeSelection();
		}
	}
	
	private void setDisablesForTreeSelection() {
		
		TreeItem<String> selectedItem = descriptionAndListsTree.getSelectionModel().getSelectedItem();
		if (selectedItem == null) {
			return;
		}
		
		boolean connected = application.metadataConnection().get() != null;
		
		if (selectedItem == descriptionAndListsTree.getRoot()) {
			if (connected) {
				describeButton.setDisable(false);
			}
			listButton.setDisable(true);
			readButton.setDisable(true);
			deleteButton.setDisable(true);
		}
		else if (selectedItem.getParent() == descriptionAndListsTree.getRoot()) {
			describeButton.setDisable(true);
			if (connected) {
				listButton.setDisable(false);
			}
			readButton.setDisable(true);
			deleteButton.setDisable(true);
			
			DescribeMetadataObject dmo = metadataDescription.get(selectedItem.getValue());
			application.getPropertiesController().showPropertiesForType(dmo);
		}
		else {
			describeButton.setDisable(true);
			listButton.setDisable(true);
			if (connected) {
				readButton.setDisable(false);
				deleteButton.setDisable(false);
			}
			
			String typeName = selectedItem.getParent().getValue();
			SortedMap<String, FileProperties> fileMap = metadataLists.get(typeName);
			String fileName = selectedItem.getValue();
			FileProperties fp = fileMap.get(fileName);
			application.getPropertiesController().showPropertiesForFile(fp);
		}
	}
	
	private void setDisablesForOperationCompletion() {
		cancelButton.setDisable(true);
		descriptionAndListsTree.setDisable(false);
		setDisablesForTreeSelection();
	}
	
	private void setDisablesForOperationCancellation() {
		cancelButton.setDisable(true);
		// TODO:
	}
	
	private void handleDescribeButtonClicked(ActionEvent e) {
		
		descriptionAndListsTree.setDisable(true);
		describeButton.setDisable(true);
		
		final DescribeWorker describeWorker = new DescribeWorker(application.metadataConnection().get(), application.apiVersion().get());
		describeWorker.setOnSucceeded(es -> {
			application.getPropertiesController().showPropertiesForType(null);
			metadataLists.clear();
			metadataDescription = describeWorker.getValue().getDescription();
			ObservableList<TreeItem<String>> describeAndListTreeItems = descriptionAndListsTree.getRoot().getChildren();
			describeAndListTreeItems.clear();
			for (String metadataTypeName : metadataDescription.keySet()) {
				describeAndListTreeItems.add(new TreeItem<String>(metadataTypeName));
			}
			application.getLogController().log(describeWorker.getValue().getLogHandler());
			
			setDisablesForOperationCompletion();
		});
		
		cancelButton.setOnAction(ec -> {
			describeWorker.cancel();
			handleCancelButtonClicked(ec);
		});
		cancelButton.setDisable(false);
		
		new Thread(describeWorker).start();
	}
	
	private void handleListButtonClicked(ActionEvent e) {
		
		descriptionAndListsTree.setDisable(true);
		listButton.setDisable(true);
		
		// TODO: Record and maintain scroll position
		
		TreeItem<String> selectedItem = descriptionAndListsTree.getSelectionModel().getSelectedItem();
		String selectedTypeName = selectedItem.getValue().toString();
		
		final ListWorker listWorker = new ListWorker(application.metadataConnection().get(), application.apiVersion().get(), selectedTypeName);
		listWorker.setOnSucceeded(es -> {
			SortedMap<String, FileProperties> listResult = listWorker.getValue().getList();
			metadataLists.put(selectedTypeName, listResult);
			if (listResult != null) {
				TreeItem<String> selectedTypeItem = descriptionAndListsTree.getSelectionModel().getSelectedItem();
				selectedTypeItem.getChildren().clear();
				for (String fileName : listResult.keySet()) {
					selectedTypeItem.getChildren().add(new TreeItem<String>(fileName));
				}
				selectedTypeItem.setExpanded(true);
			}
			application.getLogController().log(listWorker.getValue().getLogHandler());
			
			descriptionAndListsTree.getSelectionModel().select(selectedItem);
			
			setDisablesForOperationCompletion();
		});
		
		cancelButton.setOnAction(ec -> {
			listWorker.cancel();
			handleCancelButtonClicked(ec);
		});
		cancelButton.setDisable(false);
		
		new Thread(listWorker).start();
	}
	
	private void handleReadButtonClicked(ActionEvent e) {
		
		// TODO: Disable read and delete buttons
		// TODO: Enable notification that read has completed
		// TODO: Enable read and delete buttons
		
		TreeItem<String> selectedItem = descriptionAndListsTree.getSelectionModel().getSelectedItem();
		if (selectedItem == null) {
			return;
		}
		
		String typeName = selectedItem.getParent().getValue();
		SortedMap<String, FileProperties> fileMap = metadataLists.get(typeName);
		String fileName = selectedItem.getValue();
		FileProperties fp = fileMap.get(fileName);
		
		application.getEditorController().edit(typeName, fp.getFullName());
	}
	
	private void handleDeleteButtonClicked(ActionEvent e) {
		
		descriptionAndListsTree.setDisable(true);
		readButton.setDisable(true);
		deleteButton.setDisable(true);
		
		TreeItem<String> selectedItem = descriptionAndListsTree.getSelectionModel().getSelectedItem();
		if (selectedItem == null) {
			return;
		}
		
		String typeName = selectedItem.getParent().getValue();
		SortedMap<String, FileProperties> fileMap = metadataLists.get(typeName);
		String fileName = selectedItem.getValue();
		FileProperties fp = fileMap.get(fileName);
		
		final DeleteWorker deleteWorker = new DeleteWorker(application.metadataConnection().get(), typeName, fp.getFullName());
		deleteWorker.setOnSucceeded(es -> {
			
			boolean deleted = deleteWorker.getValue().getSuccess();
			if (deleted) {
				application.getEditorController().close(typeName, fp.getFullName());
				// TODO: Set selection somewhere helpful
				selectedItem.getParent().getChildren().remove(selectedItem);
				fileMap.remove(fileName);
			}
			else {
				deleteButton.setDisable(false);
				readButton.setDisable(false);
			}
			
			setDisablesForOperationCompletion();
		});
		
		cancelButton.setOnAction(ec -> {
			deleteWorker.cancel();
			handleCancelButtonClicked(ec);
		});
		cancelButton.setDisable(false);
		
		new Thread(deleteWorker).start();
	}
	
	private void handleCancelButtonClicked(ActionEvent e) {	
		cancelButton.setOnAction(null);
		
		// TODO: Fix this to wait for cancel to complete?
		
		setDisablesForOperationCancellation();
	}
}
