package com.vis.business.recruiter.resumes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntitySkill;
import com.vis.json.fields.validation.VisJsonFieldsSkills;

/**
 * Reads, by id and in a single round trip (MGIE), the ranking in {@code vis_skill} of every skill of the given resumes.
 *
 * <p>Input: {@code resumes} (the stored resumes). Output: the input with {@code rankings}, a json whose fields are the
 * skills and whose values are their rankings; a skill unknown to {@code vis_skill} is left out.
 */
public class VisBusinessLoadSkillRankings implements CcpBusiness {

	/** The single instance. */
	public static final VisBusinessLoadSkillRankings INSTANCE = new VisBusinessLoadSkillRankings();

	private VisBusinessLoadSkillRankings() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		List<CcpJsonRepresentation> resumes = json.getAsJsonList(VisRecruiterResumesFields.resumes);
		Set<String> skills = new LinkedHashSet<>();
		for (CcpJsonRepresentation resume : resumes) {
			boolean noSkill = false == resume.containsAllFields(VisEntityResume.Fields.skill);
			if(noSkill) {
				continue;
			}
			List<CcpJsonRepresentation> resumeSkills = resume.getAsJsonList(VisEntityResume.Fields.skill);
			for (CcpJsonRepresentation resumeSkill : resumeSkills) {
				String skill = resumeSkill.getAsString(VisJsonFieldsSkills.skill);
				skills.add(skill);
			}
		}

		boolean noSkills = skills.isEmpty();
		if(noSkills) {
			CcpJsonRepresentation withoutRankings = json.put(VisRecruiterResumesFields.rankings, CcpOtherConstants.EMPTY_JSON);
			return withoutRankings;
		}

		List<CcpJsonRepresentation> skillIds = new ArrayList<>();
		for (String skill : skills) {
			CcpJsonRepresentation skillId = CcpOtherConstants.EMPTY_JSON.put(VisEntitySkill.Fields.skill, skill);
			skillIds.add(skillId);
		}
		CcpJsonRepresentation[] skillIdsArray = skillIds.toArray(new CcpJsonRepresentation[skillIds.size()]);
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpSelectUnionAll skillsById = crud.unionAll(skillIdsArray, JnDeleteKeysFromCache.INSTANCE, VisEntitySkill.ENTITY);

		CcpJsonRepresentation rankings = CcpOtherConstants.EMPTY_JSON;
		for (CcpJsonRepresentation skillId : skillIds) {
			boolean unknownSkill = false == VisEntitySkill.ENTITY.isPresentInThisUnionAll(skillsById, skillId);
			if(unknownSkill) {
				continue;
			}
			Supplier<CcpJsonRepresentation> skillIdSupplier = skillId.getJsonSupplier();
			CcpJsonRepresentation skillRecord = VisEntitySkill.ENTITY.getRecordFromUnionAll(skillsById, skillIdSupplier);
			boolean noRanking = false == skillRecord.containsAllFields(VisEntitySkill.Fields.ranking);
			if(noRanking) {
				continue;
			}
			String skill = skillId.getAsString(VisEntitySkill.Fields.skill);
			Double ranking = skillRecord.getAsDoubleNumber(VisEntitySkill.Fields.ranking);
			CcpFieldName skillField = new CcpFieldName(skill);
			rankings = rankings.put(skillField, ranking);
		}
		CcpJsonRepresentation jsonWithRankings = json.put(VisRecruiterResumesFields.rankings, rankings);
		return jsonWithRankings;
	}
}
