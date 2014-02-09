package workbench;

import java.util.HashMap;
import java.util.Map;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.GetUserInfoResult;
import com.sforce.soap.enterprise.LoginResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

public class LoginController {
	
	private static class LoginWorkerResults {
		
		private SOAPLogHandler logHandler;
		private boolean success;
		private EnterpriseConnection eConnection;
		private MetadataConnection mConnection;
		private GetUserInfoResult userInfo;
		
		public SOAPLogHandler getLogHandler() {
			return logHandler;
		}
		public void setLogHandler(SOAPLogHandler logHandler) {
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
		
		public GetUserInfoResult getUserInfo() {
			return userInfo;
		}
		public void setUserInfo(GetUserInfoResult userInfo) {
			this.userInfo = userInfo;
		}
	}
	
	private static class LoginWorker extends Task<LoginWorkerResults> {
		
		private String serverUrl;
		private String apiVersion;
		private String userName;
		private String password;
		
		public LoginWorker(String serverUrl, String apiVersion, String userName, String password) {
			this.serverUrl = serverUrl;
			this.apiVersion = apiVersion;
			this.userName = userName;
			this.password = password;
		}
		
		@Override
		public LoginWorkerResults call() throws Exception {
			
			LoginWorkerResults results = new LoginWorkerResults();
			
			try {
				ConnectorConfig eConfig = new ConnectorConfig();
				eConfig.setAuthEndpoint(serverUrl + apiVersion);
				eConfig.setServiceEndpoint(serverUrl + apiVersion);
				eConfig.setManualLogin(true);
				
				SOAPLogHandler logHandler = new SOAPLogHandler();
				logHandler.setTitle("LOGIN");
				logHandler.setSummary(String.format("User Name: %s", userName));
				eConfig.addMessageHandler(logHandler);
				results.setLogHandler(logHandler);
				
				EnterpriseConnection eConnection = new EnterpriseConnection(eConfig);
				results.setEnterpriseConnection(eConnection);
				
				LoginResult loginResult = eConnection.login(userName, password);
				
				eConnection.setSessionHeader(loginResult.getSessionId());
				
				ConnectorConfig mConfig = new ConnectorConfig();
				mConfig.setServiceEndpoint(loginResult.getMetadataServerUrl());
				mConfig.setSessionId(loginResult.getSessionId());
				results.setMetadataConnection(new MetadataConnection(mConfig));
				
				GetUserInfoResult userInfo = eConnection.getUserInfo();
				results.setUserInfo(userInfo);
				
				eConfig.clearMessageHandlers();
				
				results.setSuccess(true);
			}
			catch (ConnectionException e) {
				e.printStackTrace();
			}
			
			return results;
		}
	}
	
	private static final Map<String, String> SERVERS;
	static {
		SERVERS = new HashMap<String, String>();
		// Add your servers here
		// Entries should be of the form:
		//SERVERS.put("server nick name", "http://host:port/services/Soap/c/");
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
	
	private ToolBar root;
	private Rectangle loginStatus;	
	private Button loginLogoutButton;
	private ChoiceBox<String> serverBox;
	private ChoiceBox<String> versionBox;
	private TextField userField;
	private PasswordField passwordField;
	
	public LoginController(Main application) {
		this.application = application;
		createGraph();
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

		root = new ToolBar();
		
		loginStatus = new Rectangle(10, 10);
		loginStatus.setFill(Color.RED);
		
		loginLogoutButton = new Button("Log In");
		loginLogoutButton.setGraphic(loginStatus);
		loginLogoutButton.setOnAction(e -> handleLoginLogoutButtonPressed(e));
		root.getItems().add(loginLogoutButton);
		
		serverBox = new ChoiceBox<>();
		for (String server : SERVERS.keySet()) {
			serverBox.getItems().add(server);
		}
		serverBox.getSelectionModel().select(0);
		root.getItems().add(serverBox);
		
		versionBox = new ChoiceBox<>();
		for (String version : VERSIONS) {
			versionBox.getItems().add(version);
		}
		versionBox.getSelectionModel().select(0);
		root.getItems().add(versionBox);
		
		userField = new TextField();
		userField.setPromptText("User Name");
		root.getItems().add(userField);
		
		passwordField = new PasswordField();
		passwordField.setPromptText("Password");
		root.getItems().add(passwordField);
	}
	
	private void handleLoginLogoutButtonPressed(ActionEvent e) {
		
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
			
			// Possibly logged into a different account than
			// the account being logged into now.
			if (application.enterpriseConnection().get() != null) {
				// TODO: Add comparison of credentials to avoid needless
				// logout. If credentials are the same then simply return.
				logout();
			}
			
			final LoginWorker loginWorker = new LoginWorker(serverUrl, version, userName, password);
			loginWorker.setOnSucceeded(es -> {
				
				LoginWorkerResults loginResults = loginWorker.getValue();
				
				if (loginResults.isSuccess()) {
					application.enterpriseConnection().set(loginResults.getEnterpriseConnection());
					application.metadataConnection().set(loginResults.getMetadataConnection());
					application.apiVersion().set((new Double(version)).doubleValue());
					application.userInfo().set(loginResults.getUserInfo());
		
					loginStatus.setFill(Color.GREEN);
					loginLogoutButton.setText("Log Out");
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
	}
}
