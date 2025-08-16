package com.vis.services;


import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.CcpEntity;
import com.ccp.especifications.db.utils.CcpEntityCrudOperationType;
import com.ccp.especifications.mensageria.receiver.CcpBulkHandlers;
import com.jn.mensageria.JnFunctionMensageriaSender;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.business.resume.VisBusinessGetResumeContent;
import com.vis.business.resume.VisBusinessResumeSaveViewFailed;
import com.vis.cache.VisBusinessPutSkillsInJson;
import com.vis.entities.VisEntityBalance;
import com.vis.entities.VisEntityDeniedViewToCompany;
import com.vis.entities.VisEntityFees;
import com.vis.entities.VisEntityGroupResumesByPosition;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeFreeView;
import com.vis.entities.VisEntityResumeLastView;
import com.vis.entities.VisEntityResumePerception;
import com.vis.entities.VisEntitySkill;
import com.vis.entities.VisEntitySkillPending;
import com.vis.entities.VisEntitySkillRejected;
import com.vis.status.VisProcessStatusSuggestNewSkill;
import com.vis.status.VisProcessStatusResumeView;

enum VisServicePostionConstants  implements CcpJsonFieldName{
	activePosition
	
}

public class VisServicePostion {
	
	private VisServicePostion() {}
	
	public static final VisServicePostion INSTANCE = new VisServicePostion();
	
	public CcpJsonRepresentation save(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation result = new JnFunctionMensageriaSender(VisEntityPosition.ENTITY, CcpEntityCrudOperationType.save).apply(json);
		
		return result;
	}
	
	public CcpJsonRepresentation changeStatus(CcpJsonRepresentation json) {

		CcpJsonRepresentation result = new JnFunctionMensageriaSender(VisEntityPosition.ENTITY, CcpBulkHandlers.transferToReverseEntity).apply(json);
		
		return result;
	}
	
	public CcpJsonRepresentation getData(CcpJsonRepresentation json) {
		
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		
		CcpEntity mirrorEntity = VisEntityPosition.ENTITY.getTwinEntity();
		CcpSelectUnionAll searchResults = crud.unionAll(json, JnDeleteKeysFromCache.INSTANCE, VisEntityPosition.ENTITY, mirrorEntity);
		
		boolean activeResume = VisEntityPosition.ENTITY.isPresentInThisUnionAll(searchResults, json);
		
		if(activeResume) {
			CcpJsonRepresentation requiredEntityRow = VisEntityPosition.ENTITY.getRequiredEntityRow(searchResults, json);
			CcpJsonRepresentation put = requiredEntityRow.put(VisServicePostionConstants.activePosition, true);
			return put;
		}
		
		CcpJsonRepresentation requiredEntityRow = mirrorEntity.getRequiredEntityRow(searchResults, json);
		CcpJsonRepresentation put = requiredEntityRow.put(VisServicePostionConstants.activePosition, false);
		return put;
	}

	public CcpJsonRepresentation getResumeList(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation oneById = VisEntityGroupResumesByPosition.ENTITY.getOneById(json);
		
		return oneById;
	}
	
	public CcpJsonRepresentation getImportantSkillsFromText(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation oneById = VisEntitySkill.ENTITY.getOneById(json);

		VisBusinessPutSkillsInJson putSkillsInJson = new VisBusinessPutSkillsInJson(
				VisEntityPosition.Fields.description.name(), 
				VisEntityPosition.Fields.requiredSkill.name()
				);
		
		CcpJsonRepresentation jsonWithSkills = oneById.extractInformationFromJson(putSkillsInJson);
		
		return jsonWithSkills;
	}

	public CcpJsonRepresentation getResumeContent(CcpJsonRepresentation json) {

		String context = new Object(){}.getClass().getEnclosingMethod().getName();
		CcpJsonRepresentation findById =  new CcpGetEntityId(json)
		.toBeginProcedureAnd()
			.loadThisIdFromEntity(VisEntityPosition.ENTITY).and()
			.loadThisIdFromEntity(VisEntityResumePerception.ENTITY).and()
			.loadThisIdFromEntity(VisEntityResumeFreeView.ENTITY).and()
			.loadThisIdFromEntity(VisEntityResumeLastView.ENTITY).and()
			.loadThisIdFromEntity(VisEntityPosition.ENTITY.getTwinEntity()).and()
			.loadThisIdFromEntity(VisEntityResumePerception.ENTITY.getTwinEntity()).and()
			.ifThisIdIsNotPresentInEntity(VisEntityBalance.ENTITY).returnStatus(VisProcessStatusResumeView.missingBalance).and()
			.ifThisIdIsNotPresentInEntity(VisEntityFees.ENTITY).returnStatus(VisProcessStatusResumeView.missingFee).and()
			.ifThisIdIsPresentInEntity(VisEntityDeniedViewToCompany.ENTITY).returnStatus(VisProcessStatusResumeView.notAllowedRecruiter).and()
			.ifThisIdIsPresentInEntity(VisEntityResume.ENTITY.getTwinEntity()).returnStatus(VisProcessStatusResumeView.inactiveResume).and()
			.ifThisIdIsNotPresentInEntity(VisEntityResume.ENTITY).returnStatus(VisProcessStatusResumeView.resumeNotFound).and()
			.ifThisIdIsPresentInEntity(VisEntityResume.ENTITY).executeAction(VisBusinessGetResumeContent.INSTANCE).andFinallyReturningTheseFields()
		.endThisProcedureRetrievingTheResultingData(context, VisBusinessResumeSaveViewFailed.INSTANCE, JnDeleteKeysFromCache.INSTANCE);
		
		return findById;
	}

	public CcpJsonRepresentation suggestNewSkills(CcpJsonRepresentation json) {
		
		String context = new Object(){}.getClass().getEnclosingMethod().getName();
		CcpJsonRepresentation findById =  new CcpGetEntityId(json)
		.toBeginProcedureAnd()
			.ifThisIdIsPresentInEntity(VisEntitySkill.ENTITY).returnStatus(VisProcessStatusSuggestNewSkill.alreadyExists).and()
			.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY.getTwinEntity()).returnStatus(VisProcessStatusSuggestNewSkill.approvedSkill).and()
			.ifThisIdIsPresentInEntity(VisEntitySkillRejected.ENTITY).returnStatus(VisProcessStatusSuggestNewSkill.rejectedSkill).and()
			.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY).returnStatus(VisProcessStatusSuggestNewSkill.pendingSkill)
			//TODO
			//.and()
			//.ifThisIdIsNotPresentInEntity(VisEntitySkill.ENTITY).executeAction(new JnMensageriaSender(VisAsyncBusiness.skillsSuggest))
			.andFinallyReturningTheseFields()
		.endThisProcedureRetrievingTheResultingData(context, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE);
		
		return findById;
	}

}
