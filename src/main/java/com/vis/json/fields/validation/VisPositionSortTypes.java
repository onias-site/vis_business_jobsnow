package com.vis.json.fields.validation;

/**
 * Directions accepted by the {@code type} of each item of the {@code sort} of the position ({@code VisJsonFieldsPositionSort}).
 * Each one turns the value of a criterion into a key compared in ascending order (since 2026-11-24, the e-mails of the
 * resumes of the positions); a missing value stays missing, at the end in both directions.
 */
public enum VisPositionSortTypes {
	/** Ascending. */
	asc {
		public double toKey(double value) {
			return value;
		}
	},
	/** Descending. */
	desc {
		public double toKey(double value) {
			boolean missing = value == VisPositionSortFieldTypes.MISSING;
			if(missing) {
				return value;
			}
			return -value;
		}
	}
	;

	/**
	 * The key of a value in this direction, compared in ascending order.
	 * @param value the value of the criterion ({@link VisPositionSortFieldTypes#MISSING} when the resume does not have it)
	 * @return the key
	 */
	public abstract double toKey(double value);
}