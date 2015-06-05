package GUI.model;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import GUI.MainApp;

public class TileViewResults implements EventHandler<ActionEvent> {
	
	private WebcamTask webcamTask = null;
	private TilePane tilePane = null;
	private MainApp mainApp = null;
	private int framesUntilResetSeen = 0;
	private ImageView webcam;
	
	public TileViewResults(WebcamTask webcamTask, TilePane tilePane,
			int framesUntilResetSeen, ImageView webcam) {
		this.webcamTask = webcamTask;
		this.tilePane = tilePane;
		this.framesUntilResetSeen = framesUntilResetSeen;
		this.webcam = webcam;
	}
	
	private int callCount = 0;
	private HashMap<Integer, Integer> renderedIds = new HashMap<Integer, Integer>();
	
	public Set<Entry<Integer, Integer>> getMatchCount() {
		// No need to sort
		//Set<Entry<Integer, Integer>> results = renderedIds.entrySet();
		//return results.stream().sorted().collect(Collectors.toSet());
		return renderedIds.entrySet();
	}

	@Override
	public void handle(ActionEvent event) {
		WritableImage imageToRender = webcamTask.getImageToRender();
		if (imageToRender != null)
			webcam.setImage(imageToRender);
		ObservableList<Node> children = tilePane.getChildren();
		if (++callCount == framesUntilResetSeen) {
			children.clear();
			renderedIds.clear();
			callCount = 0;
		}
		for (WebcamResult r : webcamTask.getMatches()) {
			if (renderedIds.containsKey(r.getId())) {
				renderedIds.put(r.getId(), renderedIds.get(r.getId()) + 1);
				continue;
			}
			renderedIds.put(r.getId(), 1);
			ImageView view = new ImageView(r.getImage());
			view.setFitWidth(r.getImage().getWidth() / 2);
			view.setFitHeight(r.getImage().getHeight() / 2);
	        view.setOnMouseClicked(new viewImageHandler(r.getName(), r.getImage()));
			children.add(view);
		}
	}
	
	private class viewImageHandler implements EventHandler<MouseEvent> {
		private Image image;
		private String name;
		
		public viewImageHandler(String name, Image image) {
			if (name.equals("N/A"))
				name = "< No Name >";
			this.name = name;
			this.image = image;
		}
		
        @Override
        public void handle(MouseEvent mouseEvent) {
            if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                if (mouseEvent.getClickCount() == 2) {
                    BorderPane borderPane = new BorderPane();
                    ImageView imageView = new ImageView();
                    imageView.setImage(image);
                    imageView.setStyle("-fx-background-color: BLACK");
                    imageView.setFitHeight((mainApp.getPrimaryStage().getHeight() / 2) - 10);
                    imageView.setPreserveRatio(true);
                    imageView.setSmooth(true);
                    imageView.setCache(true);
                    borderPane.setCenter(imageView);
                    borderPane.setStyle("-fx-background-color: BLACK");
                    Stage newStage = new Stage();
                    newStage.setWidth(mainApp.getPrimaryStage().getWidth() / 4);
                    newStage.setHeight(mainApp.getPrimaryStage().getHeight() / 2);
                    newStage.setTitle(name);
                    newStage.getIcons().add(mainApp.getPrimaryStage().getIcons().get(0));
                    Scene scene = new Scene(borderPane, Color.BLACK);
                    newStage.setScene(scene);
                    newStage.show();
                }
            }
        }
	}

	public void setMainApp(MainApp mainApp) {
		this.mainApp = mainApp;
	}

	public void updateFramesCount(int framesUntilResetSeen) {
		this.framesUntilResetSeen = framesUntilResetSeen;
	}
}
