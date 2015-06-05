package GUI.model;

import java.awt.image.BufferedImage;
import java.util.LinkedList;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import API.LookupResult;
import API.WMPCA.WMPCA;
import API.database.EigenfaceCache;
import API.database.Person;
import API.database.PersonDatabase;
import API.recognition.FaceRecognition;

import com.github.sarxos.webcam.Webcam;

public class WebcamTask extends Task<Void> {
	// Variables accessed externally through getters
	private WritableImage imageToRender = null;
	private BufferedImage lastImage = null;
	private volatile WebcamResult[] matchResults = new WebcamResult[0];
	
	// Variables set externally through setters
	private int numRegions = 3;
	private boolean isTabSelected = false;
	private WMPCA wmpca = null;
	private PersonDatabase database = null;
	private volatile boolean runningLive = true;
	
	// Internal
	int timeBetweenFrames = 50;
	
	public WebcamTask(int timeBetweenFrames) {
		this.timeBetweenFrames = timeBetweenFrames;
	}
	
	@Override
	protected Void call() throws Exception {
		Webcam cam = null;
		try {
    		cam = Webcam.getDefault();
    		while (cam == null) {
    			Thread.sleep(5000);
    			cam = Webcam.getDefault();
    		}
    		cam.open();
			BufferedImage img;
        	while (true) {
        		if (cam.isOpen()) {
        			if (!isTabSelected)
        				Thread.sleep(1000);
        			else {
	            		img = cam.getImage();
	            		if (img != null && runningLive) {
	            			// Display rectangles around faces
	            			lastImage = img;
	            			imageToRender = FaceRecognition.getProcessedImage(img);
	            			EigenfaceCache[] results = null;
	            			if (wmpca != null && wmpca.getResults() != null)
	            				results = wmpca.getResults();
	            			else if (database.getWMPCA() != null && database.getWMPCA().getResults() != null)
	            				results = database.getWMPCA().getResults();
		            		if (results != null) {
		            			// Process each face
	            				API.WMPCA.LookupPerson lookup = new API.WMPCA.LookupPerson(results, "temp.jpg");
	            				LookupResult[] lookupResults = lookup.lookupInCache(numRegions);
	            				LinkedList<WebcamResult> webcamResults = new LinkedList<WebcamResult>();
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
		            					Person[] people = database.getPeople();
		            					String matchName;
		            					if (people.length > 0 && current <= people.length)
		            						matchName = people[current].getName();
		            					else
		            						matchName = "N/A";
		            					Image matchToRender = API.recognition.FaceRecognition
		            							.getProcessedImage(resultPath);
		            					webcamResults.add(new WebcamResult(lResult.getResult(), matchName, matchToRender));
		            				}
	            				}
	            				matchResults = webcamResults.toArray(new WebcamResult[webcamResults.size()]);
	            			}
	            		}
	            		Thread.sleep(timeBetweenFrames);
        			}
        		}
	        }
	    } catch (Exception e) {
	    	e.printStackTrace();
	    } catch (Throwable e) {
	    	e.printStackTrace();
	    } finally {
	    	if (cam != null)
	    		cam.close();
	    }
		return null;
	}
	
	public void setRunningLive(boolean status) {
		this.runningLive = status;
	}
	
	public boolean getRunningLive() {
		return runningLive;
	}
	
	public void setWMPCA(WMPCA wmpca) {
		this.wmpca = wmpca;
	}
	
	public void setDatabase(PersonDatabase db) {
		this.database = db;
	}
	
	public void setTabSelected(boolean status) {
		this.isTabSelected = status;
	}
	
	public void setNumRegions(int num) {
		this.numRegions = num;
	}
	
	public WebcamResult[] getMatches() {
		return matchResults;
	}

	public WritableImage getImageToRender() {
		return imageToRender;
	}

	public BufferedImage getLastImage() {
		return lastImage;
	}
}
