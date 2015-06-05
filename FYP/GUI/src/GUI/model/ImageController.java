package GUI.model;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.TilePane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import javax.mail.MessagingException;

import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;

import API.LookupResult;
import API.WMPCA.WMPCA;
import API.audit.GoogleMail;
import API.database.EigenfaceCache;
import API.database.Person;
import API.database.PersonDatabase;
import API.logger.Log;
import GUI.MainApp;

@SuppressWarnings("deprecation")
public class ImageController {
	
	@FXML private TableView<ProjectImage> imageTable;
	@FXML private TableColumn<ProjectImage, String> nameColumn;
	@FXML private TableColumn<ProjectImage, String> pathColumn;
	@FXML private ImageView FaceImage;
	@FXML private ImageView SelectedFace;
	@FXML private Label Threshold;
	@FXML private TextArea logArea;
	@FXML private ImageView webcam;
	@FXML private TabPane tabPane;
	@FXML private TextField txtRegions;
	@FXML private TextField txtEigenFaces;
	@FXML private RadioButton pcaButton;
	@FXML private RadioButton wmpcaButton;
	@FXML private Button addPersonButton;
	@FXML private TilePane tilePane;
	@FXML private Label txtMatchInfo;
	@FXML private TextField txtEmail;
	@FXML private TextField txtEmail2;
	@FXML private TextField txtPassword;
	@FXML private TextField txtUpdate;
	@FXML private TextField txtMatches;
	
	// Reference to the main application.
	private MainApp mainApp;

	// References
	private WMPCA wmpca = null;
	private EigenfaceCache cache = null;
	private Thread wmpcaThread = null;
	private PersonDatabase database = new PersonDatabase("database\\");
	private WebcamTask webcamTask = null;
	private TileViewResults tileViewResults = null;

	/**
	 * The constructor. The constructor is called before the initialize()
	 * method.
	 */
	public ImageController() {
	}

