package com.vis.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpCollectionDecorator;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.crud.CcpUnionAllExecutor;
import com.ccp.especifications.db.query.CcpQueryExecutor;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.mensageria.JnFunctionMensageriaSender;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnSystemProperties;
import com.vis.business.position.VisBusinessPositionResumesSend;
import com.vis.entities.VisEntityBalance;
import com.vis.entities.VisEntityDeniedViewToCompany;
import com.vis.entities.VisEntityGroupPositionsByRecruiter;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeLastView;
import com.vis.entities.VisEntityResumePerception;
import com.vis.entities.VisEntityScheduleSendingResumeFees;
import com.vis.entities.VisEntityVirtualHashGrouper;
import com.vis.status.VisProcessStatusResumeView;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import java.util.stream.Stream;
import com.ccp.especifications.db.query.CcpQuerySimplifiedQuery;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.especifications.db.query.CcpQuery;

/**
 * Classe central de utilitários do módulo VIS. Concentra a lógica de alto nível do processo de matching
 * entre currículos e vagas: filtragem, ordenação, cálculo de hashes de compatibilidade, agrupamentos,
 * paginação e envio via mensageria. É a "cola" que conecta todos os componentes do fluxo de envio de
 * currículos para recrutadores.
 */
public class VisUtils {
	enum JsonFieldNames implements CcpJsonFieldName{
		tenant, resumes, statis, resumeOpinion, resumeLastView, requiredSkills, type, synonyms, parents, filterResumesAlreadySeen, owner, masters, index
	}
	
	public static String getTenant() {
		String tenant =  JnSystemProperties.INSTANCE.getSystemInnerProperty(JsonFieldNames.tenant);
		return tenant;
	}
	public static boolean isInsufficientFunds(int itemsCount,  
			CcpJsonRepresentation fee, CcpJsonRepresentation balance) {
	
		Double feeValue = fee.getAsDoubleNumber(VisJsonCommonsFields.fee);
		
		Double balanceValue = balance.getAsDoubleNumber(VisEntityBalance.Fields.balance);
		
		Double totalCostToThisRecruiter = feeValue * itemsCount;
		
		boolean insuficientFunds = balanceValue <= totalCostToThisRecruiter;
		
		return insuficientFunds;
	}

	
	public static List<CcpJsonRepresentation> sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(VisFrequencyOptions frequency, Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> howToObtainResumes, Function<VisFrequencyOptions, CcpJsonRepresentation> howToObtainPositionsGroupedByRecruiters) {
	
		CcpJsonRepresentation schedullingPlan = CcpOtherConstants.EMPTY_JSON.put(VisEntityPosition.Fields.frequency, frequency);
		List<CcpJsonRepresentation> sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter = sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(schedullingPlan, howToObtainResumes, howToObtainPositionsGroupedByRecruiters);
		return sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter;
	}
	
	public static List<CcpJsonRepresentation> sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(CcpJsonRepresentation schedullingPlan, Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> howToObtainResumes, Function<VisFrequencyOptions, CcpJsonRepresentation> howToObtainPositionsGroupedByRecruiters) {
		
		String frequency = schedullingPlan.getAsString(VisEntityPosition.Fields.frequency);
		
		VisFrequencyOptions valueOf = VisFrequencyOptions.valueOf(frequency);

		CcpJsonRepresentation allPositionsGroupedByRecruiters = howToObtainPositionsGroupedByRecruiters.apply(valueOf);

		List<CcpJsonRepresentation> resumes = howToObtainResumes.apply(schedullingPlan);

		List<CcpJsonRepresentation> allPositionsWithFilteredResumesAndTheirStatis = VisUtils.getAllPositionsWithFilteredAndSortedResumesAndTheirStatis(allPositionsGroupedByRecruiters, resumes, valueOf);
		Stream<CcpJsonRepresentation> stream = allPositionsWithFilteredResumesAndTheirStatis.stream();
		var streamMap = stream.map(positionsWithFilteredResumes -> getStatisToThisPosition(positionsWithFilteredResumes));

		List<CcpJsonRepresentation> allPositionsWithFilteredAndSortedResumesAndStatis = streamMap.collect(Collectors.toList());
		
		JnFunctionMensageriaSender mensageria = new JnFunctionMensageriaSender(VisBusinessPositionResumesSend.INSTANCE);
		
		mensageria.sendToMensageria(allPositionsWithFilteredAndSortedResumesAndStatis);
		
		return allPositionsWithFilteredAndSortedResumesAndStatis;
	}

