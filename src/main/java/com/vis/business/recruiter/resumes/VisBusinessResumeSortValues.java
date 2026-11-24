package com.vis.business.recruiter.resumes;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityResume;
import com.vis.json.fields.validation.VisJsonFieldsSkills;
import com.vis.json.fields.validation.VisLanguages;
import com.vis.json.fields.validation.VisResumeSkillCategories;

/**
 * The raw values of a resume by which the screen sorts the list of the tab "Currículos aderentes" of the position form
 * (since 2026-11-24), in the order the recruiter chose there: the browser sorts the list it already has, so changing the
 * order needs no new request. The claims (absent when the resume has none), the years of experience, the days to start,
 * the months of temporary job, the level of each language ({@code {name: level}}), the skills the candidate wrote and the
 * skills above them in the hierarchy (a desirable skill of the position counts when it is in either list).
 */
public class VisBusinessResumeSortValues implements CcpBusiness {

	/** The fields of the output that are not fields of the resume. */
	public static enum Fields implements CcpJsonFieldName {
		/** The skills above the ones the candidate wrote, in the hierarchy. */
		parentSkill,
	}

	/** The single instance: the class keeps no state. */
	public static final VisBusinessResumeSortValues INSTANCE = new VisBusinessResumeSortValues();

	private VisBusinessResumeSortValues() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation resume) {
		int years = VisBusinessBuildResumeRow.getYearsOfExperience(resume);
		CcpJsonRepresentation languages = getLanguageLevels(resume);
		Set<String> writtenSkills = getSkills(resume, VisResumeSkillCategories.skillsFromText);
		Set<String> parentSkills = getSkills(resume, VisResumeSkillCategories.parent);
		List<String> writtenSkillsList = List.copyOf(writtenSkills);
		List<String> parentSkillsList = List.copyOf(parentSkills);
		Object disponibility = resume.getOrDefault(VisEntityResume.Fields.disponibility, () -> 0);
		Object temporallyJobTime = resume.getOrDefault(VisEntityResume.Fields.temporallyJobTime, () -> 0);
		CcpJsonRepresentation claims = resume.getJsonPiece(VisEntityResume.Fields.clt, VisEntityResume.Fields.pj);
		CcpJsonRepresentation sortValues = claims
				.put(VisEntityResume.Fields.experience, years)
				.put(VisEntityResume.Fields.disponibility, disponibility)
				.put(VisEntityResume.Fields.temporallyJobTime, temporallyJobTime)
				.put(VisEntityResume.Fields.spokenLanguage, languages)
				.put(VisEntityResume.Fields.skill, writtenSkillsList)
				.put(Fields.parentSkill, parentSkillsList);
		return sortValues;
	}

	private static CcpJsonRepresentation getLanguageLevels(CcpJsonRepresentation resume) {
		CcpJsonRepresentation levels = CcpOtherConstants.EMPTY_JSON;
		List<CcpJsonRepresentation> languages = resume.getAsJsonList(VisEntityResume.Fields.spokenLanguage);
		for (CcpJsonRepresentation language : languages) {
			String name = language.getAsString(VisLanguages.name);
			Object level = language.get(VisLanguages.level);
			levels = levels.put(() -> name, level);
		}
		return levels;
	}

	private static Set<String> getSkills(CcpJsonRepresentation resume, VisResumeSkillCategories category) {
		Set<String> skills = new LinkedHashSet<>();
		String categoryName = category.name();
		List<CcpJsonRepresentation> resumeSkills = resume.getAsJsonList(VisEntityResume.Fields.skill);
		for (CcpJsonRepresentation resumeSkill : resumeSkills) {
			String skillCategory = resumeSkill.getAsString(VisJsonFieldsSkills.category);
			boolean sameCategory = categoryName.equals(skillCategory);
			if(false == sameCategory) {
				continue;
			}
			String skillName = resumeSkill.getAsString(VisJsonFieldsSkills.skill);
			skills.add(skillName);
		}
		return skills;
	}
}
