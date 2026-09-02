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
			boolean experienceInYearsMaior = experienceInYears > 2;

			if(experienceInYearsMaior) {
				return "JR";
			}
			boolean experienceInYearsMaior2 = experienceInYears > 5;

			if(experienceInYearsMaior2) {
				return "PL";
			}
			boolean experienceInYearsMaior3 = experienceInYears > 10;

			if(experienceInYearsMaior3) {
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
