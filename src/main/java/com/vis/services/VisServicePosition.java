package com.vis.services;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
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

public enum VisServicePosition implements JnService {
	ChangeStatus{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation result = VisEntityPosition.ENTITY.transferToReverseEntity(json);
			
			return result;
		}
	},
	GetData{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
			
			CcpEntity mirrorEntity = VisEntityPosition.ENTITY.getTwinEntity();
			CcpSelectUnionAll searchResults = crud.unionAll(json, JnDeleteKeysFromCache.INSTANCE, VisEntityPosition.ENTITY, mirrorEntity);
			
			boolean activeResume = VisEntityPosition.ENTITY.isPresentInThisUnionAll(searchResults, json);
			
			if(activeResume) {
				CcpJsonRepresentation requiredEntityRow = VisEntityPosition.ENTITY.getRequiredEntityRow(searchResults, json);
				CcpJsonRepresentation put = requiredEntityRow.put(JsonFieldNames.activePosition, true);
				return put;
			}
			
			CcpJsonRepresentation requiredEntityRow = mirrorEntity.getRequiredEntityRow(searchResults, json);
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
			String context = new Object(){}.getClass().getEnclosingMethod().getName();
			CcpJsonRepresentation findById =  new CcpGetEntityId(json)
			.toBeginProcedureAnd()
				.ifThisIdIsPresentInEntity(VisEntitySkill.ENTITY).returnStatus(VisProcessStatusSuggestNewSkill.alreadyExists).and()
				.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY.getTwinEntity()).returnStatus(VisProcessStatusSuggestNewSkill.approvedSkill).and()
				.ifThisIdIsPresentInEntity(VisEntitySkillRejected.ENTITY).returnStatus(VisProcessStatusSuggestNewSkill.rejectedSkill).and()
				.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY).returnStatus(VisProcessStatusSuggestNewSkill.pendingSkill)
				//LATER
				//.and()
				//.ifThisIdIsNotPresentInEntity(VisEntitySkill.ENTITY).executeAction(new JnMensageriaSender(VisAsyncBusiness.skillsSuggest))
				.andFinallyReturningTheseFields()
			.endThisProcedureRetrievingTheResultingData(context, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE);
			
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
			String context = new Object(){}.getClass().getEnclosingMethod().getName();
			CcpJsonRepresentation findById =  new CcpGetEntityId(json)
			.toBeginProcedureAnd()
				.ifThisIdIsPresentInEntity(VisEntitySkill.ENTITY).returnStatus(VisProcessStatusSuggestNewSkill.alreadyExists).and()
				.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY.getTwinEntity()).returnStatus(VisProcessStatusSuggestNewSkill.approvedSkill).and()
				.ifThisIdIsPresentInEntity(VisEntitySkillRejected.ENTITY).returnStatus(VisProcessStatusSuggestNewSkill.rejectedSkill).and()
				.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY).returnStatus(VisProcessStatusSuggestNewSkill.pendingSkill)
				//LATER
				//.and()
				//.ifThisIdIsNotPresentInEntity(VisEntitySkill.ENTITY).executeAction(new JnMensageriaSender(VisAsyncBusiness.skillsSuggest))
				.andFinallyReturningTheseFields()
			.endThisProcedureRetrievingTheResultingData(context, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE);
			
			return findById;
		}
	},
	;
	enum JsonFieldNames implements CcpJsonFieldName{
		activePosition
	}
	enum ChangeStatus{
		
	}
	enum GetData{
		
	}
	enum GetImportantSkillsFromText{
		
	}
	enum GetResumeContent{
		
	}
	enum GetResumeList{
		
	}
	enum Save{
		
	}
	enum SuggestNewSkills{
		
	}
}
