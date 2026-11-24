package com.vis.business.recruiter.resumes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.business.position.VisBusinessFiltersOfThePosition;
import com.vis.entities.VisEntityPosition;
import com.vis.json.fields.validation.VisJsonFieldsPositionSort;
import com.vis.json.fields.validation.VisPositionSkillCategories;
import com.vis.json.fields.validation.VisPositionSortFieldTypes;
import com.vis.json.fields.validation.VisPositionSortTypes;

/**
 * The rows of the resumes of a position in the order chosen for it (the {@code sort} of the position, since 2026-11-24),
 * with the same rules of the screen ({@code resumesSort.ts}): each row brings its {@code sortValues}; the first criterion
 * decides and each next one only breaks the ties; a resume without the value of a criterion goes to the end in both
 * directions; a tie in every criterion keeps the order the rows came in. A criterion of the positions saved before
 * 2026-11-23 may name several fields: each one counts, in the direction of the item.
 */
public class VisBusinessSortResumesOfThePosition implements CcpBusiness {

	/** The input and the output of this business. */
	public static enum Fields implements CcpJsonFieldName {
		/** The position, as stored. */
		position,
		/** The rows, each one with its {@code sortValues}; the output: the same rows in order. */
		resumes,
		/** The keys of each row in the order of the position, only while sorting. */
		positionSortKeys,
	}

	/** The single instance: the class keeps no state. */
	public static final VisBusinessSortResumesOfThePosition INSTANCE = new VisBusinessSortResumesOfThePosition();

	private VisBusinessSortResumesOfThePosition() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation position = json.getInnerJson(Fields.position);
		List<CcpJsonRepresentation> rows = json.getAsJsonList(Fields.resumes);
		Set<String> desiredSkills = VisBusinessFiltersOfThePosition.getSkills(position, VisPositionSkillCategories.shouldSkills);
		List<CcpJsonRepresentation> sortRules = position.getAsJsonList(VisEntityPosition.Fields.sort);
		List<CcpJsonRepresentation> rowsWithKeys = new ArrayList<>();
		for (CcpJsonRepresentation row : rows) {
			List<Double> keys = getKeys(row, sortRules, desiredSkills);
			CcpJsonRepresentation rowWithKeys = row.put(Fields.positionSortKeys, keys);
			rowsWithKeys.add(rowWithKeys);
		}
		rowsWithKeys.sort(VisPositionSortKeysComparator.INSTANCE);
		List<CcpJsonRepresentation> sortedRows = new ArrayList<>();
		for (CcpJsonRepresentation rowWithKeys : rowsWithKeys) {
			CcpJsonRepresentation sortedRow = rowWithKeys.removeFields(Fields.positionSortKeys);
			sortedRows.add(sortedRow);
		}
		CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON.put(Fields.resumes, sortedRows);
		return response;
	}

	private static List<Double> getKeys(CcpJsonRepresentation row, List<CcpJsonRepresentation> sortRules, Set<String> desiredSkills) {
		CcpJsonRepresentation sortValues = row.getInnerJson(VisRecruiterResumesFields.sortValues);
		List<Double> keys = new ArrayList<>();
		for (CcpJsonRepresentation sortRule : sortRules) {
			String typeName = sortRule.getAsString(VisJsonFieldsPositionSort.type);
			VisPositionSortTypes type = VisPositionSortTypes.valueOf(typeName);
			String criterionValue = sortRule.getOrDefault(VisJsonFieldsPositionSort.value, () -> "");
			List<String> fieldNames = sortRule.getAsStringList(VisJsonFieldsPositionSort.field);
			for (String fieldName : fieldNames) {
				VisPositionSortFieldTypes field = VisPositionSortFieldTypes.valueOf(fieldName);
				double value = field.getValue(sortValues, criterionValue, desiredSkills);
				double key = type.toKey(value);
				keys.add(key);
			}
		}
		return keys;
	}
}
