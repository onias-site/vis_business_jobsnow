package com.vis.services;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.services.JnService;
import com.vis.business.statistics.VisBusinessMonthsOfTheSkill;
import com.vis.business.statistics.VisBusinessSkillsOfTheMonth;

/**
 * The statistics of the skills, calculated from the positions of Telegram. They are public and read by id (OQL).
 */
public enum VisServiceSkillStatistics implements JnService {
	/** The skills of a month, region and seniority. */
	GetSkillsOfTheMonth{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessSkillsOfTheMonth.INSTANCE.execute(json);
			return response;
		}

		public Class<?> getJsonValidationClass() {
			return VisBusinessSkillsOfTheMonth.Fields.class;
		}
	},
	/** The months of a skill in a region and seniority. */
	GetMonthsOfTheSkill{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessMonthsOfTheSkill.INSTANCE.execute(json);
			return response;
		}

		public Class<?> getJsonValidationClass() {
			return VisBusinessMonthsOfTheSkill.Fields.class;
		}
	},
	;
}
