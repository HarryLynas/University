package API.database;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import API.WMPCA.WMPCA;

public class PersonDatabase implements Serializable {
	
	private static final long serialVersionUID = 2366123979517602269L;
	
	private String workDirectory;
	private ArrayList<Person> people = new ArrayList<Person>();
	private WMPCA internalDB = null;

	public PersonDatabase(String directory) {
		workDirectory = directory;
		new File(directory).mkdirs();
	}
	
	public boolean addPerson(Person person) {
		return people.add(person);
	}
	
	public String getDirectory() {
		return workDirectory;
	}
	
	public Person[] getPeople() {
		return people.toArray(new Person[people.size()]);
	}
	
	public WMPCA getWMPCA() {
		return internalDB;
	}
	
	public void setWMPCA(WMPCA wmpca) {
		internalDB = wmpca;
	}
}
