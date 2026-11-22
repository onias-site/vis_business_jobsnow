package com.vis.json.transformers;

/**
 * Produces every search a resume satisfies, as records of {@code VisEntityVirtualHashGrouper}: the hashes of
 * {@link VisHashCombinations} on the side of the record that is found ({@link VisRecordType#recordToSave}), with the
 * area declared in the resume and the seniorities its years of experience reach.
 */
public class VisResumeHashCombinations extends VisHashCombinations {
	/** The single instance: the class keeps no state. */
	public final static VisResumeHashCombinations INSTANCE = new VisResumeHashCombinations();

	private VisResumeHashCombinations() {
		super(VisRecordType.recordToSave);
	}
}
