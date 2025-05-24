package com.vis.commons.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpCollectionDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.bulk.CcpEntityBulkOperationType;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.crud.CcpUnionAllExecutor;
import com.ccp.especifications.db.query.CcpDbQueryOptions;
import com.ccp.especifications.db.query.CcpQueryExecutor;
import com.ccp.especifications.db.utils.CcpEntity;
import com.ccp.especifications.db.utils.CcpEntityField;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.mensageria.JnMensageriaSender;
import com.vis.commons.business.position.VisBusinessPositionResumesSend;
import com.vis.commons.entities.VisEntityBalance;
import com.vis.commons.entities.VisEntityDeniedViewToCompany;
import com.vis.commons.entities.VisEntityGroupPositionsByRecruiter;
import com.vis.commons.entities.VisEntityGroupResumesByPosition;
import com.vis.commons.entities.VisEntityPosition;
import com.vis.commons.entities.VisEntityResume;
import com.vis.commons.entities.VisEntityResumeLastView;
import com.vis.commons.entities.VisEntityResumePerception;
import com.vis.commons.entities.VisEntityScheduleSendingResumeFees;
import com.vis.commons.entities.VisEntitySkill;
import com.vis.commons.entities.VisEntityVirtualHashGrouper;
import com.vis.commons.exceptions.RequiredSkillsMissingInResume;
import com.vis.commons.exceptions.VisCommonsMissingFeeToFrequency;
import com.vis.commons.status.ViewResumeStatus;

public class VisAsyncUtils {
	
	//FORGOT BOTAR EM FILA SEPARANDO AS VAGAS EM LOTE DE RECRUTADORES NAO REPETIDOS
	//FORGOT UNION ALL COMEÇANDO PELOS AGRUPADORES POR CURRICULO E RECRUTADOR
	//FORGOT PAGINAÇÃO DE BUCKET

	public static List<CcpJsonRepresentation> sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(CcpJsonRepresentation schedullingPlan, Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> getResumes, Function<String, CcpJsonRepresentation> getPositions) {
		
		String frequency = schedullingPlan.getAsString(VisEntityPosition.Fields.frequency.name());
		
		CcpJsonRepresentation allPositionsGroupedByRecruiters = getPositions.apply(frequency);

		List<CcpJsonRepresentation> resumes = getResumes.apply(schedullingPlan);

		FrequencyOptions valueOf = FrequencyOptions.valueOf(frequency);

		List<CcpJsonRepresentation> allPositionsWithFilteredResumesAndTheirStatis = VisAsyncUtils.getAllPositionsWithFilteredAndSortedResumesAndTheirStatis(allPositionsGroupedByRecruiters, resumes, valueOf);

		List<CcpJsonRepresentation> allPositionsWithFilteredAndSortedResumesAndStatis = allPositionsWithFilteredResumesAndTheirStatis.stream().map(positionsWithFilteredResumes -> getStatisToThisPosition(positionsWithFilteredResumes)).collect(Collectors.toList());
		
		new JnMensageriaSender(VisBusinessPositionResumesSend.INSTANCE).send(allPositionsWithFilteredAndSortedResumesAndStatis);
		
		return allPositionsWithFilteredAndSortedResumesAndStatis;
	}

	private static CcpJsonRepresentation getStatisToThisPosition(CcpJsonRepresentation positionsWithFilteredResumes) {

		List<CcpJsonRepresentation> resumes = positionsWithFilteredResumes.getAsJsonList("resumes");
		List<String> fields = Arrays.asList(
				VisEntityResume.Fields.disponibility.name(),
				VisEntityResume.Fields.experience.name(),
				VisEntityResume.Fields.btc.name(),
				VisEntityResume.Fields.clt.name(),
				VisEntityResume.Fields.pj.name()
				);
		
		for (String field : fields) {
			int total = 0;
			double sum = 0;
			for (CcpJsonRepresentation resume : resumes) {
				boolean fieldIsMissing = resume.containsAllFields(field) == false;
				if(fieldIsMissing) {
					continue;
				}
				Double asDoubleNumber = resume.getAsDoubleNumber(field);
				sum += asDoubleNumber;
				total++;
			}	
			
			boolean hasAtLeastOneResume = total > 0;
			if(hasAtLeastOneResume) {
				double avg = sum / total;
				positionsWithFilteredResumes = positionsWithFilteredResumes.addToItem("statis", field, avg);
			}
		}
		int resumesSize = resumes.size();
		positionsWithFilteredResumes = positionsWithFilteredResumes.addToItem("statis", "resumes", resumesSize);
		return positionsWithFilteredResumes;
	}
	
