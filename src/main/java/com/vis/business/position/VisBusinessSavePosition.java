package com.vis.business.position;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityPosition;

/** Step of the creation and of the edition of a position: saves the position the user filled in. */
public class VisBusinessSavePosition implements CcpBusiness {

	/** The position to be saved. */
	private final CcpJsonRepresentation position;

	/**
	 * Keeps the position.
	 * @param position the position to be saved
	 */
	public VisBusinessSavePosition(CcpJsonRepresentation position) {
		this.position = position;
	}

	/**
	 * Saves the position.
	 * @param json the json of the procedure
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		VisEntityPosition.ENTITY.save(this.position);
		return json;
	}
}
