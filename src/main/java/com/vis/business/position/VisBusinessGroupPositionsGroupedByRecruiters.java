package com.vis.business.position;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.business.CcpBusiness;
import com.vis.utils.VisUtils;

/**
 * CcpBusiness implementation that delegates the grouping of positions by recruiter to the
 * VisUtils.groupPositionsGroupedByRecruiters utility. It is the business entry point that triggers this grouping.
 */
public class VisBusinessGroupPositionsGroupedByRecruiters implements CcpBusiness{
		

	/** The single instance. */
	public static final VisBusinessGroupPositionsGroupedByRecruiters INSTANCE = new VisBusinessGroupPositionsGroupedByRecruiters();
	
	/** Singleton; use {@link #INSTANCE}. */
	private VisBusinessGroupPositionsGroupedByRecruiters() {}
	
	
	/**
	 * Runs the business described in the class documentation.
	 * @param json the input
	 * @return the result
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation positionsGroupedByRecruiters = VisUtils.groupPositionsGroupedByRecruiters(json);
		return positionsGroupedByRecruiters;
	}

}
