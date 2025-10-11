package com.vis.utils;

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
import com.ccp.decorators.CcpPropertiesDecorator;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.bulk.CcpEntityBulkOperationType;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.crud.CcpUnionAllExecutor;
import com.ccp.especifications.db.query.CcpDbQueryOptions;
import com.ccp.especifications.db.query.CcpQueryExecutor;
import com.ccp.especifications.db.utils.CcpEntity;

import com.ccp.especifications.file.bucket.CcpFileBucketOperation;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.mensageria.JnFunctionMensageriaSender;
import com.vis.business.position.VisBusinessPositionResumesSend;
import com.vis.entities.VisEntityBalance;
import com.vis.entities.VisEntityDeniedViewToCompany;
import com.vis.entities.VisEntityFees;
import com.vis.entities.VisEntityGroupPositionsByRecruiter;
import com.vis.entities.VisEntityGroupResumesByPosition;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeLastView;
import com.vis.entities.VisEntityResumePerception;
import com.vis.entities.VisEntityScheduleSendingResumeFees;
import com.vis.entities.VisEntitySkill;
import com.vis.entities.VisEntityVirtualHashGrouper;
import com.vis.exceptions.VisErrorBusinessMissingFeeToFrequency;
import com.vis.exceptions.VisErrorBusinessRequiredSkillsMissingInResume;
import com.vis.status.VisProcessStatusResumeView;
public class VisUtils {
	enum JsonFieldNames implements CcpJsonFieldName{
		tenant, resumes, statis, resumeOpinion, resumeLastView, requiredSkills, type, synonyms, parents, filterResumesAlreadySeen, owner, masters, index
	}
	
	public static String getTenant() {
		CcpStringDecorator ccpStringDecorator = new CcpStringDecorator("application_properties");
		CcpPropertiesDecorator propertiesFrom = ccpStringDecorator.propertiesFrom();
		CcpJsonRepresentation systemProperties = propertiesFrom.environmentVariablesOrClassLoaderOrFile();
		String tenant = systemProperties.getAsString(JsonFieldNames.tenant);
		return tenant;
	}
	public static boolean isInsufficientFunds(int itemsCount, 
			CcpJsonRepresentation fee, CcpJsonRepresentation balance) {
	
		Double feeValue = fee.getAsDoubleNumber(VisEntityFees.Fields.fee);
		
		Double balanceValue = balance.getAsDoubleNumber(VisEntityBalance.Fields.balance);
		
		Double totalCostToThisRecruiter = feeValue * itemsCount;
		
		boolean insuficientFunds = balanceValue <= totalCostToThisRecruiter;
		
		return insuficientFunds;
	}
	public static CcpJsonRepresentation getResumeFromBucket(CcpJsonRepresentation json) {
		String email = json.getAsString(VisEntityResume.Fields.email);
		String folder = "resumes/" + email;
		String file = "" + json.getAsLongNumber(VisEntityResume.Fields.timestamp);
		String tenant = VisUtils.getTenant();
	
		String resumeJson = CcpFileBucketOperation.get.execute(tenant, folder, file);
		CcpJsonRepresentation resume = new CcpJsonRepresentation(resumeJson);
		return resume;
	}

	
	public static List<CcpJsonRepresentation> sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(VisFrequencyOptions frequency, Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> howToObtainResumes, Function<VisFrequencyOptions, CcpJsonRepresentation> howToObtainPositionsGroupedByRecruiters) {
	
		CcpJsonRepresentation schedullingPlan = CcpOtherConstants.EMPTY_JSON.put(VisEntityPosition.Fields.frequency, frequency);
		List<CcpJsonRepresentation> sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter = sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(schedullingPlan, howToObtainResumes, howToObtainPositionsGroupedByRecruiters);
		return sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter;
	}
	
	//FORGOT BOTAR EM FILA SEPARANDO AS VAGAS EM LOTE DE RECRUTADORES NAO REPETIDOS
	//FORGOT UNION ALL COMEÇANDO PELOS AGRUPADORES POR CURRICULO E RECRUTADOR
	//FORGOT PAGINAÇÃO DE BUCKET

