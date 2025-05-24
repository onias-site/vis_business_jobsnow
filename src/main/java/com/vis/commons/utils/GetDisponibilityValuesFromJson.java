package com.vis.commons.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.commons.entities.VisEntityResume;

public enum GetDisponibilityValuesFromJson implements Function<CcpJsonRepresentation, List<Integer>> {
	resume {
		public List<Integer> apply(CcpJsonRepresentation json) {
			List<Integer> response = new ArrayList<>();
			
			int end = json.getAsDoubleNumber(VisEntityResume.Fields.disponibility.name()).intValue();
			
			for(int k = end; k <= 70; k++) {
				response.add(k);
			}
			
			return response;
		}
	}, position {
		public List<Integer> apply(CcpJsonRepresentation json) {
			List<Integer> response = new ArrayList<>();
			
			int maxDisponibility = json.getAsDoubleNumber(VisEntityResume.Fields.disponibility.name()).intValue();
			
			for(int k = maxDisponibility; k >= 0; k--) {
				response.add(k);
			}
			
			return response;
		}
	};

	public abstract List<Integer> apply(CcpJsonRepresentation json);
	
}
