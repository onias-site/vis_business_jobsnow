package com.vis.services;

import java.util.function.Supplier;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
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

/**
 * Serviço de acesso a dados de vagas. Expõe operações de CRUD e consulta de skills relacionadas a vagas.
 */
public enum VisServicePosition implements JnService {
	ChangeStatus{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation result = VisEntityPosition.ENTITY.delete(json);
			
			return result;
		}
	},
	GetData{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
			
			CcpEntity mirrorEntity = VisEntityPosition.ENTITY.getTwinEntity();
			CcpSelectUnionAll searchResults = crud.unionAll(json, JnDeleteKeysFromCache.INSTANCE, VisEntityPosition.ENTITY, mirrorEntity);
			
			boolean activeResume = VisEntityPosition.ENTITY.isPresentInThisUnionAll(searchResults, json);
			
			Supplier<CcpJsonRepresentation> jsonSupplier = json.getJsonSupplier();
			if(activeResume) {
				CcpJsonRepresentation requiredEntityRow = VisEntityPosition.ENTITY.getRecordFromUnionAll(searchResults, jsonSupplier);
				CcpJsonRepresentation put = requiredEntityRow.put(JsonFieldNames.activePosition, true);
				return put;
			}
			
			CcpJsonRepresentation requiredEntityRow = mirrorEntity.getRecordFromUnionAll(searchResults, jsonSupplier);
			CcpJsonRepresentation put = requiredEntityRow.put(JsonFieldNames.activePosition, false);
			return put;
		}
	},
	GetImportantSkillsFromText{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			
			return json;
		}
	},
	GetResumeList{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			var object = new Object(){};
			var objectClass = object.getClass();
			var enclosingMethod = objectClass.getEnclosingMethod();
			String context = enclosingMethod.getName();
			CcpGetEntityId ccpGetEntityId = new CcpGetEntityId(json);
			CcpSelectProcedure toBeginProcedureAnd = ccpGetEntityId
			.toBeginProcedureAnd();
			var ifThisIdIsPresentInEntity = toBeginProcedureAnd
				.ifThisIdIsPresentInEntity(VisEntitySkill.ENTITY);
				var returnStatus = ifThisIdIsPresentInEntity.returnStatus(VisProcessStatusSuggestNewSkill.alreadyExists);
				var and = returnStatus.and();
				CcpEntity twinEntity = VisEntitySkillPending.ENTITY.getTwinEntity();
				var ifThisIdIsPresentInEntity2 = and
				.ifThisIdIsPresentInEntity(twinEntity);
				var returnStatus2 = ifThisIdIsPresentInEntity2.returnStatus(VisProcessStatusSuggestNewSkill.approvedSkill);
				var and2 = returnStatus2.and();
				var ifThisIdIsPresentInEntity3 = and2
				.ifThisIdIsPresentInEntity(VisEntitySkillRejected.ENTITY);
				var returnStatus3 = ifThisIdIsPresentInEntity3.returnStatus(VisProcessStatusSuggestNewSkill.rejectedSkill);
				var and3 = returnStatus3.and();
				var ifThisIdIsPresentInEntity4 = and3
				.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY);
				var returnStatus4 = ifThisIdIsPresentInEntity4.returnStatus(VisProcessStatusSuggestNewSkill.pendingSkill);
				var andFinallyReturningTheseFields = returnStatus4
				//.and()
				//.ifThisIdIsNotPresentInEntity(VisEntitySkill.ENTITY).executeAction(new JnMensageriaSender(VisAsyncBusiness.skillsSuggest))
				.andFinallyReturningTheseFields();
				CcpFieldName ccpFieldName = new CcpFieldName(context);
				CcpJsonRepresentation findById =  andFinallyReturningTheseFields
				.endThisProcedureRetrievingTheResultingData(ccpFieldName, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE);
			
			return findById;
		}
	},
	Save{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation result = VisEntityPosition.ENTITY.save(json);
			
			return result;
		}
	},
	SuggestNewSkills{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			var object2 = new Object(){};
			var object2Class = object2.getClass();
			var enclosingMethod2 = object2Class.getEnclosingMethod();
			String context = enclosingMethod2.getName();
			CcpGetEntityId ccpGetEntityId2 = new CcpGetEntityId(json);
			CcpSelectProcedure toBeginProcedureAnd2 = ccpGetEntityId2
			.toBeginProcedureAnd();
			var ifThisIdIsPresentInEntity5 = toBeginProcedureAnd2
				.ifThisIdIsPresentInEntity(VisEntitySkill.ENTITY);
				var returnStatus5 = ifThisIdIsPresentInEntity5.returnStatus(VisProcessStatusSuggestNewSkill.alreadyExists);
				var and4 = returnStatus5.and();
				CcpEntity twinEntity2 = VisEntitySkillPending.ENTITY.getTwinEntity();
				var ifThisIdIsPresentInEntity6 = and4
				.ifThisIdIsPresentInEntity(twinEntity2);
				var returnStatus6 = ifThisIdIsPresentInEntity6.returnStatus(VisProcessStatusSuggestNewSkill.approvedSkill);
				var and5 = returnStatus6.and();
				var ifThisIdIsPresentInEntity7 = and5
				.ifThisIdIsPresentInEntity(VisEntitySkillRejected.ENTITY);
				var returnStatus7 = ifThisIdIsPresentInEntity7.returnStatus(VisProcessStatusSuggestNewSkill.rejectedSkill);
				var and6 = returnStatus7.and();
				var ifThisIdIsPresentInEntity8 = and6
				.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY);
				var returnStatus8 = ifThisIdIsPresentInEntity8.returnStatus(VisProcessStatusSuggestNewSkill.pendingSkill);
				var andFinallyReturningTheseFields2 = returnStatus8
				//LATER
				//.and()
				//.ifThisIdIsNotPresentInEntity(VisEntitySkill.ENTITY).executeAction(new JnMensageriaSender(VisAsyncBusiness.skillsSuggest))
				.andFinallyReturningTheseFields();
				CcpFieldName ccpFieldName2 = new CcpFieldName(context);
				CcpJsonRepresentation findById =  andFinallyReturningTheseFields2
				.endThisProcedureRetrievingTheResultingData(ccpFieldName2, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE);
			
			return findById;
		}
	},
	;
	enum JsonFieldNames implements CcpJsonFieldName{
		activePosition
	}







}