	private static List<String> getHashes(CcpJsonRepresentation json) {

		String enumsType = json.containsField(VisEntityResume.Fields.experience.name()) 
				? VisEntityResumeLastView.Fields.resume.name() : VisEntityResumeLastView.Fields.position.name();
		List<Integer> disponibilities = json.extractInformationFromJson(GetDisponibilityValuesFromJson.valueOf(enumsType));

		List<CcpJsonRepresentation> moneyValues = getMoneyValues(enumsType, json);

		String seniority = json.extractInformationFromJson(GetSeniorityValueFromJson.valueOf(enumsType));

		List<Boolean> pcds = json.extractInformationFromJson(GetPcdValuesFromJson.valueOf(enumsType));;

		List<String> hashes = new ArrayList<>();
		// Todas as futuras possibilidades são gravadas em uma Lista
		for (Boolean pcd : pcds) {
			for (Integer disponibility : disponibilities) {// 5 (vaga) = [5, 4, 3, 2, 1, 0] || 6 (candidato) [6, 7, 8, 9
				for (CcpJsonRepresentation moneyValue : moneyValues) {
						CcpJsonRepresentation hash = CcpOtherConstants.EMPTY_JSON.put(VisEntityResume.Fields.disponibility.name(), disponibility)
								.put(VisEntityPosition.Fields.seniority.name(), seniority).putAll(moneyValue)
								.put(VisEntityPosition.Fields.pcd.name(), pcd);
						//LATER ELIMINAR NECESSIDADE DE CRIAR ESSA TABELA, ALEM DE ELIMINAR O VIRTUALENTITY
						String hashValue = VisEntityVirtualHashGrouper.ENTITY.calculateId(hash);
						hashes.add(hashValue);
					}
			}
		}
		return hashes;
	}
	
	private static List<CcpJsonRepresentation> getMoneyValues(String enumsType, CcpJsonRepresentation json){
		
		ArrayList<CcpJsonRepresentation> result = new ArrayList<>();
		
		GetMoneyValuesFromJson valueOf = GetMoneyValuesFromJson.valueOf(enumsType);
		
		List<CcpJsonRepresentation> btcValues = valueOf.apply(json,  VisEntityResume.Fields.btc.name());
		List<CcpJsonRepresentation> cltValues = valueOf.apply(json, VisEntityResume.Fields.clt.name());
		List<CcpJsonRepresentation> pjValues = valueOf.apply(json,  VisEntityResume.Fields.pj.name());

		result.addAll(btcValues);
		result.addAll(cltValues);
		result.addAll(pjValues);
		
		return result;
	}

	public static List<CcpJsonRepresentation> getLastUpdated(CcpEntity entity, FrequencyOptions valueOf, String filterFieldName) {
		
		CcpQueryExecutor queryExecutor = CcpDependencyInjection.getDependency(CcpQueryExecutor.class);
		
		CcpDbQueryOptions queryToSearchLastUpdated = 
				CcpDbQueryOptions.INSTANCE
					.startSimplifiedQuery()
						.startRange()
							.startFieldRange(filterFieldName)
								.greaterThan(System.currentTimeMillis() - valueOf.hours * 3_600_000)
							.endFieldRangeAndBackToRange()
						.endRangeAndBackToSimplifiedQuery()
					.endSimplifiedQueryAndBackToRequest()
				;
		String[] resourcesNames = entity.getEntitiesToSelect();

		List<CcpJsonRepresentation> result = queryExecutor.getResultAsList(queryToSearchLastUpdated, resourcesNames);
		
		return result;
	}

	public static CcpJsonRepresentation getAllPositionsGroupedByRecruiters(FrequencyOptions frequency) {

		CcpQueryExecutor queryExecutor = CcpDependencyInjection.getDependency(CcpQueryExecutor.class);

		CcpDbQueryOptions queryToSearchLastUpdatedResumes = 
				CcpDbQueryOptions.INSTANCE
					.startSimplifiedQuery()
						.match(VisEntityPosition.Fields.frequency, frequency.name())
					.endSimplifiedQueryAndBackToRequest()
				;
		String[] resourcesNames = VisEntityPosition.ENTITY.getEntitiesToSelect();
		CcpJsonRepresentation positionsGroupedByRecruiters = queryExecutor.getMap(queryToSearchLastUpdatedResumes, resourcesNames, VisEntityPosition.Fields.email.name());
		return positionsGroupedByRecruiters;
	}

