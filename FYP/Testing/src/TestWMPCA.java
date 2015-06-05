import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import API.LookupResult;
import API.WMPCA.WMPCA;
import API.database.PersonDatabase;

public class TestWMPCA {
	public static void main(String... args) throws Exception {
		FileInputStream fs = new FileInputStream("//ndrive/pm002501/.do_not_delete/desktop.xp/db.db.txt");
		ObjectInputStream os = new ObjectInputStream(fs);
		PersonDatabase database = (PersonDatabase) os.readObject();
		os.close();
		fs.close();

		System.out.println("Read");

		WMPCA cache = database.getWMPCA();

		int count = 0;
		int fails = 0;
		
		ArrayList<String> images = new ArrayList<String>();
		extractTests(new File("//ndrive/pm002501/.do_not_delete/desktop.xp/toMatch/"), images);
		
		for (String image : images) {
			API.WMPCA.LookupPerson lookup = new API.WMPCA.LookupPerson(cache.getResults(), image);
			LookupResult[] results = lookup.lookupInCache(3);
			++count;
			if (results.length == 0 || results[0].getDistance() > 5) {
				++fails;
				System.out.println("Failed: " + image);
			}
		}
		
		System.out.println("Found: " + count + " tests.\n" + fails + " failed.");
	}
	
	private static void extractTests(File dir, ArrayList<String> imagesExtracting) throws IOException {
		if (dir.listFiles() == null)
			return;
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				extractTests(file, imagesExtracting);
			} else {
				imagesExtracting.add(file.getPath());
			}
		}
	}
}
