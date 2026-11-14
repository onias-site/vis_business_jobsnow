package com.vis.business.bots.linkedin;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.utils.JnLanguage;
import com.vis.entities.VisEntityViewedResume;

/**
 * Moves the view of the recruiter whose LinkedIn was reported to the twin ({@code vis_inactive_viewed_resume}) with the
 * {@code comment} that the address could not be opened by this recruiter, in one write: the delete of a twin entity
 * writes in the twin the record it is given (since 2026-11-13; until then the comment was a second write, made only when
 * the support answered "Não"). A view already in the twin, or none, is not moved.
 */
public class VisSupportVerifyLinkedinInactivateTheView implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisSupportVerifyLinkedinInactivateTheView INSTANCE = new VisSupportVerifyLinkedinInactivateTheView();

	private VisSupportVerifyLinkedinInactivateTheView() {}

	/** The language of the comment for now: the recruiters keep no language yet. */
	public static final JnLanguage RECRUITER_LANGUAGE = JnLanguage.portuguese;

	/**
	 * Moves the view to the twin with the comment.
	 * @param view the key of the view ({@code linkedin} and {@code email} of the recruiter), as typed
	 * @return the same view
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation view) {
		String comment = VisSupportVerifyLinkedinMessages.viewComment.getMessage(RECRUITER_LANGUAGE);
		CcpJsonRepresentation viewWithComment = view.put(VisEntityViewedResume.Fields.comment, comment);
		VisEntityViewedResume.ENTITY.delete(viewWithComment);
		return view;
	}
}
