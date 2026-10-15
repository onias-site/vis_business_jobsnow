package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpTimeDecorator;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityVirtualHashGrouper;
import com.vis.json.fields.validation.VisJsonFieldsSkills;
import com.vis.json.fields.validation.VisLanguages;
import com.vis.json.fields.validation.VisResumeSkillCategories;

/**
 * Produces every search a resume satisfies, as records of {@code VisEntityVirtualHashGrouper}: one list of values per
 * primary key field of the grouper, then every combination among those lists (each combination is one record, whose id
 * is a hash of the resume). Only exact values take part; the numeric ranges (claims, availability, temporary job time)
 * stay out of the hash and are compared after the read by id. A spoken language always gets the neutral value
 * {@code none} too, and so does the skill, so a search that requires no language, or no skill, finds the resume. The
 * regions (ddd) stay out of the hash, compared after the read by id like the ranges: until 2026-10-14 they took part,
 * and multiplied the combinations by the number of regions (up to 48,720 groups for one resume, saves of 230 s).
 */
public class VisResumeHashCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{

	/** Neutral values of the fields. */
	enum JsonFieldValues implements CcpJsonFieldName {
		/** The language name, or the skill name, of a search that requires no language, or no skill. */
		none
	}

	/** The largest number of years of experience: a larger {@code experience} is the year the experience started. */
	private static final int MAX_YEARS_OF_EXPERIENCE = 70;

	/**
	 * Combines the values of every field: each combination becomes one record of the grouper.
	 * @param json the resume
	 * @return the records
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		List<List<CcpJsonRepresentation>> valuesByField = this.getValuesByField(json);

		List<CcpJsonRepresentation> result = Arrays.asList(CcpOtherConstants.EMPTY_JSON);

		for (List<CcpJsonRepresentation> fieldValues : valuesByField) {
			List<CcpJsonRepresentation> combinations = new ArrayList<>();
			for (CcpJsonRepresentation partialCombination : result) {
				for (CcpJsonRepresentation fieldValue : fieldValues) {
					CcpJsonRepresentation combination = partialCombination.mergeWithAnotherJson(fieldValue);
					combinations.add(combination);
				}
			}
			result = combinations;
		}
		return result;
	}

	/**
	 * Counts the records {@link #apply} produces, without building them: the product of the sizes of the lists.
	 * @param json the resume
	 * @return the number of records
	 */
	public long countCombinations(CcpJsonRepresentation json) {
		List<List<CcpJsonRepresentation>> valuesByField = this.getValuesByField(json);
		long count = 1;
		for (List<CcpJsonRepresentation> fieldValues : valuesByField) {
			int size = fieldValues.size();
			count *= size;
		}
		return count;
	}

	/**
	 * Builds, for each primary key field of the grouper, the list of values the resume generates for it.
	 * @param json the resume
	 * @return the lists, one per field
	 */
	public List<List<CcpJsonRepresentation>> getValuesByField(CcpJsonRepresentation json) {

		List<CcpJsonRepresentation> resumeTypeValues = new ArrayList<>();
		Integer resumeType = json.getAsIntegerNumber(VisEntityResume.Fields.resumeType);
		CcpJsonRepresentation resumeTypeValue = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualHashGrouper.Fields.resumeType, resumeType);
		resumeTypeValues.add(resumeTypeValue);

