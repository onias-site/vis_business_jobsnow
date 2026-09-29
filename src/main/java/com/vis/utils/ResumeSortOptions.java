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

	disponibility(VisJsonCommonsFields.disponibility.name()),
	desiredSkill(VisEntityPosition.Fields.desiredSkill.name()),
	money(VisJsonCommonsFields.clt.name(), VisJsonCommonsFields.pj.name(), VisJsonCommonsFields.btc.name()),
	experience(VisJsonCommonsFields.experience.name()),
	;
	final String[] fieldsToSort;
	
	
	private ResumeSortOptions(String... fieldsToSort) {
		this.fieldsToSort = fieldsToSort;
	}

	public int compare(CcpJsonRepresentation o1, CcpJsonRepresentation o2) {
		int comparisonResult = this.compareTo(o1, o2, this.fieldsToSort);
		return comparisonResult;
	}
	
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
