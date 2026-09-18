package com.vis.entities;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCustomDecorator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCustomDecorators;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityTwin;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldTransformer;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeBoolean;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeTimeBefore;
import com.ccp.json.validations.global.annotations.CcpJsonGlobalValidations;
import com.ccp.json.validations.global.annotations.CcpJsonValidationFieldList;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.entities.decorators.annotations.JnEntityAsyncWriter;
import com.jn.entities.decorators.annotations.JnEntityVersionable;
import com.jn.entities.decorators.builders.JnEntityAsyncWriterBuilder;
import com.jn.entities.decorators.builders.JnEntityVersionableBuilder;
import com.jn.entities.decorators.engine.JnAsyncWriterEntity;
import com.jn.entities.decorators.engine.JnVersionableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.transformers.VisJsonTransformerPutEmailHashAndDomainRecruiter;

/**
 * Representa a entidade central de Vaga (position) no sistema. Armazena todos os dados de uma vaga
 * publicada por um recrutador: cargo, senioridade, localização (DDD), disponibilidade, canais de contato,
 * skills requeridas e desejadas, faixa salarial (CLT, PJ, BTC), frequência de envio de currículos,
 * data de expiração e critérios de ordenação. Utiliza o padrão Twin Entity para controlar vagas inativas
 * (inactive_position), tem escrita assíncrona, versionamento e cache de 1 hora. Ao salvar ou deletar,
 * dispara fluxos de reagrupamento e envio de currículos para recrutadores.
 */
@CcpEntityCache(3600)
@CcpEntityCustomDecorators(value = {@CcpEntityCustomDecorator(value = JnEntityVersionableBuilder.class, priority = 2),@CcpEntityCustomDecorator(value = JnEntityAsyncWriterBuilder.class, priority = 8),})
@CcpEntityTwin(
		twinEntityName = "inactive_position",
		bulkExecutorClass = JnExecuteBulkOperation.class,
		functionToDeleteKeysInTheCacheClass = JnDeleteKeysFromCache.class
		)
@JnEntityAsyncWriter(JnAsyncWriterEntity.class)
@JnEntityVersionable(JnVersionableEntity.class)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntityPosition.Fields.class)
//@CcpEntityOperations({
//		@CcpEntityOperation(operationType = CcpEntityOperationType.beforeSaveFromMainEntity,  execute = {VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes.class}, operationHandlers = {}),
//		// TODO VAI SAIR ESSE FLUXO POR CAUSA DA RETIRADA DOS GROUPINGS
//		@CcpEntityOperation(operationType = CcpEntityOperationType.beforeDeleteFromMainEntity,  execute = {VisBusinessDuplicateFieldEmailToFieldMasters.class, VisBusinessGroupPositionsGroupedByRecruiters.class}, operationHandlers = {}),
//		//TODO REVISITAR ESTE FLUXO
//		@CcpEntityOperation(operationType = CcpEntityOperationType.beforeDeleteFromTwinEntity,  execute = {VisBusinessPositionUpdateGroupingByRecruitersAndSendResumes.class}, operationHandlers = {}),
//})

public class VisEntityPosition implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityPosition.class).entityInstance;

	static enum MaxSalaryType { maxClt, maxPj }
	static enum MinSalaryType { minClt, minPj }
	static enum CltSalaryRange { maxClt, minClt }
	static enum PjSalaryRange  { minPj, maxPj }
	
	@CcpJsonGlobalValidations(requiresAtLeastOne = {
			@CcpJsonValidationFieldList(MaxSalaryType.class),
			@CcpJsonValidationFieldList(MinSalaryType.class)
	}, requiresAllOrNone = {
			@CcpJsonValidationFieldList(CltSalaryRange.class),
			@CcpJsonValidationFieldList(PjSalaryRange.class)
	})
	public static enum Fields implements CcpJsonFieldName{
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(allowedValuesEnum = VisPositionChannelTypes.class)
		@CcpJsonFieldValidatorArray
		channel, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 100)
		contactChannel, 
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date,
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1, maxSize = 67)
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		ddd, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 10, maxLength = 10_000)
		description, 
		@CcpJsonFieldTypeNestedJson
		@CcpJsonFieldValidatorArray
		desiredSkill, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		disponibility, 
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpEntityFieldTransformer(VisJsonTransformerPutEmailHashAndDomainRecruiter.class)
		email, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeTimeBefore(minValue = 0, maxValue = 1, intervalType = CcpEntityExpurgableOptions.yearly)
		expireDate, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(allowedValuesEnum = VisPositionFrequencyTypes.class)
		frequency, 
		@CcpJsonFieldTypeBoolean
		pcd, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 30)
		requiredSkill, 
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		seniority, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(allowedValuesEnum = VisPositionSortFieldTypes.class)
		@CcpJsonFieldValidatorArray(minSize = 1)
		sortFields, 
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp, 
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		title, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeBoolean
		showSalaryExpectation,
		@CcpJsonFieldTypeNumber(minValue = 1000)
		minBtc,
		@CcpJsonFieldTypeNumber(maxValue = 100_000)
		maxBtc,
		@CcpJsonFieldTypeNumber(minValue = 1000)
		minClt,
		@CcpJsonFieldTypeNumber(maxValue = 100_000)
		maxClt,
		@CcpJsonFieldTypeNumber(minValue = 1_000)
		minPj,
		@CcpJsonFieldTypeNumber(maxValue = 100_000)
		maxPj,
		;
	}

	/**
	 * Canais de contato aceitos pelo campo {@code channel} da entidade de vaga (position), ou seja,
	 * por onde o recrutador deseja receber os currículos enviados pela plataforma.
	 */
	public static enum VisPositionChannelTypes {

		telegram,
		whatsapp,
		email,
		sms
		;
	}

	/**
	 * Frequências aceitas pelo campo {@code frequency} da entidade de vaga (position), isto é,
	 * de quanto em quanto tempo os currículos compatíveis são enviados ao recrutador.
	 */
	public static enum VisPositionFrequencyTypes {

		minute,
		hourly,
		daily,
		weekly,
		monthly
		;
	}

	/**
	 * Critérios de ordenação aceitos pelo campo {@code sortFields} da entidade de vaga (position),
	 * usados para ordenar os currículos que serão apresentados ao recrutador.
	 */
	public static enum VisPositionSortFieldTypes {

		seniority,
		pj,
		clt,
		btc,
		disponibility,
		desiredSkills
		;
	}
}
