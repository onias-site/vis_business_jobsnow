package com.vis.utils;

import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityPosition;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Defines the criteria for sorting resumes against a position. Each constant represents a numeric
 * comparison criterion between two resumes, using the relevant fields of the resume JSON.
 */
enum ResumeSortOptions {

	/** By availability (ascending: the sooner first). */
	disponibility(VisJsonCommonsFields.disponibility.name()),
	/** By the number of desired skills (stored negative, so the most first). */
	desiredSkill(VisEntityPosition.Fields.desiredSkill.name()),
	/** By CLT, then PJ, then bitcoin value (ascending). */
	money(VisJsonCommonsFields.clt.name(), VisJsonCommonsFields.pj.name(), VisJsonCommonsFields.btc.name()),
	/** By the year the experience started (ascending: the most experienced first). */
	experience(VisJsonCommonsFields.experience.name()),
	;
	/** The fields compared, in order. */
	final String[] fieldsToSort;
	
	
	/**
	 * Associates the criterion with its fields.
	 * @param fieldsToSort the fields compared, in order
	 */
	private ResumeSortOptions(String... fieldsToSort) {
		this.fieldsToSort = fieldsToSort;
	}

	/**
	 * Compares two resumes by the fields of the criterion.
	 * @param o1 the first resume
	 * @param o2 the second resume
	 * @return the comparison of the first field that differs (ascending), or 0
	 */
	public int compare(CcpJsonRepresentation o1, CcpJsonRepresentation o2) {
		int comparisonResult = this.compareTo(o1, o2, this.fieldsToSort);
		return comparisonResult;
	}
	
	/**
	 * Compares the numeric fields in order; a field absent in either resume is skipped.
	 * @param o1 the first resume
	 * @param o2 the second resume
	 * @param keys the fields
	 * @return the comparison of the first field that differs, or 0
	 */
	private int compareTo(CcpJsonRepresentation o1, CcpJsonRepresentation o2, String... keys) {
		
		for (String key : keys) {
			CcpFieldName firstKeyFieldName = new CcpFieldName(key);
			boolean firstHasField = o1.containsAllFields(firstKeyFieldName);
			boolean firstLacksField = false == firstHasField;
		
			if(firstLacksField) {
				continue;
			}
			CcpFieldName secondKeyFieldName = new CcpFieldName(key);
			boolean secondHasField = o2.containsAllFields(secondKeyFieldName);
			boolean secondLacksField = false == secondHasField;

			if(secondLacksField) {
				continue;
			}
			CcpFieldName firstValueFieldName = new CcpFieldName(key);

			Double value1 = o1.getAsDoubleNumber(firstValueFieldName);
			CcpFieldName secondValueFieldName = new CcpFieldName(key);
			Double value2 = o2.getAsDoubleNumber(secondValueFieldName);
			
			int comparisonResult = value1.compareTo(value2);
			
			boolean areEquals = comparisonResult == 0;
			
			if(areEquals) {
				continue;
			}
			
			return comparisonResult;
		}
		return 0;
	}
	
}
