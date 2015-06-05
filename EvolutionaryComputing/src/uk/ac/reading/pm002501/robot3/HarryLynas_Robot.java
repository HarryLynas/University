package uk.ac.reading.pm002501.robot3;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;

import robocode.AdvancedRobot;
import robocode.BattleEndedEvent;
import robocode.BulletHitEvent;
import robocode.DeathEvent;
import robocode.HitByBulletEvent;
import robocode.RobocodeFileOutputStream;
import robocode.RoundEndedEvent;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;
import robocode.WinEvent;
import uk.ac.reading.pm002501.misc.Random;
import uk.ac.reading.pm002501.tree.Genotype;
import uk.ac.reading.pm002501.tree.Token;

public class HarryLynas_Robot extends AdvancedRobot {

	public static int minPower = 1;
	public static int maxPower = 99;
	private int roundDamage = 0;
	// numChildren MUST be a multiple of 2
	private int numStoredResults = 0;
	private int numChildren = 100;
	private int numParents = 10;
	public static int mutationChance = 1;
	public static int colourMutationChance = 5;

	private Genotype behaviours = null;
	private Genotype onScan = null;
	private Genotype onHit = null;
	private Genotype onTake = null;
	private int[] colours = null;

	private final String childFileName = "children.dat";
	private final String scoreFileName = "scores.dat";
	private final String useFileName = "use.dat";
	private final String debugFileName = "debug.txt";
	private final String statsFileName = "stats.txt";
	private StringBuffer prevFile = new StringBuffer();

	@Override
	public void run() {
		boolean useExist = getDataFile(useFileName).exists();
		if (useExist && getDataFile(useFileName).length() > 0) {
			loadTacticToUse();
		} else {
			boolean childrenExist = getDataFile(childFileName).exists();
			if (childrenExist && getDataFile(childFileName).length() == 0)
				childrenExist = false;
			childrenExist = readPrevGeneticsNew(childrenExist);
			if (childrenExist) {
				// Load children
				loadChildToUse();
			}
		}
		// Error check
		if (behaviours == null) {
			System.out.println("ERROR: Behaviours is null.");
			return;
		}
		int p = 0;
		try {
			setBodyColor(new Color(colours[p++], colours[p++], colours[p++]));
			setGunColor(new Color(colours[p++], colours[p++], colours[p++]));
			setRadarColor(new Color(colours[p++], colours[p++], colours[p++]));
			setScanColor(new Color(colours[p++], colours[p++], colours[p++]));
			setBulletColor(new Color(colours[p++], colours[p++], colours[p++]));
		} catch (Exception ex) {
		}

		while (true) {
			runGenotype(behaviours, false);
		}
	}

