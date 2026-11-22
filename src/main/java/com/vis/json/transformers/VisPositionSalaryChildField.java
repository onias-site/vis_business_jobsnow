package com.vis.json.transformers;

import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Fields of the child of the groups of ddd of the positions ({@code childFieldsProducers}): {@code clt} and {@code pj},
 * each the middle of its range ({@code (min + max) / 2}), so the averages and the salary ranges of the statistics of
 * the screen Supply X Demand compare the offer of the positions with the expectations of the resumes, which are single
 * values. A range the position did not inform is left out.
 */
public class VisPositionSalaryChildField implements Function<CcpJsonRepresentation, CcpJsonRepresentation>{
	/** The single instance: the class keeps no state. */
	public final static VisPositionSalaryChildField INSTANCE = new VisPositionSalaryChildField();

	private VisPositionSalaryChildField() {}

	/** The ends of a range. */
	enum RangeFields implements CcpJsonFieldName {
		/** The lower end of the range. */
		min,
		/** The upper end of the range. */
		max
	}

	/**
	 * The middle of each salary range of the position.
	 * @param json the position
	 * @return {@code {"clt": <middle>, "pj": <middle>}}, only the informed ranges
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation withClt = this.putTheMiddle(json, VisJsonCommonsFields.clt, CcpOtherConstants.EMPTY_JSON);
		CcpJsonRepresentation withPj = this.putTheMiddle(json, VisJsonCommonsFields.pj, withClt);
		return withPj;
	}

	/**
	 * Puts the middle of one range, when the position informed it.
	 * @param position the position
	 * @param field {@code clt} or {@code pj}
	 * @param childField the fields already produced
	 * @return the fields with the middle of the range
	 */
	private CcpJsonRepresentation putTheMiddle(CcpJsonRepresentation position, CcpJsonFieldName field, CcpJsonRepresentation childField) {
		boolean notInformed = false == position.containsAllFields(field);

		if(notInformed) {
			return childField;
		}

		CcpJsonRepresentation range = position.getInnerJson(field);
		boolean incompleteRange = false == range.containsAllFields(RangeFields.min, RangeFields.max);

		if(incompleteRange) {
			return childField;
		}

		Double min = range.getAsDoubleNumber(RangeFields.min);
		Double max = range.getAsDoubleNumber(RangeFields.max);
		long middle = Math.round((min + max) / 2);
		CcpJsonRepresentation childFieldWithTheMiddle = childField.put(field, middle);
		return childFieldWithTheMiddle;
	}
}