	/**
	 * Initializes the controller class. This method is automatically called
	 * after the fxml file has been loaded.
	 */
	@FXML
	private void initialize() {
		wmpcaButton.setSelected(true);
		addPersonButton.setDisable(true);	
		// Initialize the person table with the two columns.
		nameColumn.setCellValueFactory(cellData -> cellData.getValue()
				.getName());
		pathColumn.setCellValueFactory(cellData -> cellData.getValue()
				.getPath());
		// Logger
		Timeline logUpdater = new Timeline(new KeyFrame(Duration.seconds(1), new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
	        	String text = "";
	        	for (String s : Log.getMessages())
	        		text += s + "\n";
	        	if (text.length() > 0)
	        		logArea.appendText(text);
			}
	    }));  
	    logUpdater.setCycleCount(Timeline.INDEFINITE);  
	    logUpdater.play();
	    
	    // Webcam
	    // ms
	    int timeBetweenFrames = 50;
	    // The amount of calls before resetting who has been seen
	    int framesUntilResetSeen = 200;
	    
		txtMatches.textProperty().addListener(new ChangeListener<String>() {
		    @Override
		    public void changed(final ObservableValue<? extends String> observable, final String oldValue, final String newValue) {
				String time = newValue;
				int framesUntilResetSeen = 200;
				try {
					framesUntilResetSeen = Integer.parseInt(time);
				} catch (NumberFormatException e) {}
				int timeBetweenFrames = 50;
			    txtMatchInfo.setText("All Recently Matched People. Reset every " + (timeBetweenFrames * framesUntilResetSeen) +
			    		"ms. Double click to view a image in a new window.");
			    tileViewResults.updateFramesCount(framesUntilResetSeen);
		    }
		});
	    
	    txtMatchInfo.setText(txtMatchInfo.getText() + ". Reset every " + (timeBetweenFrames * framesUntilResetSeen) +
	    		"ms. Double click to view a image in a new window.");
	
	    webcamTask = new WebcamTask(timeBetweenFrames);
	    webcamTask.setDatabase(database);
	    webcamTask.setWMPCA(wmpca);
	    
	    Thread webcamThread = new Thread(webcamTask);
	    webcamThread.setName("Webcam");
	    webcamThread.setDaemon(true);
	    webcamThread.start();
	    
	    tileViewResults = new TileViewResults(webcamTask, tilePane,
	    		framesUntilResetSeen, webcam);
	    
		Timeline webcamTimeline = new Timeline(new KeyFrame(Duration.millis(timeBetweenFrames), tileViewResults));  
		webcamTimeline.setCycleCount(Timeline.INDEFINITE);  
		webcamTimeline.play();
	}

	/**
	 * Is called by the main application to give a reference back to itself.
	 * 
	 * @param mainApp The main application class instance.
	 */
	public void setMainApp(MainApp mainApp) {
		this.mainApp = mainApp;

		// Add observable list data to the table
		imageTable.setItems(mainApp.getPersonData());

		imageTable.addEventHandler(MouseEvent.MOUSE_CLICKED,
				new EventHandler<MouseEvent>() {
					@Override
					public void handle(MouseEvent event) {
						eventHandlerFunc(event);
					}
				});
		imageTable.addEventHandler(KeyEvent.KEY_PRESSED,
				new EventHandler<KeyEvent>() {
					@Override
					public void handle(KeyEvent event) {
						eventHandlerFunc(event);
					}
				});
		
		tileViewResults.setMainApp(mainApp);
	}

	private final void eventHandlerFunc(Event event) {
		TableView<?> view = (TableView<?>) event.getSource();
		int index = view.getSelectionModel().getSelectedIndex();
		if (index == -1)
			return;
		ProjectImage projectImage = (ProjectImage) view.getSelectionModel()
				.getSelectedItem();
		FaceImage.setImage(API.recognition.FaceRecognition.getProcessedImage(projectImage
				.getPath().getValue()));
		if (cache != null || (database.getWMPCA() != null && database.getWMPCA().getResults() != null))
			lookupPerson(projectImage.getPath().getValue());
	}

	@FXML
	public void selectImagesFolder() {
		final DirectoryChooser directoryChooser = new DirectoryChooser();
		final File selectedDirectory = directoryChooser.showDialog(mainApp
				.getPrimaryStage());
		mainApp.resetData();
		if (selectedDirectory != null) {
			extractImages(selectedDirectory);
		}
	}

	public void extractImages(File dir) {
		if (dir.listFiles() == null)
			return;
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				extractImages(file);
			} else {
				String filePath = file.getAbsolutePath().substring(
						file.getAbsolutePath().lastIndexOf('\\') + 1);
				String lower = filePath.toLowerCase();
				if (lower.endsWith(".png") || lower.endsWith(".jpg")) {
					mainApp.addImage(new ProjectImage(filePath, file.getPath()));
				}
			}
		}
	}

	@FXML
	public void trainData() {
		Action response = Dialogs.create()
		        .owner(mainApp.getPrimaryStage())
		        .title("Use Person Database?")
		        .masthead("Use Person Database?")
		        .message("Use person database? (Alternative is image folder.)")
		        .showConfirm();
		if (response == Dialog.ACTION_YES) {
			activateTraining(true);
		} else {
			activateTraining(false);
		}
	}
	
	private void activateTraining(boolean useDatabase) {
		API.Image[] imagePaths = null;
		if (useDatabase) {
			Person[] people = database.getPeople();
			imagePaths = new API.Image[people.length];
			for (int i = 0; i < people.length; ++i)
				imagePaths[i] = new API.Image(database.getDirectory() + people[i].getFilename());
		} else {
			final ObservableList<ProjectImage> data = mainApp.getPersonData();
			// Get the paths for each image
			imagePaths = new API.Image[data.size()];
			for (int i = 0; i < data.size(); ++i)
				imagePaths[i] = new API.Image(data.get(i).getPath().getValue());
		}
		// PCA is deprecated, WMPCA is only used now. WMPCA with 1 region = PCA
		int numRegions = Integer.parseInt(txtRegions.getText());
		webcamTask.setNumRegions(numRegions);
		double percentEigenfaces = Double.parseDouble(txtEigenFaces.getText());
		wmpca = new WMPCA(imagePaths.clone(), (int) (imagePaths.length * percentEigenfaces), numRegions);
		wmpcaThread = new Thread(wmpca);
		wmpcaThread.setDaemon(true);
		wmpcaThread.setName("WMPCA-Master");
		wmpcaThread.start();
	}

	@FXML
	public void writeDatabase() {
		FileOutputStream fs;
		try {
			final FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Select file to write database to.");
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("DB files (*.db)", "*.db");
            fileChooser.getExtensionFilters().add(extFilter);
			final File selectedFile = fileChooser.showSaveDialog(mainApp
					.getPrimaryStage());
			if (selectedFile != null) {
				fs = new FileOutputStream(selectedFile.getPath());
				ObjectOutputStream os = new ObjectOutputStream(fs);
				database.setWMPCA(wmpca);
				os.writeObject(database);
				os.close();
				fs.close();
				webcamTask.setDatabase(database);
				webcamTask.setWMPCA(wmpca);
		    	Dialogs.create()
	    		.owner(mainApp.getPrimaryStage())
	    		.title("Success")
	    		.masthead("Success")
	    		.message("Saved database.")
	    		.showInformation();
			}
		} catch (Exception e) {
	    	Dialogs.create()
			.owner(mainApp.getPrimaryStage())
			.title("Error")
			.masthead("Error")
			.showException(e);
		}
		return;
	}

	@FXML
	public void lookupPerson() {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select image of person to lookup.");
		final File selectedFile = fileChooser.showOpenDialog(mainApp
				.getPrimaryStage());
		if (selectedFile != null) {
			lookupPerson(selectedFile.getPath());
			FaceImage.setImage(new Image("file:///" + selectedFile.getPath()));
		}
	}
	
	@FXML
	public void liveSelectDB() {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select database to use.");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("DB files (*.db)", "*.db");
        fileChooser.getExtensionFilters().add(extFilter);
		final File selectedFile = fileChooser.showOpenDialog(mainApp
				.getPrimaryStage());
		if (selectedFile != null) {
			FileInputStream fs;
			try {
				fs = new FileInputStream(selectedFile.getPath());
				ObjectInputStream os = new ObjectInputStream(fs);
				database = (PersonDatabase) os.readObject();
				os.close();
				fs.close();
				for (Person person : database.getPeople())
					mainApp.addImage(new ProjectImage(person.getName(),
							database.getDirectory() + person.getFilename()));
				webcamTask.setDatabase(database);
				webcamTask.setWMPCA(wmpca);
		    	Dialogs.create()
		    		.owner(mainApp.getPrimaryStage())
		    		.title("Success")
		    		.masthead("Success")
		    		.message("Loaded database.")
		    		.showInformation();
			} catch (Exception e) {
		    	Dialogs.create()
	    		.owner(mainApp.getPrimaryStage())
	    		.title("Error")
	    		.masthead("Error")
	    		.showException(e);
			}
		}
	}

	private void lookupPerson(String path) {
		EigenfaceCache[] results = null;
		if (wmpca != null && wmpca.getResults() != null)
			results = wmpca.getResults();
		else if (database.getWMPCA() != null &&
				database.getWMPCA().getResults() != null)
			results = database.getWMPCA().getResults();
		if (results != null) {
			API.WMPCA.LookupPerson lookup = new API.WMPCA.LookupPerson(results, path);
			int numRegions = Integer.parseInt(txtRegions.getText());
			webcamTask.setNumRegions(numRegions);
			LookupResult[] lookupResults = lookup.lookupInCache(numRegions);
			for (LookupResult lResult : lookupResults) {
				if (lResult.getResult() >= 0) {
					// Number of faces does not equal number of images, find the correct
					// image
					int current = 0;
					String resultPath = "";
					for (API.Image image : results[0].getImages()) {
						int numFaces = image.getWMPCAData().length;
						if ((current + numFaces) > lResult.getResult()) {
							resultPath = image.getPath();
							break;
						}
						current += numFaces;
					}
					Threshold.setText(String.valueOf(lResult.getDistance()));
					SelectedFace.setImage(API.recognition.FaceRecognition
							.getProcessedImage(resultPath));
				} else {
					Threshold.setText("ERROR: No match found.");
					SelectedFace.setImage(null);
				}
				return; // XXX, TODO: Handle multiple faces
			}
		}
	}
	
	@FXML
	public void pauseInput() {
		if (webcamTask != null) {
			boolean status = !webcamTask.getRunningLive();
			addPersonButton.setDisable(status);
			webcamTask.setRunningLive(status);
		}
	}
	
	@FXML
	public void addPerson() {
		BufferedImage lastImage = webcamTask.getLastImage();
		if (lastImage == null)
			return;
		if (API.recognition.FaceRecognition.getProcessedFaceRegions(lastImage).length == 0) {
			Dialogs.create()
			.owner(mainApp.getPrimaryStage())
			.title("ERROR")
			.masthead("Adding A New Person")
			.message("No face was detected in this image.")
			.showError();
			return;
		}
		if (wmpcaThread != null && wmpcaThread.isAlive()) {
			Dialogs.create()
				.owner(mainApp.getPrimaryStage())
				.title("ERROR")
				.masthead("Adding A New Person")
				.message("Please wait for the previous person to be added before adding another person.")
				.showError();
			return;
		}
		Optional<String> response = Dialogs.create()
		        .owner(mainApp.getPrimaryStage())
		        .title("Input Name of Person")
		        .masthead("Adding A New Person")
		        .message("Please Enter Person's Name:")
		        .showTextInput("");
		// One way to get the response value.
		if (response.isPresent()) {
		    String name = response.get();
		    if (name.length() > 0) {
		    	String fileName = name + ".jpg";
		    	String dir = database.getDirectory();
		    	try {
		    		File file = new File(dir + fileName);
		    		if (file.exists()) {
				    	Dialogs.create()
				    		.owner(mainApp.getPrimaryStage())
				    		.title("Failure")
				    		.masthead("Failure")
				    		.message("That name is already in the database!")
				    		.showError();
				    	return;
		    		}
					ImageIO.write(lastImage, "jpg", file);
			    	database.addPerson(new Person(name, fileName));
			    	activateTraining(true);
			    	webcamTask.setDatabase(database);
			    	webcamTask.setWMPCA(wmpca);
			    	Dialogs.create()
			    		.owner(mainApp.getPrimaryStage())
			    		.title("Success")
			    		.masthead("Success")
			    		.message("Person was saved to: " + dir + fileName + " sucessfully.")
			    		.showInformation();
				} catch (Exception e) {
			    	Dialogs.create()
			    		.owner(mainApp.getPrimaryStage())
			    		.title("Failure")
			    		.masthead("Failure")
			    		.showException(e);
				}
		    }
		}
	}
	
	@FXML
	public void pcaButtonPress() {
		wmpcaButton.setSelected(false);
		txtRegions.setEditable(false);
		txtRegions.setText("1");
		txtRegions.setVisible(false);
	}
	
	@FXML
	public void wmpcaButtonPress() {
		pcaButton.setSelected(false);
		txtRegions.setEditable(true);
		txtRegions.setVisible(true);
	}
	
	@FXML
	public void tab1Select(Event e) {
		if (webcamTask != null)
			webcamTask.setTabSelected(false);
	}
	
	@FXML
	public void tab2Select(Event e) {
		if (webcamTask != null)
			webcamTask.setTabSelected(true);
	}
	
	@FXML
	public void emailResults() {
		if (database == null) {
			Dialogs.create()
			.owner(mainApp.getPrimaryStage())
			.title("Failure")
			.masthead("Failure")
			.message("Database is not set.")
			.showError();
			return;
		}
		String username = txtEmail.getText();
		String password = txtPassword.getText();
		String recipient = txtEmail2.getText();
		int matchesRequired = 10;
		try {
			matchesRequired = Integer.parseInt(txtUpdate.getText());
		} catch (NumberFormatException e) {
			Dialogs.create()
			.owner(mainApp.getPrimaryStage())
			.title("Failure")
			.masthead("Failure")
			.message("Matches required must be an integer.")
			.showError();
		}
		String title = "Classroom Attendance Summary";
		
		if (username.length() == 0 || password.length() == 0 || recipient.length() == 0) {
			Dialogs.create()
			.owner(mainApp.getPrimaryStage())
			.title("Failure")
			.masthead("Failure")
			.message("All fields must be filled in.")
			.showError();
			return;
		}

		Set<Entry<Integer, Integer>> results = tileViewResults.getMatchCount();
		Set<Integer> validMatches = new HashSet<Integer>();
		for (Entry<Integer, Integer> result : results) {
			if (result.getValue() < matchesRequired)
				continue;
			validMatches.add(result.getKey());
		}
		
		Person[] people = database.getPeople();
		WMPCA wmpca = database.getWMPCA();
		if (wmpca == null)
			return;
		EigenfaceCache[] caches = database.getWMPCA().getResults();
		
		Set<String> validPaths = new HashSet<String>();
		for (Integer match : validMatches) {
			int current = 0;
			String resultPath = "";
			for (API.Image image : caches[0].getImages()) {
				int numFaces = image.getWMPCAData().length;
				if ((current + numFaces) > match) {
					resultPath = image.getPath();
					break;
				}
				current += numFaces;
			}
			validPaths.add(resultPath.substring(resultPath.lastIndexOf('\\') + 1));
		}
		
		HashMap<Person, Boolean> presentMap = new HashMap<Person, Boolean>();
		if (people != null) {
			for (Person person : people)
				presentMap.put(person, validPaths.contains(person.getFilename()));
		}
		
		StringBuilder message = new StringBuilder();
		message.append("The following pupils are absent/present:\n\n");
		
		int present = 0;
		int total = 0;
		for (Entry<Person, Boolean> result : presentMap.entrySet()) {
			message.append(result.getKey().getName() + ":\t");
			if (result.getValue()) {
				message.append("PRESENT\n");
				++present;
			} else {
				message.append("ABSENT\n");
			}
			++total;
		}
		
		message.append("\nThere are " + present + " / " + total + " pupils detected as present.");
		
		try {
			GoogleMail.Send(username, password, recipient, title, message.toString());
			Dialogs.create()
			.owner(mainApp.getPrimaryStage())
			.title("Success")
			.masthead("Success")
			.message("Email to " + recipient + " was a success!")
			.showInformation();
		} catch (MessagingException e) {
	    	Dialogs.create()
    		.owner(mainApp.getPrimaryStage())
    		.title("Failure")
    		.masthead("Failure")
    		.showException(e);
		}
	}
}
