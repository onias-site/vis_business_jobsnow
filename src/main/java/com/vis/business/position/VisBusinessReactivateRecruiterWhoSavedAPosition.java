package com.vis.business.position;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.vis.business.recruiter.VisBusinessReactivateRecruiterWhoViewed;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityRecruiter;
import com.vis.json.transformers.VisJsonTransformerRecruiterHash;

/**
 * Runs after a save of {@code vis_position} (since 2026-11-28): the recruiter who saves a position wants to receive
 * resumes again, as one who views a resume ({@link VisBusinessReactivateRecruiterWhoViewed}), so they leave the
 * unsubscribed ones ({@code vis_recruiter_unsubscribed}) and, when inactive, become active again. A save that brings only
 * the hash of the owner (a stored position saved again) changes nothing.
 * <p>Input: the position as saved ({@code recruiter} hashed, the readable one in {@code originalRecruiter}).
 */
public class VisBusinessReactivateRecruiterWhoSavedAPosition implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public static final VisBusinessReactivateRecruiterWhoSavedAPosition INSTANCE = new VisBusinessReactivateRecruiterWhoSavedAPosition();

	private VisBusinessReactivateRecruiterWhoSavedAPosition() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		boolean recruiterHashed = json.containsAllFields(VisJsonTransformerRecruiterHash.JsonFieldNames.originalRecruiter);
		CcpJsonFieldName readableRecruiterField = recruiterHashed ? VisJsonTransformerRecruiterHash.JsonFieldNames.originalRecruiter : VisEntityPosition.Fields.recruiter;
		boolean noRecruiter = false == json.containsAllFields(readableRecruiterField);
		if(noRecruiter) {
			return json;
		}
		String recruiter = json.getAsString(readableRecruiterField);
		CcpStringDecorator recruiterText = new CcpStringDecorator(recruiter);
		CcpEmailDecorator recruiterEmail = recruiterText.email();
		boolean onlyTheHash = false == recruiterEmail.isValid();
		if(onlyTheHash) {
			return json;
		}
		CcpJsonRepresentation view = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiter.Fields.email, recruiter);
		VisBusinessReactivateRecruiterWhoViewed.INSTANCE.execute(view);
		return json;
	}
}