	public static List<CcpJsonRepresentation> sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(CcpJsonRepresentation schedullingPlan, Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> howToObtainResumes, Function<VisFrequencyOptions, CcpJsonRepresentation> howToObtainPositionsGroupedByRecruiters) {
		
		String frequency = schedullingPlan.getAsString(VisEntityPosition.Fields.frequency);
		
		VisFrequencyOptions valueOf = VisFrequencyOptions.valueOf(frequency);

		CcpJsonRepresentation allPositionsGroupedByRecruiters = howToObtainPositionsGroupedByRecruiters.apply(valueOf);

		List<CcpJsonRepresentation> resumes = howToObtainResumes.apply(schedullingPlan);

		List<CcpJsonRepresentation> allPositionsWithFilteredResumesAndTheirStatis = VisUtils.getAllPositionsWithFilteredAndSortedResumesAndTheirStatis(allPositionsGroupedByRecruiters, resumes, valueOf);

		List<CcpJsonRepresentation> allPositionsWithFilteredAndSortedResumesAndStatis = allPositionsWithFilteredResumesAndTheirStatis.stream().map(positionsWithFilteredResumes -> getStatisToThisPosition(positionsWithFilteredResumes)).collect(Collectors.toList());
		
		JnFunctionMensageriaSender mensageria = new JnFunctionMensageriaSender(VisBusinessPositionResumesSend.INSTANCE);
		
		mensageria.sendToMensageria(allPositionsWithFilteredAndSortedResumesAndStatis);
		
		return allPositionsWithFilteredAndSortedResumesAndStatis;
	}

	private static CcpJsonRepresentation getStatisToThisPosition(CcpJsonRepresentation positionsWithFilteredResumes) {

		List<CcpJsonRepresentation> resumes = positionsWithFilteredResumes.getAsJsonList(JsonFieldNames.resumes);
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
				boolean fieldIsMissing = resume.getDynamicVersion().containsAllFields(field) == false;
				if(fieldIsMissing) {
					continue;
				}
				Double asDoubleNumber = resume.getDynamicVersion().getAsDoubleNumber(field);
				sum += asDoubleNumber;
				total++;
			}	
			
