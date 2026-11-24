package com.vis.business.position;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityResume;
import com.vis.json.fields.validation.VisLanguages;
import com.vis.json.transformers.VisResumeSkillsFromResumeChildField;

/**
 * Tells whether a single resume satisfies the filters of a position, with the same rules of the Painless script of
 * {@link VisBusinessResumesOfTheFilters} ({@code map.painless}), applied in Java to the resume just saved (since
 * 2026-11-24, the positions that receive the resumes instantly). The count reads the groups of {@code jn_grouper}, which the
 * save of the resume updates only after its side effects: here the resume is compared directly. Besides the rules of the
 * script, the area (resumeType) and the regions, which the count gets from the groups it reads: the resume needs one of
 * the ddds of the position, unless the position takes every ddd (no ddd in the filters).
 */
public class VisBusinessResumeMatchesTheFilters implements CcpBusiness {

	/** The input and the output of this business. */
	public static enum Fields implements CcpJsonFieldName {
		/** The filters, the body of {@link VisBusinessResumesOfTheFilters} ({@code VisBusinessFiltersOfThePosition}). */
		filters,
		/** The resume, as stored. */
		resume,
		/** The output: whether the resume satisfies the filters. */
		matches,
	}

	/** The single instance: the class keeps no state. */
	public static final VisBusinessResumeMatchesTheFilters INSTANCE = new VisBusinessResumeMatchesTheFilters();

