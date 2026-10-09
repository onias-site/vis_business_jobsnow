package com.vis.json.fields.validation;

/**
 * The salary fields of {@link VisJsonFieldsPositionStatis}; at least one is required. Public because the validation
 * engine reads it by reflection from the {@code @CcpJsonValidationFieldList} annotation: up to 2026-10-08 it was a
 * package-private enum in the same file, and every failed validation of a position statistic broke while explaining
 * the rules ({@code IllegalAccessException}).
 */
public enum SalaryType{
	/** The value as a contractor (PJ). */
	pj,
	/** The salary as an employee (CLT). */
	clt
}
