package com.vis.entities;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityAsyncWriter;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperChild;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityTwin;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionable;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionablePurge;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpStoredParentIdsField;
import com.ccp.especifications.db.utils.entity.decorators.annotations.operations.CcpBeforeSaveFromMainEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldTransformer;
import com.ccp.json.defaultvalues.annotations.CcpJsonFieldDefaultValue;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeBoolean;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;
import com.ccp.json.validations.global.annotations.CcpJsonValidationFieldList;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.entities.decorators.engine.JnAsyncWriterEntity;
import com.jn.entities.decorators.engine.JnGrouperChildEntityAfterWrite;
import com.jn.entities.decorators.engine.JnGrouperChildEntityBeforeWrite;
import com.jn.entities.decorators.engine.JnVersionableEntity;
import com.jn.entities.decorators.engine.JnVersionablePurgeEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.business.position.VisBusinessPutPositionDefaultValues;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonFieldsExpirationRange;
import com.vis.json.fields.validation.VisJsonFieldsPositionContact;
import com.vis.json.fields.validation.VisJsonFieldsPositionSkills;
import com.vis.json.fields.validation.VisJsonFieldsPositionSort;
import com.vis.json.fields.validation.VisJsonFieldsSalaryRange;
import com.vis.json.fields.validation.VisPositionFrequencyTypes;
import com.vis.json.transformers.VisJsonTransformerRecruiterHash;
import com.vis.json.transformers.VisJsonTransformerSpokenLanguageWithoutId;
import com.vis.json.transformers.VisPositionHashCombinations;
import com.vis.json.transformers.VisSeniorityType;

/**
 * Represents the Position entity: a position published by a recruiter, identified by the SHA-1 hash of the e-mail of
 * the recruiter who owns it and by its title. Stores the title, the description, the area (resumeType), the seniority, the regions (DDD), the availability,
 * the skills (required and desired), the spoken languages, the salary ranges (CLT and PJ, at least one of them), the
 * expiration, the order of the resumes found, the contacts and how the resumes are sent (frequency, groups, resumes).
 * The save fills the {@code hash} list with every search of the position ({@code VisPositionHashCombinations}) and
 * keeps the id of the position in the groups of those hashes, apart from the resumes (the {@code childName} of the
 * groups tells the entities apart). Uses the Twin Entity pattern to control inactive positions
 * ({@code vis_inactive_position}), asynchronous writing, versioning and a 1-hour cache.
 */
@CcpEntityCache(3600)
@CcpEntityAsyncWriter(JnAsyncWriterEntity.class)
@CcpEntityVersionable(JnVersionableEntity.class)
@CcpEntityVersionablePurge(JnVersionablePurgeEntity.class)
@CcpEntityFieldsValidator(VisEntityPosition.Fields.class)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityTwin(
		twinEntityName = "vis_inactive_position",
		bulkExecutorClass = JnExecuteBulkOperation.class,
		functionToDeleteKeysInTheCacheClass = JnDeleteKeysFromCache.class
		)
