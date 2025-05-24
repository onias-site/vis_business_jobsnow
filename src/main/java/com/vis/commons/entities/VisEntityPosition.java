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
import com.vis.commons.business.position.VisBusinessDuplicateFieldEmailToFieldMasters;
import com.vis.commons.business.position.VisBusinessGroupPositionsGroupedByRecruiters;
import com.vis.commons.business.resume.VisCommonsBusinessExtractSkillsFromResumeText;
import com.vis.commons.json.transformers.VisJsonTransformerPutEmailHashAndDomainRecruiter;
import com.vis.commons.utils.VisAsyncBusinessPositionUpdateGroupingByRecruitersAndSendResumes;

@CcpEntityDecorators(decorators = JnEntityVersionable.class)
@CcpEntityTwin(twinEntityName = "inactive_position")
@CcpEntitySpecifications(
		classWithFieldsValidationsRules = CcpIgnoreFieldsValidation.class,
		inactivate = @CcpEntityTransferOperationEspecification(whenRecordToTransferIsFound = @CcpEntityOperationSpecification(afterOperation = {VisBusinessDuplicateFieldEmailToFieldMasters.class, VisBusinessGroupPositionsGroupedByRecruiters.class}), whenRecordToTransferIsNotFound = @CcpEntityOperationSpecification(afterOperation = {})),
		reactivate = @CcpEntityTransferOperationEspecification(whenRecordToTransferIsFound = @CcpEntityOperationSpecification(afterOperation = {VisAsyncBusinessPositionUpdateGroupingByRecruitersAndSendResumes.class}), whenRecordToTransferIsNotFound = @CcpEntityOperationSpecification(afterOperation = {})),
		delete = @CcpEntityOperationSpecification(afterOperation = {}),
	    save = @CcpEntityOperationSpecification(afterOperation = {VisAsyncBusinessPositionUpdateGroupingByRecruitersAndSendResumes.class}),
		cacheableEntity = true
)

public class VisEntityPosition implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityPosition.class).entityInstance;

	public static enum Fields implements CcpEntityField{
		btc(false), 
		channel(false), 
		clt(false), 
		contactChannel(false), 
		date(false),
		ddd(false), 
		description(false, VisCommonsBusinessExtractSkillsFromResumeText.INSTANCE), 
		desiredSkill(false), 
		disponibility(false), 
		email(true, VisJsonTransformerPutEmailHashAndDomainRecruiter.INSTANCE), 
		expireDate(false), 
		frequency(false), 
		pcd(false), 
		pj(false),  
		requiredSkill(false), 
		seniority(true), 
		sortFields(false), 
		timestamp(false), 
		title(true), 
		
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
