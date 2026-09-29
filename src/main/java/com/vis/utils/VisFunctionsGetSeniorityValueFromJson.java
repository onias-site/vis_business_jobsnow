package com.vis.utils;

import java.util.function.Function;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpTimeDecorator;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.fields.validation.VisSeniorityTypes;

/**
 * Extracts or calculates the seniority value for the matching process, with different logic for
 * resumes (calculated from the year the experience started) and for positions (read straight from the field).
 */
public enum VisFunctionsGetSeniorityValueFromJson implements Function<CcpJsonRepresentation, String> {
	resume {
		public String apply(CcpJsonRepresentation json) {
			Integer experience = json.getAsIntegerNumber(VisJsonCommonsFields.experience);
			
			CcpTimeDecorator timeDecorator = new CcpTimeDecorator();
			int currentYear = timeDecorator.getYear();
			int experienceInYears = currentYear - experience;
			boolean isSpecialist = experienceInYears > 10;

			if(isSpecialist) {
				return VisSeniorityTypes.ES.name();
			}
			boolean isSenior = experienceInYears > 5;

			if(isSenior) {
				return VisSeniorityTypes.SR.name();
			}
			boolean isMidLevel = experienceInYears > 2;

			if(isMidLevel) {
				return VisSeniorityTypes.PL.name();
			}
			return VisSeniorityTypes.JR.name();
		}
	}, position {
		public String apply(CcpJsonRepresentation json) {
			String seniority = json.getAsString(VisJsonCommonsFields.seniority);
			return seniority;
		}
	};

	public abstract String apply(CcpJsonRepresentation json);
	
}
