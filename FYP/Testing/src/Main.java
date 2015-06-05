import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Main {
	public static void main(String... args) throws Exception {
		String path = "//ndrive/pm002501/.do_not_delete/desktop.xp/faces96/";
		File file = new File(path);
		extractImages(file);
	}
	
	private static boolean extract = false;
	private static int i = 0;
	
	private static void extractImages(File dir) throws IOException {
		if (dir.listFiles() == null)
			return;
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				extractImages(file);
			} else {
				extract = !extract;
				if (extract)
					Files.move(file.toPath(), 
							new File("//ndrive/pm002501/.do_not_delete/desktop.xp/toMatch/" + i++ + ".jpg").toPath(),
							StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}
}
