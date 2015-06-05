package API.database;

import java.io.Serializable;

public class Person implements Serializable {

	private static final long serialVersionUID = 4723777189592275388L;
	
	private String name;
	private String fileName;
	
	public Person(String name, String fileName) {
		this.name = name;
		this.fileName = fileName;
	}
	
	public String getName() {
		return name;
	}
	
	public String getFilename() {
		return fileName;
	}
}
