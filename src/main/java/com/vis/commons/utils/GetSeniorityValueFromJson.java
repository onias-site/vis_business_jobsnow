package com.vis.commons.utils;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpTimeDecorator;
import com.vis.commons.entities.VisEntityPosition;
import com.vis.commons.entities.VisEntityResume;

public enum GetSeniorityValueFromJson implements Function<CcpJsonRepresentation, String> {
	resume {
		public String apply(CcpJsonRepresentation json) {
			Integer experience = json.getAsIntegerNumber(VisEntityResume.Fields.experience.name());
			
			CcpTimeDecorator ctd = new CcpTimeDecorator();
			int currentYear = ctd.getYear();
			int experienceInYears = currentYear - experience;
			
			if(experienceInYears > 2) {
				return "JR";
			}
			
			if(experienceInYears > 5) {
				return "PL";
			}

			if(experienceInYears > 10) {
				return "SR";
			}
			return "ES";
		}
	}, position {
		public String apply(CcpJsonRepresentation json) {
			String seniority = json.getAsString(VisEntityPosition.Fields.seniority.name());
			return seniority;
		}
	};

	public abstract String apply(CcpJsonRepresentation json);
	
}