// the default values of the fields (mustSendInGroups, mustSendToResumes, frequencyType) go into the record before it is saved
@CcpBeforeSaveFromMainEntity(execute = {VisBusinessPutPositionDefaultValues.class}, operationHandlers = {})
// one group per hash (each search of the position): the combinations and the list where the save keeps their ids come
// from the field hash
@CcpEntityGrouperChild(value = {VisEntityVirtualHashGrouper.class}, beforeDecoratorClass = JnGrouperChildEntityBeforeWrite.class, afterDecoratorClass = JnGrouperChildEntityAfterWrite.class)
public class VisEntityPosition implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityPosition INSTANCE = new VisEntityPosition();

	private VisEntityPosition() {}

	/** The entity {@code vis_position}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityPosition.class).entityInstance;

	/** The salary ranges of the position; it needs at least one of them. */
	public static enum SalaryType {
		/** The value range as a contractor (PJ). */
		pj,
		/** The salary range as an employee (CLT). */
		clt
	}

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}). The default values are put before the save by
	 * {@code VisBusinessPutPositionDefaultValues}.
	 */
	@CcpJsonGlobalValidations(requiresAtLeastOne = {
			@CcpJsonValidationFieldList(SalaryType.class)
	})
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code clt} field: the salary range as an employee, {@code {min, max}} from 1,000 to 100,000. */
		@CcpJsonFieldTypeNestedJson(jsonValidation = VisJsonFieldsSalaryRange.class, allowsEmptyJson = false)
		clt,

		/** The {@code disponibility} field: required, validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		disponibility,

		/** The {@code ddd} field: required, list, validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1, maxSize = 67)
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ddd,

		/**
		 * The {@code recruiter} field: part of the primary key, the e-mail of the recruiter who owns the position, validated as in
		 * {@code VisJsonCommonsFields}; stored as its SHA-1 hash ({@code VisJsonTransformerRecruiterHash}).
		 */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpEntityFieldTransformer(VisJsonTransformerRecruiterHash.class)
		recruiter,

		/** The {@code resumeType} field: required, the area of the position, validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		resumeType,

		/** The {@code seniority} field: required, one of {@code VisSeniorityType}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(allowedValuesEnum = VisSeniorityType.class)
		seniority,

		/**
		 * The {@code hash} field: list, validated as in {@code VisJsonCommonsFields}; filled on every save by the decorator
		 * of {@code @CcpEntityGrouperChild}, it holds the id of every search of the position, one per combination of
		 * {@code VisPositionHashCombinations}.
		 */
		@CcpJsonFieldValidatorArray
		@CcpStoredParentIdsField(value = VisPositionHashCombinations.class, parent = VisEntityVirtualHashGrouper.class)
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		hash,

		/** The {@code skill} field: list, nested JSON validated by {@code VisJsonFieldsPositionSkills}. */
		@CcpJsonFieldValidatorArray
		@CcpJsonFieldTypeNestedJson(jsonValidation = VisJsonFieldsPositionSkills.class)
		skill,

		/**
		 * The {@code spokenLanguage} field: list, validated as in {@code VisJsonCommonsFields}; transformed by
		 * {@code VisJsonTransformerSpokenLanguageWithoutId}, which drops the {@code id} of each language.
		 */
		@CcpJsonFieldValidatorArray
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpEntityFieldTransformer(VisJsonTransformerSpokenLanguageWithoutId.class)
		spokenLanguage,

		/** The {@code negotiableClaim} field: validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		negotiableClaim,

		/** The {@code pcd} field: validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		pcd,

		/** The {@code pj} field: the value range as a contractor, {@code {min, max}} from 1,000 to 100,000. */
		@CcpJsonFieldTypeNestedJson(jsonValidation = VisJsonFieldsSalaryRange.class, allowsEmptyJson = false)
		pj,

		/** The {@code temporallyJobTime} field: required, validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		temporallyJobTime,

		/** The {@code travel} field: validated as in {@code VisJsonCommonsFields}. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		travel,

		/** The {@code title} field: part of the primary key, the title of the position, text of 10 to 100 characters. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 10, maxLength = 100)
		title,

		/**
		 * The {@code expiration} field: required, {@code {min, max}} timestamps, neither before the current date, the max
		 * not before the min ({@code VisJsonFieldsExpirationRange}).
		 */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeNestedJson(jsonValidation = VisJsonFieldsExpirationRange.class, allowsEmptyJson = false)
		expiration,

		/** The {@code description} field: required, the description of the position, text of 100 to 5,000 characters. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 100, maxLength = 5_000)
		description,

		/**
		 * The {@code sort} field: required, list, the preference of order of the resumes found for the position, each item
		 * validated by {@code VisJsonFieldsPositionSort}.
		 */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonFieldTypeNestedJson(jsonValidation = VisJsonFieldsPositionSort.class, allowsEmptyJson = false)
		sort,

		/** The {@code mustSendInGroups} field: boolean, {@code true} by default. */
		@CcpJsonFieldTypeBoolean
		@CcpJsonFieldDefaultValue(defaultStrings = "true")
		mustSendInGroups,

		/** The {@code mustSendToResumes} field: boolean, {@code true} by default. */
		@CcpJsonFieldTypeBoolean
		@CcpJsonFieldDefaultValue(defaultStrings = "true")
		mustSendToResumes,

		/** The {@code frequencyType} field: one of {@code VisPositionFrequencyTypes}, {@code daily} by default. */
		@CcpJsonFieldTypeString(allowedValuesEnum = VisPositionFrequencyTypes.class)
		@CcpJsonFieldDefaultValue(defaultStrings = "daily")
		frequencyType,

		/**
		 * The {@code contact} field: required, list, where the candidates reach the recruiter, each item validated by
		 * {@code VisJsonFieldsPositionContact} (the value according to its type).
		 */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonFieldTypeNestedJson(jsonValidation = VisJsonFieldsPositionContact.class, allowsEmptyJson = false)
		contact,
		;
	}
}
