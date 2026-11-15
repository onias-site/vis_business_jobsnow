package com.vis.business.recruiter.resumes;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;

/**
 * Step of the edit of a view of the recruiter: saves the view on the side of {@code vis_viewed_resume} where it is, so a
 * view of the twin ({@code vis_inactive_viewed_resume}) stays in the twin (the save on the main side would bring it back).
 */
public class VisBusinessSaveViewedResumeOnSide implements CcpBusiness {

	/** The side where the view is. */
	private final CcpEntity side;

	/** The view edited. */
	private final CcpJsonRepresentation view;

	/**
	 * Keeps what to save.
	 * @param side the side where the view is
	 * @param view the view edited
	 */
	public VisBusinessSaveViewedResumeOnSide(CcpEntity side, CcpJsonRepresentation view) {
		this.side = side;
		this.view = view;
	}

	/**
	 * Saves the view on its side.
	 * @param json the json of the procedure
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		this.side.save(this.view);
		return json;
	}
}