	private static List<CcpJsonRepresentation> getAllPositionsWithFilteredAndSortedResumesAndTheirStatis(
			CcpJsonRepresentation allPositionsGroupedByRecruiters, 
			List<CcpJsonRepresentation> resumes, 
			FrequencyOptions frequency) {
		
		List<CcpJsonRepresentation> allSearchParameters = getAllSearchParameters(allPositionsGroupedByRecruiters, resumes,	frequency);
		boolean positionsNotFound = allSearchParameters.isEmpty();
		
		if(positionsNotFound) {
			return new ArrayList<>();
		}
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		
		CcpUnionAllExecutor unionAllExecutor = crud.getUnionAllExecutor();
		CcpSelectUnionAll searchResults = unionAllExecutor.unionAll(
				allSearchParameters
				,VisEntityResume.ENTITY
				,VisEntityBalance.ENTITY
				,VisEntityResumePerception.ENTITY
				,VisEntityResumeLastView.ENTITY
				,VisEntityDeniedViewToCompany.ENTITY
				,VisEntityScheduleSendingResumeFees.ENTITY
				,VisEntityResumePerception.ENTITY.getTwinEntity()
				);
		
		CcpJsonRepresentation allPositionsWithFilteredResumes = CcpOtherConstants.EMPTY_JSON;
		
		List<CcpBulkItem> errors = new ArrayList<>();
		
		for (CcpJsonRepresentation searchParameters : allSearchParameters) {

			boolean feeNotFound = VisEntityScheduleSendingResumeFees.ENTITY.isPresentInThisUnionAll(searchResults, searchParameters) == false;

			if(feeNotFound) {
				throw new VisCommonsMissingFeeToFrequency(frequency.name());
			}
			
			boolean balanceNotFound = VisEntityBalance.ENTITY.isPresentInThisUnionAll(searchResults, searchParameters) == false;

			if(balanceNotFound) {
				CcpBulkItem error = ViewResumeStatus.missingBalance.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}

			CcpJsonRepresentation fee = VisEntityScheduleSendingResumeFees.ENTITY.getRequiredEntityRow(searchResults, searchParameters);
			
			CcpJsonRepresentation balance = VisEntityBalance.ENTITY.getRequiredEntityRow(searchResults, searchParameters);
			
			String recruiter = searchParameters.getAsString(VisEntityResumePerception.Fields.recruiter.name());
			List<CcpJsonRepresentation> positionsGroupedByThisRecruiter = allPositionsGroupedByRecruiters.getAsJsonList(recruiter);
			int countPositionsGroupedByThisRecruiter = positionsGroupedByThisRecruiter.size();
			
			boolean insuficientFunds = VisCommonsUtils.isInsufficientFunds(countPositionsGroupedByThisRecruiter, fee, balance);
			
			if(insuficientFunds) {
				CcpBulkItem error = ViewResumeStatus.insufficientFunds.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}

			boolean inactiveResume = VisEntityResume.ENTITY.getTwinEntity().isPresentInThisUnionAll(searchResults, searchParameters);
			
			if(inactiveResume) {
				CcpBulkItem error = ViewResumeStatus.inactiveResume.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}
			
			
			
			boolean resumeNotFound = VisEntityResume.ENTITY.isPresentInThisUnionAll(searchResults, searchParameters) == false;
			
			if(resumeNotFound) {
				CcpBulkItem error = ViewResumeStatus.resumeNotFound.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}

			boolean negativetedResume = VisEntityResumePerception.ENTITY.getTwinEntity().isPresentInThisUnionAll(searchResults, searchParameters);
			
			if(negativetedResume) {
				CcpBulkItem error = ViewResumeStatus.negativatedResume.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}
				/*
				 * TI -> backend -> java -> spring -> springboot
				 */
				
			boolean deniedResume = VisEntityDeniedViewToCompany.ENTITY.isPresentInThisUnionAll(searchResults, searchParameters);
			
			if(deniedResume) {
				CcpBulkItem error = ViewResumeStatus.notAllowedRecruiter.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}
			
			allPositionsWithFilteredResumes = getPositionWithFilteredResumes(positionsGroupedByThisRecruiter, 
					allPositionsGroupedByRecruiters, allPositionsWithFilteredResumes, searchParameters, searchResults);
		}
		
		JnExecuteBulkOperation.INSTANCE.executeBulk(errors);
		
	 	CcpJsonRepresentation allPositionsWithFilteredResumesCopy = CcpOtherConstants.EMPTY_JSON.putAll(allPositionsWithFilteredResumes);
		
		List<CcpJsonRepresentation> positionsWithSortedResumes = allPositionsWithFilteredResumes.fieldSet().stream().map(positionId -> getPositionWithSortedResumes(positionId, allPositionsWithFilteredResumesCopy) ).collect(Collectors.toList());
		return positionsWithSortedResumes;
	}
	
