package com.vis.business.resume;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;

/**
 * CcpBusiness implementation meant to calculate the resume hashes (for indexing or matching).
 * The implementation is marked as TODO: it returns the input JSON unchanged.
 */
public class VisBusinessCalculateResumeHashes implements CcpBusiness {

	/**
	 * Runs the business described in the class documentation.
	 * @param json the input
	 * @return the result
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		return json;
	}

}
