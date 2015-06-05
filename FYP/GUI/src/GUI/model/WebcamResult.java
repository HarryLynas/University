package GUI.model;

import javafx.scene.image.Image;

public class WebcamResult {
	private Image face;
	private String name;
	private int id;
	
	public WebcamResult(int id, String name, Image face) {
		this.id = id;
		this.name = name;
		this.face = face;
	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public Image getImage() {
		return face;
	}
}
