package com.vis.utils;

import java.util.function.Function;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpTimeDecorator;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.fields.validation.VisSeniorityTypes;

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
			boolean experienceInYearsMaior = experienceInYears > 10;

			if(experienceInYearsMaior) {
				return VisSeniorityTypes.ES.name();
			}
			boolean experienceInYearsMaior2 = experienceInYears > 5;

			if(experienceInYearsMaior2) {
				return VisSeniorityTypes.SR.name();
			}
			boolean experienceInYearsMaior3 = experienceInYears > 2;

			if(experienceInYearsMaior3) {
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
