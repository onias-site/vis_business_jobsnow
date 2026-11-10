package com.vis.business.recruiter.resumes;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.entities.JnEntityEmailParametersToSend;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * Prepares the values of an e-mail to the owner of the resume that shows the resume as the recruiter will see it: the
 * table of {@link VisBusinessBuildSavedResumeTable} in {@code resumesTable} and the original e-mail of the owner in
 * {@code email}. Shared by the e-mails of the saved, of the deactivated and of the reactivated resume. The address of the front end of the
 * links of the table comes from the sending parameters of the message, read by id.
 *
 * <p>Input: the resume, as {@code vis_resume} stores it, with {@code originalEmail}. Output: the values of the template.
 */
public class VisBusinessPutResumePreviewInTheEmail implements CcpBusiness {

	/** The message whose sending parameters hold the address of the front end. */
	private final Class<?> messageTemplate;

	/**
	 * Keeps the message.
	 * @param messageTemplate the message whose sending parameters hold the address of the front end
	 */
	public VisBusinessPutResumePreviewInTheEmail(Class<?> messageTemplate) {
		this.messageTemplate = messageTemplate;
	}

	/**
	 * Puts the table of the resume in {@code resumesTable} and the original e-mail of the owner in {@code email}.
	 * @param json the resume, with {@code originalEmail}
	 * @return the values of the template
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String templateId = this.messageTemplate.getName();
		CcpJsonRepresentation parametersId = CcpOtherConstants.EMPTY_JSON.put(JnJsonCommonsFields.templateId, templateId);
		CcpJsonRepresentation parameters = JnEntityEmailParametersToSend.ENTITY.getOneById(parametersId);
		CcpJsonRepresentation moreParameters = parameters.getInnerJson(JnJsonCommonsFields.moreParameters);
		String frontendUrl = moreParameters.getAsString(VisRecruiterResumesFields.frontendUrl);
		CcpJsonRepresentation tableInput = json.put(VisRecruiterResumesFields.frontendUrl, frontendUrl);
		CcpJsonRepresentation jsonWithTable = VisBusinessBuildSavedResumeTable.INSTANCE.execute(tableInput);
		CcpJsonRepresentation jsonWithOriginalEmail = jsonWithTable
				.renameField(JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail, JnJsonCommonsFields.email);
		return jsonWithOriginalEmail;
	}
}
