package com.vis.business.recruiter.resumes;

import java.util.Comparator;
import java.util.List;

import com.ccp.decorators.CcpJsonRepresentation;

/**
 * Order of the resumes of the e-mail to the recruiters: type of resume, CLT, PJ and years of experience, the keys put by
 * {@link VisBusinessBuildResumeRow}.
 */
class VisResumesBySortKeys implements Comparator<CcpJsonRepresentation> {

	/** The only instance: the order keeps no state. */
	public final static VisResumesBySortKeys INSTANCE = new VisResumesBySortKeys();

	private VisResumesBySortKeys() {
	}

	/**
	 * Compares the sort keys of the rows, one by one.
	 * @param first a row
	 * @param second another row
	 * @return the comparison of the first different key, or 0
	 */
	public int compare(CcpJsonRepresentation first, CcpJsonRepresentation second) {
		List<Object> firstKeys = first.getAsObjectList(VisRecruiterResumesFields.sortKeys);
		List<Object> secondKeys = second.getAsObjectList(VisRecruiterResumesFields.sortKeys);
		for (int index = 0; index < firstKeys.size(); index++) {
			Number firstKeyNumber = (Number) firstKeys.get(index);
			Number secondKeyNumber = (Number) secondKeys.get(index);
			double firstKey = firstKeyNumber.doubleValue();
			double secondKey = secondKeyNumber.doubleValue();
			int comparison = Double.compare(firstKey, secondKey);
			if(comparison != 0) {
				return comparison;
			}
		}
		return 0;
	}
}
