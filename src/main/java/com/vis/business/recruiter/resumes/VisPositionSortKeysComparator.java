package com.vis.business.recruiter.resumes;

import java.util.Comparator;
import java.util.List;

import com.ccp.decorators.CcpJsonRepresentation;

/**
 * Compares two rows by the keys of the order of the position ({@code positionSortKeys}), in ascending order: the first
 * different key decides. Equal rows keep their order (the sort of the list is stable).
 */
public class VisPositionSortKeysComparator implements Comparator<CcpJsonRepresentation> {

	/** The single instance: the class keeps no state. */
	public static final VisPositionSortKeysComparator INSTANCE = new VisPositionSortKeysComparator();

	private VisPositionSortKeysComparator() {}

	public int compare(CcpJsonRepresentation first, CcpJsonRepresentation second) {
		List<Object> firstKeys = first.getAsObjectList(VisBusinessSortResumesOfThePosition.Fields.positionSortKeys);
		List<Object> secondKeys = second.getAsObjectList(VisBusinessSortResumesOfThePosition.Fields.positionSortKeys);
		int size = Math.min(firstKeys.size(), secondKeys.size());
		for (int index = 0; index < size; index++) {
			Number firstKey = (Number) firstKeys.get(index);
			Number secondKey = (Number) secondKeys.get(index);
			double firstValue = firstKey.doubleValue();
			double secondValue = secondKey.doubleValue();
			int comparison = Double.compare(firstValue, secondValue);
			boolean different = comparison != 0;
			if(different) {
				return comparison;
			}
		}
		return 0;
	}
}
