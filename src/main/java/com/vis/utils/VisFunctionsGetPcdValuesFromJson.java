package com.vis.utils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityPosition;

/**
 * Determines which positions (or candidates) a given PCD profile can be compared with, returning
 * the list of PCD boolean values used to generate the compatibility hashes.
 */
public enum VisFunctionsGetPcdValuesFromJson implements Function<CcpJsonRepresentation, List<Boolean>> {
	/** For a resume: a candidate with a disability gets both values, the others only {@code false}. */
	resume {
		/**
		 * Returns the disability values of the resume.
		 * @param json the resume
		 * @return the values
		 */
		public List<Boolean> apply(CcpJsonRepresentation json) {
			boolean pcdCandidate = json.getAsBoolean(VisEntityPosition.Fields.pcd);

			if(pcdCandidate) {
				// PCD candidates can compete for regular positions and for PCD positions.
				return Arrays.asList(true, false);
			}
			// Regular candidates can compete only for regular positions.
			return Arrays.asList(false);
		}
	},
	/** For a position: a position for people with a disability gets {@code true} only, the others both values. */
	position {
		/**
		 * Returns the disability values of the position.
		 * @param json the position
		 * @return the values
		 */
		public List<Boolean> apply(CcpJsonRepresentation json) {
			boolean pcd = json.getAsBoolean(VisEntityPosition.Fields.pcd);

			boolean pcdPosition = pcd;
			// PCD positions can filter only PCD positions.
			if(pcdPosition) {
				return Arrays.asList(true);
			}
			// Regular positions can filter PCD candidates and regular candidates.
			return Arrays.asList(true, false);
		}
	};

	/**
	 * Returns the disability values compared in the matching.
	 * @param json the resume or the position
	 * @return the values
	 */
	public abstract List<Boolean> apply(CcpJsonRepresentation json);
	
}
