package workbench;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
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
import com.sforce.soap.metadata.SaveResult;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.bind.TypeInfo;
import com.sforce.ws.bind.TypeMapper;
import com.sforce.ws.parser.PullParserException;
import com.sforce.ws.parser.XmlInputStream;
import com.sforce.ws.wsdl.Constants;

import workbench.editor.Editor;
import workbench.editor.EditorFactory;

public class EditorController {
	
	private static class CreateWorkerResults {
		
		private SOAPLogHandler logHandler;
		private Metadata metadata;
		private boolean success;
		
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
		
		public void setSuccess(boolean success) {
			this.success = success;
		}
		public boolean getSuccess() {
			return success;
		}
	}
	
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
	
	private static class UpdateWorkerResults {
		
		private SOAPLogHandler logHandler;
		boolean success;
		
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
	
	private static class FileController {
		
		private String type;
		private String fullName;
		private Tab tab;
		private Editor editor;
		private Metadata metadata;
		private Task<ReadWorkerResults> readWorker;
		private Task<CreateWorkerResults> createWorker;
		private Task<UpdateWorkerResults> updateWorker;
		
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
		
		public Task<CreateWorkerResults> getCreateWorker() {
			return createWorker;
		}
		public void setCreateWorker(Task<CreateWorkerResults> createWorker) {
			this.createWorker = createWorker;
		}
		
		public Task<UpdateWorkerResults> getUpdateWorker() {
			return updateWorker;
		}
		public void setUpdateWorker(Task<UpdateWorkerResults> updateWorker) {
			this.updateWorker = updateWorker;
		}
		
		public boolean isNew() {
			return metadata == null;
		}
	}
	
	private static int newFileCounter = 1;
	
	private Main application;
	
	private Map<String, FileController> fileControllers = new HashMap<String, FileController>();
	
	private AnchorPane root;
	private Button newButton;
	private Button createButton;
	private Button updateButton;
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
		
		// TODO: Special handling for zero remaining file controllers and tabs
	}
	
	public void edit(String type, String fullName) {
		
		String typeQualifiedName = createTypeQualifiedName(type, fullName);
		
		if (fileControllers.get(typeQualifiedName) != null) {
			tabPane.getSelectionModel().select(fileControllers.get(typeQualifiedName).getTab());
			return;
		}
		
		final FileController fileController = new FileController();
		fileController.setType(type);
		fileController.setFullName(fullName);
		
		Tab tab = new Tab();
		tab.setText(typeQualifiedName);
		tab.setOnSelectionChanged(e -> setButtonDisablesForSelectedTab());
		tab.setOnClosed(e -> {
			fileControllers.remove(typeQualifiedName);
			setButtonDisablesForSelectedTab();
		});
		fileController.setTab(tab);
		
		final Editor editor = EditorFactory.createEditor(type);
		editor.dirty().addListener((o, oldValue, newValue) -> setButtonDisablesForSelectedTab());
		tab.setContent(editor.getRoot());
		fileController.setEditor(editor);
		
		Task<ReadWorkerResults> readWorker = createReadWorker(type, fullName);
		readWorker.setOnSucceeded(e -> {
			fileController.setReadWorker(null);
			Metadata m = readWorker.getValue().getMetadata();
			fileController.setMetadata(m);
			editor.setMetadata(m);
			application.getLogController().log(readWorker.getValue().getLogHandler());
			cancelButton.setDisable(true);
		});	
		fileController.setReadWorker(readWorker);
		
		fileControllers.put(typeQualifiedName, fileController);
		
		tabPane.getTabs().add(tab);
		tabPane.getSelectionModel().select(tab);
		
		setButtonDisablesForSelectedTab();
		
		cancelButton.setDisable(false);
		
		new Thread(readWorker).start();
	}
	
	public void close(String type, String fullName) {	
		
		String typeQualifiedName = createTypeQualifiedName(type, fullName);
		FileController fileController = fileControllers.get(typeQualifiedName);
		if (fileController == null) {
			return;
		}
		
		tabPane.getTabs().remove(fileController.getTab());
		fileControllers.remove(typeQualifiedName);
		
		setButtonDisablesForSelectedTab();
		
		// TODO: Special handling for zero remaining file controllers and tabs
	}
	
