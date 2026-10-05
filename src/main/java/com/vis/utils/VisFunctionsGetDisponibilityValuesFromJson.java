package com.vis.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Generates lists of availability values (in days) for the compatibility hash calculation process,
 * behaving differently for resumes and positions.
 */
public enum VisFunctionsGetDisponibilityValuesFromJson implements Function<CcpJsonRepresentation, List<Integer>> {
	/** For a resume: every availability from the declared one up to 70 days. */
	resume {
		/**
		 * Generates the availabilities of the resume.
		 * @param json the resume
		 * @return the availabilities
		 */
		public List<Integer> apply(CcpJsonRepresentation json) {
			List<Integer> response = new ArrayList<>();
			Double declaredDisponibility = json.getAsDoubleNumber(VisJsonCommonsFields.disponibility);

			int candidateDisponibility = declaredDisponibility.intValue();
			
			for(int k = candidateDisponibility; k <= 70; k++) {
				response.add(k);
			}
			
			return response;
		}
	},
	/** For a position: every availability from the declared maximum down to 0. */
	position {
		/**
		 * Generates the availabilities of the position.
		 * @param json the position
		 * @return the availabilities
		 */
		public List<Integer> apply(CcpJsonRepresentation json) {
			List<Integer> response = new ArrayList<>();
			Double declaredMaxDisponibility = json.getAsDoubleNumber(VisJsonCommonsFields.disponibility);

			int maxDisponibility = declaredMaxDisponibility.intValue();
			
			for(int k = maxDisponibility; k >= 0; k--) {
				response.add(k);
			}
			
			return response;
		}
	};

	/**
	 * Generates the availabilities, in days.
	 * @param json the resume or the position
	 * @return the availabilities
	 */
	public abstract List<Integer> apply(CcpJsonRepresentation json);
	
}
