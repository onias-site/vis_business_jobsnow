package com.vis.entities;

import java.util.ArrayList;
import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityDisposable;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.entities.JnEntityEmailParametersToSend;
import com.jn.entities.JnEntityEmailTemplateMessage;
import com.jn.entities.decorators.engine.JnDisposableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;
import com.vis.business.recruiter.resumes.VisBusinessSendResumesToRecruiters;
import com.vis.business.recruiter.resumes.VisRecruiterResumesFields;
import com.vis.business.recruiter.resumes.VisRecruiterResumesTexts;
import com.vis.messages.VisMessages;

/**
 * The resumes of the last e-mail of each recruiter, every one of them, also the ones beyond the 40 the e-mail shows:
 * written by {@link VisBusinessSendResumesToRecruiters} before the e-mail goes out, and read by id (the e-mail of the
 * recruiter logged in) by the screen "see all" of the front end, so that the screen runs no query (OQL). Kept for a
 * day: the next e-mail writes it again.
 *
 * <p>Its seeds are the texts of the table ({@link VisRecruiterResumesTexts}), the template of the e-mail in Portuguese
 * and English and its sending parameters, with the LinkedIn of who signs the e-mail and the address of the front end.
 */
@CcpEntityCache(3600)
@CcpEntityDisposable(value = JnDisposableEntity.class, timeOption = CcpEntityExpurgableOptions.daily)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityRecruiterResumes.Fields.class)
public class VisEntityRecruiterResumes implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityRecruiterResumes INSTANCE = new VisEntityRecruiterResumes();

	private VisEntityRecruiterResumes() {}

	/** The entity {@code vis_recruiter_resumes}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityRecruiterResumes.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code email} field: the primary key, the recruiter, validated as in {@code JnJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,

		/** The {@code resumes} field: required, the rows of the table, as the e-mail shows them. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1)
		resumes,
		;
	}

	/** The values of the sending parameters of the e-mail. */
	static enum Parameters implements CcpJsonFieldName {
		SENDER { public String getValue() { return "devs.jobsnow@gmail.com"; } },
		ONIAS_LINKEDIN { public String getValue() { return "https://www.linkedin.com/in/onias85/"; } },
		FRONTEND_URL { public String getValue() { return "http://localhost:3000"; } },
	}

	/** The style of the e-mail, the same of the legacy e-mail to the recruiters ({@code vaga.html}). */
	static enum Style implements CcpJsonFieldName {
		CSS { public String getValue() { return "<style>"
				+ "p,h4{font-family: \"Trebuchet MS\", Arial, Helvetica, sans-serif; width: 90%;}"
				+ "#customers {font-family: \"Trebuchet MS\", Arial, Helvetica, sans-serif; border-collapse: collapse; width: 100%;}"
				+ "#customers td, #customers th {border: 1px solid #ddd; padding: 8px; vertical-align: top;}"
				+ "#customers tr:nth-child(even){background-color: #f2f2f2;}"
				+ "#customers tr:hover {background-color: #ddd;}"
				+ "#customers th {padding-top: 12px; padding-bottom: 12px; text-align: left; background-color: #7FBCEC; color: white;}"
				+ "#customers p {margin: 2px 0;}"
				+ "</style>"; } },
	}

	/**
	 * The texts of the table, the template of the e-mail (Portuguese and English) and its sending parameters.
	 * @return the seed records
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		String templateId = VisMessages.VisNotifyRecruiterAboutResumes.class.getName();
		String css = Style.CSS.getValue();
		String oniasLinkedin = "{" + VisRecruiterResumesFields.oniasLinkedin + "}";
		String totalOfResumes = "{" + VisRecruiterResumesFields.totalOfResumes + "}";
		String unsubscribeLink = "{" + VisRecruiterResumesFields.unsubscribeLink + "}";
		String resumesTable = "{" + VisRecruiterResumesFields.resumesTable + "}";
		String seeAll = "{" + VisRecruiterResumesFields.seeAll + "}";

		String portugueseMessage = "<html><head>" + css + "</head><body>"
				+ "<p>Olá, caso você não me conheça ou não se lembre de mim, sou o <a href=\"" + oniasLinkedin + "\" target=\"_blank\">Onias</a>.</p>"
				+ "<p>Seguem os currículos inseridos ou alterados no JobsNow nos últimos 7 dias (" + totalOfResumes + " no total).</p>"
				+ "<p><strong>Caso não queira mais receber este tipo de e-mail, favor <a href=\"" + unsubscribeLink + "\" target=\"_blank\">clicar aqui</a></strong>.</p>"
				+ resumesTable
				+ "<p>" + seeAll + "</p>"
				+ "<p>Dica para sucesso em abordagem a candidatos: ao abordar o dono do currículo que te interessou, por favor, dê preferência a meios menos invasivos "
				+ "de comunicação do que chamada telefônica ou chamada de WhatsApp. Exemplo: envie mensagem por WhatsApp, LinkedIn ou e-mail e espere o candidato responder.</p>"
				+ "</body></html>";
		String englishMessage = "<html><head>" + css + "</head><body>"
				+ "<p>Hello, in case you do not know me or do not remember me, I am <a href=\"" + oniasLinkedin + "\" target=\"_blank\">Onias</a>.</p>"
				+ "<p>Here are the resumes added or changed on JobsNow in the last 7 days (" + totalOfResumes + " in total).</p>"
				+ "<p><strong>If you no longer want to receive this kind of e-mail, please <a href=\"" + unsubscribeLink + "\" target=\"_blank\">click here</a></strong>.</p>"
				+ resumesTable
				+ "<p>" + seeAll + "</p>"
				+ "<p>A tip for approaching candidates: when you contact the owner of a resume you liked, please prefer less invasive ways of communication than "
				+ "a phone call or a WhatsApp call. For example: send a message by WhatsApp, LinkedIn or e-mail and wait for the candidate to answer.</p>"
				+ "</body></html>";

		CcpJsonRepresentation templateWithTemplateId = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.templateId, templateId);
		CcpJsonRepresentation portugueseTemplate = templateWithTemplateId
				.put(JnJsonCommonsFields.language, JnLanguage.portuguese)
				.put(JnJsonCommonsFields.subject, "Currículos inseridos ou alterados nos últimos 7 dias")
				.put(JnJsonCommonsFields.message, portugueseMessage);
		CcpJsonRepresentation englishTemplate = templateWithTemplateId
				.put(JnJsonCommonsFields.language, JnLanguage.english)
				.put(JnJsonCommonsFields.subject, "Resumes added or changed in the last 7 days")
				.put(JnJsonCommonsFields.message, englishMessage);

		String sender = Parameters.SENDER.getValue();
		String oniasLinkedinAddress = Parameters.ONIAS_LINKEDIN.getValue();
		String frontendUrl = Parameters.FRONTEND_URL.getValue();
		CcpJsonRepresentation parameters = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.sender, sender)
				.put(JnJsonCommonsFields.subjectType, templateId)
				.put(JnJsonCommonsFields.templateId, templateId)
				.addToItem(JnJsonCommonsFields.moreParameters, VisRecruiterResumesFields.oniasLinkedin, oniasLinkedinAddress)
				.addToItem(JnJsonCommonsFields.moreParameters, VisRecruiterResumesFields.frontendUrl, frontendUrl);

		List<CcpBulkItem> templateItems = CcpEntityConfigurator.super.toCreateBulkItems(JnEntityEmailTemplateMessage.ENTITY, portugueseTemplate, englishTemplate);
		List<CcpBulkItem> parametersItems = CcpEntityConfigurator.super.toCreateBulkItems(JnEntityEmailParametersToSend.ENTITY, parameters);
		List<CcpBulkItem> textItems = VisRecruiterResumesTexts.getSeeds();

		List<CcpBulkItem> firstRecords = new ArrayList<>(templateItems);
		firstRecords.addAll(parametersItems);
		firstRecords.addAll(textItems);
		return firstRecords;
	}
}