	private void createGraph() {
		
		root = new AnchorPane();
		
		HBox editorOperationsBar = new HBox();
		editorOperationsBar.setAlignment(Pos.BASELINE_CENTER);
		AnchorPane.setTopAnchor(editorOperationsBar, 6.0);
		AnchorPane.setLeftAnchor(editorOperationsBar, 0.0);
		AnchorPane.setRightAnchor(editorOperationsBar, 0.0);
		root.getChildren().add(editorOperationsBar);
		
		newButton = new Button("New");
		newButton.setOnAction(e -> handleNewButtonClicked(e));
		editorOperationsBar.getChildren().add(newButton);
		
		createButton = new Button("Create");
		createButton.setDisable(true);
		createButton.setOnAction(e -> handleCreateButtonClicked(e));
		editorOperationsBar.getChildren().add(createButton);
		
		updateButton = new Button("Update");
		updateButton.setDisable(true);
		updateButton.setOnAction(e -> handleUpdateButtonClicked(e));
		editorOperationsBar.getChildren().add(updateButton);
		
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
			updateButton.setDisable(true);
		}
	}
	
	private void handleNewButtonClicked(ActionEvent e) {
		
		String newFileName = "New " + newFileCounter;
		newFileCounter++;
		
		final FileController fileController = new FileController();
		
		Tab tab = new Tab();
		tab.setText(newFileName);
		tab.setOnSelectionChanged(es -> setButtonDisablesForSelectedTab());
		tab.setOnClosed(ec -> {
			// TODO: Remember to add logic to change the handler to use real file name after
			// create is completed.
			fileControllers.remove(newFileName);
			setButtonDisablesForSelectedTab();
		});
		fileController.setTab(tab);
		
		// TODO: Enable editor factory to handle null type
		final Editor editor = EditorFactory.createEditor(null);
		editor.dirty().addListener((o, oldValue, newValue) -> setButtonDisablesForSelectedTab());
		tab.setContent(editor.getRoot());
		fileController.setEditor(editor);
		
		fileControllers.put(newFileName, fileController);
		
		tabPane.getTabs().add(tab);
		tabPane.getSelectionModel().select(tab);
		
		setButtonDisablesForSelectedTab();
	}
	
	private void handleCreateButtonClicked(ActionEvent e) {
		
		if (application.metadataConnection().get() == null) {
			return;
		}
		
		createButton.setDisable(true);
		updateButton.setDisable(true);
		
		String newFileName = tabPane.getSelectionModel().getSelectedItem().getText();
		FileController fileController = fileControllers.get(newFileName);
		Editor editor = fileController.getEditor();
		
		if (!fileController.isNew() || !editor.dirty().get()) {
			return;
		}
		
		editor.lock();
		String xml = editor.getMetadataAsXml();
		
		Task<CreateWorkerResults> createWorker = createCreateWorker(xml);
		createWorker.setOnSucceeded(es -> {
			fileController.setCreateWorker(null);
			application.getLogController().log(createWorker.getValue().getLogHandler());
			cancelButton.setDisable(true);
			boolean created = createWorker.getValue().getSuccess();
			if (created) {
				fileControllers.remove(newFileName);
				Metadata metadata = createWorker.getValue().getMetadata();
				String typeQualifiedName = createTypeQualifiedName(metadata.getClass().getSimpleName(), metadata.getFullName());
				fileController.getTab().setText(typeQualifiedName);
				fileControllers.put(typeQualifiedName, fileController);
				editor.setMetadata(metadata);
			}
			editor.unlock();
			setButtonDisablesForSelectedTab();
		});
		fileController.setCreateWorker(createWorker);
		
		cancelButton.setDisable(false);
		
		new Thread(createWorker).start();
	}
	
	private void handleUpdateButtonClicked(ActionEvent e) {
		
		if (application.metadataConnection().get() == null) {
			return;
		}
		
		createButton.setDisable(true);
		updateButton.setDisable(true);
		
		String typeQualifiedName = tabPane.getSelectionModel().getSelectedItem().getText();
		FileController fileController = fileControllers.get(typeQualifiedName);
		Editor editor = fileController.getEditor();
		
		if (!editor.dirty().get()) {
			return;
		}
		
		editor.lock();
		Metadata m = editor.getMetadata();
		
		Task<UpdateWorkerResults> updateWorker = createUpdateWorker(m);
		updateWorker.setOnSucceeded(es -> {
			fileController.setUpdateWorker(null);
			application.getLogController().log(updateWorker.getValue().getLogHandler());
			cancelButton.setDisable(true);
			boolean updated = updateWorker.getValue().getSuccess();
			if (updated) {
				editor.dirty().set(false);
			}
			editor.unlock();
			setButtonDisablesForSelectedTab();
		});
		fileController.setUpdateWorker(updateWorker);
		
		cancelButton.setDisable(false);
		
		new Thread(updateWorker).start();
	}
	
	private void handleCancelButtonClicked(ActionEvent e) {
		
		cancelButton.setDisable(true);
		String fileName = tabPane.getSelectionModel().getSelectedItem().getText();
		FileController fileController = fileControllers.get(fileName);
		if (fileController.getReadWorker() != null) {
			fileController.getReadWorker().cancel();
		}
		if (fileController.getCreateWorker() != null) {
			fileController.getCreateWorker().cancel();
		}
		if (fileController.getUpdateWorker() != null) {
			fileController.getUpdateWorker().cancel();
		}
	}
	
	private void setButtonDisablesForSelectedTab() {
		
		if (tabPane.getTabs().size() == 0) {
			updateButton.setDisable(true);
		}
		else {
			boolean connected = application.metadataConnection().get() != null;
			
			Tab tab = tabPane.getSelectionModel().getSelectedItem();
			String fileName = tab.getText();
			FileController fileController = fileControllers.get(fileName);
			Editor editor = fileController.getEditor();
			if (fileController.isNew()) {
				if (editor.dirty().get() && connected) {
					createButton.setDisable(false);
				}
				else {
					createButton.setDisable(true);
				}
				updateButton.setDisable(true);
			}
			else {
				if (editor.dirty().get() && connected) {
					updateButton.setDisable(false);
				}
				else {
					updateButton.setDisable(true);
				}
				createButton.setDisable(true);
			}
		}
	}
	
	private String createTypeQualifiedName(String type, String fullName) {
		return type + ":" + fullName;
	}
	
	private Task<CreateWorkerResults> createCreateWorker(String xml) {
		
		Task<CreateWorkerResults> worker = new Task<CreateWorkerResults>() {
			
			@Override
			protected CreateWorkerResults call() throws Exception {
			
				CreateWorkerResults results = new CreateWorkerResults();
				
				try {
					XmlInputStream xin = new XmlInputStream();
					xin.setInput(new ByteArrayInputStream(xml.getBytes(Charset.forName("UTF-8"))), "UTF-8");
					
					TypeMapper mapper = new TypeMapper();
					TypeInfo typeInfo = new TypeInfo(Main.getMetadataNamespace(), 
												     "Metadata", 
												     Main.getMetadataNamespace(), 
												     "Metadata", 
												     0, 
												     1, 
												     true);
					xin.peekTag();
					Metadata metadata = (Metadata)mapper.readObject(xin, typeInfo, Metadata.class);
					results.setMetadata(metadata);
					
					MetadataConnection conn = application.metadataConnection().get();
					SOAPLogHandler logHandler = new SOAPLogHandler("CREATE: ");
					conn.getConfig().addMessageHandler(logHandler);
					results.setLogHandler(logHandler);
					
					SaveResult[] mdapiCreate = conn.createMetadata(new Metadata[]{metadata});
					if (mdapiCreate != null && mdapiCreate.length == 1) {
						results.setSuccess(mdapiCreate[0].isSuccess());
						// TODO: Add error reporting
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
	
	private Task<UpdateWorkerResults> createUpdateWorker(Metadata m) {
		
		Task<UpdateWorkerResults> worker = new Task<UpdateWorkerResults>() {
			
			@Override
			protected UpdateWorkerResults call() throws Exception {
				
				UpdateWorkerResults results = new UpdateWorkerResults();
				
				try {
					MetadataConnection conn = application.metadataConnection().get();
					SOAPLogHandler logHandler = new SOAPLogHandler("UPDATE: " + m.getFullName());
					conn.getConfig().addMessageHandler(logHandler);
					results.setLogHandler(logHandler);
					
					SaveResult[] mdapiUpdate = conn.updateMetadata(new Metadata[]{m});
					if (mdapiUpdate != null && mdapiUpdate.length == 1) {
						results.setSuccess(mdapiUpdate[0].isSuccess());
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
