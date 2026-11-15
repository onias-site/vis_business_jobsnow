package com.vis.business.bots.linkedin;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTextDecorator;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.entities.JnEntityInstantMessengerTemplateMessage;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnLanguage;
import com.vis.business.resume.VisResumeOwnerChats;
import com.vis.entities.VisEntityResume;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.fields.validation.VisResumeMessageTypes;
import com.vis.messages.VisMessages;

/**
 * The operator confirmed that the LinkedIn address reported by the recruiter is invalid ("Sim" in
 * {@code /verifyLinkedinAddress}):
 * <ul>
 * <li>the report of the recruiter is closed with the answer ({@link VisSupportVerifyLinkedinCloseTheReport});</li>
 * <li>the resume, active or inactive, gets {@code invalidLinkedinAddress = true}: it no longer goes to the recruiters and
 * its link no longer opens. Only this field is written (a partial update of the document), so the owner does not get the
 * e-mail of a resume saved by them;</li>
 * <li>the view of the recruiter is kept: the report already moved it to the twin ({@code vis_inactive_viewed_resume}, tab
 * "Indesejados"), with its comment (since 2026-11-15, rule of the user; until then it was deleted everywhere, as if it
 * never existed);</li>
 * <li>an owner whose e-mail has a chat in {@code jn_instant_messenger_user} gets {@link VisMessages.VisInvalidLinkedinAddress}
 * through the user bot, by a record of {@code vis_resume_message}.</li>
 * </ul>
 */
public class VisSupportVerifyLinkedinConfirmInvalid implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisSupportVerifyLinkedinConfirmInvalid INSTANCE = new VisSupportVerifyLinkedinConfirmInvalid();

	private VisSupportVerifyLinkedinConfirmInvalid() {}

	/** The language of the message to the owner for now: the resumes keep no language yet. */
	static final JnLanguage OWNER_LANGUAGE = JnLanguage.portuguese;

	/**
	 * Marks the resume and tells the owner; the view stays in the twin.
	 * @param json the session, with the parameters of the command
	 * @return the same session
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String linkedinAddress = json.getAsString(VisJsonCommonsFields.linkedinAddress);
		new VisSupportVerifyLinkedinCloseTheReport(VisSupportVerifyLinkedinMessages.reportAnsweredInvalid).execute(json);
		// the view of the recruiter stays in the twin, where the report put it

		String resumeId = json.getAsString(VisSupportVerifyLinkedinFields.resumeEmailSha1);
		CcpJsonRepresentation side = VisSupportVerifyLinkedinConversation.getSideOfTheResume(resumeId);
		boolean resumeNotFound = side.isEmpty();
		if(resumeNotFound) {
			return json;
		}
		String entityName = side.getAsString(JnJsonCommonsFields.entity);
		CcpJsonRepresentation resume = markTheResume(entityName, resumeId);
		this.tellTheOwner(resume, linkedinAddress);
		return json;
	}

	/**
	 * Writes {@code invalidLinkedinAddress = true} in the resume, on the side where it is, and reads it.
	 * @param entityName the name of the entity where the resume is
	 * @param resumeId the id of the resume
	 * @return the resume as stored
	 */
	private static CcpJsonRepresentation markTheResume(String entityName, String resumeId) {
		String activeName = VisEntityResume.ENTITY.getEntityMetaData().entityName;
		boolean active = activeName.equals(entityName);
		CcpEntity twinEntity = VisEntityResume.ENTITY.getTwinEntity();
		CcpEntity entity = active ? VisEntityResume.ENTITY : twinEntity;
		CcpJsonRepresentation invalidAddress = CcpOtherConstants.EMPTY_JSON.put(VisEntityResume.Fields.invalidLinkedinAddress, true);
		CcpBulkItem item = new CcpBulkItem(invalidAddress, CcpBulkEntityOperationType.update, entity, resumeId);
		List<CcpBulkItem> items = List.of(item);
		JnExecuteBulkOperation.INSTANCE.executeBulk(items, JnDeleteKeysFromCache.INSTANCE);
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpJsonRepresentation resume = crud.getOneById(entityName, resumeId);
		return resume;
	}

	/**
	 * Saves the message to the owner in {@code vis_resume_message}, whose save sends it through the user bot, when the
	 * e-mail of the resume has a chat in {@code jn_instant_messenger_user} (since 2026-11-13; until then the chat came
	 * from the resume).
	 * @param resume the resume as stored, with the e-mail already hashed
	 * @param linkedinAddress the address confirmed as invalid
	 */
	private void tellTheOwner(CcpJsonRepresentation resume, String linkedinAddress) {
		String hashedEmail = resume.getAsString(VisEntityResume.Fields.email);
		List<String> hashedEmails = List.of(hashedEmail);
		String message = getMessageToTheOwner(linkedinAddress, OWNER_LANGUAGE);
		VisResumeOwnerChats.saveTheMessage(this, hashedEmails, VisResumeMessageTypes.invalidLinkedinAddress, message);
	}

	/**
	 * The text of {@link VisMessages.VisInvalidLinkedinAddress} in the language, with the address: the template of the
	 * instant message, also shown at the top of the form of the resume.
	 * @param linkedinAddress the address confirmed as invalid
	 * @param language the language
	 * @return the text
	 */
	public static String getMessageToTheOwner(String linkedinAddress, JnLanguage language) {
		String templateId = VisMessages.VisInvalidLinkedinAddress.class.getName();
		CcpJsonRepresentation templateKey = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.templateId, templateId)
				.put(JnJsonCommonsFields.language, language);
		CcpJsonRepresentation template = JnEntityInstantMessengerTemplateMessage.ENTITY.getOneById(templateKey);
		String templateText = template.getAsString(JnJsonCommonsFields.message);
		CcpJsonRepresentation values = CcpOtherConstants.EMPTY_JSON.put(VisJsonCommonsFields.linkedinAddress, linkedinAddress);
		CcpStringDecorator templateDecorator = new CcpStringDecorator(templateText);
		CcpTextDecorator templateTextDecorator = templateDecorator.text();
		CcpTextDecorator resolved = templateTextDecorator.resolveTemplate(values);
		return resolved.content;
	}
}
