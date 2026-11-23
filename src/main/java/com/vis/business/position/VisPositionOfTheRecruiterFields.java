package com.vis.business.position;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;
import com.ccp.json.validations.global.annotations.CcpJsonValidationFieldList;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityPosition;
import com.vis.json.fields.validation.VisJsonValidatorPositionResumesEmailRequired;

/**
 * The input of the creation and of the edition of a position by the logged-in user (screen "Meu Recrutamento - Vagas"):
 * the e-mail of the session, which becomes the {@code recruiter} of the position, and what the user fills in, with the
 * rules of {@code VisEntityPosition} (the copy does not bring {@code @CcpJsonFieldValidatorArray}: the lists declare it). Validated before the save, which is asynchronous: an invalid position answers 422
 * instead of failing in the queue. The {@code hash} and the {@code insertionDate} are calculated by the save.
 */
@CcpJsonGlobalValidations(requiresAtLeastOne = {
		@CcpJsonValidationFieldList(VisEntityPosition.SalaryType.class)
}, customJsonValidators = {VisJsonValidatorPositionResumesEmailRequired.class})
public enum VisPositionOfTheRecruiterFields implements CcpJsonFieldName {
	/** The e-mail of the logged-in user, the recruiter of the position. */
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	email,

	/** The {@code title} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	title,

	/** The {@code description} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	description,

	/** The {@code resumeType} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	resumeType,

	/** The {@code seniority} field of the position, validated as in {@code VisEntityPosition}: a list since 2026-11-23. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	@CcpJsonFieldValidatorArray(minSize = 1, maxSize = 6)
	seniority,

	/** The {@code ddd} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	ddd,

	/** The {@code clt} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	clt,

	/** The {@code pj} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	pj,

	/** The {@code disponibility} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	disponibility,

	/** The {@code temporallyJobTime} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	temporallyJobTime,

	/** The {@code spokenLanguage} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	spokenLanguage,

	/** The {@code skill} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	@CcpJsonFieldValidatorArray
	skill,

	/** The {@code negotiableClaim} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	negotiableClaim,

	/** The {@code pcd} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	pcd,

	/** The {@code travel} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	travel,

	/** The {@code expiration} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	expiration,

	/** The {@code sort} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	@CcpJsonFieldValidatorArray
	sort,

	/** The {@code contact} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	@CcpJsonFieldValidatorArray
	contact,

	/** The {@code mustSendInGroups} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	mustSendInGroups,

	/** The {@code visibleToCandidates} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	visibleToCandidates,

	/** The {@code mustSendToResumes} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	mustSendToResumes,

	/** The {@code frequencyType} field of the position, validated as in {@code VisEntityPosition}. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	frequencyType,

	/**
	 * The {@code resumesEmail} field of the position, validated as in {@code VisEntityPosition}; required unless the
	 * {@code frequencyType} is {@code none} ({@code VisJsonValidatorPositionResumesEmailRequired}).
	 */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	resumesEmail,
	;
}
