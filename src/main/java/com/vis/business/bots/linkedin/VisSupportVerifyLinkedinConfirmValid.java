package com.vis.business.bots.linkedin;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.jn.business.messages.JnMessageType;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.messages.JnSendMessageToUser;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnLanguage;
import com.vis.entities.VisEntityViewedResume;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.messages.VisMessages;

/**
 * The operator found that the LinkedIn address reported by the recruiter exists ("Não" in {@code /verifyLinkedinAddress}):
 * <ul>
 * <li>the recruiter gets the e-mail {@link VisMessages.VisNotifyRecruiterAboutUnreachableLinkedin}: maybe only they cannot
 * see that profile, but the user exists on LinkedIn;</li>
 * <li>the view of the recruiter goes to the twin ({@code vis_inactive_viewed_resume}) with the {@code comment} that the
 * address could not be opened by this recruiter, so the resume no longer goes to them. A view already in the twin only
 * gets the comment.</li>
 * </ul>
 */
public class VisSupportVerifyLinkedinConfirmValid implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisSupportVerifyLinkedinConfirmValid INSTANCE = new VisSupportVerifyLinkedinConfirmValid();

	private VisSupportVerifyLinkedinConfirmValid() {}

	/** The language of the e-mail and of the comment for now: the recruiters keep no language yet. */
	static final JnLanguage RECRUITER_LANGUAGE = JnLanguage.portuguese;

	/**
	 * Tells the recruiter and moves the view to the twin.
	 * @param json the session, with the parameters of the command
	 * @return the same session
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String linkedinAddress = json.getAsString(VisJsonCommonsFields.linkedinAddress);
		String recruiterEmail = json.getAsString(VisSupportVerifyLinkedinFields.recruiterEmail);
		tellTheRecruiter(recruiterEmail, linkedinAddress);
		CcpJsonRepresentation view = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityViewedResume.Fields.linkedin, linkedinAddress)
				.put(VisEntityViewedResume.Fields.email, recruiterEmail);
		// the delete of the main side moves the view to the twin; a view already there, or none, is not moved
		VisEntityViewedResume.ENTITY.delete(view);
		CcpEntity twinEntity = VisEntityViewedResume.ENTITY.getTwinEntity();
		boolean notInTheTwin = false == twinEntity.exists(view);
		if(notInTheTwin) {
			return json;
		}
		commentTheView(view, twinEntity);
		return json;
	}

	/**
	 * Sends the e-mail to the recruiter.
	 * @param recruiterEmail the e-mail of the recruiter
	 * @param linkedinAddress the address the recruiter could not open
	 */
	private static void tellTheRecruiter(String recruiterEmail, String linkedinAddress) {
		CcpJsonRepresentation message = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.email, recruiterEmail)
				.put(VisJsonCommonsFields.linkedinAddress, linkedinAddress)
				.put(JnJsonCommonsFields.language, RECRUITER_LANGUAGE);
		String topic = VisMessages.VisNotifyRecruiterAboutUnreachableLinkedin.class.getName();
		JnMessageType[] messageTypes = {JnMessageType.email};
		new JnSendMessageToUser().sendAllMessages(message, topic, messageTypes, JnMessageSenderExceptionHandler.LENIENT);
	}

	/**
	 * Writes the {@code comment} in the view of the twin: only this field (a partial update of the document).
	 * @param view the key of the view, as typed
	 * @param twinEntity the twin of {@code vis_viewed_resume}
	 */
	private static void commentTheView(CcpJsonRepresentation view, CcpEntity twinEntity) {
		CcpJsonRepresentation hashedView = VisEntityViewedResume.ENTITY.getJsonWithHandledPrimaryKey(view);
		String viewId = twinEntity.calculateId(hashedView);
		String comment = VisSupportVerifyLinkedinMessages.viewComment.getMessage(RECRUITER_LANGUAGE);
		CcpJsonRepresentation commentField = CcpOtherConstants.EMPTY_JSON.put(VisEntityViewedResume.Fields.comment, comment);
		CcpBulkItem item = new CcpBulkItem(commentField, CcpBulkEntityOperationType.update, twinEntity, viewId);
		List<CcpBulkItem> items = List.of(item);
		JnExecuteBulkOperation.INSTANCE.executeBulk(items, JnDeleteKeysFromCache.INSTANCE);
	}
}
