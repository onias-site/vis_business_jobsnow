package com.vis.business.recruiter.resumes;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;
import com.vis.entities.VisEntityResume;

/**
 * The alias suggested to a recruiter for a resume they open: the five first languages and skills of the column
 * "languages and skills" (the fluent English and Spanish first, then the skills by ranking), the salary expectations,
 * the three first regions (in the order of the DDD) and the years of experience, joined by {@code _} and cut at the
 * size accepted by {@code vis_viewed_resume} (200).
 *
 * <p>Input: the stored resume, {@code rankings} and {@code language}. Output: the input with {@code defaultAlias}.
 */
public class VisBusinessDefaultResumeAlias implements CcpBusiness {

	/** The single instance. */
	public static final VisBusinessDefaultResumeAlias INSTANCE = new VisBusinessDefaultResumeAlias();

	private VisBusinessDefaultResumeAlias() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String languageName = json.getAsString(JnJsonCommonsFields.language);
		JnLanguage language = JnLanguage.valueOf(languageName);
		List<String> topLanguages = VisBusinessBuildResumeRow.getTopLanguagesAndOthers(json, language, true);
		List<String> skills = VisBusinessBuildResumeRow.getSkillsByRanking(json);
		List<String> regions = VisBusinessBuildResumeRow.getRegions(json, language);

		List<String> languagesAndSkills = new ArrayList<>(topLanguages);
		languagesAndSkills.addAll(skills);
		int skillsCount = Math.min(VisBusinessBuildResumeRow.SKILLS_IN_THE_ALIAS, languagesAndSkills.size());
		List<String> aliasSkills = languagesAndSkills.subList(0, skillsCount);

		List<String> parts = new ArrayList<>(aliasSkills);
		boolean hasClt = json.containsAllFields(VisEntityResume.Fields.clt);
		if(hasClt) {
			String clt = VisBusinessBuildResumeRow.getSalary(json, VisEntityResume.Fields.clt, language);
			parts.add(clt);
		}
		boolean hasPj = json.containsAllFields(VisEntityResume.Fields.pj);
		if(hasPj) {
			String pj = VisBusinessBuildResumeRow.getSalary(json, VisEntityResume.Fields.pj, language);
			parts.add(pj);
		}
		int regionsCount = Math.min(VisBusinessBuildResumeRow.VISIBLE_REGIONS, regions.size());
		List<String> aliasRegions = regions.subList(0, regionsCount);
		parts.addAll(aliasRegions);

		int years = VisBusinessBuildResumeRow.getYearsOfExperience(json);
		CcpJsonRepresentation yearsValues = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.years, years);
		String yearsPart = VisBusinessBuildResumeRow.getLine(VisRecruiterResumesTexts.aliasYears, language, yearsValues);
		parts.add(yearsPart);

		String separator = VisBusinessBuildResumeRow.Constants.ALIAS_SEPARATOR.getValue();
		String alias = String.join(separator, parts);
		int aliasLength = Math.min(VisBusinessBuildResumeRow.ALIAS_MAX_LENGTH, alias.length());
		String cutAlias = alias.substring(0, aliasLength);
		CcpJsonRepresentation jsonWithDefaultAlias = json.put(VisRecruiterResumesFields.defaultAlias, cutAlias);
		return jsonWithDefaultAlias;
	}
}
