package GUI.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ProjectImage {
	private final StringProperty name;
	private final StringProperty path;

	/**
	 * Default constructor.
	 */
	public ProjectImage() {
		this(null, null);
	}

	/**
	 * Constructor with some initial data.
	 */
	public ProjectImage(String name, String path) {
		this.name = new SimpleStringProperty(name);
		this.path = new SimpleStringProperty(path);
	}

	public StringProperty getName() {
		return name;
	}

	public StringProperty getPath() {
		return path;
	}
}