	private static CcpJsonRepresentation getPositionWithFilteredResumes(
			List<CcpJsonRepresentation> positionsGroupedByThisRecruiter, 
			CcpJsonRepresentation allPositionsGroupedByRecruiters,
			CcpJsonRepresentation allPositionsWithFilteredResumes,
			CcpJsonRepresentation searchParameters,
			CcpSelectUnionAll searchResults
			) {
	
		CcpJsonRepresentation positionWithFilteredResumes = CcpOtherConstants.EMPTY_JSON;
		
		for (CcpJsonRepresentation positionByThisRecruiter : positionsGroupedByThisRecruiter) {

			CcpJsonRepresentation resume = VisEntityResume.ENTITY.getRequiredEntityRow(searchResults, searchParameters);
			
			CcpCollectionDecorator dddsPosition = positionByThisRecruiter.getAsCollectionDecorator(VisEntityResume.Fields.ddd.name());
			CcpCollectionDecorator dddsResume = resume.getAsCollectionDecorator(VisEntityResume.Fields.ddd.name());
			boolean differentDdds = dddsResume.hasIntersect(dddsPosition.content) == false;
			
			if(differentDdds) {
				continue;
			}
			
			List<String> positionHashes = getHashes(positionByThisRecruiter);
			List<String> resumeHashes = getHashes(resume);
			
			boolean resumeDoesNotMatch = resumeHashes.containsAll(positionHashes) == false;
		
			if(resumeDoesNotMatch) {
				continue;
			}
			
			List<CcpJsonRepresentation> requiredSkills;
			
			try {
				requiredSkills = getRequiredSkillsInThisResume(positionByThisRecruiter, resume);
			} catch (RequiredSkillsMissingInResume e) {
				// LATER: salvar skills faltando no curriculo
				continue;
			}
			
			
			boolean resumeAlreadySeen = resumeAlreadySeen(positionByThisRecruiter, searchResults, searchParameters);
			
			if(resumeAlreadySeen) {
				continue;
			}
			
			String positionId = VisEntityPosition.ENTITY.calculateId(positionByThisRecruiter);
			
			CcpJsonRepresentation emailMessageValuesToSent = allPositionsWithFilteredResumes.getInnerJson(positionId);

			CcpJsonRepresentation resumeLastView = VisEntityResumeLastView.ENTITY.getRecordFromUnionAll(searchResults, searchParameters);

			CcpJsonRepresentation resumeOpinion = VisEntityResumePerception.ENTITY.getRecordFromUnionAll(searchResults, searchParameters);
	
			CcpJsonRepresentation resumeWithCommentAndVisualizationDetails = resume
					.put("resumeOpinion", resumeOpinion).put("resumeLastView", resumeLastView);

			emailMessageValuesToSent = emailMessageValuesToSent
					.addToList("resumes", resumeWithCommentAndVisualizationDetails)
					.put(VisEntityResumeLastView.Fields.position.name(), allPositionsGroupedByRecruiters)
					.put("requiredSkills", requiredSkills)
					;
			
			allPositionsWithFilteredResumes = allPositionsWithFilteredResumes.put(positionId, emailMessageValuesToSent);
		}
		return positionWithFilteredResumes;
	}

