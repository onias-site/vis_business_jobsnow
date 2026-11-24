package com.vis.business.position;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityPosition;
import com.vis.json.fields.validation.VisJsonFieldsPositionSkills;
import com.vis.json.fields.validation.VisLanguages;
import com.vis.json.fields.validation.VisPositionSkillCategories;

/**
 * The filters of a stored position, as the body of {@code VisBusinessResumesOfTheFilters} (the same the position form
 * sends to count its resumes), for the processes that send the resumes of the positions (since 2026-11-24): the area;
 * the ddds, left out when the position takes the home office (every ddd); the availability and the temporary job time
 * when greater than zero; the seniorities; the required skills (the main list of the position); the languages; the flags
 * that are checked; the salary ranges; and the domain of the e-mail that receives the resumes, so the resumes that forbid
 * that company stay out.
 */
public class VisBusinessFiltersOfThePosition implements CcpBusiness {

	/** The texts of this class. */
	static enum Constants implements CcpJsonFieldName {
		/** The ddd of the home office: with it, the position takes every ddd. */
		HOME_OFFICE_DDD { public String getValue() { return "10"; } },
		/** Separates the user from the domain in an e-mail. */
		AT { public String getValue() { return "@"; } },
	}

	/** The single instance: the class keeps no state. */
	public static final VisBusinessFiltersOfThePosition INSTANCE = new VisBusinessFiltersOfThePosition();