	private static CcpJsonRepresentation getStatisToThisPosition(CcpJsonRepresentation positionsWithFilteredResumes) {

		List<CcpJsonRepresentation> resumes = positionsWithFilteredResumes.getAsJsonList(JsonFieldNames.resumes);
		String disponibilityName = VisJsonCommonsFields.disponibility.name();
		String experienceName = VisJsonCommonsFields.experience.name();
		String btcName = VisJsonCommonsFields.btc.name();
		String cltName = VisJsonCommonsFields.clt.name();
		String pjName = VisJsonCommonsFields.pj.name();
		List<String> fields = Arrays.asList(
				disponibilityName,
				experienceName,
				btcName,
				cltName,
				pjName
				);
		
		for (String field : fields) {
			int total = 0;
			double sum = 0;
			for (CcpJsonRepresentation resume : resumes) {
				CcpFieldName ccpFieldName = new CcpFieldName(field);
				boolean containsAllFields = resume.containsAllFields(ccpFieldName);
				boolean fieldIsMissing = false == containsAllFields;
				if(fieldIsMissing) {
					continue;
				}
				CcpFieldName ccpFieldName2 = new CcpFieldName(field);
				Double asDoubleNumber = resume.getAsDoubleNumber(ccpFieldName2);
				sum += asDoubleNumber;
				total++;
			}	
			
			boolean hasAtLeastOneResume = total > 0;
		
			if(hasAtLeastOneResume) {
				double avg = sum / total;
				CcpFieldName ccpFieldName3 = new CcpFieldName(field);
				positionsWithFilteredResumes = positionsWithFilteredResumes.addToItem(JsonFieldNames.statis, ccpFieldName3, avg);
			}
		}
		int resumesSize = resumes.size();
		positionsWithFilteredResumes = positionsWithFilteredResumes.addToItem(JsonFieldNames.statis, JsonFieldNames.resumes, resumesSize);
		return positionsWithFilteredResumes;
	}
	