	private VisBusinessResumeMatchesTheFilters() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation filters = json.getInnerJson(Fields.filters);
		CcpJsonRepresentation storedResume = json.getInnerJson(Fields.resume);
		CcpJsonRepresentation skillNames = VisResumeSkillsFromResumeChildField.INSTANCE.apply(storedResume);
		CcpJsonRepresentation resume = storedResume.mergeWithAnotherJson(skillNames);
		CcpJsonRepresentation params = VisBusinessResumesOfTheFilters.getFilters(filters);
		boolean matches = isOfTheAreaAndRegions(filters, resume) && matches(params, resume);
		CcpJsonRepresentation response = json.put(Fields.matches, matches);
		return response;
	}

	private static boolean isOfTheAreaAndRegions(CcpJsonRepresentation filters, CcpJsonRepresentation resume) {
		String positionType = getNumberText(filters, VisBusinessResumesOfTheFilters.Fields.resumeType);
		String resumeType = getNumberText(resume, VisEntityResume.Fields.resumeType);
		boolean otherArea = false == positionType.equals(resumeType);
		if(otherArea) {
			return false;
		}
		boolean everyDdd = false == filters.containsAllFields(VisBusinessResumesOfTheFilters.Fields.ddd);
		if(everyDdd) {
			return true;
		}
		List<String> positionDdds = getNumberTexts(filters, VisBusinessResumesOfTheFilters.Fields.ddd);
		List<String> resumeDdds = getNumberTexts(resume, VisEntityResume.Fields.ddd);
		boolean sharesADdd = resumeDdds.stream().anyMatch(positionDdds::contains);
		return sharesADdd;
	}

	private static boolean matches(CcpJsonRepresentation params, CcpJsonRepresentation resume) {
		boolean late = isAbove(params, VisBusinessResumesOfTheFilters.SearchFields.maxDisponibility, resume, VisEntityResume.Fields.disponibility);
		if(late) {
			return false;
		}
		boolean tooLong = isAbove(params, VisBusinessResumesOfTheFilters.SearchFields.maxTemporallyJobTime, resume, VisEntityResume.Fields.temporallyJobTime);
		if(tooLong) {
			return false;
		}
		double experience = getNumber(resume, VisEntityResume.Fields.experience);
		List<CcpJsonRepresentation> experienceRanges = params.getAsJsonList(VisBusinessResumesOfTheFilters.SearchFields.experience);
		boolean otherSeniority = isOutOfEveryRange(experience, experienceRanges);
		if(otherSeniority) {
			return false;
		}
		boolean withoutAFlag = lacksAFlag(params, resume, VisBusinessResumesOfTheFilters.Fields.negotiableClaim, VisBusinessResumesOfTheFilters.Fields.pcd, VisBusinessResumesOfTheFilters.Fields.travel);
		if(withoutAFlag) {
			return false;
		}
		boolean outOfTheBudgets = isOutOfTheBudgets(params, resume);
		if(outOfTheBudgets) {
			return false;
		}
		boolean forbidsTheRecruiter = forbidsTheRecruiter(params, resume);
		if(forbidsTheRecruiter) {
			return false;
		}
		boolean speaksTheLanguages = speaksTheLanguages(params, resume);
		if(false == speaksTheLanguages) {
			return false;
		}
		boolean hasTheSkills = hasTheSkills(params, resume);
		return hasTheSkills;
	}

	private static boolean isAbove(CcpJsonRepresentation params, CcpJsonFieldName limitField, CcpJsonRepresentation resume, CcpJsonFieldName resumeField) {
		boolean noLimit = false == params.containsAllFields(limitField);
		if(noLimit) {
			return false;
		}
		Double limit = params.getAsDoubleNumber(limitField);
		double value = getNumber(resume, resumeField);
		boolean above = value > limit;
		return above;
	}

	// a range {min, max}, inclusive; a resume without the value is out of an informed range
	private static boolean isOutOfRange(double value, CcpJsonRepresentation range) {
		if(value < 0) {
			return true;
		}
		boolean belowTheMinimum = range.containsAllFields(VisBusinessResumesOfTheFilters.SearchFields.min) && value < range.getAsDoubleNumber(VisBusinessResumesOfTheFilters.SearchFields.min);
		if(belowTheMinimum) {
			return true;
		}
		boolean aboveTheMaximum = range.containsAllFields(VisBusinessResumesOfTheFilters.SearchFields.max) && value > range.getAsDoubleNumber(VisBusinessResumesOfTheFilters.SearchFields.max);
		return aboveTheMaximum;
	}

	private static boolean isOutOfEveryRange(double value, List<CcpJsonRepresentation> ranges) {
		boolean noRange = ranges.isEmpty();
		if(noRange) {
			return false;
		}
		boolean insideOne = ranges.stream().anyMatch(range -> false == isOutOfRange(value, range));
		return false == insideOne;
	}

	private static boolean lacksAFlag(CcpJsonRepresentation params, CcpJsonRepresentation resume, CcpJsonFieldName... flags) {
		for (CcpJsonFieldName flag : flags) {
			boolean asked = params.containsAllFields(flag) && params.getAsBoolean(flag);
			if(false == asked) {
				continue;
			}
			boolean has = resume.containsAllFields(flag) && resume.getAsBoolean(flag);
			if(false == has) {
				return true;
			}
		}
		return false;
	}

	// with both budgets filled, one claim inside its budget is enough; with one budget, the claim of that type must fit it
	private static boolean isOutOfTheBudgets(CcpJsonRepresentation params, CcpJsonRepresentation resume) {
		boolean hasCltBudget = params.containsAllFields(VisBusinessResumesOfTheFilters.Fields.clt);
		boolean hasPjBudget = params.containsAllFields(VisBusinessResumesOfTheFilters.Fields.pj);
		boolean noBudget = false == hasCltBudget && false == hasPjBudget;
		if(noBudget) {
			return false;
		}
		boolean cltInside = hasCltBudget && isClaimInside(params, resume, VisBusinessResumesOfTheFilters.Fields.clt, VisEntityResume.Fields.clt);
		if(cltInside) {
			return false;
		}
		boolean pjInside = hasPjBudget && isClaimInside(params, resume, VisBusinessResumesOfTheFilters.Fields.pj, VisEntityResume.Fields.pj);
		return false == pjInside;
	}

	private static boolean isClaimInside(CcpJsonRepresentation params, CcpJsonRepresentation resume, CcpJsonFieldName budgetField, CcpJsonFieldName claimField) {
		double claim = getNumber(resume, claimField);
		CcpJsonRepresentation budget = params.getInnerJson(budgetField);
		boolean inside = claim >= 0 && false == isOutOfRange(claim, budget);
		return inside;
	}

	private static boolean forbidsTheRecruiter(CcpJsonRepresentation params, CcpJsonRepresentation resume) {
		boolean noDomain = false == params.containsAllFields(VisBusinessResumesOfTheFilters.Fields.recruiterDomain);
		if(noDomain) {
			return false;
		}
		String recruiterDomain = params.getAsString(VisBusinessResumesOfTheFilters.Fields.recruiterDomain);
		List<String> companies = resume.getAsStringList(VisEntityResume.Fields.notAllowedCompany);
		boolean forbids = companies.stream().anyMatch(company -> company.toLowerCase().equals(recruiterDomain));
		return forbids;
	}

	private static boolean speaksTheLanguages(CcpJsonRepresentation params, CcpJsonRepresentation resume) {
		List<CcpJsonRepresentation> required = params.getAsJsonList(VisBusinessResumesOfTheFilters.Fields.spokenLanguage);
		boolean noLanguage = required.isEmpty();
		if(noLanguage) {
			return true;
		}
		Map<String, Double> levelByName = new HashMap<>();
		List<CcpJsonRepresentation> languages = resume.getAsJsonList(VisEntityResume.Fields.spokenLanguage);
		for (CcpJsonRepresentation language : languages) {
			boolean incomplete = false == language.containsAllFields(VisLanguages.name, VisLanguages.level);
			if(incomplete) {
				continue;
			}
			String name = language.getAsString(VisLanguages.name);
			Double level = language.getAsDoubleNumber(VisLanguages.level);
			levelByName.put(name, level);
		}
		for (CcpJsonRepresentation language : required) {
			String name = language.getAsString(VisLanguages.name);
			Double askedLevel = language.getAsDoubleNumber(VisLanguages.level);
			Double level = levelByName.get(name);
			boolean belowTheLevel = level == null || level < askedLevel;
			if(belowTheLevel) {
				return false;
			}
		}
		return true;
	}

	// each required skill is a list of alternatives (the skill and its descendants): the resume needs one of each list
	private static boolean hasTheSkills(CcpJsonRepresentation params, CcpJsonRepresentation resume) {
		List<Object> requiredSkills = params.getAsObjectList(VisBusinessResumesOfTheFilters.Fields.skill);
		List<String> skills = resume.getAsStringList(VisEntityResume.Fields.skill);
		for (Object alternatives : requiredSkills) {
			List<?> alternativeList = (List<?>) alternatives;
			boolean found = skills.stream().anyMatch(alternativeList::contains);
			if(false == found) {
				return false;
			}
		}
		return true;
	}

	// a number of the resume, -1 when absent (as the script)
	private static double getNumber(CcpJsonRepresentation json, CcpJsonFieldName field) {
		boolean absent = false == json.containsAllFields(field);
		if(absent) {
			return -1;
		}
		Double number = json.getAsDoubleNumber(field);
		return number;
	}

	private static String getNumberText(CcpJsonRepresentation json, CcpJsonFieldName field) {
		Double number = json.getAsDoubleNumber(field);
		long whole = number.longValue();
		String text = String.valueOf(whole);
		return text;
	}

	private static List<String> getNumberTexts(CcpJsonRepresentation json, CcpJsonFieldName field) {
		List<String> texts = json.getAsStringList(field);
		List<String> wholeTexts = texts.stream().map(text -> text.replace(".0", "")).toList();
		return wholeTexts;
	}
}
