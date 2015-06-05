package uk.ac.reading.pm002501.misc;

import uk.ac.reading.pm002501.tree.Token;

public class Store {
	public Token[] sequence;
	public Token[] onScan;
	public Token[] onHit;
	public Token[] onTake;
	public int[] colours;

	public Store(Token[] seq, Token[] scan, Token[] hit, Token[] take,
			int[] colours) {
		sequence = seq;
		onScan = scan;
		onHit = hit;
		onTake = take;
		this.colours = colours;
	}
}