			boolean hasAtLeastOneResume = total > 0;
			if(hasAtLeastOneResume) {
				double avg = sum / total;
				positionsWithFilteredResumes = positionsWithFilteredResumes.addToItem(JsonFieldNames.statis, field, avg);
			}
		}
		int resumesSize = resumes.size();
		positionsWithFilteredResumes = positionsWithFilteredResumes.addToItem(JsonFieldNames.statis, JsonFieldNames.resumes, resumesSize);
		return positionsWithFilteredResumes;
	}
	
	private static List<String> getHashes(CcpJsonRepresentation json) {

		String enumsType = json.containsField(VisEntityResume.Fields.experience) 
				? VisEntityResumeLastView.Fields.resume.name() : VisEntityResumeLastView.Fields.position.name();
		List<Integer> disponibilities = json.extractInformationFromJson(VisFunctionsGetDisponibilityValuesFromJson.valueOf(enumsType));

		List<CcpJsonRepresentation> moneyValues = getMoneyValues(enumsType, json);

		String seniority = json.extractInformationFromJson(VisFunctionsGetSeniorityValueFromJson.valueOf(enumsType));

		List<Boolean> pcds = json.extractInformationFromJson(VisFunctionsGetPcdValuesFromJson.valueOf(enumsType));;

		List<String> hashes = new ArrayList<>();
		// Todas as futuras possibilidades são gravadas em uma Lista
		for (Boolean pcd : pcds) {
			for (Integer disponibility : disponibilities) {// 5 (vaga) = [5, 4, 3, 2, 1, 0] || 6 (candidato) [6, 7, 8, 9
				for (CcpJsonRepresentation moneyValue : moneyValues) {
						CcpJsonRepresentation hash = CcpOtherConstants.EMPTY_JSON.put(VisEntityResume.Fields.disponibility, disponibility)
								.put(VisEntityPosition.Fields.seniority, seniority).putAll(moneyValue)
								.put(VisEntityPosition.Fields.pcd, pcd);
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

	public static List<CcpJsonRepresentation> getLastUpdated(CcpEntity entity, VisFrequencyOptions valueOf, String filterFieldName) {
		
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

	
	public static CcpJsonRepresentation getAllPositionsGroupedByRecruiters(VisFrequencyOptions frequency) {

		CcpQueryExecutor queryExecutor = CcpDependencyInjection.getDependency(CcpQueryExecutor.class);

		CcpDbQueryOptions queryToSearchLastUpdatedResumes = 
				CcpDbQueryOptions.INSTANCE
					.startSimplifiedQuery()
						.match(VisEntityPosition.Fields.frequency, frequency)
					.endSimplifiedQueryAndBackToRequest()
				;
		String[] resourcesNames = VisEntityPosition.ENTITY.getEntitiesToSelect();
		CcpJsonRepresentation positionsGroupedByRecruiters = queryExecutor.getMap(queryToSearchLastUpdatedResumes, resourcesNames, VisEntityPosition.Fields.email.name());
		return positionsGroupedByRecruiters;
	}

	private static List<CcpJsonRepresentation> getAllPositionsWithFilteredAndSortedResumesAndTheirStatis(
			CcpJsonRepresentation allPositionsGroupedByRecruiters, 
			List<CcpJsonRepresentation> resumes, 
			VisFrequencyOptions frequency) {
		
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
				throw new VisErrorBusinessMissingFeeToFrequency(frequency.name());
			}
			
			boolean balanceNotFound = VisEntityBalance.ENTITY.isPresentInThisUnionAll(searchResults, searchParameters) == false;

			if(balanceNotFound) {
				CcpBulkItem error = VisProcessStatusResumeView.missingBalance.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}

			CcpJsonRepresentation fee = VisEntityScheduleSendingResumeFees.ENTITY.getRequiredEntityRow(searchResults, searchParameters);
			
			CcpJsonRepresentation balance = VisEntityBalance.ENTITY.getRequiredEntityRow(searchResults, searchParameters);
			
			String recruiter = searchParameters.getAsString(VisEntityResumePerception.Fields.recruiter);
			List<CcpJsonRepresentation> positionsGroupedByThisRecruiter = allPositionsGroupedByRecruiters.getDynamicVersion().getAsJsonList(recruiter);
			int countPositionsGroupedByThisRecruiter = positionsGroupedByThisRecruiter.size();
			
			boolean insuficientFunds = VisUtils.isInsufficientFunds(countPositionsGroupedByThisRecruiter, fee, balance);
			
			if(insuficientFunds) {
				CcpBulkItem error = VisProcessStatusResumeView.insufficientFunds.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}

			boolean inactiveResume = VisEntityResume.ENTITY.getTwinEntity().isPresentInThisUnionAll(searchResults, searchParameters);
			
			if(inactiveResume) {
				CcpBulkItem error = VisProcessStatusResumeView.inactiveResume.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}
			
			
			
			boolean resumeNotFound = VisEntityResume.ENTITY.isPresentInThisUnionAll(searchResults, searchParameters) == false;
			
			if(resumeNotFound) {
				CcpBulkItem error = VisProcessStatusResumeView.resumeNotFound.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}

			boolean negativetedResume = VisEntityResumePerception.ENTITY.getTwinEntity().isPresentInThisUnionAll(searchResults, searchParameters);
			
			if(negativetedResume) {
				CcpBulkItem error = VisProcessStatusResumeView.negativatedResume.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}
				/*
				 * TI -> backend -> java -> spring -> springboot
				 */
				
			boolean deniedResume = VisEntityDeniedViewToCompany.ENTITY.isPresentInThisUnionAll(searchResults, searchParameters);
			
			if(deniedResume) {
				CcpBulkItem error = VisProcessStatusResumeView.notAllowedRecruiter.toBulkItemCreate(searchParameters);	
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
			} catch (VisErrorBusinessRequiredSkillsMissingInResume e) {
				// LATER: salvar skills faltando no curriculo
				continue;
			}
			
			
			boolean resumeAlreadySeen = resumeAlreadySeen(positionByThisRecruiter, searchResults, searchParameters);
			
			if(resumeAlreadySeen) {
				continue;
			}
			
			String positionId = VisEntityPosition.ENTITY.calculateId(positionByThisRecruiter);
			
			CcpJsonRepresentation emailMessageValuesToSent = allPositionsWithFilteredResumes.getDynamicVersion().getInnerJson(positionId);

			CcpJsonRepresentation resumeLastView = VisEntityResumeLastView.ENTITY.getRecordFromUnionAll(searchResults, searchParameters);

			CcpJsonRepresentation resumeOpinion = VisEntityResumePerception.ENTITY.getRecordFromUnionAll(searchResults, searchParameters);
	
			CcpJsonRepresentation resumeWithCommentAndVisualizationDetails = resume
					.put(JsonFieldNames.resumeOpinion, resumeOpinion).put(JsonFieldNames.resumeLastView, resumeLastView);

			emailMessageValuesToSent = emailMessageValuesToSent
					.addToList("resumes", resumeWithCommentAndVisualizationDetails)
					.put(VisEntityResumeLastView.Fields.position, allPositionsGroupedByRecruiters)
					.put(JsonFieldNames.requiredSkills, requiredSkills)
					;
			
			allPositionsWithFilteredResumes = allPositionsWithFilteredResumes.getDynamicVersion().put(positionId, emailMessageValuesToSent);
		}
		return positionWithFilteredResumes;
	}

	private static List<CcpJsonRepresentation> getRequiredSkillsInThisResume(
			CcpJsonRepresentation positionByThisRecruiter, 
			CcpJsonRepresentation resume) {

		List<String> requiredSkillsFromPosition = positionByThisRecruiter.getAsStringList(VisEntityPosition.Fields.requiredSkill);
		
		List<CcpJsonRepresentation> skillsFromResume = resume.getAsJsonList(VisEntityResume.Fields.skill);
		List<String> requiredSkillsMissingInResume = new ArrayList<String>();
		List<CcpJsonRepresentation> response = new ArrayList<>();
		for (String requiredSkillFromPosition : requiredSkillsFromPosition) {
			
			boolean skillDirectlyFoundInResume = skillsFromResume.stream().filter(s -> s.getAsString(VisEntityResume.Fields.skill).equals(requiredSkillFromPosition)).findFirst().isPresent();
			
			if(skillDirectlyFoundInResume) {
				CcpJsonRepresentation skill = CcpOtherConstants.EMPTY_JSON
					.put(JsonFieldNames.type, ResumeSkillFoundType.CONTAINED_IN_RESUME)
					.put(VisEntityResume.Fields.skill, requiredSkillFromPosition);
				response.add(skill);
				continue;
			}
			
			Optional<CcpJsonRepresentation> synonymFound = skillsFromResume.stream().filter(s -> s.getAsStringList(JsonFieldNames.synonyms).contains(requiredSkillFromPosition)).findFirst();
			boolean skillFoundBySynonymInResume = synonymFound.isPresent();
			
			if(skillFoundBySynonymInResume) {
				CcpJsonRepresentation synonym = synonymFound.get();
				String synonymName = synonym.getAsString(VisEntityResume.Fields.skill);
				CcpJsonRepresentation skill = CcpOtherConstants.EMPTY_JSON
						.put(JsonFieldNames.type, ResumeSkillFoundType.SYNONYM)
						.put(VisEntityResume.Fields.skill, requiredSkillFromPosition)
						.put(VisEntitySkill.Fields.synonym, synonymName)
						;
					response.add(skill);
					continue;
			}
			List<String> parents = skillsFromResume.stream().filter(s -> 
			s.getAsStringList(VisEntitySkill.Fields.parent).contains(requiredSkillFromPosition))
			.map(s -> s.getAsString(VisEntitySkill.Fields.skill))
			.collect(Collectors.toList());
			
			boolean skillFoundByParentsInResume = parents.isEmpty() == false;
			
			if(skillFoundByParentsInResume) {
				CcpJsonRepresentation skill = CcpOtherConstants.EMPTY_JSON
						.put(VisEntitySkill.Fields.skill, requiredSkillFromPosition)
						.put(JsonFieldNames.type, ResumeSkillFoundType.PARENT)
						.put(JsonFieldNames.parents, parents)
						;
					response.add(skill);
				continue;
			}
			
			requiredSkillsMissingInResume.add(requiredSkillFromPosition);
		}
		
		
		boolean itIsMissingRequiredSkillInThisResume = requiredSkillsMissingInResume.isEmpty() == false;
		
		if(itIsMissingRequiredSkillInThisResume) {
			throw new VisErrorBusinessRequiredSkillsMissingInResume(requiredSkillsMissingInResume);
		}
	
		return response;
	}

	private static boolean resumeAlreadySeen(CcpJsonRepresentation positionByThisRecruiter, CcpSelectUnionAll searchResults, CcpJsonRepresentation searchParameters) {
	
		boolean doNotFilterResumesAlreadySeen = positionByThisRecruiter.getAsBoolean(JsonFieldNames.filterResumesAlreadySeen) == false;
		
		if(doNotFilterResumesAlreadySeen) {
			return false;
		}
		
		boolean thisResumeWasNeverSeenBefore = VisEntityResumeLastView.ENTITY.isPresentInThisUnionAll(searchResults, searchParameters) == false;
		
		if(thisResumeWasNeverSeenBefore) {
			return false;
		}
		
		CcpJsonRepresentation resumeLastView = VisEntityResumeLastView.ENTITY.getRequiredEntityRow(searchResults, searchParameters);
		
		CcpJsonRepresentation resume = VisEntityResume.ENTITY.getRequiredEntityRow(searchResults, resumeLastView);
		
		Long resumeLastSeen = resumeLastView.getAsLongNumber(VisEntityResumeLastView.Fields.timestamp);

		Long resumeLastUpdate = resume.getAsLongNumber(VisEntityResume.Fields.timestamp);
		
		return resumeLastUpdate <= resumeLastSeen;
	}

	private static CcpJsonRepresentation getPositionWithSortedResumes(String positionId, CcpJsonRepresentation allPositionsWithFilteredResumes) {
		
		CcpJsonRepresentation positionWithResumes = allPositionsWithFilteredResumes.getDynamicVersion().getInnerJson(positionId);
		
		List<CcpJsonRepresentation> resumes = positionWithResumes.getAsJsonList(JsonFieldNames.resumes);
		
		boolean singleResume = resumes.size() <= 1;
		
		if(singleResume) {
			return positionWithResumes;
		}
		CcpJsonRepresentation position = positionWithResumes.getInnerJson(VisEntityResumeLastView.Fields.position);
		VisSorterResumesByPosition positionResumesSort = new VisSorterResumesByPosition(position);
		resumes.sort(positionResumesSort);
		CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON.putAll(positionWithResumes).put(JsonFieldNames.resumes, resumes);
		return put;
	}
	
	private static List<CcpJsonRepresentation> getAllSearchParameters(
			CcpJsonRepresentation allPositionsGroupedByRecruiters, List<CcpJsonRepresentation> resumes, VisFrequencyOptions frequency) {
		
		boolean positionsNotFound = allPositionsGroupedByRecruiters.isEmpty();

		if(positionsNotFound) {
			return new ArrayList<>();
		}
		
		List<CcpJsonRepresentation> allSearchParameters = new ArrayList<>();
		
		Set<String> recruiters = allPositionsGroupedByRecruiters.fieldSet();
		for (String recruiter : recruiters) {
			for (CcpJsonRepresentation resume : resumes) {

				String email = resume.getAsString(VisEntityResume.Fields.email);
				
				CcpJsonRepresentation searchParameters = CcpOtherConstants.EMPTY_JSON
						.put(VisEntityResumePerception.Fields.recruiter, recruiter)
						.put(VisEntityPosition.Fields.frequency, frequency)
						.put(JsonFieldNames.owner, recruiter)
						.put(VisEntityResume.Fields.email, email)
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
			CcpJsonFieldName masterField, 
			CcpJsonFieldName ascField) {
		//1
		List<String> masters = json.getAsStringList(JsonFieldNames.masters);
		
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
		
		VisGroupDetailsByMasters detailsGroupedByMasters = new VisGroupDetailsByMasters(masterField.name(), entityToRead, entityWhereGroup);
		
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
				CcpJsonRepresentation put = resume.put(JsonFieldNames.index, index);
				page.add(put);
			}
			CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON
					.put(VisEntityGroupResumesByPosition.Fields.detail, page)
					.put(VisEntityGroupResumesByPosition.Fields.listSize, listSize)
					.put(VisEntityGroupResumesByPosition.Fields.from, from)
					.putAll(primaryKeySupplier)
					;
			CcpBulkItem bulkItem = entity.getMainBulkItem(put, CcpEntityBulkOperationType.create);
			allPagesTogether.add(bulkItem);
		}
		return allPagesTogether;
	}



}
