package com.vis.business.job.posting;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.business.position.VisBusinessResumesOfTheFilters;
import com.vis.business.recruiter.resumes.VisBusinessSendResumesToRecruiters;
import com.vis.business.recruiter.resumes.VisRecruiterResumesFields;
import com.vis.entities.VisEntityJobPosting;
import com.vis.json.fields.validation.VisJsonFieldsPositionSkills;
import com.vis.json.fields.validation.VisJsonFieldsResumeFilter;
import com.vis.json.fields.validation.VisPositionSkillCategories;
import com.vis.json.fields.validation.VisPositionSkillTypes;
import com.vis.services.VisServiceResumeFilter;

/**
 * Fourth business before the save of {@code vis_job_posting} (since 2026-11-26; the third one until 2026-11-27): the
 * search of resumes of the screen {@code curriculos/buscar} over the skills that some resume has
 * ({@code possibleSkills}), all of them required, in the area of technology, by the business of its endpoint
 * ({@code POST /resume-filter}). The record keeps the count and the averages of the search and the link of the screen
 * with the filters and the text shared under the e-mail {@code curriculos@onias.site}; it goes on whole (since
 * 2026-11-27 the last business removes what the businesses added for one another), plus the ids of the resumes found ({@link Fields#resumeIds}, the hashes of their e-mails), for the e-mail
 * to the recruiters of the text ({@code VisBusinessSendTheResumesOfTheJobPosting}), and the id of the filters
 * ({@link Fields#filterId}), under which {@link VisBusinessSaveTheDescriptionOfTheJobPosting} shares the text (here until
 * 2026-11-27). No possible skill (no resume has any skill of the text), no search: the count is zero, there is no link
 * and no id.
 */
public class VisBusinessSaveTheResumeFilterOfTheJobPosting implements CcpBusiness {

	/** The output of this business besides the fields of the entity. */
	public static enum Fields implements CcpJsonFieldName {
		/** The ids of the resumes found by the search (the hashes of their e-mails). */
		resumeIds,
		/** The id of the filters of the search, under which the next business shares the text of the job posting. */
		filterId,
	}

	/** The single instance: the class keeps no state. */
	public static final VisBusinessSaveTheResumeFilterOfTheJobPosting INSTANCE = new VisBusinessSaveTheResumeFilterOfTheJobPosting();

	private VisBusinessSaveTheResumeFilterOfTheJobPosting() {}

	/** The texts of this class. */
	static enum Constants implements CcpJsonFieldName {
		/** The e-mail that shares the text of the job posting, as the logged user. */
		SHARING_EMAIL { public String getValue() { return "curriculos@onias.site"; } },
		/** The path of the screen of the search of resumes, with the parameter of the id of the filters. */
		SEARCH_PATH { public String getValue() { return "/curriculos/buscar?filtro="; } },
		/** The parameter of the e-mail that shared the text. */
		EMAIL_PARAMETER { public String getValue() { return "&email="; } },
	}

	/** The area of the search: technology, the only one whose text goes to the search of skills. */
	private static final int TECHNOLOGY = 1;