	private static List<CcpJsonRepresentation> getRequiredSkillsInThisResume(
			CcpJsonRepresentation positionByThisRecruiter, 
			CcpJsonRepresentation resume) {

		List<String> requiredSkillsFromPosition = positionByThisRecruiter.getAsStringList(VisEntityPosition.Fields.requiredSkill.name());
		
		List<CcpJsonRepresentation> skillsFromResume = resume.getAsJsonList(VisEntityResume.Fields.skill.name());
		List<String> requiredSkillsMissingInResume = new ArrayList<String>();
		List<CcpJsonRepresentation> response = new ArrayList<>();
		for (String requiredSkillFromPosition : requiredSkillsFromPosition) {
			
			boolean skillDirectlyFoundInResume = skillsFromResume.stream().filter(s -> s.getAsString(VisEntityResume.Fields.skill.name()).equals(requiredSkillFromPosition)).findFirst().isPresent();
			
			if(skillDirectlyFoundInResume) {
				CcpJsonRepresentation skill = CcpOtherConstants.EMPTY_JSON
					.put("type", ResumeSkillFoundType.CONTAINED_IN_RESUME)
					.put(VisEntityResume.Fields.skill.name(), requiredSkillFromPosition);
				response.add(skill);
				continue;
			}
			
			Optional<CcpJsonRepresentation> synonymFound = skillsFromResume.stream().filter(s -> s.getAsStringList("synonyms").contains(requiredSkillFromPosition)).findFirst();
			boolean skillFoundBySynonymInResume = synonymFound.isPresent();
			
			if(skillFoundBySynonymInResume) {
				CcpJsonRepresentation synonym = synonymFound.get();
				String synonymName = synonym.getAsString(VisEntityResume.Fields.skill.name());
				CcpJsonRepresentation skill = CcpOtherConstants.EMPTY_JSON
						.put("type", ResumeSkillFoundType.SYNONYM)
						.put(VisEntityResume.Fields.skill.name(), requiredSkillFromPosition)
						.put(VisEntitySkill.Fields.synonym.name(), synonymName)
						;
					response.add(skill);
					continue;
			}
			List<String> parents = skillsFromResume.stream().filter(s -> 
			s.getAsStringList(VisEntitySkill.Fields.parent.name()).contains(requiredSkillFromPosition))
			.map(s -> s.getAsString(VisEntitySkill.Fields.skill.name()))
			.collect(Collectors.toList());
			
			boolean skillFoundByParentsInResume = parents.isEmpty() == false;
			
			if(skillFoundByParentsInResume) {
				CcpJsonRepresentation skill = CcpOtherConstants.EMPTY_JSON
						.put(VisEntitySkill.Fields.skill.name(), requiredSkillFromPosition)
						.put("type", ResumeSkillFoundType.PARENT)
						.put("parents", parents)
						;
					response.add(skill);
				continue;
			}
			
			requiredSkillsMissingInResume.add(requiredSkillFromPosition);
		}
		
		
		boolean itIsMissingRequiredSkillInThisResume = requiredSkillsMissingInResume.isEmpty() == false;
		
		if(itIsMissingRequiredSkillInThisResume) {
			throw new RequiredSkillsMissingInResume(requiredSkillsMissingInResume);
		}
	
		return response;
	}

	private static boolean resumeAlreadySeen(CcpJsonRepresentation positionByThisRecruiter, CcpSelectUnionAll searchResults, CcpJsonRepresentation searchParameters) {
	
		boolean doNotFilterResumesAlreadySeen = positionByThisRecruiter.getAsBoolean("filterResumesAlreadySeen") == false;
		
		if(doNotFilterResumesAlreadySeen) {
			return false;
		}
		
		boolean thisResumeWasNeverSeenBefore = VisEntityResumeLastView.ENTITY.isPresentInThisUnionAll(searchResults, searchParameters) == false;
		
		if(thisResumeWasNeverSeenBefore) {
			return false;
		}
		
		CcpJsonRepresentation resumeLastView = VisEntityResumeLastView.ENTITY.getRequiredEntityRow(searchResults, searchParameters);
		
		CcpJsonRepresentation resume = VisEntityResume.ENTITY.getRequiredEntityRow(searchResults, resumeLastView);
		
		Long resumeLastSeen = resumeLastView.getAsLongNumber(VisEntityResumeLastView.Fields.timestamp.name());

		Long resumeLastUpdate = resume.getAsLongNumber(VisEntityResume.Fields.timestamp.name());
		
		return resumeLastUpdate <= resumeLastSeen;
	}

	private static CcpJsonRepresentation getPositionWithSortedResumes(String positionId, CcpJsonRepresentation allPositionsWithFilteredResumes) {
		
		CcpJsonRepresentation positionWithResumes = allPositionsWithFilteredResumes.getInnerJson(positionId);
		
		List<CcpJsonRepresentation> resumes = positionWithResumes.getAsJsonList("resumes");
		
		boolean singleResume = resumes.size() <= 1;
		
		if(singleResume) {
			return positionWithResumes;
		}
		CcpJsonRepresentation position = positionWithResumes.getInnerJson(VisEntityResumeLastView.Fields.position.name());
		PositionResumesSort positionResumesSort = new PositionResumesSort(position);
		resumes.sort(positionResumesSort);
		CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON.putAll(positionWithResumes).put("resumes", resumes);
		return put;
	}
	
