package com.vis.business.bots.job.posting;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;

/**
 * First step of {@code /saveJobPosting} of the support bot (since 2026-11-26): asks the operator for the text of the
 * job posting, from 100 to 5,000 characters; the answer goes to {@link VisSupportSaveJobPostingSaveTheText}.
 */
public class VisSupportSaveJobPostingAskTheText implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisSupportSaveJobPostingAskTheText INSTANCE = new VisSupportSaveJobPostingAskTheText();

	private VisSupportSaveJobPostingAskTheText() {}

	/**
	 * Asks for the text.
	 * @param json the session
	 * @return the session with the question
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation sessionWithTheQuestion = VisSupportSaveJobPostingConversation.reply(json, VisSupportSaveJobPostingMessages.askTheText, CcpOtherConstants.EMPTY_JSON);
		return sessionWithTheQuestion;
	}
}
