package com.vis.json.transformers;

/**
 * Produces the searches of a position, as records of {@code VisEntityVirtualHashGrouper}: the hashes of
 * {@link VisHashCombinations} on the side of the record that searches ({@link VisRecordType#recordToSearch}), with
 * every area of resume (the position declares none) and the seniority the position asks for.
 */
public class VisPositionHashCombinations extends VisHashCombinations {
	/** The single instance: the class keeps no state. */
	public final static VisPositionHashCombinations INSTANCE = new VisPositionHashCombinations();

	private VisPositionHashCombinations() {
		super(VisRecordType.recordToSearch);
	}
}
