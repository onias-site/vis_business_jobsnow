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

import com.ccp.json.fields.validation.CcpJsonCommonsFields;

/**
 * Core utility class of the VIS module. Holds the high-level logic of the matching process between
 * resumes and positions: filtering, sorting, compatibility hash calculation, groupings,
 * pagination and sending through the messaging system. It is the "glue" that connects every component of
 * the flow that sends resumes to recruiters.
 */
public class VisUtils {
	enum JsonFieldNames implements CcpJsonFieldName{
		tenant, statis, resumeOpinion, resumeLastView, requiredSkills, synonyms, parents, filterResumesAlreadySeen, owner, index
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
		List<CcpJsonRepresentation> positionsWithResumesAndStatis = sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(schedullingPlan, howToObtainResumes, howToObtainPositionsGroupedByRecruiters);
		return positionsWithResumesAndStatis;
	}
	
	public static List<CcpJsonRepresentation> sendFilteredAndSortedResumesAndTheirStatisByEachPositionToEachRecruiter(CcpJsonRepresentation schedullingPlan, Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> howToObtainResumes, Function<VisFrequencyOptions, CcpJsonRepresentation> howToObtainPositionsGroupedByRecruiters) {
		
		String frequency = schedullingPlan.getAsString(VisEntityPosition.Fields.frequency);
		
		VisFrequencyOptions frequencyOption = VisFrequencyOptions.valueOf(frequency);

		CcpJsonRepresentation allPositionsGroupedByRecruiters = howToObtainPositionsGroupedByRecruiters.apply(frequencyOption);

		List<CcpJsonRepresentation> resumes = howToObtainResumes.apply(schedullingPlan);

		List<CcpJsonRepresentation> allPositionsWithFilteredResumesAndTheirStatis = VisUtils.getAllPositionsWithFilteredAndSortedResumesAndTheirStatis(allPositionsGroupedByRecruiters, resumes, frequencyOption);
		Stream<CcpJsonRepresentation> positionsStream = allPositionsWithFilteredResumesAndTheirStatis.stream();
		var positionsWithStatisStream = positionsStream.map(positionsWithFilteredResumes -> getStatisToThisPosition(positionsWithFilteredResumes));

		List<CcpJsonRepresentation> allPositionsWithFilteredAndSortedResumesAndStatis = positionsWithStatisStream.collect(Collectors.toList());
		
		JnFunctionMensageriaSender mensageria = new JnFunctionMensageriaSender(VisBusinessPositionResumesSend.INSTANCE);
		
		mensageria.sendToMensageria(allPositionsWithFilteredAndSortedResumesAndStatis);
		
		return allPositionsWithFilteredAndSortedResumesAndStatis;
	}

	private static CcpJsonRepresentation getStatisToThisPosition(CcpJsonRepresentation positionsWithFilteredResumes) {

		List<CcpJsonRepresentation> resumes = positionsWithFilteredResumes.getAsJsonList(VisJsonCommonsFields.resumes);
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
				CcpFieldName fieldKey = new CcpFieldName(field);
				boolean resumeHasField = resume.containsAllFields(fieldKey);
				boolean fieldIsMissing = false == resumeHasField;
				if(fieldIsMissing) {
					continue;
				}
				CcpFieldName sameFieldKey = new CcpFieldName(field);
				Double fieldValue = resume.getAsDoubleNumber(sameFieldKey);
				sum += fieldValue;
				total++;
			}	
			
			boolean hasAtLeastOneResume = total > 0;
		
