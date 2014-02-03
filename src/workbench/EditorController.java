package workbench;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;

import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.ws.ConnectionException;

import workbench.editor.Editor;
import workbench.editor.EditorFactory;

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
		private Editor editor;
		private Metadata metadata;
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
		
		public Editor getEditor() {
			return editor;
		}
		public void setEditor(Editor editor) {
			this.editor = editor;
		}
		
		public Metadata getMetadata() {
			return metadata;
		}
		public void setMetadata(Metadata metadata) {
			this.metadata = metadata;
		}
		
		public Task<ReadWorkerResults> getReadWorker() {
			return readWorker;
		}
		public void setReadWorker(Task<ReadWorkerResults> readWorker) {
			this.readWorker = readWorker;
		}
	}
	
	private Main application;
	
	private Map<String, FileController> fileControllers = new HashMap<String, FileController>();
	
	private AnchorPane root;
	private Button createButton;
	private Button readButton;
	private Button updateButton;
	private Button deleteButton;
	private Button cancelButton;
	private TabPane tabPane;
	
	public EditorController(Main application) {
		this.application = application;
		createGraph();
		application.metadataConnection().addListener((o, oldValue, newValue) -> handleMetadataConnectionChanged());
	}

	public Node getRoot() {
		return root;
	}
	
	public void closeAll() {
		Iterator<Tab> tabIter = tabPane.getTabs().iterator();
		while (tabIter.hasNext()) {
			Tab tab = tabIter.next();
			tabIter.remove();
			EventHandler<Event> onClosedHandler = tab.getOnClosed();
			if (onClosedHandler != null) {
				onClosedHandler.handle(null);
			}
		}
	}
	
	public void edit(String type, String fullName) {
		
		String typeQualifiedName = type + ":" + fullName;
		
		if (fileControllers.get(typeQualifiedName) != null) {
			tabPane.getSelectionModel().select(fileControllers.get(typeQualifiedName).getTab());
			return;
		}
		
		final FileController fileController = new FileController();
		fileController.setType(type);
		fileController.setFullName(fullName);
		
		Tab tab = new Tab();
		tab.setText(typeQualifiedName);
		tab.setOnClosed(e -> {
			fileControllers.remove(tab.getText());
			if (fileControllers.size() == 0) {
				readButton.setDisable(true);
			}
		});
		fileController.setTab(tab);
		
		final Editor editor = EditorFactory.createEditor(type);
		tab.setContent(editor.getRoot());
		fileController.setEditor(editor);
		
		Task<ReadWorkerResults> readWorker = createReadWorker(type, fullName);
		readWorker.setOnSucceeded(e -> {
			Metadata m = readWorker.getValue().getMetadata();
			fileController.setMetadata(m);
			editor.setMetadata(m);
			application.getLogController().log(readWorker.getValue().getLogHandler());
			cancelButton.setDisable(true);
			readButton.setDisable(false);
		});	
		fileController.setReadWorker(readWorker);
		
		fileControllers.put(typeQualifiedName, fileController);
		
		tabPane.getTabs().add(tab);
		tabPane.getSelectionModel().select(tab);
		
		readButton.setDisable(true);
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
		
		createButton = new Button("Create");
		createButton.setDisable(true);
		editorOperationsBar.getChildren().add(createButton);
		
		readButton = new Button("Read");
		readButton.setDisable(true);
		readButton.setOnAction(e -> handleReadButtonClicked(e));
		editorOperationsBar.getChildren().add(readButton);
		
		updateButton = new Button("Update");
		updateButton.setDisable(true);
		editorOperationsBar.getChildren().add(updateButton);
		
		deleteButton = new Button("Delete");
		deleteButton.setDisable(true);
		editorOperationsBar.getChildren().add(deleteButton);
		
		cancelButton = new Button("Cancel");
		cancelButton.setDisable(true);
		cancelButton.setOnAction(e -> handleCancelButtonClicked(e));
		editorOperationsBar.getChildren().add(cancelButton);
		
		tabPane = new TabPane();
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
			createButton.setDisable(true);
			readButton.setDisable(true);
			updateButton.setDisable(true);
		}
	}
	
	private void handleReadButtonClicked(ActionEvent e) {
		
		String typeQualifiedName = tabPane.getSelectionModel().getSelectedItem().getText();
		FileController fileController = fileControllers.get(typeQualifiedName);
		Task<ReadWorkerResults> readWorker = createReadWorker(fileController.getType(), fileController.getFullName());
		readWorker.setOnSucceeded(es -> {
			Metadata m = readWorker.getValue().getMetadata();
			Editor editor = fileController.getEditor();
			editor.setMetadata(m);
			application.getLogController().log(readWorker.getValue().getLogHandler());
			cancelButton.setDisable(true);
			readButton.setDisable(false);
		});	
		fileController.setReadWorker(readWorker);
		
		readButton.setDisable(true);
		cancelButton.setDisable(false);
		
		new Thread(readWorker).start();
	}
	
	private void handleCancelButtonClicked(ActionEvent e) {
		
		cancelButton.setDisable(true);
		String typeQualifiedName = tabPane.getSelectionModel().getSelectedItem().getText();
		FileController fileController = fileControllers.get(typeQualifiedName);
		fileController.getReadWorker().cancel();
		readButton.setDisable(false);
	}
	
	private Task<ReadWorkerResults> createReadWorker(String type, String fullName) {
		
		Task<ReadWorkerResults> worker = new Task<ReadWorkerResults>() {
			
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
		
		return worker;
	}
}
