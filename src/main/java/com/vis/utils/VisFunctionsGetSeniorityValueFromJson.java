package com.vis.utils;

import java.util.function.Function;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpTimeDecorator;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Extrai ou calcula o valor de senioridade para o processo de matching, com lógica diferente para
 * currículo (calcula a partir do ano de início de experiência) e para vaga (lê diretamente do campo).
 */
public enum VisFunctionsGetSeniorityValueFromJson implements Function<CcpJsonRepresentation, String> {
	resume {
		public String apply(CcpJsonRepresentation json) {
			Integer experience = json.getAsIntegerNumber(VisJsonCommonsFields.experience);
			
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
			String seniority = json.getAsString(VisJsonCommonsFields.seniority);
			return seniority;
		}
	};

	public abstract String apply(CcpJsonRepresentation json);
	
}