			if(hasAtLeastOneResume) {
				double avg = sum / total;
				CcpFieldName statisFieldKey = new CcpFieldName(field);
				positionsWithFilteredResumes = positionsWithFilteredResumes.addToItem(JsonFieldNames.statis, statisFieldKey, avg);
			}
		}
		int resumesSize = resumes.size();
		positionsWithFilteredResumes = positionsWithFilteredResumes.addToItem(JsonFieldNames.statis, VisJsonCommonsFields.resumes, resumesSize);
		return positionsWithFilteredResumes;
	}
	
	private static List<String> getHashes(CcpJsonRepresentation json) {
		boolean containsField = json.containsField(VisJsonCommonsFields.experience);

		String enumsType = containsField 
				? VisEntityResumeLastView.Fields.resume.name() : VisEntityResumeLastView.Fields.position.name();
				VisFunctionsGetDisponibilityValuesFromJson disponibilityGetter = VisFunctionsGetDisponibilityValuesFromJson.valueOf(enumsType);
				List<Integer> disponibilities = json.extractInformationFromJson(disponibilityGetter);

		List<CcpJsonRepresentation> moneyValues = getMoneyValues(enumsType, json);
		VisFunctionsGetSeniorityValueFromJson seniorityGetter = VisFunctionsGetSeniorityValueFromJson.valueOf(enumsType);

		String seniority = json.extractInformationFromJson(seniorityGetter);
		VisFunctionsGetPcdValuesFromJson pcdGetter = VisFunctionsGetPcdValuesFromJson.valueOf(enumsType);

		List<Boolean> pcds = json.extractInformationFromJson(pcdGetter);;

		List<String> hashes = new ArrayList<>();
		// Every future possibility is stored in a List
		for (Boolean pcd : pcds) {
			for (Integer disponibility : disponibilities) {// 5 (position) = [5, 4, 3, 2, 1, 0] || 6 (candidate) [6, 7, 8, 9
				for (CcpJsonRepresentation moneyValue : moneyValues) {
					CcpJsonRepresentation jsonWithDisponibility = CcpOtherConstants.EMPTY_JSON.put(VisJsonCommonsFields.disponibility, disponibility);
					CcpJsonRepresentation jsonWithSeniority = jsonWithDisponibility
								.put(VisJsonCommonsFields.seniority, seniority);
								CcpJsonRepresentation jsonWithMoneyValue = jsonWithSeniority.mergeWithAnotherJson(moneyValue);
								CcpJsonRepresentation hash = jsonWithMoneyValue
								.put(VisEntityPosition.Fields.pcd, pcd);
						//LATER REMOVE THE NEED TO CREATE THIS TABLE, AND ALSO REMOVE THE VIRTUALENTITY
						String hashValue = VisEntityVirtualHashGrouper.ENTITY.calculateId(hash);
						hashes.add(hashValue);
					}
			}
		}
		return hashes;
	}
	
	private static List<CcpJsonRepresentation> getMoneyValues(String enumsType, CcpJsonRepresentation json){
		
		ArrayList<CcpJsonRepresentation> result = new ArrayList<>();
		
		GetMoneyValuesFromJson moneyValuesGetter = GetMoneyValuesFromJson.valueOf(enumsType);
		String btcFieldName = VisJsonCommonsFields.btc.name();

		List<CcpJsonRepresentation> btcValues = moneyValuesGetter.apply(json,  btcFieldName);
		String cltFieldName = VisJsonCommonsFields.clt.name();
		List<CcpJsonRepresentation> cltValues = moneyValuesGetter.apply(json, cltFieldName);
		String pjFieldName = VisJsonCommonsFields.pj.name();
		List<CcpJsonRepresentation> pjValues = moneyValuesGetter.apply(json,  pjFieldName);

		result.addAll(btcValues);
		result.addAll(cltValues);
		result.addAll(pjValues);
		
		return result;
	}

	public static List<CcpJsonRepresentation> getLastUpdated(CcpEntity entity, VisFrequencyOptions frequencyOption, String filterFieldName) {
		
		CcpQueryExecutor queryExecutor = CcpDependencyInjection.getDependency(CcpQueryExecutor.class);
		CcpQuerySimplifiedQuery startSimplifiedQuery = CcpQueryOptions.INSTANCE
					.startSimplifiedQuery();
					var startRange = startSimplifiedQuery
						.startRange();
						var startFieldRange = startRange
							.startFieldRange(filterFieldName);
							long currentTimeMillis = System.currentTimeMillis();
							double periodInMillis = frequencyOption.hours * 3_600_000;
							double periodStartInMillis = currentTimeMillis - periodInMillis;
							var greaterThan = startFieldRange
								.greaterThan(periodStartInMillis);
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
		CcpQuerySimplifiedQuery startSimplifiedQuery = CcpQueryOptions.INSTANCE
					.startSimplifiedQuery();
					var match = startSimplifiedQuery
						.match(VisEntityPosition.Fields.frequency, frequency);

		CcpQueryOptions queryToSearchPositionsByFrequency = 
				match
					.endSimplifiedQueryAndBackToRequest()
				;
				CcpEntityMetaData positionMetaData = VisEntityPosition.ENTITY.getEntityMetaData();
				String[] resourcesNames = positionMetaData.getEntitiesToSelect();
				String emailName = VisJsonCommonsFields.email.name();
				CcpJsonRepresentation positionsGroupedByRecruiters = queryExecutor.getMap(queryToSearchPositionsByFrequency, resourcesNames, emailName);
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
		CcpEntity resumePerceptionTwinEntity = VisEntityResumePerception.ENTITY.getTwinEntity();
		CcpSelectUnionAll searchResults = unionAllExecutor.unionAll(
				allSearchParameters
				,VisEntityResume.ENTITY
				,VisEntityBalance.ENTITY
				,VisEntityResumePerception.ENTITY
				,VisEntityResumeLastView.ENTITY
				,VisEntityDeniedViewToCompany.ENTITY
				,VisEntityScheduleSendingResumeFees.ENTITY
				,
				resumePerceptionTwinEntity);
		
		CcpJsonRepresentation allPositionsWithFilteredResumes = CcpOtherConstants.EMPTY_JSON;
		
		List<CcpBulkItem> errors = new ArrayList<>();
		
		for (CcpJsonRepresentation searchParameters : allSearchParameters) {
			boolean feeFound = VisEntityScheduleSendingResumeFees.ENTITY.isPresentInThisUnionAll(searchResults, searchParameters);

			boolean feeNotFound = false == feeFound;

			if(feeNotFound) {
				String frequencyName = frequency.name();
				VisErrorBusinessMissingFeeToFrequency missingFeeError = new VisErrorBusinessMissingFeeToFrequency(frequencyName);
				throw missingFeeError;
			}
			boolean balanceFound = VisEntityBalance.ENTITY.isPresentInThisUnionAll(searchResults, searchParameters);

			boolean balanceNotFound = false == balanceFound;

			if(balanceNotFound) {
				CcpBulkItem error = VisProcessStatusResumeView.missingBalance.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}

			Supplier<CcpJsonRepresentation> jsonSupplier = searchParameters.getJsonSupplier();
			
			CcpJsonRepresentation fee = VisEntityScheduleSendingResumeFees.ENTITY.getRecordFromUnionAll(searchResults, jsonSupplier);
			
			CcpJsonRepresentation balance = VisEntityBalance.ENTITY.getRecordFromUnionAll(searchResults, jsonSupplier);
			
			String recruiter = searchParameters.getAsString(VisJsonCommonsFields.recruiter);
			CcpFieldName recruiterKey = new CcpFieldName(recruiter);
			List<CcpJsonRepresentation> positionsGroupedByThisRecruiter = allPositionsGroupedByRecruiters.getAsJsonList(recruiterKey);
			int countPositionsGroupedByThisRecruiter = positionsGroupedByThisRecruiter.size();
			
			boolean insuficientFunds = VisUtils.isInsufficientFunds(countPositionsGroupedByThisRecruiter, fee, balance);
			
			if(insuficientFunds) {
				CcpBulkItem error = VisProcessStatusResumeView.insufficientFunds.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}
			CcpEntity inactiveResumesEntity = VisEntityResume.ENTITY.getTwinEntity();

			boolean inactiveResume = inactiveResumesEntity.isPresentInThisUnionAll(searchResults, searchParameters);
			
			if(inactiveResume) {
				CcpBulkItem error = VisProcessStatusResumeView.inactiveResume.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}
			boolean resumeFound = VisEntityResume.ENTITY.isPresentInThisUnionAll(searchResults, searchParameters);

			
			
			boolean resumeNotFound = false == resumeFound;
			
			if(resumeNotFound) {
				CcpBulkItem error = VisProcessStatusResumeView.resumeNotFound.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}
			CcpEntity negativatedResumesEntity = VisEntityResumePerception.ENTITY.getTwinEntity();

			boolean negativetedResume = negativatedResumesEntity.isPresentInThisUnionAll(searchResults, searchParameters);
			
			if(negativetedResume) {
				CcpBulkItem error = VisProcessStatusResumeView.negativatedResume.toBulkItemCreate(searchParameters);	
				errors.add(error);
				continue;
			}
				/*
				 * IT -> backend -> java -> spring -> springboot
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
			Set<String> positionIds = allPositionsWithFilteredResumes.fieldSet();
			Stream<String> positionIdsStream = positionIds.stream();
			var positionsWithSortedResumesStream = positionIdsStream.map(positionId -> getPositionWithSortedResumes(positionId, allPositionsWithFilteredResumesCopy) );

			List<CcpJsonRepresentation> positionsWithSortedResumes = positionsWithSortedResumesStream.collect(Collectors.toList());
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
			String dddFieldName = VisJsonCommonsFields.ddd.name();

			CcpCollectionDecorator dddsPosition = positionByThisRecruiter.getAsCollectionDecorator(dddFieldName);
			String sameDddFieldName = VisJsonCommonsFields.ddd.name();
			CcpCollectionDecorator dddsResume = resume.getAsCollectionDecorator(sameDddFieldName);
			boolean differentDdds = false == dddsResume.hasIntersect(dddsPosition.content);
			
			if(differentDdds) {
				continue;
			}
			
			List<String> positionHashes = getHashes(positionByThisRecruiter);
			List<String> resumeHashes = getHashes(resume);
			boolean resumeHasAllPositionHashes = resumeHashes.containsAll(positionHashes);

			boolean resumeDoesNotMatch = false == resumeHasAllPositionHashes;
		
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
			CcpFieldName positionKey = new CcpFieldName(positionId);

			CcpJsonRepresentation emailMessageValuesToSent = allPositionsWithFilteredResumes.getInnerJson(positionKey);

			CcpJsonRepresentation resumeLastView = VisEntityResumeLastView.ENTITY.getRecordFromUnionAll(searchResults, jsonSupplier);

			CcpJsonRepresentation resumeOpinion = VisEntityResumePerception.ENTITY.getRecordFromUnionAll(searchResults, jsonSupplier);
			CcpJsonRepresentation resumeWithOpinion = resume
					.put(JsonFieldNames.resumeOpinion, resumeOpinion);

					CcpJsonRepresentation resumeWithCommentAndVisualizationDetails = resumeWithOpinion.put(JsonFieldNames.resumeLastView, resumeLastView);
					CcpJsonRepresentation messageValuesWithResume = emailMessageValuesToSent
					.addToList(VisJsonCommonsFields.resumes, resumeWithCommentAndVisualizationDetails);
					CcpJsonRepresentation messageValuesWithPosition = messageValuesWithResume
					.put(VisEntityResumeLastView.Fields.position, allPositionsGroupedByRecruiters);

					emailMessageValuesToSent = messageValuesWithPosition
					.put(JsonFieldNames.requiredSkills, requiredSkills)
					;
					CcpFieldName samePositionKey = new CcpFieldName(positionId);

					allPositionsWithFilteredResumes = allPositionsWithFilteredResumes.put(samePositionKey, emailMessageValuesToSent);
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
			Stream<CcpJsonRepresentation> resumeSkillsStream = skillsFromResume.stream();
			var skillsWithSameName = resumeSkillsStream.filter(s -> s.getAsString(VisJsonCommonsFields.skill).equals(requiredSkillFromPosition));
			var skillWithSameName = skillsWithSameName.findFirst();

			boolean skillDirectlyFoundInResume = skillWithSameName.isPresent();
			
			if(skillDirectlyFoundInResume) {
				CcpJsonRepresentation jsonWithContainedType = CcpOtherConstants.EMPTY_JSON
					.put(CcpJsonCommonsFields.type, ResumeSkillFoundType.CONTAINED_IN_RESUME);
					CcpJsonRepresentation skill = jsonWithContainedType
					.put(VisJsonCommonsFields.skill, requiredSkillFromPosition);
				response.add(skill);
				continue;
			}
			Stream<CcpJsonRepresentation> resumeSkillsForSynonymsStream = skillsFromResume.stream();
			var skillsWithThisSynonym = resumeSkillsForSynonymsStream.filter(s -> s.getAsStringList(JsonFieldNames.synonyms).contains(requiredSkillFromPosition));

			Optional<CcpJsonRepresentation> synonymFound = skillsWithThisSynonym.findFirst();
			boolean skillFoundBySynonymInResume = synonymFound.isPresent();
			
			if(skillFoundBySynonymInResume) {
				CcpJsonRepresentation synonym = synonymFound.get();
				String synonymName = synonym.getAsString(VisJsonCommonsFields.skill);
				CcpJsonRepresentation jsonWithSynonymType = CcpOtherConstants.EMPTY_JSON
						.put(CcpJsonCommonsFields.type, ResumeSkillFoundType.SYNONYM);
						CcpJsonRepresentation jsonWithSynonymTypeAndSkill = jsonWithSynonymType
						.put(VisJsonCommonsFields.skill, requiredSkillFromPosition);
						CcpJsonRepresentation skill = jsonWithSynonymTypeAndSkill
						.put(VisJsonCommonsFields.synonym, synonymName)
						;
					response.add(skill);
					continue;
			}
			Stream<CcpJsonRepresentation> resumeSkillsForParentsStream = skillsFromResume.stream();
			var skillsWithThisParent = resumeSkillsForParentsStream.filter(s -> 
			s.getAsStringList(VisJsonCommonsFields.parent).contains(requiredSkillFromPosition));
			var namesOfSkillsWithThisParent = skillsWithThisParent
			.map(s -> s.getAsString(VisJsonCommonsFields.skill));
			List<String> parents = namesOfSkillsWithThisParent
			.collect(Collectors.toList());
			boolean parentsEmpty = parents.isEmpty();

			boolean skillFoundByParentsInResume = false == parentsEmpty;
			
			if(skillFoundByParentsInResume) {
				CcpJsonRepresentation jsonWithSkill = CcpOtherConstants.EMPTY_JSON
						.put(VisJsonCommonsFields.skill, requiredSkillFromPosition);
						CcpJsonRepresentation jsonWithSkillAndParentType = jsonWithSkill
						.put(CcpJsonCommonsFields.type, ResumeSkillFoundType.PARENT);
						CcpJsonRepresentation skill = jsonWithSkillAndParentType
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
			VisErrorBusinessRequiredSkillsMissingInResume missingSkillsError = new VisErrorBusinessRequiredSkillsMissingInResume(requiredSkillsMissingInResume);
			throw missingSkillsError;
		}
	
		return response;
	}

	private static boolean resumeAlreadySeen(CcpJsonRepresentation positionByThisRecruiter, CcpSelectUnionAll searchResults, CcpJsonRepresentation searchParameters) {
		boolean mustFilterResumesAlreadySeen = positionByThisRecruiter.getAsBoolean(JsonFieldNames.filterResumesAlreadySeen);

		boolean doNotFilterResumesAlreadySeen = false == mustFilterResumesAlreadySeen;
		
		if(doNotFilterResumesAlreadySeen) {
			return false;
		}
		boolean resumeWasSeenBefore = VisEntityResumeLastView.ENTITY.isPresentInThisUnionAll(searchResults, searchParameters);

		boolean thisResumeWasNeverSeenBefore = false == resumeWasSeenBefore;
		
		if(thisResumeWasNeverSeenBefore) {
			return false;
		}
		
		Supplier<CcpJsonRepresentation> jsonSupplier = searchParameters.getJsonSupplier();
		
		CcpJsonRepresentation resumeLastView =  VisEntityResumeLastView.ENTITY.getRecordFromUnionAll(searchResults, jsonSupplier);
		
		Supplier<CcpJsonRepresentation> resumeLastViewSupplier = resumeLastView.getJsonSupplier();
		
		CcpJsonRepresentation resume = VisEntityResume.ENTITY.getRecordFromUnionAll(searchResults, resumeLastViewSupplier);
		
		Long resumeLastSeen = resumeLastView.getAsLongNumber(JnJsonCommonsFields.timestamp);

		Long resumeLastUpdate = resume.getAsLongNumber(JnJsonCommonsFields.timestamp);
		boolean resumeNotUpdatedSinceLastView = resumeLastUpdate <= resumeLastSeen;

		return resumeNotUpdatedSinceLastView;
	}

	private static CcpJsonRepresentation getPositionWithSortedResumes(String positionId, CcpJsonRepresentation allPositionsWithFilteredResumes) {
		CcpFieldName positionKey = new CcpFieldName(positionId);
	
		CcpJsonRepresentation positionWithResumes = allPositionsWithFilteredResumes.getInnerJson(positionKey);
		
		List<CcpJsonRepresentation> resumes = positionWithResumes.getAsJsonList(VisJsonCommonsFields.resumes);
		int resumesCount = resumes.size();

		boolean singleResume = resumesCount <= 1;
		
		if(singleResume) {
			return positionWithResumes;
		}
		CcpJsonRepresentation position = positionWithResumes.getInnerJson(VisEntityResumeLastView.Fields.position);
		VisSorterResumesByPosition positionResumesSort = new VisSorterResumesByPosition(position);
		resumes.sort(positionResumesSort);
		CcpJsonRepresentation positionWithResumesCopy = CcpOtherConstants.EMPTY_JSON.mergeWithAnotherJson(positionWithResumes);
		CcpJsonRepresentation positionWithSortedResumes = positionWithResumesCopy.put(VisJsonCommonsFields.resumes, resumes);
		return positionWithSortedResumes;
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
				CcpJsonRepresentation jsonWithRecruiter = CcpOtherConstants.EMPTY_JSON
						.put(VisJsonCommonsFields.recruiter, recruiter);
						CcpJsonRepresentation jsonWithFrequency = jsonWithRecruiter
						.put(VisEntityPosition.Fields.frequency, frequency);
						CcpJsonRepresentation jsonWithOwner = jsonWithFrequency
						.put(JsonFieldNames.owner, recruiter);

						CcpJsonRepresentation searchParameters = jsonWithOwner
						.put(VisJsonCommonsFields.email, email)
						;
				allSearchParameters.add(searchParameters);
			}
		}
		return allSearchParameters;
	}
	
	


	
	public static CcpJsonRepresentation groupPositionsGroupedByRecruiters(CcpJsonRepresentation json) {
		
		CcpJsonRepresentation groupingResult = groupDetailsByMasters(json, VisEntityPosition.ENTITY, 
				VisEntityGroupPositionsByRecruiter.ENTITY, VisJsonCommonsFields.email, JnJsonCommonsFields.timestamp);
		
		return groupingResult;
	}
	
	public static CcpJsonRepresentation groupDetailsByMasters(
			CcpJsonRepresentation json, 
			CcpEntity entityToRead, 
			CcpEntity entityWhereGroup, 
			CcpJsonFieldName masterField, 
			CcpJsonFieldName ascField) {
		//1
		List<String> masters = json.getAsStringList(VisJsonCommonsFields.masters);
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
		CcpEntityMetaData entityToReadMetaData = entityToRead.getEntityMetaData();

		String[] entitiesToSelect = entityToReadMetaData.getEntitiesToSelect();
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
		int recordsSizeRemainder = recordsSize  % listSize;
		int totalPages = recordsSizeRemainder + 1;
		int index = 0;

		for(int from = 0; from < totalPages; from++) {
			List<CcpJsonRepresentation> page = new ArrayList<>();
			for(;(index + 1) % listSize !=0 && index < records.size(); index++) {
				CcpJsonRepresentation resume = records.get(index);
				CcpJsonRepresentation indexedRecord = resume.put(JsonFieldNames.index, index);
				page.add(indexedRecord);
			}
			CcpJsonRepresentation jsonWithDetail = CcpOtherConstants.EMPTY_JSON
					.put(VisJsonCommonsFields.detail, page);
					CcpJsonRepresentation jsonWithListSize = jsonWithDetail
					.put(VisJsonCommonsFields.listSize, listSize);
					CcpJsonRepresentation jsonWithFrom = jsonWithListSize
					.put(VisJsonCommonsFields.from, from);
					CcpJsonRepresentation pageRecord = jsonWithFrom
					.mergeWithAnotherJson(primaryKeySupplier)
					;
			var bulkItem = entity.toBulkItems(pageRecord, CcpBulkEntityOperationType.create);
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