	private VisBusinessFiltersOfThePosition() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation position) {
		Object resumeType = position.get(VisEntityPosition.Fields.resumeType);
		CcpJsonRepresentation filters = CcpOtherConstants.EMPTY_JSON.put(VisBusinessResumesOfTheFilters.Fields.resumeType, resumeType);
		CcpJsonRepresentation withDdds = putDdds(filters, position);
		CcpJsonRepresentation withDisponibility = putPositive(withDdds, position, VisEntityPosition.Fields.disponibility, VisBusinessResumesOfTheFilters.Fields.disponibility);
		CcpJsonRepresentation withTemporallyJobTime = putPositive(withDisponibility, position, VisEntityPosition.Fields.temporallyJobTime, VisBusinessResumesOfTheFilters.Fields.temporallyJobTime);
		CcpJsonRepresentation withSeniority = putSeniorities(withTemporallyJobTime, position);
		CcpJsonRepresentation withSkills = putRequiredSkills(withSeniority, position);
		CcpJsonRepresentation withLanguages = putLanguages(withSkills, position);
		CcpJsonRepresentation withNegotiableClaim = putChecked(withLanguages, position, VisEntityPosition.Fields.negotiableClaim, VisBusinessResumesOfTheFilters.Fields.negotiableClaim);
		CcpJsonRepresentation withPcd = putChecked(withNegotiableClaim, position, VisEntityPosition.Fields.pcd, VisBusinessResumesOfTheFilters.Fields.pcd);
		CcpJsonRepresentation withTravel = putChecked(withPcd, position, VisEntityPosition.Fields.travel, VisBusinessResumesOfTheFilters.Fields.travel);
		CcpJsonRepresentation withClt = putRange(withTravel, position, VisEntityPosition.Fields.clt, VisBusinessResumesOfTheFilters.Fields.clt);
		CcpJsonRepresentation withPj = putRange(withClt, position, VisEntityPosition.Fields.pj, VisBusinessResumesOfTheFilters.Fields.pj);
		CcpJsonRepresentation withDomain = putRecruiterDomain(withPj, position);
		return withDomain;
	}

	private static CcpJsonRepresentation putDdds(CcpJsonRepresentation filters, CcpJsonRepresentation position) {
		List<String> ddds = position.getAsStringList(VisEntityPosition.Fields.ddd);
		String homeOfficeDdd = Constants.HOME_OFFICE_DDD.getValue();
		boolean homeOffice = ddds.stream().anyMatch(ddd -> ddd.replace(".0", "").equals(homeOfficeDdd));
		if(homeOffice) {
			return filters;
		}
		List<Object> dddValues = position.getAsObjectList(VisEntityPosition.Fields.ddd);
		CcpJsonRepresentation withDdds = filters.put(VisBusinessResumesOfTheFilters.Fields.ddd, dddValues);
		return withDdds;
	}

	private static CcpJsonRepresentation putPositive(CcpJsonRepresentation filters, CcpJsonRepresentation position, CcpJsonFieldName positionField, CcpJsonFieldName filterField) {
		boolean absent = false == position.containsAllFields(positionField);
		if(absent) {
			return filters;
		}
		Double value = position.getAsDoubleNumber(positionField);
		boolean noFilter = value <= 0;
		if(noFilter) {
			return filters;
		}
		int intValue = value.intValue();
		CcpJsonRepresentation withValue = filters.put(filterField, intValue);
		return withValue;
	}

	// the positions saved before 2026-11-23 keep a single seniority as a text
	private static CcpJsonRepresentation putSeniorities(CcpJsonRepresentation filters, CcpJsonRepresentation position) {
		Object seniority = position.getOrDefault(VisEntityPosition.Fields.seniority, () -> List.of());
		boolean singleSeniority = seniority instanceof String;
		List<String> seniorities = singleSeniority ? List.of((String) seniority) : position.getAsStringList(VisEntityPosition.Fields.seniority);
		boolean noSeniority = seniorities.isEmpty();
		if(noSeniority) {
			return filters;
		}
		CcpJsonRepresentation withSeniorities = filters.put(VisBusinessResumesOfTheFilters.Fields.seniority, seniorities);
		return withSeniorities;
	}

	private static CcpJsonRepresentation putRequiredSkills(CcpJsonRepresentation filters, CcpJsonRepresentation position) {
		Set<String> skills = getSkills(position, VisPositionSkillCategories.skillsFromText);
		boolean noSkill = skills.isEmpty();
		if(noSkill) {
			return filters;
		}
		List<String> skillList = new ArrayList<>(skills);
		CcpJsonRepresentation withSkills = filters.put(VisBusinessResumesOfTheFilters.Fields.skill, skillList);
		return withSkills;
	}

	/**
	 * The names of the skills of a category of the position, each one once.
	 * @param position the stored position
	 * @param category the category (the required skills are the main list, {@code skillsFromText})
	 * @return the names
	 */
	public static Set<String> getSkills(CcpJsonRepresentation position, VisPositionSkillCategories category) {
		Set<String> skills = new LinkedHashSet<>();
		String categoryName = category.name();
		List<CcpJsonRepresentation> positionSkills = position.getAsJsonList(VisEntityPosition.Fields.skill);
		for (CcpJsonRepresentation positionSkill : positionSkills) {
			String skillCategory = positionSkill.getAsString(VisJsonFieldsPositionSkills.category);
			boolean otherCategory = false == categoryName.equals(skillCategory);
			if(otherCategory) {
				continue;
			}
			String skillName = positionSkill.getAsString(VisJsonFieldsPositionSkills.skill);
			skills.add(skillName);
		}
		return skills;
	}

	private static CcpJsonRepresentation putLanguages(CcpJsonRepresentation filters, CcpJsonRepresentation position) {
		List<CcpJsonRepresentation> positionLanguages = position.getAsJsonList(VisEntityPosition.Fields.spokenLanguage);
		boolean noLanguage = positionLanguages.isEmpty();
		if(noLanguage) {
			return filters;
		}
		List<CcpJsonRepresentation> languages = new ArrayList<>();
		for (CcpJsonRepresentation positionLanguage : positionLanguages) {
			CcpJsonRepresentation language = positionLanguage.getJsonPiece(VisLanguages.name, VisLanguages.level);
			languages.add(language);
		}
		CcpJsonRepresentation withLanguages = filters.put(VisBusinessResumesOfTheFilters.Fields.spokenLanguage, languages);
		return withLanguages;
	}

	private static CcpJsonRepresentation putChecked(CcpJsonRepresentation filters, CcpJsonRepresentation position, CcpJsonFieldName positionField, CcpJsonFieldName filterField) {
		boolean checked = position.containsAllFields(positionField) && position.getAsBoolean(positionField);
		if(false == checked) {
			return filters;
		}
		CcpJsonRepresentation withFlag = filters.put(filterField, true);
		return withFlag;
	}

	private static CcpJsonRepresentation putRange(CcpJsonRepresentation filters, CcpJsonRepresentation position, CcpJsonFieldName positionField, CcpJsonFieldName filterField) {
		boolean absent = false == position.containsAllFields(positionField);
		if(absent) {
			return filters;
		}
		CcpJsonRepresentation range = position.getInnerJson(positionField);
		CcpJsonRepresentation withRange = filters.put(filterField, range);
		return withRange;
	}

	private static CcpJsonRepresentation putRecruiterDomain(CcpJsonRepresentation filters, CcpJsonRepresentation position) {
		String resumesEmail = position.getOrDefault(VisEntityPosition.Fields.resumesEmail, () -> "");
		String at = Constants.AT.getValue();
		int atIndex = resumesEmail.indexOf(at);
		boolean noDomain = atIndex < 0;
		if(noDomain) {
			return filters;
		}
		String domain = resumesEmail.substring(atIndex + 1);
		String lowerCaseDomain = domain.toLowerCase();
		CcpJsonRepresentation withDomain = filters.put(VisBusinessResumesOfTheFilters.Fields.recruiterDomain, lowerCaseDomain);
		return withDomain;
	}
}
