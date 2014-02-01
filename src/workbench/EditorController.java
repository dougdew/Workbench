package workbench;

import java.util.HashMap;
import java.util.Map;

import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;

import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.ws.ConnectionException;

public class EditorController {
	
	private static class ReadWorkerResults {
		
		private SOAPLogHandler logHandler;
		private Metadata metadata;
		
		public void setLogHandler(SOAPLogHandler logHandler) {
			this.logHandler = logHandler;
		}
		public SOAPLogHandler getLogHandler() {
			return logHandler;
		}
		
		public void setMetadata(Metadata metadata) {
			this.metadata = metadata;
		}
		public Metadata getMetadata() {
			return metadata;
		}
	}
	
	private static class FileController {
		
		private String type;
		private String fullName;
		private Tab tab;
		private TextArea textArea;
		private Task<ReadWorkerResults> readWorker;
		
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		
		public String getFullName() {
			return fullName;
		}
		public void setFullName(String fullName) {
			this.fullName = fullName;
		}
		
		public Tab getTab() {
			return tab;
		}
		public void setTab(Tab tab) {
			this.tab = tab;
		}
		
		public TextArea getTextArea() {
			return textArea;
		}
		public void setTextArea(TextArea textArea) {
			this.textArea = textArea;
		}
		
		public Task<ReadWorkerResults> getReadWorker() {
			return readWorker;
		}
		public void setReadWorker(Task<ReadWorkerResults> readWorker) {
			this.readWorker = readWorker;
		}
	}
	
	private Main application;
	
	private AnchorPane root;
	private final Button refreshButton = new Button("Refresh");
	private final Button cancelButton = new Button("Cancel");
	private final TabPane tabPane = new TabPane();
	
	private Map<String, FileController> fileControllers = new HashMap<String, FileController>();
	
	public EditorController(Main application) {
		this.application = application;
		createGraph();
		application.metadataConnection().addListener((o, oldValue, newValue) -> handleMetadataConnectionChanged());
	}

	public Node getRoot() {
		return root;
	}
	
	public void edit(String type, String fullName) {
		
		String documentName = type + ":" + fullName;
		
		FileController fileController = fileControllers.get(documentName);
		if(fileController != null) {
			tabPane.getSelectionModel().select(fileController.getTab());
			return;
		}
		
		fileController = new FileController();
		fileController.setType(type);
		fileController.setFullName(fullName);
		
		Tab tab = new Tab();
		tab.setText(documentName);
		tab.setOnClosed(e -> {
			fileControllers.remove(tab.getText());
			if (fileControllers.size() == 0) {
				refreshButton.setDisable(true);
			}
		});
		fileController.setTab(tab);
		
		final TextArea textArea = new TextArea();
		textArea.setEditable(false);
		tab.setContent(textArea);
		fileController.setTextArea(textArea);
		
		Task<ReadWorkerResults> readWorker = createReadWorker(type, fullName);
		readWorker.setOnSucceeded(e -> {
			Metadata m = readWorker.getValue().getMetadata();
			if (m != null) {
				textArea.appendText(m.getClass().getName());
			}
			else {
				textArea.appendText("Unable to read");
			}
			application.getLogController().log(readWorker.getValue().getLogHandler());
			cancelButton.setDisable(true);
			refreshButton.setDisable(false);
		});	
		fileController.setReadWorker(readWorker);
		
		fileControllers.put(documentName, fileController);
		
		tabPane.getTabs().add(tab);
		tabPane.getSelectionModel().select(tab);
		
		refreshButton.setDisable(true);
		cancelButton.setDisable(false);
		
		new Thread(readWorker).start();
	}
	
	private void createGraph() {
		
		root = new AnchorPane();
		
		HBox editorOperationsBar = new HBox();
		editorOperationsBar.setAlignment(Pos.BASELINE_CENTER);
		AnchorPane.setTopAnchor(editorOperationsBar, 6.0);
		AnchorPane.setLeftAnchor(editorOperationsBar, 0.0);
		AnchorPane.setRightAnchor(editorOperationsBar, 0.0);
		root.getChildren().add(editorOperationsBar);
		
		refreshButton.setDisable(true);
		refreshButton.setOnAction(e -> {
			String documentName = tabPane.getSelectionModel().getSelectedItem().getText();
			FileController fileController = fileControllers.get(documentName);
			Task<ReadWorkerResults> readWorker = createReadWorker(fileController.getType(), fileController.getFullName());
			readWorker.setOnSucceeded(es -> {
				Metadata m = readWorker.getValue().getMetadata();
				TextArea textArea = fileController.getTextArea();
				textArea.clear();
				if (m != null) {
					textArea.appendText(m.getClass().getName());
				}
				else {
					textArea.appendText("Unable to read");
				}
				application.getLogController().log(readWorker.getValue().getLogHandler());
				cancelButton.setDisable(true);
				refreshButton.setDisable(false);
			});	
			fileController.setReadWorker(readWorker);
			
			refreshButton.setDisable(true);
			cancelButton.setDisable(false);
			
			new Thread(readWorker).start();
		});
		editorOperationsBar.getChildren().add(refreshButton);
		
		cancelButton.setDisable(true);
		cancelButton.setOnAction(e -> {
			cancelButton.setDisable(true);
			String documentName = tabPane.getSelectionModel().getSelectedItem().getText();
			FileController fileController = fileControllers.get(documentName);
			fileController.getReadWorker().cancel();
			refreshButton.setDisable(false);
		});
		editorOperationsBar.getChildren().add(cancelButton);
		
		tabPane.setSide(Side.BOTTOM);
		AnchorPane.setTopAnchor(tabPane, 40.0);
		AnchorPane.setBottomAnchor(tabPane, 0.0);
		AnchorPane.setLeftAnchor(tabPane, 0.0);
		AnchorPane.setRightAnchor(tabPane, 0.0);
		root.getChildren().add(tabPane);
	}
	
	private void handleMetadataConnectionChanged() {
		
		if (application.metadataConnection().get() != null) {
		}
		else {
			// TODO: This matters
		}
	}
	
	private Task<ReadWorkerResults> createReadWorker(String type, String fullName) {
		
		return new Task<ReadWorkerResults>() {
			
			@Override
			protected ReadWorkerResults call() throws Exception {
				
				ReadWorkerResults results = new ReadWorkerResults();
				
				try {
					MetadataConnection conn = application.metadataConnection().get();
					SOAPLogHandler logHandler = new SOAPLogHandler("READ: " + fullName);
					conn.getConfig().addMessageHandler(logHandler);
					results.setLogHandler(logHandler);
					
					Metadata[] records = conn.readMetadata(type, new String[]{fullName}).getRecords();
					if (records != null && records.length == 1) {
						results.setMetadata(records[0]);
					}
					conn.getConfig().clearMessageHandlers();
				}
				catch (ConnectionException e) {
					e.printStackTrace();
				}
				
				return results;
			}
		};
	}
}
