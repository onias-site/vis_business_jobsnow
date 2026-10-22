package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityArraysAppendedOnSave;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityDisposable;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperChild;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpStoredParentIdsField;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldAppendedOnSave;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.decorators.engine.JnArraysAppendedOnSaveEntity;
import com.jn.entities.decorators.engine.JnDisposableEntity;
import com.jn.entities.decorators.engine.JnGrouperChildEntityAfterWrite;
import com.jn.entities.decorators.engine.JnGrouperChildEntityBeforeWrite;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.vis.json.transformers.VisResumeViewersCombinations;

/**
 * The recruiters who viewed a resume in a month, by the domain of their e-mails (the screen "Quem me viu" of the
 * candidate): one record per resume ({@code email}, kept only as its SHA-1, LGPD) and month ({@code @CcpEntityDisposable}
 * monthly: the id changes at the turn of the month), with one item in {@code domain} per recruiter who viewed the resume
 * for the first time in that month. Written by {@code VisBusinessCountTheFirstViewOfTheResume} after each insert of
 * {@code vis_viewed_resume}; read by id (OQL) by {@code VisBusinessResumeViewersOfTheMonth}, which counts the items of
 * each domain.
 * <p>
 * Two recruiters of the same domain viewing the same resume at the same moment both count:
 * {@code @CcpEntityArraysAppendedOnSave} makes the database add each domain to the stored list, atomically, instead of
 * the save replacing the list with the one read before it. {@code @CcpEntityGrouperChild} keeps the record of each
 * month in the group of the resume ({@code jn_grouper}, parent {@code vis_resume}).
 */
@CcpEntityDisposable(value = JnDisposableEntity.class, timeOption = CcpEntityExpurgableOptions.monthly)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityResumeViewersOfTheMonth.Fields.class)
@CcpEntityGrouperChild(value = VisEntityResume.class, beforeDecoratorClass = JnGrouperChildEntityBeforeWrite.class, afterDecoratorClass = JnGrouperChildEntityAfterWrite.class)
@CcpEntityArraysAppendedOnSave(JnArraysAppendedOnSaveEntity.class)
public class VisEntityResumeViewersOfTheMonth implements CcpEntityConfigurator {

	/** The entity {@code vis_resume_viewers_of_the_month}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityResumeViewersOfTheMonth.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/**
		 * The {@code email} field: the primary key, the e-mail of the viewed resume. It is written already as its SHA-1
		 * (the e-mails of a LinkedIn address are kept only so, in {@code vis_resume_linkedin}), which the {@code email}
		 * transformer of {@code JnJsonTransformersFieldsEntityDefault} keeps as is; a read by the readable e-mail of the
		 * candidate gets the same hash from it.
		 */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 128)
		email,

		/**
		 * The {@code domain} field: required, list with the domain of the e-mail of each recruiter who viewed the resume
		 * for the first time in the month, repeated once per recruiter; each save adds its items to the stored ones.
		 */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 255)
		@CcpEntityFieldAppendedOnSave
		domain,

		/**
		 * The {@code resumeGroup} field: list, filled on every save by the decorator of {@code @CcpEntityGrouperChild}
		 * with the id of the resume ({@code VisResumeViewersCombinations}).
		 */
		@CcpJsonFieldValidatorArray
		@CcpStoredParentIdsField(VisResumeViewersCombinations.class)
		resumeGroup,
	}
}
