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
	
	private static class ReadWorkerResult {
		
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
	
	private static class Document {
		
		private String type;
		private String fullName;
		private Tab tab;
		private TextArea textArea;
		private Task<ReadWorkerResult> readWorker;
		
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
		
		public Task<ReadWorkerResult> getReadWorker() {
			return readWorker;
		}
		public void setReadWorker(Task<ReadWorkerResult> readWorker) {
			this.readWorker = readWorker;
		}
	}
	
	private Main application;
	
	private AnchorPane root;
	private final Button refreshButton = new Button("Refresh");
	private final Button cancelButton = new Button("Cancel");
	private final TabPane tabPane = new TabPane();
	
	private Map<String, Document> documents = new HashMap<String, Document>();
	
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
		
		Document document = documents.get(documentName);
		if(document != null) {
			tabPane.getSelectionModel().select(document.getTab());
			return;
		}
		
		document = new Document();
		document.setType(type);
		document.setFullName(fullName);
		
		Tab tab = new Tab();
		tab.setText(documentName);
		tab.setOnClosed(e -> {
			documents.remove(tab.getText());
			if (documents.size() == 0) {
				refreshButton.setDisable(true);
			}
		});
		document.setTab(tab);
		
		final TextArea textArea = new TextArea();
		textArea.setEditable(false);
		tab.setContent(textArea);
		document.setTextArea(textArea);
		
		Task<ReadWorkerResult> readWorker = createReadWorker(type, fullName);
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
		document.setReadWorker(readWorker);
		
		documents.put(documentName, document);
		
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
			Document document = documents.get(documentName);
			Task<ReadWorkerResult> readWorker = createReadWorker(document.getType(), document.getFullName());
			readWorker.setOnSucceeded(es -> {
				Metadata m = readWorker.getValue().getMetadata();
				TextArea textArea = document.getTextArea();
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
			document.setReadWorker(readWorker);
			
			refreshButton.setDisable(true);
			cancelButton.setDisable(false);
			
			new Thread(readWorker).start();
		});
		editorOperationsBar.getChildren().add(refreshButton);
		
		cancelButton.setDisable(true);
		cancelButton.setOnAction(e -> {
			cancelButton.setDisable(true);
			String documentName = tabPane.getSelectionModel().getSelectedItem().getText();
			Document document = documents.get(documentName);
			document.getReadWorker().cancel();
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
	
	private Task<ReadWorkerResult> createReadWorker(String type, String fullName) {
		
		return new Task<ReadWorkerResult>() {
			
			@Override
			protected ReadWorkerResult call() throws Exception {
				
				ReadWorkerResult result = new ReadWorkerResult();
				
				try {
					MetadataConnection conn = application.metadataConnection().get();
					SOAPLogHandler logHandler = new SOAPLogHandler("READ: " + fullName);
					conn.getConfig().addMessageHandler(logHandler);
					result.setLogHandler(logHandler);
					Metadata[] records = conn.readMetadata(type, new String[]{fullName}).getRecords();
					if (records != null && records.length == 1) {
						result.setMetadata(records[0]);
					}
					conn.getConfig().clearMessageHandlers();
				}
				catch (ConnectionException e) {
					e.printStackTrace();
				}
				
				return result;
			}
		};
	}
}
