package com.vis.business.bots.linkedin;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.flow.CcpErrorFlowDisturb;

/**
 * First step of the {@code verifyLinkedinAddress} command
 * ({@code /verifyLinkedinAddress <linkedinAddress> <resumeEmailSha1> <recruiterEmail>}): shows the operator the report of
 * the recruiter and asks whether they are right in saying that the LinkedIn address is invalid. A resume that no longer
 * exists, active or inactive, diverts the flow with {@code resumeNotFound}.
 */
public class VisSupportVerifyLinkedinShow implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisSupportVerifyLinkedinShow INSTANCE = new VisSupportVerifyLinkedinShow();

	private VisSupportVerifyLinkedinShow() {}

	/**
	 * Shows the report.
	 * @param json the session, with the parameters of the command
	 * @return the session with the reply
	 * @throws CcpErrorFlowDisturb with {@code resumeNotFound}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String resumeId = json.getAsString(VisSupportVerifyLinkedinFields.resumeEmailSha1);
		CcpJsonRepresentation side = VisSupportVerifyLinkedinConversation.getSideOfTheResume(resumeId);
		boolean resumeNotFound = side.isEmpty();
		if(resumeNotFound) {
			CcpJsonRepresentation jsonWithReply = VisSupportVerifyLinkedinConversation.replyAboutTheTicket(json, VisSupportVerifyLinkedinMessages.resumeNotFound);
			return VisSupportVerifyLinkedinStatus.resumeNotFound.throwException(jsonWithReply);
		}
		CcpJsonRepresentation jsonWithReply = VisSupportVerifyLinkedinConversation.replyWithTheOptions(json, VisSupportVerifyLinkedinMessages.ticket);
		return jsonWithReply;
	}
}
