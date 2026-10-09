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
import com.vis.entities.VisEntitySkillApproved;
import com.vis.entities.VisEntitySkillPending;
import com.vis.entities.VisEntitySkillRejected;
import com.vis.status.VisProcessStatusSuggestNewSkill;
import com.ccp.especifications.db.crud.CcpSelectProcedure;

import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * Position data access service. Exposes CRUD operations and queries of the skills related to positions.
 */
public enum VisServicePosition implements JnService {  
	/** Toggles the position between active and inactive: deleting it moves it to the twin (inactive). */
	ChangeStatus{
		/**
		 * Deletes (deactivates) the position.
		 * @param json the position key
		 * @return the same JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			VisEntityPosition.ENTITY.delete(json);

			return json;
		}
	},
	/** Returns the position, active or inactive, with {@code activePosition}. */
	GetData{
		/**
		 * Reads the position from the main entity and the twin in one search.
		 * @param json the position key
		 * @return the position plus {@code activePosition}
		 */
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
	/** Extracts the important skills of a text (not implemented yet: returns the request). */
	GetImportantSkillsFromText{
		/**
		 * Not implemented yet.
		 * @param json the request
		 * @return the same JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			
			return json;
		}
	},
	/** Should list the resumes of the position; see finding: it is a copy of {@link #SuggestNewSkills}. */
	GetResumeList{
		/**
		 * Runs the same checks as {@link #SuggestNewSkills}.
		 * @param json the request
		 * @return the resulting data
		 */
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
				CcpEntity approvedSkillsEntity = VisEntitySkillApproved.ENTITY;
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
	/** Saves the position. */
	Save{
		/**
		 * Saves the position.
		 * @param json the position
		 * @return the same JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			VisEntityPosition.ENTITY.save(json);

			return json;
		}
	},
	/**
	 * Checks a suggested skill: already a skill (409), already approved (200), already rejected (420) or already pending
	 * (202).
	 */
	SuggestNewSkills{
		/**
		 * Runs the checks.
		 * @param json the suggested skill
		 * @return the resulting data
		 */
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
				CcpEntity approvedSkillsEntity = VisEntitySkillApproved.ENTITY;
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
