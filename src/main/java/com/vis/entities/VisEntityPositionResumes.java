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
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.business.messages.JnEmailFooter;
import com.jn.entities.JnEntityEmailParametersToSend;
import com.jn.entities.JnEntityEmailTemplateMessage;
import com.jn.entities.decorators.engine.JnDisposableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;
import com.vis.business.recruiter.resumes.VisRecruiterResumesFields;
import com.vis.messages.VisMessages;

/**
 * The whole list of resumes of the last e-mail of a position (since 2026-11-24): the positions that receive their resumes
 * instantly, hourly, daily or weekly get an e-mail with the first 30, and the link "see all" of that e-mail opens this list
 * (the screen {@code meu-recrutamento/curriculos-da-vaga}, a link of its own, apart from the resumes of the week). One
 * record per e-mail that receives the resumes and title of the position, replaced by every e-mail; discarded with the month.
 * <p>
 * Also seeds the templates of the two e-mails of the positions (Portuguese and English) and their sending parameters.
 */
@CcpEntityCache(3600)
@CcpEntityDisposable(value = JnDisposableEntity.class, timeOption = CcpEntityExpurgableOptions.monthly)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityPositionResumes.Fields.class)
public class VisEntityPositionResumes implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityPositionResumes INSTANCE = new VisEntityPositionResumes();

	private VisEntityPositionResumes() {}

	/** The entity {@code vis_position_resumes}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityPositionResumes.class).entityInstance;

	/** The fields of the entity, with their validation rules. */
	public static enum Fields implements CcpJsonFieldName{
		/** The e-mail that receives the resumes of the position ({@code resumesEmail}), part of the primary key. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,

		/** The title of the position, part of the primary key. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 10, maxLength = 100)
		title,

		/** The rows of the resumes, as the e-mail shows them, in the order of the position. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1)
		resumes,
		;
	}

	/** The local values of the sending parameters; production has its own ({@code documentation/deploy}). */
	static enum Parameters implements CcpJsonFieldName {
		SENDER { public String getValue() { return "curriculos@onias.site"; } },
	}

	/** The pieces of the texts of the templates. */
	static enum Texts implements CcpJsonFieldName {
		INSTANT_SUBJECT_PORTUGUESE { public String getValue() { return "Cv recebido para a vaga {positionTitle}"; } },
		INSTANT_SUBJECT_ENGLISH { public String getValue() { return "Resume received for the position {positionTitle}"; } },
		PERIODIC_SUBJECT_PORTUGUESE { public String getValue() { return "{totalOfResumes} Cvs recebidos para a vaga {positionTitle}"; } },
		PERIODIC_SUBJECT_ENGLISH { public String getValue() { return "{totalOfResumes} resumes received for the position {positionTitle}"; } },
		INSTANT_OPENING_PORTUGUESE { public String getValue() { return "<p>Olá, aqui é o <a href=\"{oniasLinkedin}\" target=\"_blank\">Onias</a>, do "
				+ "<a href=\"{frontendUrl}\" target=\"_blank\">onias.site</a>. Acabou de ser salvo um currículo aderente à vaga <strong>{positionTitle}</strong>:</p>"; } },
		INSTANT_OPENING_ENGLISH { public String getValue() { return "<p>Hello, this is <a href=\"{oniasLinkedin}\" target=\"_blank\">Onias</a>, from "
				+ "<a href=\"{frontendUrl}\" target=\"_blank\">onias.site</a>. A resume that matches the position <strong>{positionTitle}</strong> has just been saved:</p>"; } },
		PERIODIC_OPENING_PORTUGUESE { public String getValue() { return "<p>Olá, aqui é o <a href=\"{oniasLinkedin}\" target=\"_blank\">Onias</a>, do "
				+ "<a href=\"{frontendUrl}\" target=\"_blank\">onias.site</a>. Seguem os currículos aderentes à vaga <strong>{positionTitle}</strong> "
				+ "inseridos ou alterados desde o último envio, {totalOfResumes} no total (<a href=\"{seeAllUrl}\" target=\"_blank\">ver todos</a>), "
				+ "na ordem escolhida para a vaga.</p>"; } },
		PERIODIC_OPENING_ENGLISH { public String getValue() { return "<p>Hello, this is <a href=\"{oniasLinkedin}\" target=\"_blank\">Onias</a>, from "
				+ "<a href=\"{frontendUrl}\" target=\"_blank\">onias.site</a>. Here are the resumes that match the position <strong>{positionTitle}</strong> "
				+ "added or changed since the last sending, {totalOfResumes} in total (<a href=\"{seeAllUrl}\" target=\"_blank\">see all</a>), "
				+ "in the order chosen for the position.</p>"; } },
		CLOSING_PORTUGUESE { public String getValue() { return "<p>Ao abordar o candidato encontrado aqui no nosso site, sempre lhe diga que o encontrou aqui, "
				+ "isso ajuda muito o nosso trabalho de lhes entregar currículos gratuitamente.</p>"
				+ "<p>Para mudar a frequência deste envio ou parar de recebê-lo, edite a vaga em <a href=\"{frontendUrl}/meu-recrutamento/vagas\" target=\"_blank\">Minhas vagas</a>.</p>"; } },
		CLOSING_ENGLISH { public String getValue() { return "<p>When you contact a candidate found here on our site, always tell them you found them here: "
				+ "it helps a lot our work of delivering resumes to you for free.</p>"
				+ "<p>To change how often you receive this e-mail or to stop it, edit the position in <a href=\"{frontendUrl}/meu-recrutamento/vagas\" target=\"_blank\">My positions</a>.</p>"; } },
	}

	/**
	 * The templates of the e-mail of a single resume (the positions that receive them instantly) and of the e-mail of the
	 * resumes of a period (hourly, daily, weekly), in Portuguese and English, and their sending parameters.
	 * @return the seed records
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		String instantTemplateId = VisMessages.VisNotifyRecruiterAboutResumeOfThePosition.class.getName();
		String periodicTemplateId = VisMessages.VisNotifyRecruiterAboutResumesOfThePosition.class.getName();
		CcpJsonRepresentation instantPortuguese = getTemplate(instantTemplateId, JnLanguage.portuguese, Texts.INSTANT_SUBJECT_PORTUGUESE, Texts.INSTANT_OPENING_PORTUGUESE, Texts.CLOSING_PORTUGUESE, JnEmailFooter.portuguese);
		CcpJsonRepresentation instantEnglish = getTemplate(instantTemplateId, JnLanguage.english, Texts.INSTANT_SUBJECT_ENGLISH, Texts.INSTANT_OPENING_ENGLISH, Texts.CLOSING_ENGLISH, JnEmailFooter.english);
		CcpJsonRepresentation periodicPortuguese = getTemplate(periodicTemplateId, JnLanguage.portuguese, Texts.PERIODIC_SUBJECT_PORTUGUESE, Texts.PERIODIC_OPENING_PORTUGUESE, Texts.CLOSING_PORTUGUESE, JnEmailFooter.portuguese);
		CcpJsonRepresentation periodicEnglish = getTemplate(periodicTemplateId, JnLanguage.english, Texts.PERIODIC_SUBJECT_ENGLISH, Texts.PERIODIC_OPENING_ENGLISH, Texts.CLOSING_ENGLISH, JnEmailFooter.english);
		CcpJsonRepresentation instantParameters = getParameters(instantTemplateId);
		CcpJsonRepresentation periodicParameters = getParameters(periodicTemplateId);

		List<CcpBulkItem> templateItems = CcpEntityConfigurator.super.toCreateBulkItems(JnEntityEmailTemplateMessage.ENTITY, instantPortuguese, instantEnglish, periodicPortuguese, periodicEnglish);
		List<CcpBulkItem> parametersItems = CcpEntityConfigurator.super.toCreateBulkItems(JnEntityEmailParametersToSend.ENTITY, instantParameters, periodicParameters);
		List<CcpBulkItem> firstRecords = new ArrayList<>(templateItems);
		firstRecords.addAll(parametersItems);
		return firstRecords;
	}

	private static CcpJsonRepresentation getTemplate(String templateId, JnLanguage language, Texts subject, Texts opening, Texts closing, JnEmailFooter footer) {
		String css = VisEntityRecruiterResumes.Style.CSS.getValue();
		String resumesTable = "{" + VisRecruiterResumesFields.resumesTable + "}";
		String seeAll = "{" + VisRecruiterResumesFields.seeAll + "}";
		String message = "<html><head>" + css + "</head><body>"
				+ opening.getValue()
				+ resumesTable
				+ "<p>" + seeAll + "</p>"
				+ closing.getValue()
				+ footer.getValue()
				+ "</body></html>";
		String subjectText = subject.getValue();
		CcpJsonRepresentation template = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.templateId, templateId)
				.put(JnJsonCommonsFields.language, language)
				.put(JnJsonCommonsFields.subject, subjectText)
				.put(JnJsonCommonsFields.message, message);
		return template;
	}

	private static CcpJsonRepresentation getParameters(String templateId) {
		String sender = Parameters.SENDER.getValue();
		String oniasLinkedin = VisEntityRecruiterResumes.Parameters.ONIAS_LINKEDIN.getValue();
		String frontendUrl = VisEntityRecruiterResumes.Parameters.FRONTEND_URL.getValue();
		CcpJsonRepresentation parameters = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.sender, sender)
				.put(JnJsonCommonsFields.subjectType, templateId)
				.put(JnJsonCommonsFields.templateId, templateId)
				.addToItem(JnJsonCommonsFields.moreParameters, VisRecruiterResumesFields.oniasLinkedin, oniasLinkedin)
				.addToItem(JnJsonCommonsFields.moreParameters, VisRecruiterResumesFields.frontendUrl, frontendUrl);
		return parameters;
	}
}
