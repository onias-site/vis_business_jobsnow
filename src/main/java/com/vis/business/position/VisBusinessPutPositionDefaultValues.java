package com.vis.business.position;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.json.defaultvalues.engine.CcpJsonFieldDefaultValuesEngine;
import com.vis.entities.VisEntityPosition;

/**
 * Runs before the save of the position ({@code @CcpBeforeSaveFromMainEntity} of {@code VisEntityPosition}): fills the
 * absent fields that declare a default value in {@code VisEntityPosition.Fields} ({@code mustSendInGroups},
 * {@code mustSendToResumes} and {@code visibleToCandidates} {@code true}, {@code frequencyType} {@code daily}). The save of an entity only validates
 * its fields; without this business the defaults would never reach the record. Fields already present are kept.
 */
public class VisBusinessPutPositionDefaultValues implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessPutPositionDefaultValues INSTANCE = new VisBusinessPutPositionDefaultValues();

	private VisBusinessPutPositionDefaultValues() {}

	/**
	 * Fills the default values of the position.
	 * @param json the position about to be saved
	 * @return the position with the default values of the absent fields
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation jsonWithDefaultValues = CcpJsonFieldDefaultValuesEngine.INSTANCE.putDefaultValues(VisEntityPosition.Fields.class, json);
		return jsonWithDefaultValues;
	}
}
