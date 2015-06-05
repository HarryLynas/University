package uk.ac.reading.pm002501.tree;

import uk.ac.reading.pm002501.misc.Random;
import uk.ac.reading.pm002501.robot3.HarryLynas_Robot;
import uk.ac.reading.pm002501.tree.Token.TOKENS;

/**
 * Genotype defines a sequence of tokens in a tree data structure. The last
 * branch can never be a condition token.
 */
public class Genotype implements Cloneable {
	public Genotype left = null;
	public Genotype right = null;
	public Token token = null;

	/**
	 * Create a node with the given token.
	 */
	public Genotype(Token t) {
		token = t;
	}

	@Override
	public Genotype clone() {
		Genotype retValue = new Genotype(token.clone());
		retValue.left = left == null ? null : left.clone();
		retValue.right = right == null ? null : right.clone();
		return retValue;
	}

	/**
	 * Create a binary tree with randomly generated tokens of a given size. The
	 * size may not be exact since any branches that end with a condition will
	 * be extended.
	 * 
	 * @param size
	 *            The estimated size of the tree to generate.
	 */
	public Genotype(int size) {
		token = new Token(true);
		// Create all nodes
		for (int i = 0; i < size; ++i) {
			if (left == null)
				place(left, this);
			else if (right == null)
				place(right, this);
			else {
				int cleft = count(left);
				int cright = count(right);
				if (cleft <= cright)
					place(left, this);
				else
					place(right, this);
			}
		}
		// Fix any branches ending with a condition
		fixupTree(this);
	}

	/**
	 * This function fixes any branches ending with a condition by adding the
	 * missing cases to this node.
	 * 
	 * @param g
	 *            The root node.
	 */
	private void fixupTree(Genotype g) {
		if (g.token.getToken() == TOKENS.H_LT_N
				|| g.token.getToken() == TOKENS.H_GT_N) {
			if (g.left == null)
				g.left = new Genotype(new Token(false));
			if (g.right == null)
				g.right = new Genotype(new Token(false));
		}
		if (g.left != null)
			fixupTree(g.left);
		if (g.right != null)
			fixupTree(g.right);
	}

	/**
	 * This function tries to place a new node in a tree at the correct
	 * location.
	 * 
	 * @param g
	 *            The node to place.
	 * @param previous
	 *            The previous node.
	 */
	private void place(Genotype g, Genotype previous) {
		if (g == null) {
			g = new Genotype(new Token(true));
			if (previous.left == null)
				previous.left = g;
			else
				previous.right = g;
			return;
		}
		if (g.left == null)
			place(g.left, g);
		else if (g.right == null)
			place(g.right, g);
		else {
			int cleft = count(g.left);
			int cright = count(g.right);
			if (cleft <= cright)
				place(g.left, g);
			else
				place(g.right, g);
		}
	}

	/**
	 * This function swaps two genotypes at a given position in each tree.
	 * 
	 * @param g
	 *            The second genotype tree.
	 * @param p1
	 *            Position 1
	 * @param p2
	 *            Position 2
	 */
	public void swap(Genotype g, int p1, int p2) {
		Genotype[] pos1 = getPosition(this, new int[] { 1, p1 });
		Genotype[] pos2 = getPosition(g, new int[] { 1, p2 });
		Genotype temp = pos1[0].clone();

		pos1[0].left = pos2[0].left;
		pos1[0].right = pos2[0].right;
		pos1[0].token = pos2[0].token;

		pos2[0].left = temp.left;
		pos2[0].right = temp.right;
		pos2[0].token = temp.token;

		fixupTree(this);
		fixupTree(g);
	}

