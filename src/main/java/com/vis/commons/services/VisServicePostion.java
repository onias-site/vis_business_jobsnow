package com.vis.commons.services;


import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.CcpEntity;
import com.ccp.especifications.db.utils.CcpEntityCrudOperationType;
import com.ccp.especifications.mensageria.receiver.CcpBulkHandlers;
import com.jn.mensageria.JnMensageriaSender;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.commons.business.resume.GetResumeContent;
import com.vis.commons.business.resume.ResumeSaveViewFailed;
import com.vis.commons.cache.tasks.PutSkillsInJson;
import com.vis.commons.entities.VisEntityBalance;
import com.vis.commons.entities.VisEntityDeniedViewToCompany;
import com.vis.commons.entities.VisEntityFees;
import com.vis.commons.entities.VisEntityGroupResumesByPosition;
import com.vis.commons.entities.VisEntityPosition;
import com.vis.commons.entities.VisEntityResume;
import com.vis.commons.entities.VisEntityResumeFreeView;
import com.vis.commons.entities.VisEntityResumeLastView;
import com.vis.commons.entities.VisEntityResumePerception;
import com.vis.commons.entities.VisEntitySkill;
import com.vis.commons.entities.VisEntitySkillPending;
import com.vis.commons.entities.VisEntitySkillRejected;
import com.vis.commons.status.SuggestNewSkillStatus;
import com.vis.commons.status.ViewResumeStatus;

public class VisServicePostion {
	
	private VisServicePostion() {}
	
	public static final VisServicePostion INSTANCE = new VisServicePostion();
	
	public CcpJsonRepresentation save(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation result = new JnMensageriaSender(VisEntityPosition.ENTITY, CcpEntityCrudOperationType.save).apply(json);
		
		return result;
	}
	
	public CcpJsonRepresentation changeStatus(CcpJsonRepresentation json) {

		CcpJsonRepresentation result = new JnMensageriaSender(VisEntityPosition.ENTITY, CcpBulkHandlers.transferToReverseEntity).apply(json);
		
		return result;
	}
	
	public CcpJsonRepresentation getData(CcpJsonRepresentation json) {
		
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		
		CcpEntity mirrorEntity = VisEntityPosition.ENTITY.getTwinEntity();
		CcpSelectUnionAll searchResults = crud.unionAll(json, JnDeleteKeysFromCache.INSTANCE, VisEntityPosition.ENTITY, mirrorEntity);
		
		boolean activeResume = VisEntityPosition.ENTITY.isPresentInThisUnionAll(searchResults, json);
		
		if(activeResume) {
			CcpJsonRepresentation requiredEntityRow = VisEntityPosition.ENTITY.getRequiredEntityRow(searchResults, json);
			CcpJsonRepresentation put = requiredEntityRow.put("activePosition", true);
			return put;
		}
		
		CcpJsonRepresentation requiredEntityRow = mirrorEntity.getRequiredEntityRow(searchResults, json);
		CcpJsonRepresentation put = requiredEntityRow.put("activePosition", false);
		return put;
	}

	public CcpJsonRepresentation getResumeList(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation oneById = VisEntityGroupResumesByPosition.ENTITY.getOneById(json);
		
		return oneById;
	}
	
	public CcpJsonRepresentation getImportantSkillsFromText(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation oneById = VisEntitySkill.ENTITY.getOneById(json);

		PutSkillsInJson putSkillsInJson = new PutSkillsInJson(
				VisEntityPosition.Fields.description.name(), 
				VisEntityPosition.Fields.requiredSkill.name()
				);
		
		CcpJsonRepresentation jsonWithSkills = oneById.extractInformationFromJson(putSkillsInJson);
		
		return jsonWithSkills;
	}

	public CcpJsonRepresentation getResumeContent(CcpJsonRepresentation json) {

		CcpJsonRepresentation findById =  new CcpGetEntityId(json)
		.toBeginProcedureAnd()
			.loadThisIdFromEntity(VisEntityPosition.ENTITY).and()
			.loadThisIdFromEntity(VisEntityResumePerception.ENTITY).and()
			.loadThisIdFromEntity(VisEntityResumeFreeView.ENTITY).and()
			.loadThisIdFromEntity(VisEntityResumeLastView.ENTITY).and()
			.loadThisIdFromEntity(VisEntityPosition.ENTITY.getTwinEntity()).and()
			.loadThisIdFromEntity(VisEntityResumePerception.ENTITY.getTwinEntity()).and()
			.ifThisIdIsNotPresentInEntity(VisEntityBalance.ENTITY).returnStatus(ViewResumeStatus.missingBalance).and()
			.ifThisIdIsNotPresentInEntity(VisEntityFees.ENTITY).returnStatus(ViewResumeStatus.missingFee).and()
			.ifThisIdIsPresentInEntity(VisEntityDeniedViewToCompany.ENTITY).returnStatus(ViewResumeStatus.notAllowedRecruiter).and()
			.ifThisIdIsPresentInEntity(VisEntityResume.ENTITY.getTwinEntity()).returnStatus(ViewResumeStatus.inactiveResume).and()
			.ifThisIdIsNotPresentInEntity(VisEntityResume.ENTITY).returnStatus(ViewResumeStatus.resumeNotFound).and()
			.ifThisIdIsPresentInEntity(VisEntityResume.ENTITY).executeAction(GetResumeContent.INSTANCE).andFinallyReturningTheseFields()
		.endThisProcedureRetrievingTheResultingData(ResumeSaveViewFailed.INSTANCE, JnDeleteKeysFromCache.INSTANCE);
		
		return findById;
	}

	public CcpJsonRepresentation suggestNewSkills(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation findById =  new CcpGetEntityId(json)
		.toBeginProcedureAnd()
			.ifThisIdIsPresentInEntity(VisEntitySkill.ENTITY).returnStatus(SuggestNewSkillStatus.alreadyExists).and()
			.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY.getTwinEntity()).returnStatus(SuggestNewSkillStatus.approvedSkill).and()
			.ifThisIdIsPresentInEntity(VisEntitySkillRejected.ENTITY).returnStatus(SuggestNewSkillStatus.rejectedSkill).and()
			.ifThisIdIsPresentInEntity(VisEntitySkillPending.ENTITY).returnStatus(SuggestNewSkillStatus.pendingSkill)
			//TODO
			//.and()
			//.ifThisIdIsNotPresentInEntity(VisEntitySkill.ENTITY).executeAction(new JnMensageriaSender(VisAsyncBusiness.skillsSuggest))
			.andFinallyReturningTheseFields()
		.endThisProcedureRetrievingTheResultingData(CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE);
		
		return findById;
	}

}
