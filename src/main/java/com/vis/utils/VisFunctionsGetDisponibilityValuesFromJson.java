package com.vis.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Gera listas de valores de disponibilidade (em dias) para o processo de cálculo de hashes de compatibilidade,
 * com comportamento diferente para currículo e vaga.
 */
public enum VisFunctionsGetDisponibilityValuesFromJson implements Function<CcpJsonRepresentation, List<Integer>> {
	resume {
		public List<Integer> apply(CcpJsonRepresentation json) {
			List<Integer> response = new ArrayList<>();
			Double asDoubleNumber = json.getAsDoubleNumber(VisJsonCommonsFields.disponibility);

			int end = asDoubleNumber.intValue();
			
			for(int k = end; k <= 70; k++) {
				response.add(k);
			}
			
			return response;
		}
	}, position {
		public List<Integer> apply(CcpJsonRepresentation json) {
			List<Integer> response = new ArrayList<>();
			Double asDoubleNumber2 = json.getAsDoubleNumber(VisJsonCommonsFields.disponibility);

			int maxDisponibility = asDoubleNumber2.intValue();
			
			for(int k = maxDisponibility; k >= 0; k--) {
				response.add(k);
			}
			
			return response;
		}
	};

	public abstract List<Integer> apply(CcpJsonRepresentation json);
	
}
