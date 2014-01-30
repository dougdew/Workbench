package workbench;

import java.util.HashMap;
import java.util.Map;

import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.LoginResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

import workbench.LogController.LogHandler;

public class LoginController {
	
	private static class LoginWorkerResults {
		
		private LogHandler logHandler;
		private boolean success;
		private EnterpriseConnection eConnection;
		private MetadataConnection mConnection;
		
		public LogHandler getLogHandler() {
			return logHandler;
		}
		public void setLogHandler(LogHandler logHandler) {
			this.logHandler = logHandler;
		}
		
		public boolean isSuccess() {
			return success;
		}
		public void setSuccess(boolean success) {
			this.success = success;
		}
		
		public EnterpriseConnection getEnterpriseConnection() {
			return eConnection;
		}
		public void setEnterpriseConnection(EnterpriseConnection eConnection) {
			this.eConnection = eConnection;
		}
		
		public MetadataConnection getMetadataConnection() {
			return mConnection;
		}
		public void setMetadataConnection(MetadataConnection mConnection) {
			this.mConnection = mConnection;
		}
	}
	
	private static final Map<String, String> SERVERS;
	static {
		SERVERS = new HashMap<String, String>();
		SERVERS.put("localhost", "http://ddew-wsl.internal.salesforce.com:8080/services/Soap/c/");
		SERVERS.put("Blitz01 NA1", "https://na1-blitz01.soma.salesforce.com/services/Soap/u/");
		SERVERS.put("Blitz01 NA9", "https://na9-blitz01.soma.salesforce.com/services/Soap/u/");
		SERVERS.put("Blitz01 AP1", "https://ap1-blitz01.soma.salesforce.com/services/Soap/u/");
		SERVERS.put("Blitz01 CS0", "https://cs0-blitz01.soma.salesforce.com/services/Soap/u/");
		SERVERS.put("Blitz02 NA1", "https://na1-blitz02.soma.salesforce.com/services/Soap/u/");
		SERVERS.put("Blitz02 AP1", "https://ap1-blitz02.soma.salesforce.com/services/Soap/u/");
		SERVERS.put("Blitz02 CS0", "https://cs0-blitz02.soma.salesforce.com/services/Soap/u/");
	}
	
	private static final String[] VERSIONS = {
		"31.0",
		"30.0",
		"29.0",
		"28.0",
		"27.0",
		"26.0",
		"25.0",
		"24.0",
		"23.0",
		"22.0",
		"21.0",
		"20.0",
		"19.0",
		"18.0",
		"17.0",
		"16.0",
		"15.0",
		"14.0",
		"13.0",
		"12.0",
		"11.0",
		"10.0",
		"9.0",
		"8.0"
	};
	
	private Main application;
	private HBox root;
	private Rectangle loginStatus;
	
	private Task<LoginWorkerResults> loginWorker;

	public LoginController(Main application) {
		this.application = application;
		createGraph();
		application.enterpriseConnection().addListener((o, oldValue, newValue) -> handleEnterpriseConnectionChanged());
		application.metadataConnection().addListener((o, oldValue, newValue) -> handleMetadataConnectionChanged());
	}
	
	public Node getRoot() {
		return root;
	}
	
	public void logout() {
		
		try {
			if (application.enterpriseConnection().get() != null) {
				application.enterpriseConnection().get().logout();
			}		
			application.enterpriseConnection().set(null);
			application.metadataConnection().set(null);
			
			loginStatus.setFill(Color.RED);
		}
		catch (ConnectionException e) {
			e.printStackTrace();
		}
	}
	