		// a fluent speaker (2) also answers a search for the intermediate level (1)
		List<CcpJsonRepresentation> languageValues = new ArrayList<>();
		String noneName = JsonFieldValues.none.name();
		CcpJsonRepresentation jsonWithNoLanguageName = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualHashGrouper.Fields.languageName, noneName);
		CcpJsonRepresentation noLanguageValue = jsonWithNoLanguageName.put(VisEntityVirtualHashGrouper.Fields.languageLevel, 0);
		languageValues.add(noLanguageValue);
		List<CcpJsonRepresentation> spokenLanguages = json.getAsJsonList(VisEntityResume.Fields.spokenLanguage);
		for (CcpJsonRepresentation language : spokenLanguages) {
			String languageName = language.getAsString(VisLanguages.name);
			Integer declaredLevel = language.getAsIntegerNumber(VisLanguages.level);
			for(int languageLevel = declaredLevel; languageLevel >= 1; languageLevel--) {
				CcpJsonRepresentation jsonWithLanguageName = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualHashGrouper.Fields.languageName, languageName);
				CcpJsonRepresentation languageValue = jsonWithLanguageName.put(VisEntityVirtualHashGrouper.Fields.languageLevel, languageLevel);
				languageValues.add(languageValue);
			}
		}

		// only the skills by which the candidate wants to be found, plus their parents; each name once. The neutral value
		// comes first: a search that requires no skill (the usual one out of information technology) finds the resume too
		Set<String> skillNames = new LinkedHashSet<>();
		skillNames.add(noneName);
		List<CcpJsonRepresentation> skills = json.getAsJsonList(VisEntityResume.Fields.skill);
		for (CcpJsonRepresentation skill : skills) {
			VisResumeSkillCategories category = skill.getAsEnum(VisJsonFieldsSkills.category, VisResumeSkillCategories.class);
			boolean isInvalidSkill = false == category.equals(VisResumeSkillCategories.skillsFromText);

			if(isInvalidSkill) {
				continue;
			}

			String skillName = skill.getAsString(VisJsonFieldsSkills.skill);
			skillNames.add(skillName);
			List<String> parents = skill.getAsStringList(VisJsonFieldsSkills.parent);
			for (String parent : parents) {
				skillNames.add(parent);
			}
		}
		List<CcpJsonRepresentation> skillValues = new ArrayList<>();
		for (String skillName : skillNames) {
			CcpJsonRepresentation skillValue = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualHashGrouper.Fields.skillName, skillName);
			skillValues.add(skillValue);
		}

		Integer[] pcds = VisRecordType.recordToSave.fromBooleanField(VisEntityResume.Fields.pcd, json);
		List<CcpJsonRepresentation> pcdValues = this.toValues(VisEntityVirtualHashGrouper.Fields.pcd, pcds);

		Integer[] negotiableClaims = VisRecordType.recordToSave.fromBooleanField(VisEntityResume.Fields.negotiableClaim, json);
		List<CcpJsonRepresentation> negotiableClaimValues = this.toValues(VisEntityVirtualHashGrouper.Fields.negotiableClaim, negotiableClaims);

		Integer[] travels = VisRecordType.recordToSave.fromBooleanField(VisEntityResume.Fields.travel, json);
		List<CcpJsonRepresentation> travelValues = this.toValues(VisEntityVirtualHashGrouper.Fields.travel, travels);

		// the seniority needs the years of experience, also when the json already carries the year the experience started
		int yearsOfExperience = this.getYearsOfExperience(json);
		CcpJsonRepresentation jsonWithYearsOfExperience = json.put(VisEntityResume.Fields.experience, yearsOfExperience);
		List<CcpJsonRepresentation> seniorityValues = new ArrayList<>();
		List<VisSeniorityType> seniorities = Arrays.asList(VisSeniorityType.values());
		for (VisSeniorityType seniority : seniorities) {
			boolean incompatibleExperience = false == seniority.matches(VisEntityResume.Fields.experience, jsonWithYearsOfExperience);
			if(incompatibleExperience) {
				continue;
			}
			String seniorityName = seniority.name();
			CcpJsonRepresentation seniorityValue = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualHashGrouper.Fields.seniority, seniorityName);
			seniorityValues.add(seniorityValue);
		}

		List<List<CcpJsonRepresentation>> valuesByField = Arrays.asList(
				resumeTypeValues,
				languageValues,
				skillValues,
				pcdValues,
				negotiableClaimValues,
				travelValues,
				seniorityValues
				);
		return valuesByField;
	}

	/**
	 * Reads the years of experience: the field itself on the input of the resume, or the current year minus the field
	 * once {@code VisJsonTransformerExperienceStartYear} turned it into the year the experience started.
	 * @param json the resume
	 * @return the years of experience
	 */
	private int getYearsOfExperience(CcpJsonRepresentation json) {
		Integer experience = json.getAsIntegerNumber(VisEntityResume.Fields.experience);
		boolean isYearsOfExperience = experience <= MAX_YEARS_OF_EXPERIENCE;

		if(isYearsOfExperience) {
			return experience;
		}

		CcpTimeDecorator now = new CcpTimeDecorator();
		int currentYear = now.getYear();
		int yearsOfExperience = currentYear - experience;
		return yearsOfExperience;
	}

	/**
	 * Turns each number into a json with the field.
	 * @param field the field
	 * @param values the numbers
	 * @return one json per number
	 */
	private List<CcpJsonRepresentation> toValues(CcpJsonFieldName field, Integer[] values) {
		List<CcpJsonRepresentation> jsonValues = new ArrayList<>();
		for (Integer value : values) {
			CcpJsonRepresentation jsonValue = CcpOtherConstants.EMPTY_JSON.put(field, value);
			jsonValues.add(jsonValue);
		}
		return jsonValues;
	}
}
