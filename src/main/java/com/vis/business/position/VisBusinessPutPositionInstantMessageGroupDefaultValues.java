package com.vis.business.position;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.json.defaultvalues.engine.CcpJsonFieldDefaultValuesEngine;
import com.vis.entities.VisEntityPositionInstantMessageGroup;

/**
 * Runs before the save of the group of positions ({@code @CcpBeforeSaveFromMainEntity} of
 * {@code VisEntityPositionInstantMessageGroup}): fills the absent fields that declare a default value
 * ({@code botType} {@code user}). The save of an entity only validates its fields; without this business the default
 * would never reach the record. Fields already present are kept.
 */
public class VisBusinessPutPositionInstantMessageGroupDefaultValues implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessPutPositionInstantMessageGroupDefaultValues INSTANCE = new VisBusinessPutPositionInstantMessageGroupDefaultValues();

	private VisBusinessPutPositionInstantMessageGroupDefaultValues() {}

	/**
	 * Fills the default values of the group of positions.
	 * @param json the record about to be saved
	 * @return the record with the default values of the absent fields
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation jsonWithDefaultValues = CcpJsonFieldDefaultValuesEngine.INSTANCE.putDefaultValues(VisEntityPositionInstantMessageGroup.Fields.class, json);
		return jsonWithDefaultValues;
	}
}
