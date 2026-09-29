package com.vis.business.resume;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;

/**
 * CcpBusiness implementation meant to calculate the resume hashes (for indexing or matching).
 * The implementation is marked as TODO: it returns the input JSON unchanged.
 */
public class VisBusinessCalculateResumeHashes implements CcpBusiness {

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		return json;
	}

}
