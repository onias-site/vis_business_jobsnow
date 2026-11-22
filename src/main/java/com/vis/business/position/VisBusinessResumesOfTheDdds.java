package com.vis.business.position;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.jn.utils.JnGroupers;
import com.vis.business.statistics.VisResumeStatisticsSources;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * The active resumes of the regions a position is offered in (tab "Opções" of the position form, since 2026-11-22): for
 * each ddd asked, the children of its group of active resumes in {@code jn_grouper} (parent
 * {@code vis_virtual_ddd_grouper}), each one a JSON with part of the resume (salaries, experience, languages, the flags
 * and the skills) and its id, never a contact. One read by id of every group (MGIE, OQL): the screen keeps each ddd in
 * the browser for one hour and asks only for the ddds it does not have, so a resume listed by several ddds is counted
 * once by its id. A repeated ddd is refused by the validation of the list. With no ddd, every ddd (the allowed values of
 * {@link VisJsonCommonsFields#ddd}): a home office position takes resumes from every region.
 */
public class VisBusinessResumesOfTheDdds implements CcpBusiness {

	/** The input and the output of this business, with the validation rules of the input. */
	public static enum Fields implements CcpJsonFieldName {
		/** The regions: one or more ddds of the resume, home office (10) included; absent, every ddd (home office position). */
		@CcpJsonFieldValidatorArray(minSize = 1, maxSize = 67)
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ddd,

		/** The output: {ddd: [resume, ...]}, every ddd asked present, an empty list for a ddd with no active resume. */
		resumes,
	}

	/** The single instance. */
	public static final VisBusinessResumesOfTheDdds INSTANCE = new VisBusinessResumesOfTheDdds();

	private VisBusinessResumesOfTheDdds() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	/**
	 * Reads the groups of active resumes of the ddds asked.
	 * @param json the ddds
	 * @return {@code resumes}: the resumes of each ddd, by ddd
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		List<Integer> ddds = getDdds(json);
		List<CcpJsonRepresentation> groupKeys = new ArrayList<>();
		for (Integer ddd : ddds) {
			CcpJsonRepresentation groupKey = VisResumeStatisticsSources.resumes.getGroupKey(ddd);
			groupKeys.add(groupKey);
		}
		List<List<String>> childrenOfEachDdd = JnGroupers.getChildrenOfEachGroup(groupKeys);
		CcpJsonRepresentation resumesByDdd = CcpOtherConstants.EMPTY_JSON;
		int dddsTotal = ddds.size();
		for (int index = 0; index < dddsTotal; index++) {
			Integer ddd = ddds.get(index);
			List<String> children = childrenOfEachDdd.get(index);
			List<Map<String, Object>> resumes = toResumes(children);
			String dddText = String.valueOf(ddd);
			CcpFieldName dddField = new CcpFieldName(dddText);
			resumesByDdd = resumesByDdd.put(dddField, resumes);
		}
		CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON.put(Fields.resumes, resumesByDdd);
		return response;
	}

	/**
	 * The ddds asked, as integers (the ids of the groups are calculated from integers), or every ddd when none was asked.
	 * @param json the input
	 * @return the ddds, in the order they were asked
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

	/**
	 * The declared field of an item of {@link VisJsonCommonsFields}, which always exists for an item.
	 * @param fieldName the name of the item
	 * @return the field
	 */
	private static Field getEnumField(String fieldName) {
		try {
			Field field = VisJsonCommonsFields.class.getField(fieldName);
			return field;
		} catch (NoSuchFieldException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * The children of a group, parsed.
	 * @param children one JSON text per resume
	 * @return one JSON per resume
	 */
	private static List<Map<String, Object>> toResumes(List<String> children) {
		List<Map<String, Object>> resumes = new ArrayList<>();
		for (String child : children) {
			CcpJsonRepresentation resume = new CcpJsonRepresentation(child);
			resumes.add(resume.content);
		}
		return resumes;
	}
}
