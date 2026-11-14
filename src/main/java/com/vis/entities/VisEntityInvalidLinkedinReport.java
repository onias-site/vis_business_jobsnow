package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperChild;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperParentCombinations;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityTwin;
import com.ccp.especifications.db.utils.entity.decorators.annotations.messages.write.CcpAfterInsertFromMainEntitySendAnInstantMessageAndIfFailsSaveAWarning;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.entities.JnEntityLoginEmail;
import com.jn.entities.decorators.engine.JnGrouperChildAsJsonEntityAfterWrite;
import com.jn.entities.decorators.engine.JnGrouperChildAsJsonOnBothSidesEntityBeforeWrite;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.transformers.JnLoginEmailParentCombinations;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.messages.VisMessages;

/**
 * The LinkedIn addresses that recruiters reported as not opening, from the page of a resume of the e-mail of resumes
 * (since 2026-11-13): the recruiter ({@code email}), the address ({@code linkedinAddress}) and the id of the resume
 * ({@code resumeEmailSha1}). One open report per recruiter and address: reporting it again while it waits changes nothing.
 * <p>
 * Inserting a report sends the support bot operator
 * {@code /verifyLinkedinAddress <linkedinAddress> <resumeEmailSha1> <recruiterEmail>}, a ticket of {@code /pendingTickets}
 * ({@link VisMessages.VisNotifySupportAboutInvalidLinkedinTicket}). The answer of the operator (the {@code supportResponse})
 * deletes it, which moves it to the twin entity {@code vis_invalid_linkedin_report_solved}.
 * <p>
 * Each report is a ticket of the recruiter in the group of the e-mail ({@code jn_login_email}) of {@code jn_grouper}, as a
 * JSON text, while it waits for the support; the answer moves it to the same group of {@code jn_inactive_grouper}
 * ({@link JnGrouperChildAsJsonOnBothSidesEntityBeforeWrite}): the screen of the tickets of the user reads both by id.
 */
@CcpEntityGrouperChild(value = {JnEntityLoginEmail.class}, beforeDecoratorClass = JnGrouperChildAsJsonOnBothSidesEntityBeforeWrite.class, afterDecoratorClass = JnGrouperChildAsJsonEntityAfterWrite.class)
@CcpEntityGrouperParentCombinations(parent = JnEntityLoginEmail.class, value = JnLoginEmailParentCombinations.class)
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityInvalidLinkedinReport.Fields.class)
@CcpAfterInsertFromMainEntitySendAnInstantMessageAndIfFailsSaveAWarning(VisMessages.VisNotifySupportAboutInvalidLinkedinTicket.class)
@CcpEntityTwin(
		twinEntityName = "vis_invalid_linkedin_report_solved",
		bulkExecutorClass = JnExecuteBulkOperation.class,
		functionToDeleteKeysInTheCacheClass = JnDeleteKeysFromCache.class
		)
public class VisEntityInvalidLinkedinReport implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityInvalidLinkedinReport INSTANCE = new VisEntityInvalidLinkedinReport();

	private VisEntityInvalidLinkedinReport() {}

	/** The entity {@code vis_invalid_linkedin_report}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityInvalidLinkedinReport.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code email} field: the recruiter who reported, part of the primary key (kept as SHA-1 hash), validated as in {@code JnJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		email,

		/** The {@code linkedinAddress} field: the address reported, part of the primary key, validated as in {@code VisJsonCommonsFields}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		linkedinAddress,

		/** The {@code resumeEmailSha1} field: required, the id of the resume in {@code vis_resume} or {@code vis_inactive_resume}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 100)
		resumeEmailSha1,

		/** The {@code supportResponse} field: text of 10 to 500 characters, the answer of the support, given when it solves the report. */
		@CcpJsonFieldTypeString(minLength = 10, maxLength = 500)
		supportResponse,

		/**
		 * The {@code date} field: text, the date and time of the last save ({@code dd/MM/yyyy HH:mm:ss}), filled by the
		 * decorator of {@code @CcpEntityGrouperChild}; it goes with the report to the groups of the user.
		 */
		@CcpJsonFieldTypeString(exactLength = 19)
		date,
		;
	}
}
