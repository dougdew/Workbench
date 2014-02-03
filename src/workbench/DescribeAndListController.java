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

import com.sforce.soap.metadata.DescribeMetadataObject;
import com.sforce.soap.metadata.DescribeMetadataResult;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
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
	
	private Main application;
	
	private Task<DescribeWorkerResults> describeWorker;
	private Task<ListWorkerResults> listWorker;
	
	private SortedMap<String, DescribeMetadataObject> metadataDescription;
	private Map<String, SortedMap<String, FileProperties>> metadataLists = new TreeMap<>();
	
	private AnchorPane root;
	private HBox treeOperationsBar;
	private TreeView<String> describeAndListTree;
	private TreeItem<String> describeAndListTreeRoot;
	private Button describeButton;
	private Button listButton;
	private Button cancelButton;
	
	public DescribeAndListController(Main application) {
		this.application = application;
		createGraph();
		application.metadataConnection().addListener((o, oldValue, newValue) -> handleMetadataConnectionChanged());
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
		
		cancelButton = new Button("Cancel");
		cancelButton.setDisable(true);
		cancelButton.setOnAction(e -> handleCancelButtonClicked(e));
		treeOperationsBar.getChildren().add(cancelButton);
		
		describeAndListTree = new TreeView<>();
		describeAndListTree.setDisable(true);
		describeAndListTree.setShowRoot(false);
		describeAndListTree.setOnMouseClicked(e -> handleTreeItemClicked(e));
		AnchorPane.setTopAnchor(describeAndListTree, 40.0);
		AnchorPane.setBottomAnchor(describeAndListTree, 0.0);
		AnchorPane.setLeftAnchor(describeAndListTree, 0.0);
		AnchorPane.setRightAnchor(describeAndListTree, 0.0);
		root.getChildren().add(describeAndListTree);
		
		describeAndListTreeRoot = new TreeItem<>("REPLACE WITH ORG ID WHEN LOGGED IN");
		describeAndListTree.setRoot(describeAndListTreeRoot);
		describeAndListTreeRoot.setExpanded(true);
	}
	
	private void handleMetadataConnectionChanged() {
		if (application.enterpriseConnection().get() != null && application.metadataConnection().get() != null) {
			String orgName = application.orgName().get();
			describeAndListTree.getRoot().setValue(orgName);
			describeAndListTree.setShowRoot(true);
			describeAndListTree.setDisable(false);
		}
		else {
			describeAndListTree.setDisable(true);
			describeButton.setDisable(true);
			listButton.setDisable(true);
		}
	}
	
	private void handleTreeItemClicked(MouseEvent e) {
		
		if (e.getClickCount() == 1) {		
			setButtonDisablesForTreeSelection();
		}
	}
	
	private void setButtonDisablesForTreeSelection() {
		
		TreeItem<String> selectedItem = describeAndListTree.getSelectionModel().getSelectedItem();
		if (selectedItem == describeAndListTree.getRoot()) {
			describeButton.setDisable(false);
			listButton.setDisable(true);
		}
		else if (selectedItem.getParent() == describeAndListTree.getRoot()) {
			describeButton.setDisable(true);
			listButton.setDisable(false);
			
			DescribeMetadataObject dmo = metadataDescription.get(selectedItem.getValue());
			application.getPropertiesController().showPropertiesForType(dmo);
		}
		else {
			describeButton.setDisable(true);
			listButton.setDisable(true);
			
			String typeName = selectedItem.getParent().getValue();
			SortedMap<String, FileProperties> fileMap = metadataLists.get(typeName);
			String fileName = selectedItem.getValue();
			FileProperties fp = fileMap.get(fileName);
			application.getPropertiesController().showPropertiesForFile(fp);
			application.getEditorController().edit(typeName, fp.getFullName());
		}
	}
	
	private void handleDescribeButtonClicked(ActionEvent e) {
		
		describeAndListTree.setDisable(true);
		describeButton.setDisable(true);
		cancelButton.setDisable(false);
		
		describeWorker = createDescribeWorker();
		describeWorker.setOnSucceeded(es -> {
			application.getPropertiesController().showPropertiesForType(null);
			metadataLists.clear();
			metadataDescription = describeWorker.getValue().getDescription();
			ObservableList<TreeItem<String>> describeAndListTreeItems = describeAndListTree.getRoot().getChildren();
			describeAndListTreeItems.clear();
			for (String metadataTypeName : metadataDescription.keySet()) {
				describeAndListTreeItems.add(new TreeItem<String>(metadataTypeName));
			}
			application.getLogController().log(describeWorker.getValue().getLogHandler());
			
			describeWorker = null;
			
			cancelButton.setDisable(true);
			describeButton.setDisable(false);
			describeAndListTree.setDisable(false);
		});
		
		new Thread(describeWorker).start();
	}
	
	private void handleListButtonClicked(ActionEvent e) {
		
		describeAndListTree.setDisable(true);
		listButton.setDisable(true);
		cancelButton.setDisable(false);
		
		// TODO: Record and maintain scroll position
		
		TreeItem<String> selectedItem = describeAndListTree.getSelectionModel().getSelectedItem();
		String selectedTypeName = selectedItem.getValue().toString();
		
		listWorker = createListWorker(selectedTypeName);
		listWorker.setOnSucceeded(es -> {
			SortedMap<String, FileProperties> listResult = listWorker.getValue().getList();
			metadataLists.put(selectedTypeName, listResult);
			if (listResult != null) {
				TreeItem<String> selectedTypeItem = describeAndListTree.getSelectionModel().getSelectedItem();
				selectedTypeItem.getChildren().clear();
				for (String fileName : listResult.keySet()) {
					selectedTypeItem.getChildren().add(new TreeItem<String>(fileName));
				}
				selectedTypeItem.setExpanded(true);
			}
			application.getLogController().log(listWorker.getValue().getLogHandler());
			
			listWorker = null;
			
			describeAndListTree.getSelectionModel().select(selectedItem);
			
			cancelButton.setDisable(true);
			listButton.setDisable(false);
			describeAndListTree.setDisable(false);
		});
		
		new Thread(listWorker).start();
	}
	
	private void handleCancelButtonClicked(ActionEvent e) {
		
		cancelButton.setDisable(true);
		
		if (describeWorker != null) {
			describeWorker.cancel(true);
		}
		if (listWorker != null) {
			listWorker.cancel(true);
		}
		
		// TODO: Fix this to wait for cancel to complete
		setButtonDisablesForTreeSelection();
	}
	
	private Task<DescribeWorkerResults> createDescribeWorker() {
		
		return new Task<DescribeWorkerResults>() {
			
			@Override
			protected DescribeWorkerResults call() throws Exception {
				
				DescribeWorkerResults workerResults = new DescribeWorkerResults();
				
				try {
					SOAPLogHandler logHandler = new SOAPLogHandler("DESCRIBE");
					application.metadataConnection().get().getConfig().addMessageHandler(logHandler);
					workerResults.setLogHandler(logHandler);
					
					double apiVersion = application.apiVersion().get();
					DescribeMetadataResult mdapiDescribe = application.metadataConnection().get().describeMetadata(apiVersion);
					
					SortedMap<String, DescribeMetadataObject> describeResult = new TreeMap<>();
					for (DescribeMetadataObject dmo : mdapiDescribe.getMetadataObjects()) {
						describeResult.put(dmo.getXmlName(), dmo);
					}
					workerResults.setDescription(describeResult);
					
					application.metadataConnection().get().getConfig().clearMessageHandlers();
				}
				catch (ConnectionException e) {
					// TODO: Fix this
					e.printStackTrace();
				}
				return workerResults;
			}
		};
	}
	
	private Task<ListWorkerResults> createListWorker(String typeName) {
		
		return new Task<ListWorkerResults>() {
			
			@Override
			protected ListWorkerResults call() throws Exception {
				
				ListWorkerResults workerResults = new ListWorkerResults();
				
				try {
					SOAPLogHandler logHandler = new SOAPLogHandler("LIST");
					application.metadataConnection().get().getConfig().addMessageHandler(logHandler);
					workerResults.setLogHandler(logHandler);
					
					SortedMap<String, FileProperties> listResult = new TreeMap<>();
					ListMetadataQuery query = new ListMetadataQuery();
					query.setType(typeName);
					
					double apiVersion = application.apiVersion().get();
					FileProperties[] mdapiList = application.metadataConnection().get().listMetadata(new ListMetadataQuery[]{query}, apiVersion);
					
					for (FileProperties fp : mdapiList) {
						listResult.put(fp.getFileName(), fp);
					}
					workerResults.setList(listResult);
					
					application.metadataConnection().get().getConfig().clearMessageHandlers();
				}
				catch (ConnectionException e) {
					// TODO: Fix this
					e.printStackTrace();
				}
				
				return workerResults;
			}
		};
	}
}