	private static List<String> getHashes(CcpJsonRepresentation json) {
		boolean containsField = json.containsField(VisJsonCommonsFields.experience);

		String enumsType = containsField 
				? VisEntityResumeLastView.Fields.resume.name() : VisEntityResumeLastView.Fields.position.name();
				VisFunctionsGetDisponibilityValuesFromJson valueOf2 = VisFunctionsGetDisponibilityValuesFromJson.valueOf(enumsType);
				List<Integer> disponibilities = json.extractInformationFromJson(valueOf2);

		List<CcpJsonRepresentation> moneyValues = getMoneyValues(enumsType, json);
		VisFunctionsGetSeniorityValueFromJson valueOf3 = VisFunctionsGetSeniorityValueFromJson.valueOf(enumsType);

		String seniority = json.extractInformationFromJson(valueOf3);
		VisFunctionsGetPcdValuesFromJson valueOf4 = VisFunctionsGetPcdValuesFromJson.valueOf(enumsType);

		List<Boolean> pcds = json.extractInformationFromJson(valueOf4);;

		List<String> hashes = new ArrayList<>();
		// Todas as futuras possibilidades são gravadas em uma Lista
		for (Boolean pcd : pcds) {
			for (Integer disponibility : disponibilities) {// 5 (vaga) = [5, 4, 3, 2, 1, 0] || 6 (candidato) [6, 7, 8, 9
				for (CcpJsonRepresentation moneyValue : moneyValues) {
					CcpJsonRepresentation put2 = CcpOtherConstants.EMPTY_JSON.put(VisJsonCommonsFields.disponibility, disponibility);
					CcpJsonRepresentation put3 = put2
								.put(VisJsonCommonsFields.seniority, seniority);
								CcpJsonRepresentation mergeWithAnotherJson = put3.mergeWithAnotherJson(moneyValue);
								CcpJsonRepresentation hash = mergeWithAnotherJson
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
		String btcName2 = VisJsonCommonsFields.btc.name();

		List<CcpJsonRepresentation> btcValues = valueOf.apply(json,  btcName2);
		String cltName2 = VisJsonCommonsFields.clt.name();
		List<CcpJsonRepresentation> cltValues = valueOf.apply(json, cltName2);
		String pjName2 = VisJsonCommonsFields.pj.name();
		List<CcpJsonRepresentation> pjValues = valueOf.apply(json,  pjName2);

		result.addAll(btcValues);
		result.addAll(cltValues);
		result.addAll(pjValues);
		
		return result;
	}

	public static List<CcpJsonRepresentation> getLastUpdated(CcpEntity entity, VisFrequencyOptions valueOf, String filterFieldName) {
		
		CcpQueryExecutor queryExecutor = CcpDependencyInjection.getDependency(CcpQueryExecutor.class);
		CcpQuerySimplifiedQuery startSimplifiedQuery = CcpQueryOptions.INSTANCE
					.startSimplifiedQuery();
					var startRange = startSimplifiedQuery
						.startRange();
						var startFieldRange = startRange
							.startFieldRange(filterFieldName);
							long currentTimeMillis = System.currentTimeMillis();
							double hoursVezes = valueOf.hours * 3_600_000;
							double currentTimeMillisMenos = currentTimeMillis - hoursVezes;
							var greaterThan = startFieldRange
								.greaterThan(currentTimeMillisMenos);
								var endFieldRangeAndBackToRange = greaterThan
								.endFieldRangeAndBackToRange();
								var endRangeAndBackToSimplifiedQuery = endFieldRangeAndBackToRange
								.endRangeAndBackToSimplifiedQuery();

		CcpQueryOptions queryToSearchLastUpdated = 
				endRangeAndBackToSimplifiedQuery
					.endSimplifiedQueryAndBackToRequest()
				;
				CcpEntityMetaData entityMetaData = entity.getEntityMetaData();
				String[] resourcesNames = entityMetaData.getEntitiesToSelect();

		List<CcpJsonRepresentation> result = queryExecutor.getResultAsList(queryToSearchLastUpdated, resourcesNames);
		
		return result;
	}

	
	public static CcpJsonRepresentation getAllPositionsGroupedByRecruiters(VisFrequencyOptions frequency) {

		CcpQueryExecutor queryExecutor = CcpDependencyInjection.getDependency(CcpQueryExecutor.class);
		CcpQuerySimplifiedQuery startSimplifiedQuery2 = CcpQueryOptions.INSTANCE
					.startSimplifiedQuery();
					var match = startSimplifiedQuery2
						.match(VisEntityPosition.Fields.frequency, frequency);

		CcpQueryOptions queryToSearchLastUpdatedResumes = 
				match
					.endSimplifiedQueryAndBackToRequest()
				;
				CcpEntityMetaData entityMetaData2 = VisEntityPosition.ENTITY.getEntityMetaData();
				String[] resourcesNames = entityMetaData2.getEntitiesToSelect();
				String emailName = VisJsonCommonsFields.email.name();
				CcpJsonRepresentation positionsGroupedByRecruiters = queryExecutor.getMap(queryToSearchLastUpdatedResumes, resourcesNames, emailName);
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
		CcpEntity twinEntity = VisEntityResumePerception.ENTITY.getTwinEntity();
		CcpSelectUnionAll searchResults = unionAllExecutor.unionAll(
				allSearchParameters
				,VisEntityResume.ENTITY
				,VisEntityBalance.ENTITY
				,VisEntityResumePerception.ENTITY
				,VisEntityResumeLastView.ENTITY
				,VisEntityDeniedViewToCompany.ENTITY
				,VisEntityScheduleSendingResumeFees.ENTITY
				,
				twinEntity);
		
		CcpJsonRepresentation allPositionsWithFilteredResumes = CcpOtherConstants.EMPTY_JSON;
		
		List<CcpBulkItem> errors = new ArrayList<>();
		
		for (CcpJsonRepresentation searchParameters : allSearchParameters) {
			boolean presentInThisUnionAll = VisEntityScheduleSendingResumeFees.ENTITY.isPresentInThisUnionAll(searchResults, searchParameters);

			boolean feeNotFound = false == presentInThisUnionAll;

			if(feeNotFound) {
				String frequencyName = frequency.name();
				VisErrorBusinessMissingFeeToFrequency visErrorBusinessMissingFeeToFrequency = new VisErrorBusinessMissingFeeToFrequency(frequencyName);
				throw visErrorBusinessMissingFeeToFrequency;
			}
			boolean presentInThisUnionAll2 = VisEntityBalance.ENTITY.isPresentInThisUnionAll(searchResults, searchParameters);

			boolean balanceNotFound = false == presentInThisUnionAll2;

			if(balanceNotFound) {
				CcpBulkItem error = VisProcessStatusResumeView.missingBalance.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}

			Supplier<CcpJsonRepresentation> jsonSupplier = searchParameters.getJsonSupplier();
			
			CcpJsonRepresentation fee = VisEntityScheduleSendingResumeFees.ENTITY.getRecordFromUnionAll(searchResults, jsonSupplier);
			
			CcpJsonRepresentation balance = VisEntityBalance.ENTITY.getRecordFromUnionAll(searchResults, jsonSupplier);
			
			String recruiter = searchParameters.getAsString(VisJsonCommonsFields.recruiter);
			CcpFieldName ccpFieldName4 = new CcpFieldName(recruiter);
			List<CcpJsonRepresentation> positionsGroupedByThisRecruiter = allPositionsGroupedByRecruiters.getAsJsonList(ccpFieldName4);
			int countPositionsGroupedByThisRecruiter = positionsGroupedByThisRecruiter.size();
			
			boolean insuficientFunds = VisUtils.isInsufficientFunds(countPositionsGroupedByThisRecruiter, fee, balance);
			
			if(insuficientFunds) {
				CcpBulkItem error = VisProcessStatusResumeView.insufficientFunds.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}
			CcpEntity twinEntity2 = VisEntityResume.ENTITY.getTwinEntity();

			boolean inactiveResume = twinEntity2.isPresentInThisUnionAll(searchResults, searchParameters);
			
			if(inactiveResume) {
				CcpBulkItem error = VisProcessStatusResumeView.inactiveResume.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}
			boolean presentInThisUnionAll3 = VisEntityResume.ENTITY.isPresentInThisUnionAll(searchResults, searchParameters);

			
			
			boolean resumeNotFound = false == presentInThisUnionAll3;
			
			if(resumeNotFound) {
				CcpBulkItem error = VisProcessStatusResumeView.resumeNotFound.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}
			CcpEntity twinEntity3 = VisEntityResumePerception.ENTITY.getTwinEntity();

			boolean negativetedResume = twinEntity3.isPresentInThisUnionAll(searchResults, searchParameters);
			
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
		
		JnExecuteBulkOperation.INSTANCE.executeBulk(errors, JnDeleteKeysFromCache.INSTANCE);
		
	 	CcpJsonRepresentation allPositionsWithFilteredResumesCopy = CcpOtherConstants.EMPTY_JSON.mergeWithAnotherJson(allPositionsWithFilteredResumes);
			Set<String> fieldSet = allPositionsWithFilteredResumes.fieldSet();
			Stream<String> stream2 = fieldSet.stream();
			var stream2Map = stream2.map(positionId -> getPositionWithSortedResumes(positionId, allPositionsWithFilteredResumesCopy) );

			List<CcpJsonRepresentation> positionsWithSortedResumes = stream2Map.collect(Collectors.toList());
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

			Supplier<CcpJsonRepresentation> jsonSupplier = searchParameters.getJsonSupplier();
			
			CcpJsonRepresentation resume = VisEntityResume.ENTITY.getRecordFromUnionAll(searchResults, jsonSupplier);
			String dddName = VisJsonCommonsFields.ddd.name();

			CcpCollectionDecorator dddsPosition = positionByThisRecruiter.getAsCollectionDecorator(dddName);
			String dddName2 = VisJsonCommonsFields.ddd.name();
			CcpCollectionDecorator dddsResume = resume.getAsCollectionDecorator(dddName2);
			boolean differentDdds = false == dddsResume.hasIntersect(dddsPosition.content);
			
			if(differentDdds) {
				continue;
			}
			
			List<String> positionHashes = getHashes(positionByThisRecruiter);
			List<String> resumeHashes = getHashes(resume);
			boolean containsAll = resumeHashes.containsAll(positionHashes);

			boolean resumeDoesNotMatch = false == containsAll;
		
			if(resumeDoesNotMatch) {
				continue;
			}
			
			List<CcpJsonRepresentation> requiredSkills;
			
			try {
				requiredSkills = getRequiredSkillsInThisResume(positionByThisRecruiter, resume);
			} catch (VisErrorBusinessRequiredSkillsMissingInResume e) {
				continue;
			}
			
			
			boolean resumeAlreadySeen = resumeAlreadySeen(positionByThisRecruiter, searchResults, searchParameters);
			
			if(resumeAlreadySeen) {
				continue;
			}
			String positionId = VisEntityPosition.ENTITY.calculateId(positionByThisRecruiter);
			CcpFieldName ccpFieldName5 = new CcpFieldName(positionId);

			CcpJsonRepresentation emailMessageValuesToSent = allPositionsWithFilteredResumes.getInnerJson(ccpFieldName5);

			CcpJsonRepresentation resumeLastView = VisEntityResumeLastView.ENTITY.getRecordFromUnionAll(searchResults, jsonSupplier);

			CcpJsonRepresentation resumeOpinion = VisEntityResumePerception.ENTITY.getRecordFromUnionAll(searchResults, jsonSupplier);
			CcpJsonRepresentation put4 = resume
					.put(JsonFieldNames.resumeOpinion, resumeOpinion);

					CcpJsonRepresentation resumeWithCommentAndVisualizationDetails = put4.put(JsonFieldNames.resumeLastView, resumeLastView);
					CcpJsonRepresentation addToList = emailMessageValuesToSent
					.addToList(JsonFieldNames.resumes, resumeWithCommentAndVisualizationDetails);
					CcpJsonRepresentation put5 = addToList
					.put(VisEntityResumeLastView.Fields.position, allPositionsGroupedByRecruiters);

					emailMessageValuesToSent = put5
					.put(JsonFieldNames.requiredSkills, requiredSkills)
					;
					CcpFieldName ccpFieldName6 = new CcpFieldName(positionId);

					allPositionsWithFilteredResumes = allPositionsWithFilteredResumes.put(ccpFieldName6, emailMessageValuesToSent);
		}
		return positionWithFilteredResumes;
	}

	private static List<CcpJsonRepresentation> getRequiredSkillsInThisResume(
			CcpJsonRepresentation positionByThisRecruiter, 
			CcpJsonRepresentation resume) {

		List<String> requiredSkillsFromPosition = positionByThisRecruiter.getAsStringList(VisEntityPosition.Fields.requiredSkill);
		
		List<CcpJsonRepresentation> skillsFromResume = resume.getAsJsonList(VisJsonCommonsFields.skill);
		List<String> requiredSkillsMissingInResume = new ArrayList<String>();
		List<CcpJsonRepresentation> response = new ArrayList<>();
		for (String requiredSkillFromPosition : requiredSkillsFromPosition) {
			Stream<CcpJsonRepresentation> stream3 = skillsFromResume.stream();
			var filter = stream3.filter(s -> s.getAsString(VisJsonCommonsFields.skill).equals(requiredSkillFromPosition));
			var findFirst = filter.findFirst();

			boolean skillDirectlyFoundInResume = findFirst.isPresent();
			
			if(skillDirectlyFoundInResume) {
				CcpJsonRepresentation put6 = CcpOtherConstants.EMPTY_JSON
					.put(JsonFieldNames.type, ResumeSkillFoundType.CONTAINED_IN_RESUME);
					CcpJsonRepresentation skill = put6
					.put(VisJsonCommonsFields.skill, requiredSkillFromPosition);
				response.add(skill);
				continue;
			}
			Stream<CcpJsonRepresentation> stream4 = skillsFromResume.stream();
			var filter2 = stream4.filter(s -> s.getAsStringList(JsonFieldNames.synonyms).contains(requiredSkillFromPosition));

			Optional<CcpJsonRepresentation> synonymFound = filter2.findFirst();
			boolean skillFoundBySynonymInResume = synonymFound.isPresent();
			
			if(skillFoundBySynonymInResume) {
				CcpJsonRepresentation synonym = synonymFound.get();
				String synonymName = synonym.getAsString(VisJsonCommonsFields.skill);
				CcpJsonRepresentation put7 = CcpOtherConstants.EMPTY_JSON
						.put(JsonFieldNames.type, ResumeSkillFoundType.SYNONYM);
						CcpJsonRepresentation put8 = put7
						.put(VisJsonCommonsFields.skill, requiredSkillFromPosition);
						CcpJsonRepresentation skill = put8
						.put(VisJsonCommonsFields.synonym, synonymName)
						;
					response.add(skill);
					continue;
			}
			Stream<CcpJsonRepresentation> stream5 = skillsFromResume.stream();
			var filter3 = stream5.filter(s -> 
			s.getAsStringList(VisJsonCommonsFields.parent).contains(requiredSkillFromPosition));
			var filter3Map = filter3
			.map(s -> s.getAsString(VisJsonCommonsFields.skill));
			List<String> parents = filter3Map
			.collect(Collectors.toList());
			boolean parentsEmpty = parents.isEmpty();

			boolean skillFoundByParentsInResume = false == parentsEmpty;
			
			if(skillFoundByParentsInResume) {
				CcpJsonRepresentation put9 = CcpOtherConstants.EMPTY_JSON
						.put(VisJsonCommonsFields.skill, requiredSkillFromPosition);
						CcpJsonRepresentation put10 = put9
						.put(JsonFieldNames.type, ResumeSkillFoundType.PARENT);
						CcpJsonRepresentation skill = put10
						.put(JsonFieldNames.parents, parents)
						;
					response.add(skill);
				continue;
			}
			
			requiredSkillsMissingInResume.add(requiredSkillFromPosition);
		}
		boolean requiredSkillsMissingInResumeEmpty = requiredSkillsMissingInResume.isEmpty();

		
		boolean itIsMissingRequiredSkillInThisResume = false == requiredSkillsMissingInResumeEmpty;
		
		if(itIsMissingRequiredSkillInThisResume) {
			VisErrorBusinessRequiredSkillsMissingInResume visErrorBusinessRequiredSkillsMissingInResume = new VisErrorBusinessRequiredSkillsMissingInResume(requiredSkillsMissingInResume);
			throw visErrorBusinessRequiredSkillsMissingInResume;
		}
	
		return response;
	}

	private static boolean resumeAlreadySeen(CcpJsonRepresentation positionByThisRecruiter, CcpSelectUnionAll searchResults, CcpJsonRepresentation searchParameters) {
		boolean asBoolean = positionByThisRecruiter.getAsBoolean(JsonFieldNames.filterResumesAlreadySeen);

		boolean doNotFilterResumesAlreadySeen = false == asBoolean;
		
		if(doNotFilterResumesAlreadySeen) {
			return false;
		}
		boolean presentInThisUnionAll4 = VisEntityResumeLastView.ENTITY.isPresentInThisUnionAll(searchResults, searchParameters);

		boolean thisResumeWasNeverSeenBefore = false == presentInThisUnionAll4;
		
		if(thisResumeWasNeverSeenBefore) {
			return false;
		}
		
		Supplier<CcpJsonRepresentation> jsonSupplier = searchParameters.getJsonSupplier();
		
		CcpJsonRepresentation resumeLastView =  VisEntityResumeLastView.ENTITY.getRecordFromUnionAll(searchResults, jsonSupplier);
		
		Supplier<CcpJsonRepresentation> jsonSupplier2 = resumeLastView.getJsonSupplier();
		
		CcpJsonRepresentation resume = VisEntityResume.ENTITY.getRecordFromUnionAll(searchResults, jsonSupplier2);
		
		Long resumeLastSeen = resumeLastView.getAsLongNumber(JnJsonCommonsFields.timestamp);

		Long resumeLastUpdate = resume.getAsLongNumber(JnJsonCommonsFields.timestamp);
		boolean resumeLastUpdateMenorOuIgual = resumeLastUpdate <= resumeLastSeen;

		return resumeLastUpdateMenorOuIgual;
	}

	private static CcpJsonRepresentation getPositionWithSortedResumes(String positionId, CcpJsonRepresentation allPositionsWithFilteredResumes) {
		CcpFieldName ccpFieldName7 = new CcpFieldName(positionId);
	
		CcpJsonRepresentation positionWithResumes = allPositionsWithFilteredResumes.getInnerJson(ccpFieldName7);
		
		List<CcpJsonRepresentation> resumes = positionWithResumes.getAsJsonList(JsonFieldNames.resumes);
		int resumesSize2 = resumes.size();

		boolean singleResume = resumesSize2 <= 1;
		
		if(singleResume) {
			return positionWithResumes;
		}
		CcpJsonRepresentation position = positionWithResumes.getInnerJson(VisEntityResumeLastView.Fields.position);
		VisSorterResumesByPosition positionResumesSort = new VisSorterResumesByPosition(position);
		resumes.sort(positionResumesSort);
		CcpJsonRepresentation mergeWithAnotherJson2 = CcpOtherConstants.EMPTY_JSON.mergeWithAnotherJson(positionWithResumes);
		CcpJsonRepresentation put = mergeWithAnotherJson2.put(JsonFieldNames.resumes, resumes);
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

				String email = resume.getAsString(VisJsonCommonsFields.email);
				CcpJsonRepresentation put11 = CcpOtherConstants.EMPTY_JSON
						.put(VisJsonCommonsFields.recruiter, recruiter);
						CcpJsonRepresentation put12 = put11
						.put(VisEntityPosition.Fields.frequency, frequency);
						CcpJsonRepresentation put13 = put12
						.put(JsonFieldNames.owner, recruiter);

						CcpJsonRepresentation searchParameters = put13
						.put(VisJsonCommonsFields.email, email)
						;
				allSearchParameters.add(searchParameters);
			}
		}
		return allSearchParameters;
	}
	
	


	
	public static CcpJsonRepresentation groupPositionsGroupedByRecruiters(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation groupDetailsByMasters = groupDetailsByMasters(json, VisEntityPosition.ENTITY, 
				VisEntityGroupPositionsByRecruiter.ENTITY, VisJsonCommonsFields.email, JnJsonCommonsFields.timestamp);
		
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
		CcpQuery startQuery = CcpQueryOptions.INSTANCE
				.startQuery();
				var startBool = startQuery
					.startBool();
					var startMust = startBool
						.startMust();
						var terms = startMust
							.terms(masterField, masters);
							var endMustAndBackToBool = terms
							.endMustAndBackToBool();
							var endBoolAndBackToQuery = endMustAndBackToBool
							.endBoolAndBackToQuery();
							var endQueryAndBackToRequest = endBoolAndBackToQuery
							.endQueryAndBackToRequest();
							String ascFieldName = ascField.name();

							CcpQueryOptions query = endQueryAndBackToRequest
							.addAscSorting(ascFieldName)
		;
		CcpQueryExecutor queryExecutor = CcpDependencyInjection.getDependency(CcpQueryExecutor.class);
		CcpEntityMetaData entityMetaData3 = entityToRead.getEntityMetaData();

		String[] entitiesToSelect = entityMetaData3.getEntitiesToSelect();
		String masterFieldName = masterField.name();

		VisGroupDetailsByMasters detailsGroupedByMasters = new VisGroupDetailsByMasters(masterFieldName, entityToRead, entityWhereGroup);
		
		queryExecutor.consumeQueryResult(query, entitiesToSelect, "10s", 10000, detailsGroupedByMasters);
		
		detailsGroupedByMasters.saveAllDetailsGroupedByMasters();
		
		return json;
	}
	
	
	
	public static void saveRecordsInPages(
			List<CcpJsonRepresentation> records,
			CcpJsonRepresentation primaryKeySupplier,
			CcpEntity entity) {

		List<CcpBulkItem> allPagesTogether = getRecordsInPages(records, primaryKeySupplier, entity);

		JnExecuteBulkOperation.INSTANCE.executeBulk(allPagesTogether, JnDeleteKeysFromCache.INSTANCE);
	}

	public static List<CcpBulkItem> getRecordsInPages(List<CcpJsonRepresentation> records,
			CcpJsonRepresentation primaryKeySupplier, CcpEntity entity) {
		List<CcpBulkItem> allPagesTogether = new ArrayList<>();
		int listSize = 10;
		int recordsSize = records.size();
		int recordsSizeResto = recordsSize  % listSize;
		int totalPages = recordsSizeResto + 1;
		int index = 0;

		for(int from = 0; from < totalPages; from++) {
			List<CcpJsonRepresentation> page = new ArrayList<>();
			for(;(index + 1) % listSize !=0 && index < records.size(); index++) {
				CcpJsonRepresentation resume = records.get(index);
				CcpJsonRepresentation put = resume.put(JsonFieldNames.index, index);
				page.add(put);
			}
			CcpJsonRepresentation put14 = CcpOtherConstants.EMPTY_JSON
					.put(VisJsonCommonsFields.detail, page);
					CcpJsonRepresentation put15 = put14
					.put(VisJsonCommonsFields.listSize, listSize);
					CcpJsonRepresentation put16 = put15
					.put(VisJsonCommonsFields.from, from);
					CcpJsonRepresentation put = put16
					.mergeWithAnotherJson(primaryKeySupplier)
					;
			var bulkItem = entity.toBulkItems(put, CcpBulkEntityOperationType.create);
			allPagesTogether.addAll(bulkItem);
		}
		return allPagesTogether;
	}

	@SuppressWarnings("serial")
	public static class VisErrorBusinessMissingFeeToFrequency extends RuntimeException {
		private VisErrorBusinessMissingFeeToFrequency(String frequency) {
			super("It is missing the fee of frequency " + frequency);
		}
	}

	@SuppressWarnings("serial")
	public static class VisErrorBusinessRequiredSkillsMissingInResume extends RuntimeException {
		public final List<String> requiredSkillsNotFoundInResume;
		private VisErrorBusinessRequiredSkillsMissingInResume(List<String> requiredSkillsNotFoundInResume) {
			this.requiredSkillsNotFoundInResume = requiredSkillsNotFoundInResume;
		}
	}



}