	private void createGraph() {
		
		root = new HBox();
		root.setAlignment(Pos.BASELINE_CENTER);
		root.setSpacing(5.0);
		
		loginStatus = new Rectangle(10, 10);
		loginStatus.setFill(Color.RED);
		
		Button loginLogoutButton = new Button("Log In");
		loginLogoutButton.setGraphic(loginStatus);
		root.getChildren().add(loginLogoutButton);
		
		ChoiceBox<String> serverBox = new ChoiceBox<>();
		for (String server : SERVERS.keySet()) {
			serverBox.getItems().add(server);
		}
		serverBox.getSelectionModel().select(0);
		root.getChildren().add(serverBox);
		
		ChoiceBox<String> versionBox = new ChoiceBox<>();
		for (String version : VERSIONS) {
			versionBox.getItems().add(version);
		}
		versionBox.getSelectionModel().select(0);
		root.getChildren().add(versionBox);
		
		TextField userField = new TextField();
		userField.setPromptText("User Name");
		root.getChildren().add(userField);
		
		PasswordField passwordField = new PasswordField();
		passwordField.setPromptText("Password");
		root.getChildren().add(passwordField);
		
		loginLogoutButton.setOnAction(e -> {
			
			if (loginLogoutButton.getText().equals("Log In")) {
				String serverName = serverBox.getValue();
				String version = versionBox.getValue();
				String userName = userField.getText();
				if (userName == null || userName.length() == 0) {
					return;
				}
				String password = passwordField.getText();
				if (password == null || password.length() == 0) {
					return;
				}
				
				String serverUrl = SERVERS.get(serverName);
				
				if (application.enterpriseConnection().get() != null) {
					logout();
				}
				loginWorker = createLoginWorker(serverUrl, version, userName, password);
				loginWorker.setOnSucceeded(es -> {
					LoginWorkerResults loginResults = loginWorker.getValue();
					if (loginResults.isSuccess()) {
						application.enterpriseConnection().set(loginResults.getEnterpriseConnection());
						application.metadataConnection().set(loginResults.getMetadataConnection());
						loginStatus.setFill(Color.GREEN);
						loginLogoutButton.setText("Log Out");
						application.apiVersion().set((new Double(version)).doubleValue());
					}
					else {
						application.enterpriseConnection().set(null);
						application.metadataConnection().set(null);
						loginStatus.setFill(Color.RED);
					}
					application.getLogController().log(loginResults.getLogHandler());
				});
				
				new Thread(loginWorker).start();
			}
			else {
				logout();
				loginLogoutButton.setText("Log In");
			}
		});
	}
	
	private void handleEnterpriseConnectionChanged() {
		if (application.metadataConnection().get() != null) {
			// TODO: ?
		}
		else {
			// TODO: ?
		}
	}
	
	private void handleMetadataConnectionChanged() {
		if (application.metadataConnection().get() != null) {
			// TODO: ?
		}
		else {
			// TODO: ?
		}
	}
	
	private Task<LoginWorkerResults> createLoginWorker(String serverUrl, String apiVersion, String userName, String password) {
		
		return new Task<LoginWorkerResults>() {
			
			@Override
			public LoginWorkerResults call() throws Exception {
				
				LoginWorkerResults results = new LoginWorkerResults();
				
				try {
					ConnectorConfig eConfig = new ConnectorConfig();
					eConfig.setAuthEndpoint(serverUrl + apiVersion);
					eConfig.setServiceEndpoint(serverUrl + apiVersion);
					eConfig.setManualLogin(true);
					LogHandler logHandler = new LogHandler();
					logHandler.setTitle("LOGIN");
					eConfig.addMessageHandler(logHandler);
					results.setLogHandler(logHandler);
					EnterpriseConnection eConnection = new EnterpriseConnection(eConfig);
					results.setEnterpriseConnection(eConnection);
					
					LoginResult loginResult = eConnection.login(userName, password);
					
					eConfig.clearMessageHandlers();
					
					eConnection.setSessionHeader(loginResult.getSessionId());
					
					ConnectorConfig mConfig = new ConnectorConfig();
					mConfig.setServiceEndpoint(loginResult.getMetadataServerUrl());
					mConfig.setSessionId(loginResult.getSessionId());
					results.setMetadataConnection(new MetadataConnection(mConfig));
					
					results.setSuccess(true);
				}
				catch (ConnectionException e) {
					e.printStackTrace();
				}
				
				return results;
			}
		};
	}
}
