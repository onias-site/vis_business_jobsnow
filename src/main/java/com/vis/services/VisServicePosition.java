package com.vis.services;

import java.util.function.Supplier;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.services.JnService;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntitySkill;
import com.vis.entities.VisEntitySkillPending;
import com.vis.entities.VisEntitySkillRejected;
import com.vis.status.VisProcessStatusSuggestNewSkill;
import com.ccp.especifications.db.crud.CcpSelectProcedure;

import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * Position data access service. Exposes CRUD operations and queries of the skills related to positions.
 */
public enum VisServicePosition implements JnService {  
	ChangeStatus{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			VisEntityPosition.ENTITY.delete(json);

			return json;
		}
	},
	GetData{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
			
			CcpEntity mirrorEntity = VisEntityPosition.ENTITY.getTwinEntity();
			CcpSelectUnionAll searchResults = crud.unionAll(json, JnDeleteKeysFromCache.INSTANCE, VisEntityPosition.ENTITY, mirrorEntity);
			
			boolean isActivePosition = VisEntityPosition.ENTITY.isPresentInThisUnionAll(searchResults, json);
			
			Supplier<CcpJsonRepresentation> jsonSupplier = json.getJsonSupplier();
			if(isActivePosition) {
				CcpJsonRepresentation requiredEntityRow = VisEntityPosition.ENTITY.getRecordFromUnionAll(searchResults, jsonSupplier);
				CcpJsonRepresentation positionWithStatus = requiredEntityRow.put(JnJsonCommonsFields.activePosition, true);
				return positionWithStatus;
			}
			
			CcpJsonRepresentation requiredEntityRow = mirrorEntity.getRecordFromUnionAll(searchResults, jsonSupplier);
			CcpJsonRepresentation positionWithStatus = requiredEntityRow.put(JnJsonCommonsFields.activePosition, false);
			return positionWithStatus;
		}
	},
	GetImportantSkillsFromText{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			
			return json;
		}
	},
	GetResumeList{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			var methodLocator = new Object(){};
			var methodLocatorClass = methodLocator.getClass(); 
			var currentMethod = methodLocatorClass.getEnclosingMethod();
			String context = currentMethod.getName();
			CcpGetEntityId entityIdGetter = new CcpGetEntityId(json);
			CcpSelectProcedure procedure = entityIdGetter
			.toBeginProcedureAnd();
			var ifPresentInSkills = procedure
				.ifThisIdIsPresentInEntity(VisEntitySkill.ENTITY);
				var statusIfSkillExists = ifPresentInSkills.returnStatus(VisProcessStatusSuggestNewSkill.alreadyExists);
				var afterSkillCheck = statusIfSkillExists.and();
				CcpEntity approvedSkillsEntity = VisEntitySkillPending.ENTITY.getTwinEntity();
				var ifPresentInApprovedSkills = afterSkillCheck
				.ifThisIdIsPresentInEntity(approvedSkillsEntity);
				var statusIfApproved = ifPresentInApprovedSkills.returnStatus(VisProcessStatusSuggestNewSkill.approvedSkill);
				var afterApprovedCheck = statusIfApproved.and();
				var ifPresentInRejectedSkills = afterApprovedCheck
				.ifThisIdIsPresentInEntity(VisEntitySkillRejected.ENTITY);
				var statusIfRejected = ifPresentInRejectedSkills.returnStatus(VisProcessStatusSuggestNewSkill.rejectedSkill);
				var afterRejectedCheck = statusIfRejected.and();
				var ifPresentInPendingSkills = afterRejectedCheck
				.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY);
				var statusIfPending = ifPresentInPendingSkills.returnStatus(VisProcessStatusSuggestNewSkill.pendingSkill);
				var andFinallyReturningTheseFields = statusIfPending
				//.and()
				//.ifThisIdIsNotPresentInEntity(VisEntitySkill.ENTITY).executeAction(new JnMensageriaSender(VisAsyncBusiness.skillsSuggest))
				.andFinallyReturningTheseFields();
				CcpFieldName contextFieldName = new CcpFieldName(context);
				CcpJsonRepresentation procedureResult =  andFinallyReturningTheseFields
				.endThisProcedureRetrievingTheResultingData(contextFieldName, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE);
			
			return procedureResult;
		}
	},
	Save{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			VisEntityPosition.ENTITY.save(json);

			return json;
		}
	},
	SuggestNewSkills{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			var methodLocator = new Object(){};
			var methodLocatorClass = methodLocator.getClass();
			var currentMethod = methodLocatorClass.getEnclosingMethod();
			String context = currentMethod.getName();
			CcpGetEntityId entityIdGetter = new CcpGetEntityId(json);
			CcpSelectProcedure procedure = entityIdGetter
			.toBeginProcedureAnd();
			var ifPresentInSkills = procedure
				.ifThisIdIsPresentInEntity(VisEntitySkill.ENTITY);
				var statusIfSkillExists = ifPresentInSkills.returnStatus(VisProcessStatusSuggestNewSkill.alreadyExists);
				var afterSkillCheck = statusIfSkillExists.and();
				CcpEntity approvedSkillsEntity = VisEntitySkillPending.ENTITY.getTwinEntity();
				var ifPresentInApprovedSkills = afterSkillCheck
				.ifThisIdIsPresentInEntity(approvedSkillsEntity);
				var statusIfApproved = ifPresentInApprovedSkills.returnStatus(VisProcessStatusSuggestNewSkill.approvedSkill);
				var afterApprovedCheck = statusIfApproved.and();
				var ifPresentInRejectedSkills = afterApprovedCheck
				.ifThisIdIsPresentInEntity(VisEntitySkillRejected.ENTITY);
				var statusIfRejected = ifPresentInRejectedSkills.returnStatus(VisProcessStatusSuggestNewSkill.rejectedSkill);
				var afterRejectedCheck = statusIfRejected.and();
				var ifPresentInPendingSkills = afterRejectedCheck
				.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY);
				var statusIfPending = ifPresentInPendingSkills.returnStatus(VisProcessStatusSuggestNewSkill.pendingSkill);
				var andFinallyReturningTheseFields = statusIfPending
				//LATER
				//.and()
				//.ifThisIdIsNotPresentInEntity(VisEntitySkill.ENTITY).executeAction(new JnMensageriaSender(VisAsyncBusiness.skillsSuggest))
				.andFinallyReturningTheseFields();
				CcpFieldName contextFieldName = new CcpFieldName(context);
				CcpJsonRepresentation procedureResult =  andFinallyReturningTheseFields
				.endThisProcedureRetrievingTheResultingData(contextFieldName, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE);
			
			return procedureResult;
		}
	},
	;







}