	private void loadChildToUse() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					getDataFile(childFileName)));
			int count = 0;
			String line = null;
			while ((line = reader.readLine()) != null) {
				++count;
				if (count > numStoredResults) {
					String[] parts = line.split(",");
					behaviours = new Genotype(parts[0]);
					onScan = new Genotype(parts[1]);
					onHit = new Genotype(parts[2]);
					onTake = new Genotype(parts[3]);
					colours = new int[15];
					int i = 0;
					for (String s : parts[4].split("-"))
						colours[i++] = Integer.parseInt(s);
					reader.close();
					return;
				}
			}

			System.out.println("ERROR: Failed to find child to use.");
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadTacticToUse() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					getDataFile(useFileName)));
			String line = reader.readLine();
			reader.close();

			if (line == null) {
				System.out.println(useFileName + " is corrupt!");
				return;
			}

			decodeLine(line, behaviours, onScan, onHit, onTake, colours);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void decodeLine(String line, Genotype behaviours, Genotype onScan,
			Genotype onHit, Genotype onTake, int[] colours) {
		String parts[] = line.split(",");
		behaviours = new Genotype(parts[0]);
		onScan = new Genotype(parts[1]);
		onHit = new Genotype(parts[2]);
		onTake = new Genotype(parts[3]);

		String[] coloursD = parts[4].split("-");
		colours = new int[15]; // 5 * 3 colours
		for (int i = 0; i < 15; ++i)
			colours[i] = Integer.parseInt(coloursD[i]);
	}

	private boolean readPrevGeneticsNew(boolean childrenExist) {
		// Read in previous genetics
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					getDataFile(scoreFileName)));
			int count = 0;
			String line = null;

			ArrayList<Integer> scores = new ArrayList<Integer>();
			ArrayList<String> lines = new ArrayList<String>();

			while ((line = reader.readLine()) != null) {
				prevFile.append(line);
				prevFile.append(System.lineSeparator());
				if (line.length() > 1) {
					++count;
					int lineScore = Integer.parseInt(line.split(",")[0]);
					lines.add(line);
					scores.add(lineScore);
				}
			}
			numStoredResults = count;
			if (numStoredResults >= numChildren) {
				reader.close();
				prevFile = new StringBuffer();
				numStoredResults = 0;
				getMutationData(scores.toArray(new Integer[scores.size()]),
						lines.toArray(new String[lines.size()]));
				return true;
			} else if (!childrenExist) {
				reader.close();
				// Generate data
				behaviours = new Genotype(15);
				onScan = new Genotype(10);
				onHit = new Genotype(10);
				onTake = new Genotype(10);
				colours = Random.getRandomColours();
				return false;
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void getMutationData(Integer[] scores, String[] lines) {
		// First sort by scores
		sortScores(scores, lines);
		// Now scores are sorted, pick parents
		String[] parents = pickParents(lines);
		// Now parents are created, create children from parents
		String[] children = populateChildren(parents);
		// Save children to file
		try {
			PrintStream writer = new PrintStream(new RobocodeFileOutputStream(
					getDataFile(childFileName)));
			for (String child : children)
				writer.println(child);
			writer.close();
		} catch (Exception ex) {
			ex.printStackTrace(out);
		}
		// Read back as error check
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					getDataFile(childFileName)));
			int count = 0;
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.length() > 0)
					++count;
			}
			reader.close();
			if (count < numChildren) {
				System.out.println(childFileName + " is corrupt - deleting!");
				getDataFile(childFileName).delete();
				readPrevGeneticsNew(false);
			}
		} catch (Exception e) {
			e.printStackTrace(out);
		}
	}

	private String[] populateChildren(String[] parents) {
		String[] children = new String[numChildren];

		for (int i = 0; i < numChildren - 1; i += 2) {
			// Pick two parents
			int parentA = Random.getRandom(0, numParents - 1);
			int parentB = parentA;
			while (parentA == parentB)
				parentB = Random.getRandom(0, numParents - 1);
			// Get the two lines
			String lineA = parents[parentA];
			String lineB = parents[parentB];
			// Remove the score from the string
			lineA = lineA.substring(lineA.indexOf(",") + 1, lineA.length());
			lineB = lineB.substring(lineB.indexOf(",") + 1, lineB.length());

			// Perform a swap on a random part
			String[] partsA = lineA.split(",");
			String[] partsB = lineB.split(",");

			String coloursA = partsA[4];
			String coloursB = partsB[4];

			int part = Random.getRandom(0, partsA.length - 2);

			Genotype a = new Genotype(partsA[part]);
			Genotype b = new Genotype(partsB[part]);
			a.swap(b, Random.getRandom(2, a.length() - 2),
					Random.getRandom(2, b.length() - 2));

			// Combine back into a single string
			lineA = "";
			lineB = "";
			for (int p = 0; p < partsA.length; ++p) {
				if (p == part) {
					lineA += a.getStorageString() + ",";
					lineB += b.getStorageString() + ",";
					continue;
				}
				lineA += partsA[p] + ",";
				lineB += partsB[p] + ",";
			}
			lineA = lineA.substring(0, lineA.length() - 1);
			lineB = lineB.substring(0, lineB.length() - 1);

			// Perform mutations
			StringBuffer bA = new StringBuffer();
			StringBuffer bB = new StringBuffer();
			partsA = lineA.split(",");
			partsB = lineB.split(",");
			for (int p = 0; p < partsA.length - 1; ++p) {
				Genotype g = new Genotype(partsA[p]);
				g.performMutations();
				partsA[p] = g.getStorageString();
				g = new Genotype(partsB[p]);
				g.performMutations();
				partsB[p] = g.getStorageString();
				bA.append(partsA[p] + ",");
				bB.append(partsB[p] + ",");
			}
			bA.deleteCharAt(bA.length() - 1);
			bB.deleteCharAt(bB.length() - 1);

			children[i] = bA.toString() + "," + coloursA;
			children[i + 1] = bB.toString() + "," + coloursB;
		}

		return children;
	}

	private String[] pickParents(String[] lines) {
		String[] parents = new String[numParents];
		for (int i = 0; i < numParents; ++i) {
			int selection = Random.getRandom(1, 100);
			if (selection > 70) {
				// Pick a high range sequence
				parents[i] = lines[Random.getRandom(0, 5)];
			} else if (selection > 50) {
				// Pick a high range sequence
				parents[i] = lines[Random.getRandom(6, 11)];
			} else if (selection > 20) {
				// Pick a mid range sequence
				parents[i] = lines[Random.getRandom(12, 21)];
			} else {
				// Pick a bad sequence
				parents[i] = lines[Random.getRandom(22, 35)];
			}
		}
		return parents;
	}

	private void sortScores(Integer[] scores, String[] lines) {
		// Bubble sort
		boolean sorted = false;
		while (!sorted) {
			sorted = true;
			for (int i = 0; i < scores.length - 1; ++i) {
				int score = scores[i];
				int ahead = scores[i + 1];
				if (score < ahead) {
					sorted = false;
					scores[i] = ahead;
					scores[i + 1] = score;
					String temp = lines[i];
					lines[i] = lines[i + 1];
					lines[i + 1] = temp;
				}
			}
		}
		// Output best score for debug
		try {
			PrintStream writer = new PrintStream(new RobocodeFileOutputStream(
					getDataFile(debugFileName)));
			// TODO
			writer.close();

			// Output stats
			BufferedReader reader = new BufferedReader(new FileReader(
					getDataFile(statsFileName)));
			String line = null;
			StringBuffer buffer = new StringBuffer();
			while ((line = reader.readLine()) != null)
				buffer.append(line + "\n");
			reader.close();

			writer = new PrintStream(new RobocodeFileOutputStream(
					getDataFile(statsFileName)));
			double average = 0;
			for (int sco : scores)
				average += sco;
			average /= scores.length;
			writer.print(buffer.toString());
			writer.println(scores[0] + "," + scores[scores.length - 1] + ","
					+ average);
			writer.close();
		} catch (Exception ex) {
			ex.printStackTrace(out);
		}
	}

	private void runGenotype(Genotype g, boolean event) {
		int result = event ? handleEventBehaviour(g.token)
				: performBehaviour(g.token);
		if (result == 0) {
			if (g.left != null)
				runGenotype(g.left, event);
			if (g.right != null)
				runGenotype(g.right, event);
		} else if (result == 1)
			runGenotype(g.left, event);
		else if (result == -1)
			runGenotype(g.right, event);
		if (event) {
			execute();
			while (getDistanceRemaining() > 0 || getTurnRemaining() > 0)
				execute();
		}
	}

	private int performBehaviour(Token t) {
		int amount = t.getAmount();
		switch (t.getToken()) {
		case M_LEFT:
			turnLeft(amount);
			break;
		case M_RIGHT:
			turnRight(amount);
			break;
		case FIREGUN:
			fire(normaliseFire(amount));
			break;
		case M_BACK:
			back(amount);
			break;
		case FOWARDS:
			ahead(amount);
			break;
		case T_LEFT:
			turnGunLeft(amount);
			break;
		case T_RIGHT:
			turnGunRight(amount);
			break;
		case H_GT_N:
			if (getEnergy() > t.getAmount())
				return 1;
			else
				return -1;
		case H_LT_N:
			if (getEnergy() < t.getAmount())
				return 1;
			else
				return -1;
		}
		return 0;
	}

	@Override
	public void onStatus(StatusEvent e) {
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		if (onScan == null)
			return;
		runGenotype(onScan, true);
	}

	@Override
	public void onHitByBullet(HitByBulletEvent e) {
		if (onTake == null)
			return;
		runGenotype(onTake, true);
	}

	@Override
	public void onBulletHit(BulletHitEvent e) {
		roundDamage += getDamage(e.getBullet().getPower());

		if (onHit == null)
			return;
		runGenotype(onHit, true);
	}

	private int handleEventBehaviour(Token t) {
		int amount = t.getAmount();
		switch (t.getToken()) {
		case M_LEFT:
			setTurnLeft(amount);
			break;
		case M_RIGHT:
			setTurnRight(amount);
			break;
		case FIREGUN:
			setFire(normaliseFire(amount));
			break;
		case M_BACK:
			setBack(amount);
			break;
		case FOWARDS:
			setAhead(amount);
			break;
		case T_LEFT:
			setTurnGunLeft(amount);
			break;
		case T_RIGHT:
			setTurnGunRight(amount);
			break;
		case H_GT_N:
			if (getEnergy() > t.getAmount())
				return 1;
			else
				return -1;
		case H_LT_N:
			if (getEnergy() < t.getAmount())
				return 1;
			else
				return -1;
		}
		return 0;
	}

	@Override
	public void onWin(WinEvent e) {
	}

	@Override
	public void onDeath(DeathEvent e) {
	}

	@Override
	public void onRoundEnded(RoundEndedEvent e) {
		if (behaviours == null)
			return;
		StringBuilder b = new StringBuilder();
		b.append((roundDamage + ((int) getEnergy())) + ",");

		b.append(behaviours.getStorageString());
		b.append(",");
		b.append(onScan.getStorageString());
		b.append(",");
		b.append(onHit.getStorageString());
		b.append(",");
		b.append(onTake.getStorageString());
		b.append(",");
		for (int colour : colours)
			b.append(String.format("%03d", colour) + "-");
		b.deleteCharAt(b.length() - 1);
		b.append(System.lineSeparator());

		try {
			PrintStream writer = new PrintStream(new RobocodeFileOutputStream(
					getDataFile(scoreFileName)));
			writer.print(prevFile.toString());
			writer.print(b.toString());
			writer.close();
		} catch (Exception ex) {
			ex.printStackTrace(out);
		}
	}

	@Override
	public void onBattleEnded(BattleEndedEvent e) {
	}

	private int getDamage(double firePower) {
		// 4 * firepower. If firepower > 1, it does an additional damage = 2 *
		// (power - 1).
		double damage = 4 * firePower;
		if (firePower > 1)
			damage += 2 * (firePower - 1);
		return (int) damage;
	}

	private double normaliseFire(int fire) {
		return (((double) fire) / maxPower) * 3;
	}
}