	/**
	 * This function returns the node at a given position.
	 * 
	 * @param g
	 *            The current node.
	 * @param cp
	 *            A two index array containing the current position and the
	 *            position to be obtained.
	 * @return Returns the node at a given position. It is returned as a 1 index
	 *         array so that the reference is passed and not only the value.
	 */
	private Genotype[] getPosition(Genotype g, int[] cp) {
		if (cp[0] == cp[1])
			return new Genotype[] { g };
		if (g.left != null) {
			cp[0] += 1;
			Genotype[] temp = getPosition(g.left, cp);
			if (temp != null)
				return temp;
		}
		if (g.right != null) {
			cp[0] += 1;
			Genotype[] temp = getPosition(g.right, cp);
			if (temp != null)
				return temp;
		}
		return null;
	}

	/**
	 * Returns how many nodes are in the genotype.
	 * 
	 * @return How many nodes are in the genotype.
	 */
	public int length() {
		return count(this);
	}

	/**
	 * Recurisvely counts the number of nodes in the genotype.
	 * 
	 * @param g
	 *            The current node.
	 * @return How many nodes there are.
	 */
	private int count(Genotype g) {
		int length = 1;
		if (g.left != null)
			length += count(g.left);
		if (g.right != null)
			length += count(g.right);
		return length;
	}

	/**
	 * This function constructs the tree for debug purposes in a text view.
	 */
	public StringBuilder toString(StringBuilder prefix, boolean isTail,
			StringBuilder sb) {
		if (right != null) {
			right.toString(
					new StringBuilder().append(prefix).append(
							isTail ? "│   " : "    "), false, sb);
		}
		sb.append(prefix).append(isTail ? "└── " : "┌── ")
				.append(token.toString()).append("\n");
		if (left != null) {
			left.toString(
					new StringBuilder().append(prefix).append(
							isTail ? "    " : "│   "), true, sb);
		}
		return sb;
	}

	@Override
	/**
	 * Constructs the tree in a text view for debugging.
	 */
	public String toString() {
		return this.toString(new StringBuilder(), true, new StringBuilder())
				.toString();
	}

	/**
	 * Returns the genotype encoded as a string.
	 * 
	 * @return The genotype encoded as a string.
	 */
	public String getStorageString() {
		StringBuilder b = new StringBuilder();
		buildStorageString(this, b);
		return b.toString();
	}

	/**
	 * A recusrive function that builds the genotype string.
	 * 
	 * @param g
	 *            The current node.
	 * @param b
	 *            The string builder.
	 */
	private void buildStorageString(Genotype g, StringBuilder b) {
		if (g == null) {
			b.append(";");
			return;
		}
		b.append(String.format("%02d", TokenMap.get(g.token.getToken()))
				+ String.format("%02d", g.token.getAmount()));
		buildStorageString(g.left, b);
		buildStorageString(g.right, b);
	}

	/**
	 * A constructor that is used to decode a string into a genotype.
	 * 
	 * @param s
	 *            The string to decode.
	 */
	public Genotype(String s) {
		decodeString(this, new String[] { s });
	}

	/**
	 * A recursive function that converts a string into a genotype.
	 * 
	 * @param g
	 *            The current node.
	 * @param s
	 *            The string (array to pass by reference).
	 * @return The filled node.
	 */
	private Genotype decodeString(Genotype g, String[] s) {
		char in = s[0].charAt(0);
		if (in == ';') {
			s[0] = s[0].substring(1);
			return null;
		}
		int token = Integer.parseInt(s[0].substring(0, 2));
		int amount = Integer.parseInt(s[0].substring(2, 4));
		if (g != null)
			g.token = new Token(TokenMap.get(token), amount);
		else
			g = new Genotype(new Token(TokenMap.get(token), amount));
		s[0] = s[0].substring(4);
		g.left = decodeString(g.left, s);
		g.right = decodeString(g.right, s);
		return g;
	}

	public void performMutations() {
		tryMutate(this);
		fixupTree(this);
	}

	private void tryMutate(Genotype g) {
		if (Random.getRandom(1, 100) <= HarryLynas_Robot.mutationChance)
			g.token = new Token(true);
		if (g.left != null)
			tryMutate(g.left);
		if (g.right != null)
			tryMutate(g.right);
	}
}