	/**
	 * Searches the resumes of the possible skills and shares the text with the filters.
	 * @param json the record, with {@code description} and {@code possibleSkills}
	 * @return the record, with the count, the averages, the link, the ids of the resumes found and the id of the filters
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		// the whole record goes on: the recruiters of the first business are still needed by the last one, which reduces
		// the record to the fields of the entity
		CcpJsonRepresentation record = json;
		List<String> possibleSkills = json.getAsStringList(VisBusinessFindTheMostCoveredSkills.Fields.possibleSkills);
		boolean noPossibleSkill = possibleSkills.isEmpty();

		if(noPossibleSkill) {
			return record.put(VisEntityJobPosting.Fields.resumesCount, 0);
		}

		CcpJsonRepresentation filters = getFilters(possibleSkills);
		CcpJsonRepresentation count = VisServiceResumeFilter.SearchResumes.execute(filters);
		String filterId = count.getAsString(JnJsonCommonsFields.id);
		String sharingEmail = Constants.SHARING_EMAIL.getValue();
		String link = getLink(filterId, sharingEmail);
		Integer resumesCount = count.getAsIntegerNumber(VisBusinessResumesOfTheFilters.Fields.resumes);
		List<String> resumeIds = count.getAsStringList(VisBusinessResumesOfTheFilters.Fields.ids);
		CcpJsonRepresentation recordWithTheCount = record
				.put(VisEntityJobPosting.Fields.linkToShowResumes, link)
				.put(VisEntityJobPosting.Fields.resumesCount, resumesCount)
				.put(Fields.resumeIds, resumeIds)
				.put(Fields.filterId, filterId);
		CcpJsonRepresentation recordWithClt = putAverage(recordWithTheCount, count, VisBusinessResumesOfTheFilters.Fields.cltAverage, VisEntityJobPosting.Fields.avgClt);
		CcpJsonRepresentation recordWithPj = putAverage(recordWithClt, count, VisBusinessResumesOfTheFilters.Fields.pjAverage, VisEntityJobPosting.Fields.avgPj);
		CcpJsonRepresentation recordWithExperience = putAverage(recordWithPj, count, VisBusinessResumesOfTheFilters.Fields.experienceAverage, VisEntityJobPosting.Fields.avgExperience);
		return recordWithExperience;
	}

	/**
	 * The filters of the search: the area of technology and every possible skill as required (category
	 * {@code skillsFromText}, as the screen sends the skills of the text).
	 * @param possibleSkills the skills
	 * @return the filters
	 */
	private static CcpJsonRepresentation getFilters(List<String> possibleSkills) {
		String category = VisPositionSkillCategories.skillsFromText.name();
		String type = VisPositionSkillTypes.skillsFromResume.name();
		List<CcpJsonRepresentation> requiredSkills = new ArrayList<>();
		for (String possibleSkill : possibleSkills) {
			CcpJsonRepresentation requiredSkill = CcpOtherConstants.EMPTY_JSON
					.put(VisJsonFieldsPositionSkills.skill, possibleSkill)
					.put(VisJsonFieldsPositionSkills.word, possibleSkill)
					.put(VisJsonFieldsPositionSkills.type, type)
					.put(VisJsonFieldsPositionSkills.category, category);
			requiredSkills.add(requiredSkill);
		}
		CcpJsonRepresentation filters = CcpOtherConstants.EMPTY_JSON
				.put(VisJsonFieldsResumeFilter.resumeType, TECHNOLOGY)
				.put(VisJsonFieldsResumeFilter.skill, requiredSkills);
		return filters;
	}

	/**
	 * The address of the screen of the search of resumes with the filters and the text shared by the e-mail.
	 * @param filterId the id of the filters
	 * @param sharingEmail the e-mail that shared the text
	 * @return the address
	 */
	private static String getLink(String filterId, String sharingEmail) {
		CcpJsonRepresentation linkAddresses = VisBusinessSendResumesToRecruiters.getLinkAddresses();
		String frontendUrl = linkAddresses.getAsString(VisRecruiterResumesFields.frontendUrl);
		String searchPath = Constants.SEARCH_PATH.getValue();
		String emailParameter = Constants.EMAIL_PARAMETER.getValue();
		String link = frontendUrl + searchPath + filterId + emailParameter + sharingEmail;
		return link;
	}

	/**
	 * Copies an average of the search to the record, when the search has it (no resume found informed it, no average).
	 * @param record the record
	 * @param count the answer of the search
	 * @param averageOfTheSearch the field of the average in the search
	 * @param averageOfTheRecord the field of the average in the record
	 * @return the record, with the average when there is one
	 */
	private static CcpJsonRepresentation putAverage(CcpJsonRepresentation record, CcpJsonRepresentation count, CcpJsonFieldName averageOfTheSearch, CcpJsonFieldName averageOfTheRecord) {
		boolean noAverage = false == count.containsAllFields(averageOfTheSearch);

		if(noAverage) {
			return record;
		}

		Double average = count.getAsDoubleNumber(averageOfTheSearch);
		CcpJsonRepresentation recordWithTheAverage = record.put(averageOfTheRecord, average);
		return recordWithTheAverage;
	}
}
