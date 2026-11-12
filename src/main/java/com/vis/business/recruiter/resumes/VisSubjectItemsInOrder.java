package com.vis.business.recruiter.resumes;

import java.util.Comparator;

import com.ccp.decorators.CcpJsonRepresentation;

/**
 * The order of the items of the subject of the e-mail to the recruiters, the same of the column "languages and skills":
 * by group (fluent English and Spanish, skills, other languages), then by the order inside the group (the position of the
 * language, the ranking of the skill), then by the text.
 */
public class VisSubjectItemsInOrder implements Comparator<CcpJsonRepresentation> {

	/** The single instance: the class keeps no state. */
	public static final VisSubjectItemsInOrder INSTANCE = new VisSubjectItemsInOrder();

	private VisSubjectItemsInOrder() {}

	public int compare(CcpJsonRepresentation first, CcpJsonRepresentation second) {
		Integer firstGroup = first.getAsIntegerNumber(VisRecruiterResumesFields.itemGroup);
		Integer secondGroup = second.getAsIntegerNumber(VisRecruiterResumesFields.itemGroup);
		int byGroup = firstGroup.compareTo(secondGroup);
		if(byGroup != 0) {
			return byGroup;
		}
		Double firstOrder = first.getAsDoubleNumber(VisRecruiterResumesFields.itemOrder);
		Double secondOrder = second.getAsDoubleNumber(VisRecruiterResumesFields.itemOrder);
		int byOrder = firstOrder.compareTo(secondOrder);
		if(byOrder != 0) {
			return byOrder;
		}
		String firstLabel = first.getAsString(VisRecruiterResumesFields.itemLabel);
		String secondLabel = second.getAsString(VisRecruiterResumesFields.itemLabel);
		int byLabel = firstLabel.compareTo(secondLabel);
		return byLabel;
	}
}
