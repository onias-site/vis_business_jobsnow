package com.vis.business.statistics;

import java.util.Arrays;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityPhase;
import com.jn.entities.JnEntityVirtualRecordsTotal;
import com.jn.utils.JnGrouperChart;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityVirtualAvoidedSkillsCounter;
import com.vis.entities.VisEntityVirtualNotAllowedCompanyCounter;

/**
 * The provider of the charts of vis in the screen Curiosities, run by class name by {@code JnBusinessGetGrouperCharts}
 * (seeded in {@code JnEntityGrouperChartsProviders} by {@link VisEntityResume}): the groups of {@code jn_grouper} behind
 * each chart, read by id.
 */
public class VisBusinessCuriositiesCharts implements CcpBusiness {

	/** The charts of vis and the slices named here. No input. */
	public static enum Fields implements CcpJsonFieldName {
		/** The companies the owners of the resumes forbid the most ({@code {domain: n}}). */
		notAllowedCompanies,
		/** The skills the owners of the resumes exclude the most ({@code {skill: n}}). */
		avoidedSkills,
		/** The resumes active and inactive now. */
		resumeSituation,
		/** The slice of the active resumes. */
		active,
		/** The slice of the inactive resumes. */
		inactive,
	}

	/** The single instance. */
	public static final VisBusinessCuriositiesCharts INSTANCE = new VisBusinessCuriositiesCharts();

	private VisBusinessCuriositiesCharts() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	/**
	 * The groups of the charts of vis.
	 * @param json not used
	 * @return {@code {"charts": [...]}}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation notAllowedCompaniesCombination = VisEntityVirtualNotAllowedCompanyCounter.getCombination();
		CcpJsonRepresentation notAllowedCompanies = JnGrouperChart.counters(Fields.notAllowedCompanies, VisEntityVirtualNotAllowedCompanyCounter.ENTITY, VisEntityResume.ENTITY, notAllowedCompaniesCombination);
		CcpJsonRepresentation avoidedSkillsCombination = VisEntityVirtualAvoidedSkillsCounter.getCombination();
		CcpJsonRepresentation avoidedSkills = JnGrouperChart.counters(Fields.avoidedSkills, VisEntityVirtualAvoidedSkillsCounter.ENTITY, VisEntityResume.ENTITY, avoidedSkillsCombination);
		// the total of the resumes is one group on each side: the active ones on the main side, the inactive ones on the twin
		CcpEntityMetaData resumeDetails = VisEntityResume.ENTITY.getEntityMetaData();
		CcpEntityMetaData totalDetails = JnEntityVirtualRecordsTotal.ENTITY.getEntityMetaData();
		String totalParentId = JnEntityVirtualRecordsTotal.getParentId(resumeDetails.entityName);
		CcpJsonRepresentation activeResumes = JnGrouperChart.sum(Fields.resumeSituation, Fields.active, totalDetails.entityName, resumeDetails.entityName, totalParentId, CcpEntityPhase.mainEntity);
		CcpJsonRepresentation inactiveResumes = JnGrouperChart.sum(Fields.resumeSituation, Fields.inactive, totalDetails.entityName, resumeDetails.entityName, totalParentId, CcpEntityPhase.twinEntity);
		List<CcpJsonRepresentation> charts = Arrays.asList(notAllowedCompanies, avoidedSkills, activeResumes, inactiveResumes);
		CcpJsonRepresentation output = JnGrouperChart.output(charts);
		return output;
	}
}
