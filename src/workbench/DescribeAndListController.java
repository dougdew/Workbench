package workbench;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
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
	
	private AnchorPane root;
	private final TreeView<String> describeAndListTree = new TreeView<>();
	private final Button describeStartButton = new Button("Describe");
	private final Button listStartButton = new Button("List");
	private final Button describeAndListCancelButton = new Button("Cancel");
	
	private Task<DescribeWorkerResults> describeWorker;
	private Task<ListWorkerResults> listWorker;
	
	private SortedMap<String, DescribeMetadataObject> metadataDescription;
	private Map<String, SortedMap<String, FileProperties>> metadataLists = new TreeMap<>();
	
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
		
		HBox treeOperationsBar = new HBox();
		treeOperationsBar.setAlignment(Pos.BASELINE_CENTER);
		AnchorPane.setTopAnchor(treeOperationsBar, 6.0);
		AnchorPane.setLeftAnchor(treeOperationsBar, 0.0);
		AnchorPane.setRightAnchor(treeOperationsBar, 0.0);
		root.getChildren().add(treeOperationsBar);
			
		describeStartButton.setDisable(true);
		treeOperationsBar.getChildren().add(describeStartButton);
		
		listStartButton.setDisable(true);
		treeOperationsBar.getChildren().add(listStartButton);
		
		describeAndListCancelButton.setDisable(true);
		treeOperationsBar.getChildren().add(describeAndListCancelButton);
		
		describeAndListTree.setShowRoot(false);
		AnchorPane.setTopAnchor(describeAndListTree, 40.0);
		AnchorPane.setBottomAnchor(describeAndListTree, 0.0);
		AnchorPane.setLeftAnchor(describeAndListTree, 0.0);
		AnchorPane.setRightAnchor(describeAndListTree, 0.0);
		root.getChildren().add(describeAndListTree);
		
		TreeItem<String> describeAndListTreeRoot = new TreeItem<>("Describe And List Tree Root");
		describeAndListTree.setRoot(describeAndListTreeRoot);
		describeAndListTreeRoot.setExpanded(true);
		
		describeAndListTree.setOnMouseClicked(e -> {
			if (e.getClickCount() == 1) {
				
				TreeItem<String> selectedItem = describeAndListTree.getSelectionModel().getSelectedItem();
				if (selectedItem.getParent() == describeAndListTree.getRoot()) {
					listStartButton.setDisable(false);
					
					DescribeMetadataObject dmo = metadataDescription.get(selectedItem.getValue());
					application.getPropertiesController().showPropertiesForType(dmo);
				}
				else {
					listStartButton.setDisable(true);
					
					String typeName = selectedItem.getParent().getValue();
					SortedMap<String, FileProperties> fileMap = metadataLists.get(typeName);
					String fileName = selectedItem.getValue();
					FileProperties fp = fileMap.get(fileName);
					application.getPropertiesController().showPropertiesForFile(fp);
					application.getEditorController().edit(typeName, fp.getFullName());
				}
			}
		});
		
		describeStartButton.setOnAction(e -> {
			
			describeStartButton.setDisable(true);
			listStartButton.setDisable(true);
			describeAndListCancelButton.setDisable(false);
			
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
				describeAndListCancelButton.setDisable(true);
				describeStartButton.setDisable(false);
			});
			
			new Thread(describeWorker).start();
		});
		
		listStartButton.setOnAction(e -> {
			
			listStartButton.setDisable(true);
			describeStartButton.setDisable(true);
			describeAndListCancelButton.setDisable(false);
			
			// TODO: Check that tree selection is appropriate
			// TODO: Fix this as it assumes that only types can be selected, not instances.
			String selectedTypeName = describeAndListTree.getSelectionModel().getSelectedItem().getValue().toString();
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
				describeAndListCancelButton.setDisable(true);
				listStartButton.setDisable(false);
				describeStartButton.setDisable(false);
			});
			
			new Thread(listWorker).start();
		});
		
		describeAndListCancelButton.setOnAction(e -> {
			
			describeAndListCancelButton.setDisable(true);
			// TODO: Fix this to work with describe and list
			describeWorker.cancel(true);
			// TODO: Fix this to wait for cancel to complete
			describeStartButton.setDisable(false);
			listStartButton.setDisable(false);
		});
	}
	
	private void handleMetadataConnectionChanged() {
		if (application.metadataConnection().get() != null) {
			// TODO: Fix this. There should just be flags indicating that a describe or list operation is in progress
			if (describeStartButton.isDisabled() && listStartButton.isDisabled() && describeAndListCancelButton.isDisabled()) {
				describeStartButton.setDisable(false);
				//listStartButton.setDisable(false);
			}
		}
		else {
			describeStartButton.setDisable(true);
			listStartButton.setDisable(true);
		}
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
