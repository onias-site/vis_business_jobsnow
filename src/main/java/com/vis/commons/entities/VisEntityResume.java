package com.vis.commons.entities;

import java.util.function.Function;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.CcpEntity;
import com.ccp.especifications.db.utils.CcpEntityField;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntityDecorators;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntityOperationSpecification;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntityTransferOperationEspecification;
import com.ccp.especifications.db.utils.decorators.configurations.CcpEntityTwin;
import com.ccp.especifications.db.utils.decorators.configurations.CcpIgnoreFieldsValidation;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.decorators.engine.CcpEntityFactory;
import com.jn.entities.decorators.JnEntityVersionable;
import com.jn.json.transformers.JnDefaultEntityFields;
import com.vis.commons.business.resume.VisCommonsBusinessExtractSkillsFromResumeText;
import com.vis.commons.business.resume.VisCommonsBusinessExtractTextFromResume;
import com.vis.commons.business.resume.VisCommonsBusinessSaveResumeInBucket;
import com.vis.commons.utils.VisAsyncBusinessResumeSendToRecruiters;

@CcpEntityDecorators(decorators = JnEntityVersionable.class)
@CcpEntityTwin(twinEntityName = "inactive_resume")
@CcpEntitySpecifications(
		classWithFieldsValidationsRules = CcpIgnoreFieldsValidation.class,
		inactivate = @CcpEntityTransferOperationEspecification(whenRecordToTransferIsFound = @CcpEntityOperationSpecification(afterOperation = {}), whenRecordToTransferIsNotFound = @CcpEntityOperationSpecification(afterOperation = {})),
		reactivate = @CcpEntityTransferOperationEspecification(whenRecordToTransferIsFound = @CcpEntityOperationSpecification(afterOperation = {}), whenRecordToTransferIsNotFound = @CcpEntityOperationSpecification(afterOperation = {})),
		delete = @CcpEntityOperationSpecification(afterOperation = {}),
	    save = @CcpEntityOperationSpecification(afterOperation = {VisCommonsBusinessSaveResumeInBucket.class, VisAsyncBusinessResumeSendToRecruiters.class }),
		cacheableEntity = true
)
public class VisEntityResume implements CcpEntityConfigurator {
	
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityResume.class).entityInstance;
	//DOUBT DÁ PRA MANTER A ORDEM?
	public static enum Fields implements CcpEntityField{
		clt(false, VisCommonsBusinessExtractTextFromResume.INSTANCE), // VEM DO FRONT
		btc(false, VisCommonsBusinessExtractSkillsFromResumeText.INSTANCE), // VEM DO FRONT
		companiesNotAllowed(false), // VEM DO FRONT 
		date(false),// AUTOMATICO
		ddd(false), // VEM DO FRONT
		desiredJob(false), // VEM DO FRONT
		disabilities(false), // VEM DO FRONT
		disponibility(false), // VEM DO FRONT
		email(true),// VEM DO FRONT
		experience(false), // VEM DO FRONT
		lastJob(false), // VEM DO FRONT
		pj(false), // VEM DO FRONT
		skill(false), // CALCULADO
		timestamp(false),//AUTOMATICO
		;
		private final boolean primaryKey;

		private final Function<CcpJsonRepresentation, CcpJsonRepresentation> transformer;
		
		private Fields(boolean primaryKey) {
			this(primaryKey, CcpOtherConstants.DO_NOTHING);
		}

		private Fields(boolean primaryKey, Function<CcpJsonRepresentation, CcpJsonRepresentation> transformer) {
			this.transformer = transformer;
			this.primaryKey = primaryKey;
		}
		
		public Function<CcpJsonRepresentation, CcpJsonRepresentation> getTransformer() {
			return this.transformer == CcpOtherConstants.DO_NOTHING ? JnDefaultEntityFields.getTransformer(this) : this.transformer;
		}

		public boolean isPrimaryKey() {
			return this.primaryKey;
		}
	}
}