	private static List<CcpJsonRepresentation> getAllSearchParameters(
			CcpJsonRepresentation allPositionsGroupedByRecruiters, List<CcpJsonRepresentation> resumes, FrequencyOptions frequency) {
		
		boolean positionsNotFound = allPositionsGroupedByRecruiters.isEmpty();

		if(positionsNotFound) {
			return new ArrayList<>();
		}
		
		List<CcpJsonRepresentation> allSearchParameters = new ArrayList<>();
		
		Set<String> recruiters = allPositionsGroupedByRecruiters.fieldSet();
		for (String recruiter : recruiters) {
			for (CcpJsonRepresentation resume : resumes) {

				String email = resume.getAsString(VisEntityResume.Fields.email.name());
				
				CcpJsonRepresentation searchParameters = CcpOtherConstants.EMPTY_JSON
						.put(VisEntityResumePerception.Fields.recruiter.name(), recruiter)
						.put(VisEntityPosition.Fields.frequency.name(), frequency)
						.put("owner", recruiter)
						.put(VisEntityResume.Fields.email.name(), email)
						;
				allSearchParameters.add(searchParameters);
			}
		}
		return allSearchParameters;
	}
	
	


	
	public static CcpJsonRepresentation groupPositionsGroupedByRecruiters(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation groupDetailsByMasters = groupDetailsByMasters(json, VisEntityPosition.ENTITY, 
				VisEntityGroupPositionsByRecruiter.ENTITY, VisEntityPosition.Fields.email, VisEntityPosition.Fields.timestamp);
		
		return groupDetailsByMasters;
	}
	
	public static CcpJsonRepresentation groupDetailsByMasters(
			CcpJsonRepresentation json, 
			CcpEntity entityToRead, 
			CcpEntity entityWhereGroup, 
			CcpEntityField masterField, 
			CcpEntityField ascField) {
		//1
		List<String> masters = json.getAsStringList("masters");
		
		CcpDbQueryOptions query = CcpDbQueryOptions.INSTANCE
				.startQuery()
					.startBool()
						.startMust()
							.terms(masterField, masters)
						.endMustAndBackToBool()
					.endBoolAndBackToQuery()
				.endQueryAndBackToRequest()
				.addAscSorting(ascField.name())
		;
		CcpQueryExecutor queryExecutor = CcpDependencyInjection.getDependency(CcpQueryExecutor.class);
		
		String[] entitiesToSelect = entityToRead.getEntitiesToSelect();
		
		GroupDetailsByMasters detailsGroupedByMasters = new GroupDetailsByMasters(masterField.name(), entityToRead, entityWhereGroup);
		
		queryExecutor.consumeQueryResult(query, entitiesToSelect, "10s", 10000, detailsGroupedByMasters);
		
		detailsGroupedByMasters.saveAllDetailsGroupedByMasters();
		
		return json;
	}
	
	
	
	public static void saveRecordsInPages(
			List<CcpJsonRepresentation> records, 
			CcpJsonRepresentation primaryKeySupplier,
			CcpEntity entity) {

		List<CcpBulkItem> allPagesTogether = getRecordsInPages(records, primaryKeySupplier, entity);
		
		JnExecuteBulkOperation.INSTANCE.executeBulk(allPagesTogether);
	}

	public static List<CcpBulkItem> getRecordsInPages(List<CcpJsonRepresentation> records,
			CcpJsonRepresentation primaryKeySupplier, CcpEntity entity) {
		List<CcpBulkItem> allPagesTogether = new ArrayList<>();
		int listSize = 10;
		int totalPages = records.size()  % listSize + 1;
		int index = 0;
		
		for(int from = 0; from < totalPages; from++) {
			List<CcpJsonRepresentation> page = new ArrayList<>();
			for(;(index + 1) % listSize !=0 && index < records.size(); index++) {
				CcpJsonRepresentation resume = records.get(index);
				CcpJsonRepresentation put = resume.put("index", index);
				page.add(put);
			}
			CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON
					.put(VisEntityGroupResumesByPosition.Fields.detail.name(), page)
					.put(VisEntityGroupResumesByPosition.Fields.listSize.name(), listSize)
					.put(VisEntityGroupResumesByPosition.Fields.from.name(), from)
					.putAll(primaryKeySupplier)
					;
			CcpBulkItem bulkItem = entity.getMainBulkItem(put, CcpEntityBulkOperationType.create);
			allPagesTogether.add(bulkItem);
		}
		return allPagesTogether;
	}


}
