package com.vis.business.resume;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpTimeDecorator;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityDecoratorTypes;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeMessage;
import com.vis.entities.VisEntityViewedResume;
import com.vis.json.fields.validation.VisResumeMessageTypes;
import com.vis.json.transformers.VisJsonTransformerLinkedinHash;
import com.vis.json.transformers.VisViewedResumeRecruiterDomain;

/**
 * Tells the owner of the viewed resume about the view (since 2026-11-12), after each insertion in {@code vis_viewed_resume} (a save
 * of a view that already exists, as the reopening of the resume, tells nothing):
 * each resume that uses the viewed LinkedIn address ({@code vis_resume_linkedin}) whose {@code chatId} is greater than
 * zero gets a record in {@code vis_resume_message} ({@code newViewInYourResume}), whose save sends it through the user
 * bot ({@code VisNewViewInYourResume}). The text names the moment of the view and the domain of the recruiter, or tells
 * that the recruiter is a freelancer (an e-mail of a non-professional domain).
 */
public class VisBusinessTellTheOwnerAboutTheView implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public final static VisBusinessTellTheOwnerAboutTheView INSTANCE = new VisBusinessTellTheOwnerAboutTheView();

	private VisBusinessTellTheOwnerAboutTheView() {}

	/** The parameters of the texts of {@link VisResumeViewMessages}. */
	public enum Fields implements CcpJsonFieldName {
		/** The domain of the e-mail of the recruiter. */
		recruiterDomain,
	}

	/** The format of the moment of the view. */
	enum Patterns implements CcpJsonFieldName {
		/** Day, month, year, hours, minutes and seconds. */
		dateTime {
			public String getValue() {
				return "dd/MM/yyyy HH:mm:ss";
			}
		},
	}

	/**
	 * Saves the message of the view for each owner with a chat.
	 * @param json the saved view
	 * @return the view, as it came
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation view = getReadableView(json);
		String linkedinAddress = view.getAsString(VisEntityViewedResume.Fields.linkedin);
		CcpJsonRepresentation addressKey = VisResumeLinkedinEmails.getAddressKey(linkedinAddress);
		List<String> hashedEmails = VisResumeLinkedinEmails.getEmails(addressKey);
		boolean resumeNotFound = hashedEmails.isEmpty();
		if(resumeNotFound) {
			return json;
		}
		String message = getMessage(view);
		// the e-mails of vis_resume_linkedin are already hashed: without the fields transformer, the id is calculated from
		// them as they are, instead of from a second hash
		CcpEntity resumeWithHashedKey = CcpEntityFactory.getCustomEntity(VisEntityResume.ENTITY, CcpEntityDecoratorTypes.FieldsTransformer);
		for (String hashedEmail : hashedEmails) {
			CcpJsonRepresentation resume = getResume(resumeWithHashedKey, hashedEmail);
			boolean withoutChat = false == resume.containsField(VisEntityResume.Fields.chatId);
			if(withoutChat) {
				continue;
			}
			// the stored number comes back from the JSON as a Double
			Long chatId = resume.getAsLongNumber(VisEntityResume.Fields.chatId);
			boolean noChat = chatId <= 0;
			if(noChat) {
				continue;
			}
			CcpJsonRepresentation resumeMessage = CcpOtherConstants.EMPTY_JSON
					.put(VisEntityResumeMessage.Fields.chatId, chatId)
					.put(VisEntityResumeMessage.Fields.messageType, VisResumeMessageTypes.newViewInYourResume)
					.put(VisEntityResumeMessage.Fields.message, message);
			VisEntityResumeMessage.ENTITY.save(resumeMessage);
		}
		return json;
	}

	/**
	 * The view with its LinkedIn address and the e-mail of the recruiter as they came: the save may hand them over
	 * already hashed, with the readable values in {@code originalLinkedin} and {@code originalEmail}.
	 */
	private static CcpJsonRepresentation getReadableView(CcpJsonRepresentation json) {
		boolean addressHashed = json.containsField(VisJsonTransformerLinkedinHash.JsonFieldNames.originalLinkedin);
		CcpJsonFieldName addressField = addressHashed ? VisJsonTransformerLinkedinHash.JsonFieldNames.originalLinkedin : VisEntityViewedResume.Fields.linkedin;
		boolean emailHashed = json.containsField(JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail);
		CcpJsonFieldName emailField = emailHashed ? JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail : VisEntityViewedResume.Fields.email;
		String linkedinAddress = json.getAsString(addressField);
		String recruiterEmail = json.getAsString(emailField);
		CcpJsonRepresentation viewWithLinkedin = json.put(VisEntityViewedResume.Fields.linkedin, linkedinAddress);
		CcpJsonRepresentation readableView = viewWithLinkedin.put(VisEntityViewedResume.Fields.email, recruiterEmail);
		return readableView;
	}

	/**
	 * The text to the owner, in Portuguese while the resumes keep no language: the moment of the view (filled by the save
	 * in {@code date}, in the time zone of Brazil; now, when absent) and the domain of the recruiter, or the text of the
	 * freelancer.
	 */
	private static String getMessage(CcpJsonRepresentation view) {
		String recruiterDomain = VisViewedResumeRecruiterDomain.INSTANCE.apply(view);
		String freelancerKey = VisViewedResumeRecruiterDomain.Fields.freelancer.getValue();
		boolean freelancer = freelancerKey.equals(recruiterDomain);
		VisResumeViewMessages text = freelancer ? VisResumeViewMessages.newViewByFreelancer : VisResumeViewMessages.newViewByCompany;
		CcpTimeDecorator now = new CcpTimeDecorator();
		String pattern = Patterns.dateTime.getValue();
		String currentDate = now.getFormattedDateTime(pattern);
		String date = view.getOrDefault(VisEntityViewedResume.Fields.date, () -> currentDate);
		CcpJsonRepresentation parameters = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.date, date)
				.put(Fields.recruiterDomain, recruiterDomain);
		String message = text.getMessage(JnLanguage.portuguese, parameters);
		return message;
	}

	/**
	 * The active resume of the hashed e-mail, read by id.
	 * @return the resume, or an empty JSON when the e-mail has no active resume
	 */
	private static CcpJsonRepresentation getResume(CcpEntity resumeWithHashedKey, String hashedEmail) {
		CcpJsonRepresentation resumeKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityResume.Fields.email, hashedEmail);
		try {
			CcpJsonRepresentation resume = resumeWithHashedKey.getOneById(resumeKey);
			return resume;
		} catch (CcpErrorFlowDisturb noActiveResume) {
			return CcpOtherConstants.EMPTY_JSON;
		}
	}
}
