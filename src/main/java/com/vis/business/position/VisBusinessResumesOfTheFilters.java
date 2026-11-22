package com.vis.business.position;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpInputStreamDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTimeDecorator;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.utils.CcpDbRequester;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.especifications.http.CcpHttpMethods;
import com.ccp.especifications.http.CcpHttpResponseType;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.JnEntityGrouper;
import com.jn.utils.JnGroupers;
import com.vis.business.skill.VisSkillDictionary;
import com.vis.business.telegram.VisSkillWordsInMemory;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityVirtualDddResumeTypeGrouper;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonFieldsSalaryRange;
import com.vis.json.transformers.VisSeniorityType;

/**
 * The resumes that satisfy the filters of a position (tabs of the position form, since 2026-11-22): the active resumes
 * of the area (resumeType) in the regions of the position (every region when it is home office), filtered by a Painless
 * script inside Elasticsearch, so the resumes themselves never leave it. The groups of {@code jn_grouper} of each ddd
 * and area ({@code vis_virtual_ddd_resume_type_grouper}) are read by id (MGIE) and a scripted metric keeps the resumes
 * that pass every filter informed, once each by their id. The answer is the set of ids (SHA-1 of the e-mail), its size
 * and the averages of the CLT and PJ claims of those resumes. The filters left out (or unchecked) do not filter:
 * <ul>
 * <li>{@code disponibility} and {@code temporallyJobTime}: at most the number informed (0, immediate hiring or no
 * temporary job, does not filter);</li>
 * <li>{@code seniority}: the years of experience of its range ({@link VisSeniorityType});</li>
 * <li>{@code skill}: every required skill, found in the resume itself or in a skill below it in the hierarchy; a
 * required skill above another required one gives way to it ("backend" and "php" ask for "php");</li>
 * <li>{@code spokenLanguage}: each language, at least in the level asked;</li>
 * <li>{@code negotiableClaim}, {@code pcd} and {@code travel}: checked, only the resumes that have them;</li>
 * <li>{@code clt} and {@code pj} (the budgets): each claim the resume informs of a type offered inside its range, and at
 * least one of them informed. Both filled, the resume of CLT only, PJ only or both enters; one only, the resume needs
 * that claim inside it;</li>
 * <li>{@code recruiterDomain}: the resumes that forbid the company of the logged recruiter are left out.</li>
 * </ul>
 */
public class VisBusinessResumesOfTheFilters implements CcpBusiness {

	/** The input and the output of this business, with the validation rules of the input. */
	public static enum Fields implements CcpJsonFieldName {
		/** The area of the resumes (resumeType of the position): always a filter. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		resumeType,

		/** The regions: one or more ddds; absent, every ddd (home office position). */
		@CcpJsonFieldValidatorArray(minSize = 1, maxSize = 67)
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ddd,

		/** Days for the candidate to start: the resumes that start in up to these days; absent or 0, immediate hiring, no filter. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		disponibility,

		/** Months of the temporary job: the resumes that accept up to these months; absent or 0, no filter. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		temporallyJobTime,

		/** The seniority of the position: the years of experience of its range. */
		@CcpJsonFieldTypeString(allowedValuesEnum = VisSeniorityType.class)
		seniority,

		/** The required skills of the position (names of skills). */
		@CcpJsonFieldValidatorArray(maxSize = 100)
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 100)
		skill,

		/** The languages of the position, each one with its minimum level. */
		@CcpJsonFieldValidatorArray
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		spokenLanguage,

		/** Checked, only the resumes that negotiate the salary claim. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		negotiableClaim,

		/** Checked, only the resumes of people with disabilities. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		pcd,

		/** Checked, only the resumes available to travel. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		travel,

		/** The range of the CLT claim. */
		@CcpJsonFieldTypeNestedJson(jsonValidation = VisJsonFieldsSalaryRange.class, allowsEmptyJson = false)
		clt,

		/** The range of the PJ claim. */
		@CcpJsonFieldTypeNestedJson(jsonValidation = VisJsonFieldsSalaryRange.class, allowsEmptyJson = false)
		pj,

		/** The domain of the e-mail of the logged recruiter: the resumes that forbid it are left out. */
		@CcpJsonFieldTypeString(minLength = 4, maxLength = 50, regexValidation = "^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,63}$")
		recruiterDomain,

		/** The output: the ids of the resumes found (SHA-1 of the e-mail). */
		ids,

		/** The output: how many resumes were found (the size of the ids). */
		resumes,

		/** The output: the average CLT claim of the resumes found that informed it; absent when none did. */
		cltAverage,

		/** The output: the average PJ claim of the resumes found that informed it; absent when none did. */
		pjAverage,
	}

	/** The fields of the search sent to Elasticsearch and of its answer. */
	static enum SearchFields implements CcpJsonFieldName {
		size, query, ids, values, aggs, aggregations, value, params, filters, min, max,
		maxDisponibility, maxTemporallyJobTime, experience,
		scriptedMetric { public String getValue() { return "scripted_metric"; } },
		initScript { public String getValue() { return "init_script"; } },
		mapScript { public String getValue() { return "map_script"; } },
		combineScript { public String getValue() { return "combine_script"; } },
		reduceScript { public String getValue() { return "reduce_script"; } },
		searchPath { public String getValue() { return "/_search"; } },
		initScriptFile { public String getValue() { return "painless/resumes-of-the-filters/init.painless"; } },
		mapScriptFile { public String getValue() { return "painless/resumes-of-the-filters/map.painless"; } },
		combineScriptFile { public String getValue() { return "painless/resumes-of-the-filters/combine.painless"; } },
		reduceScriptFile { public String getValue() { return "painless/resumes-of-the-filters/reduce.painless"; } },
		slash { public String getValue() { return "/"; } },
	}

	/** The single instance. */
	public static final VisBusinessResumesOfTheFilters INSTANCE = new VisBusinessResumesOfTheFilters();

	private VisBusinessResumesOfTheFilters() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	/**
	 * Runs the filters over the groups of the regions and of the area of the position.
	 * @param json the filters
	 * @return the ids, their size and the averages of the claims
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		List<String> groupIds = getGroupIds(json);
		CcpJsonRepresentation filters = getFilters(json);
		CcpJsonRepresentation search = getSearch(groupIds, filters);
		CcpEntityMetaData grouperDetails = JnEntityGrouper.ENTITY.getEntityMetaData();
		String url = SearchFields.slash.getValue() + grouperDetails.entityName + SearchFields.searchPath.getValue();
		CcpDbRequester requester = CcpDependencyInjection.getDependency(CcpDbRequester.class);
		CcpJsonRepresentation answer = requester.executeHttpRequest(this.getClass().getSimpleName(), url, CcpHttpMethods.POST, 200, search, CcpHttpResponseType.singleRecord);
		CcpJsonRepresentation aggregations = answer.getInnerJson(SearchFields.aggregations);
		CcpJsonRepresentation resumesAggregation = aggregations.getInnerJson(Fields.resumes);
		CcpJsonRepresentation value = resumesAggregation.getInnerJson(SearchFields.value);
		CcpJsonRepresentation response = getResponse(value);
		return response;
	}

	/**
	 * The answer of the scripted metric, with the count as an integer and the averages only when there are.
	 * @param value the value of the scripted metric
	 * @return the response
	 */
	private static CcpJsonRepresentation getResponse(CcpJsonRepresentation value) {
		List<String> ids = value.getAsStringList(Fields.ids);
		Integer resumes = value.getAsIntegerNumber(Fields.resumes);
		CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON
				.put(Fields.ids, ids)
				.put(Fields.resumes, resumes);
		CcpJsonRepresentation withCltAverage = value.containsAllFields(Fields.cltAverage) ? response.put(Fields.cltAverage, value.getAsDoubleNumber(Fields.cltAverage)) : response;
		CcpJsonRepresentation withPjAverage = value.containsAllFields(Fields.pjAverage) ? withCltAverage.put(Fields.pjAverage, value.getAsDoubleNumber(Fields.pjAverage)) : withCltAverage;
		return withPjAverage;
	}

	/**
	 * The search: the groups by id and the scripted metric with the filters.
	 * @param groupIds the ids of the groups in {@code jn_grouper}
	 * @param filters the filters, as the map script reads them
	 * @return the body of the search
	 */
	private static CcpJsonRepresentation getSearch(List<String> groupIds, CcpJsonRepresentation filters) {
		CcpJsonRepresentation ids = CcpOtherConstants.EMPTY_JSON.put(SearchFields.values, groupIds);
		CcpJsonRepresentation query = CcpOtherConstants.EMPTY_JSON.put(SearchFields.ids, ids);
		CcpJsonRepresentation params = CcpOtherConstants.EMPTY_JSON.put(SearchFields.filters, filters);
		String initScript = readScript(SearchFields.initScriptFile);
		String mapScript = readScript(SearchFields.mapScriptFile);
		String combineScript = readScript(SearchFields.combineScriptFile);
		String reduceScript = readScript(SearchFields.reduceScriptFile);
		CcpJsonRepresentation scriptedMetric = CcpOtherConstants.EMPTY_JSON
				.put(SearchFields.initScript, initScript)
				.put(SearchFields.mapScript, mapScript)
				.put(SearchFields.combineScript, combineScript)
				.put(SearchFields.reduceScript, reduceScript)
				.put(SearchFields.params, params);
		CcpJsonRepresentation resumesAggregation = CcpOtherConstants.EMPTY_JSON.put(SearchFields.scriptedMetric, scriptedMetric);
		CcpJsonRepresentation aggs = CcpOtherConstants.EMPTY_JSON.put(Fields.resumes, resumesAggregation);
		CcpJsonRepresentation search = CcpOtherConstants.EMPTY_JSON
				.put(SearchFields.size, 0)
				.put(SearchFields.query, query)
				.put(SearchFields.aggs, aggs);
		return search;
	}

	/**
	 * A script of the classpath.
	 * @param file the path of the script
	 * @return the text of the script
	 */
	private static String readScript(SearchFields file) {
		String path = file.getValue();
		CcpStringDecorator pathDecorator = new CcpStringDecorator(path);
		CcpInputStreamDecorator inputStreamFrom = pathDecorator.inputStreamFrom();
		InputStream inputStream = inputStreamFrom.classLoader();
		CcpStringDecorator script = new CcpStringDecorator(inputStream);
		return script.content;
	}

	/**
	 * The filters informed, as the map script reads them: the ones that do not filter are left out.
	 * @param json the input
	 * @return the filters
	 */
	private static CcpJsonRepresentation getFilters(CcpJsonRepresentation json) {
		CcpJsonRepresentation filters = CcpOtherConstants.EMPTY_JSON;
		filters = putPositiveNumber(json, Fields.disponibility, filters, SearchFields.maxDisponibility);
		filters = putPositiveNumber(json, Fields.temporallyJobTime, filters, SearchFields.maxTemporallyJobTime);
		filters = putExperience(json, filters);
		filters = putCheckedFlag(json, Fields.negotiableClaim, filters);
		filters = putCheckedFlag(json, Fields.pcd, filters);
		filters = putCheckedFlag(json, Fields.travel, filters);
		filters = putRange(json, Fields.clt, filters);
		filters = putRange(json, Fields.pj, filters);
		filters = putRecruiterDomain(json, filters);
		filters = putLanguages(json, filters);
		filters = putSkills(json, filters);
		return filters;
	}

	private static CcpJsonRepresentation putPositiveNumber(CcpJsonRepresentation json, Fields field, CcpJsonRepresentation filters, SearchFields filter) {
		boolean absent = false == json.containsAllFields(field);

		if(absent) {
			return filters;
		}

		Integer number = json.getAsIntegerNumber(field);

		if(number <= 0) {
			return filters;
		}

		CcpJsonRepresentation withTheNumber = filters.put(filter, number);
		return withTheNumber;
	}

	private static CcpJsonRepresentation putExperience(CcpJsonRepresentation json, CcpJsonRepresentation filters) {
		boolean absent = false == json.containsAllFields(Fields.seniority);

		if(absent) {
			return filters;
		}

		VisSeniorityType seniority = json.getAsEnum(Fields.seniority, VisSeniorityType.class);
		CcpTimeDecorator time = new CcpTimeDecorator();
		int currentYear = time.getYear();
		int firstYear = seniority.getFirstExperienceYear(currentYear);
		int lastYear = seniority.getLastExperienceYear(currentYear);
		CcpJsonRepresentation range = CcpOtherConstants.EMPTY_JSON
				.put(SearchFields.min, firstYear)
				.put(SearchFields.max, lastYear);
		CcpJsonRepresentation withTheExperience = filters.put(SearchFields.experience, range);
		return withTheExperience;
	}

	private static CcpJsonRepresentation putCheckedFlag(CcpJsonRepresentation json, Fields field, CcpJsonRepresentation filters) {
		boolean checked = json.containsAllFields(field) && json.getAsBoolean(field);

		if(false == checked) {
			return filters;
		}

		CcpJsonRepresentation withTheFlag = filters.put(field, true);
		return withTheFlag;
	}

	private static CcpJsonRepresentation putRange(CcpJsonRepresentation json, Fields field, CcpJsonRepresentation filters) {
		boolean absent = false == json.containsAllFields(field);

		if(absent) {
			return filters;
		}

		CcpJsonRepresentation range = json.getInnerJson(field);
		CcpJsonRepresentation withTheRange = filters.put(field, range);
		return withTheRange;
	}

	private static CcpJsonRepresentation putRecruiterDomain(CcpJsonRepresentation json, CcpJsonRepresentation filters) {
		boolean absent = false == json.containsAllFields(Fields.recruiterDomain);

		if(absent) {
			return filters;
		}

		String domain = json.getAsString(Fields.recruiterDomain);
		String lowerCaseDomain = domain.toLowerCase();
		CcpJsonRepresentation withTheDomain = filters.put(Fields.recruiterDomain, lowerCaseDomain);
		return withTheDomain;
	}

	private static CcpJsonRepresentation putLanguages(CcpJsonRepresentation json, CcpJsonRepresentation filters) {
		List<CcpJsonRepresentation> askedLanguages = json.getAsJsonList(Fields.spokenLanguage);

		if(askedLanguages.isEmpty()) {
			return filters;
		}

		List<Map<String, Object>> languages = new ArrayList<>();
		for (CcpJsonRepresentation askedLanguage : askedLanguages) {
			languages.add(askedLanguage.content);
		}
		CcpJsonRepresentation withTheLanguages = filters.put(Fields.spokenLanguage, languages);
		return withTheLanguages;
	}

	/**
	 * The required skills, each one as the list of its alternatives (the skill and the skills below it in the hierarchy).
	 * A required skill that has another required skill below it is too broad and gives way to the narrower one.
	 * @param json the input
	 * @param filters the filters so far
	 * @return the filters with the skills
	 */
	private static CcpJsonRepresentation putSkills(CcpJsonRepresentation json, CcpJsonRepresentation filters) {
		List<String> requiredSkills = json.getAsStringList(Fields.skill);

		if(requiredSkills.isEmpty()) {
			return filters;
		}

		Set<String> upperCaseSkills = new LinkedHashSet<>();
		for (String requiredSkill : requiredSkills) {
			String upperCaseSkill = requiredSkill.toUpperCase();
			upperCaseSkills.add(upperCaseSkill);
		}
		VisSkillWordsInMemory words = VisSkillDictionary.getWords();
		List<List<String>> alternativesOfEachSkill = new ArrayList<>();
		for (String requiredSkill : upperCaseSkills) {
			Set<String> alternatives = words.getSkillAndItsDescendants(requiredSkill);
			boolean tooBroad = hasAnotherRequiredSkill(alternatives, requiredSkill, upperCaseSkills);

			if(tooBroad) {
				continue;
			}

			List<String> alternativesList = new ArrayList<>(alternatives);
			alternativesOfEachSkill.add(alternativesList);
		}
		CcpJsonRepresentation withTheSkills = filters.put(Fields.skill, alternativesOfEachSkill);
		return withTheSkills;
	}

	private static boolean hasAnotherRequiredSkill(Set<String> alternatives, String requiredSkill, Set<String> requiredSkills) {
		for (String otherSkill : requiredSkills) {
			boolean itself = otherSkill.equals(requiredSkill);

			if(itself) {
				continue;
			}

			boolean below = alternatives.contains(otherSkill);

			if(below) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The ids in {@code jn_grouper} of the groups of the ddds asked (every ddd when none was asked) and of the area.
	 * @param json the input
	 * @return the ids of the groups
	 */
	private static List<String> getGroupIds(CcpJsonRepresentation json) {
		List<Integer> ddds = getDdds(json);
		Integer resumeType = json.getAsIntegerNumber(Fields.resumeType);
		CcpEntity parentEntity = VisEntityVirtualDddResumeTypeGrouper.ENTITY;
		CcpEntityMetaData parentDetails = parentEntity.getEntityMetaData();
		CcpEntityMetaData childDetails = VisEntityResume.ENTITY.getEntityMetaData();
		List<String> groupIds = new ArrayList<>();
		for (Integer ddd : ddds) {
			CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON
					.put(VisEntityVirtualDddResumeTypeGrouper.Fields.ddd, ddd)
					.put(VisEntityVirtualDddResumeTypeGrouper.Fields.resumeType, resumeType);
			String parentId = parentEntity.calculateId(combination);
			CcpJsonRepresentation groupKey = JnGroupers.getGroupKey(parentDetails.entityName, childDetails.entityName, parentId);
			String groupId = JnEntityGrouper.ENTITY.calculateId(groupKey);
			groupIds.add(groupId);
		}
		return groupIds;
	}

	/**
	 * The ddds asked, as integers (the ids of the groups are calculated from integers), or every ddd when none was asked.
	 * @param json the input
	 * @return the ddds
	 */
	private static List<Integer> getDdds(CcpJsonRepresentation json) {
		boolean noDddAsked = false == json.containsAllFields(Fields.ddd);

		if(noDddAsked) {
			List<Integer> allDdds = getAllDdds();
			return allDdds;
		}

		List<Object> askedDdds = json.getAsObjectList(Fields.ddd);
		List<Integer> ddds = new ArrayList<>();
		for (Object askedDdd : askedDdds) {
			CcpJsonRepresentation rawDdd = CcpOtherConstants.EMPTY_JSON.put(Fields.ddd, askedDdd);
			Integer ddd = rawDdd.getAsIntegerNumber(Fields.ddd);
			ddds.add(ddd);
		}
		return ddds;
	}

	/**
	 * Every ddd: the allowed values of {@link VisJsonCommonsFields#ddd}, the same list that validates the ddds asked.
	 * @return the ddds, in the order of the annotation
	 */
	private static List<Integer> getAllDdds() {
		String fieldName = VisJsonCommonsFields.ddd.name();
		Field field = getEnumField(fieldName);
		CcpJsonFieldTypeNumberUnsigned annotation = field.getAnnotation(CcpJsonFieldTypeNumberUnsigned.class);
		long[] allowedValues = annotation.allowedValues();
		List<Integer> ddds = new ArrayList<>();
		for (long allowedValue : allowedValues) {
			int ddd = (int) allowedValue;
			ddds.add(ddd);
		}
		return ddds;
	}

	private static Field getEnumField(String fieldName) {
		try {
			Field field = VisJsonCommonsFields.class.getField(fieldName);
			return field;
		} catch (NoSuchFieldException e) {
			throw new IllegalStateException(e);
		}
	}
}
