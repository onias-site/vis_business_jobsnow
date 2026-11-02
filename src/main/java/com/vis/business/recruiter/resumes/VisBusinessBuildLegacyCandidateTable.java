package com.vis.business.recruiter.resumes;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpTimeDecorator;
import com.ccp.json.fields.validation.CcpJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;
import com.vis.entities.VisEntityResume;
import com.vis.json.fields.validation.VisJsonFieldsSkills;
import com.vis.json.fields.validation.VisResumeSkillCategories;
import com.vis.services.VisServiceSkills;

/**
 * The table of the invitation to a candidate of the legacy index {@code profissionais2}: the same table the recruiters
 * receive, with a single row. Only what the candidate informed is shown: the legacy index has no travel nor temporary job
 * data, and a number it does not have gives no line, because the row of a stored resume would read an absence as "no".
 * Nothing is invented. The candidate is not a resume yet, so the row has no signed link.
 *
 * <p>Input: {@code legacyCandidate} (the document of the legacy index), {@code email} and {@code frontendUrl}. Output: the
 * input with {@code resumesTable}.
 */
public class VisBusinessBuildLegacyCandidateTable implements CcpBusiness {

	/** The single instance. */
	public static final VisBusinessBuildLegacyCandidateTable INSTANCE = new VisBusinessBuildLegacyCandidateTable();

	private VisBusinessBuildLegacyCandidateTable() {}

	/** The language of the invitation. */
	static final JnLanguage LANGUAGE = JnLanguage.portuguese;

	/** The first year accepted as the year the experience began. */
	static final int FIRST_YEAR_OF_EXPERIENCE = 1900;

	/** The type of resume whose skills come from its text, as in the screen of the resume. */
	static final int RESUME_TYPE_WITH_SKILLS = 1;

	/** The positions of the lines of the column "what matters" of {@link VisBusinessBuildResumeRow}. */
	static final int CLAIMS_LINES = 2, NEGOTIABLE_LINE = 2, EXPERIENCE_LINE = 3;

	/** The positions of the lines of the column "other information" of {@link VisBusinessBuildResumeRow}. */
	static final int DISPONIBILITY_LINE = 0, PCD_LINE = 3;

	/** The fields of the legacy index. */
	public static enum Legacy implements CcpJsonFieldName {
		id,
		tipoVaga,
		ddd,
		homeoffice,
		experiencia,
		disponibilidade,
		pretensaoClt,
		pretensaoPj,
		pretensaoNegociavel,
		pcd,
		profissaoDesejada,
		ultimaProfissao,
		/** Only the text of the file: {@code curriculo} as a whole brings the file itself. */
		RESUME_TEXT { public String getValue() { return "curriculo.t"; } },
	}

	/** The input of the search of the skills of a text. */
	static enum SkillsInput implements CcpJsonFieldName {
		excludedSkill,
	}

	/** The object of the legacy index that holds the text of the resume. */
	static enum LegacyResumeFile implements CcpJsonFieldName {
		curriculo,
		t,
	}

	/**
	 * The resume of the candidate, as {@code vis_resume} would store it, only with what the legacy index has.
	 * @param legacy the document of the legacy index
	 * @return the resume
	 */
	public static CcpJsonRepresentation toResume(CcpJsonRepresentation legacy) {
		String email = legacy.getAsString(Legacy.id);
		int resumeType = (int) getNumber(legacy, Legacy.tipoVaga);
		List<Integer> ddds = getDdds(legacy);
		CcpTimeDecorator now = new CcpTimeDecorator();
		int experienceYear = (int) getNumber(legacy, Legacy.experiencia);
		boolean hasExperience = experienceYear > FIRST_YEAR_OF_EXPERIENCE;
		int startYear = hasExperience ? experienceYear : now.getYear();
		String desiredJob = legacy.getAsString(Legacy.profissaoDesejada);
		String lastJob = legacy.getAsString(Legacy.ultimaProfissao);
		List<CcpJsonRepresentation> skills = getSkills(legacy, resumeType);
		boolean negotiable = legacy.getOrDefault(Legacy.pretensaoNegociavel, () -> false);
		boolean pcd = legacy.getOrDefault(Legacy.pcd, () -> false);
		double clt = getNumber(legacy, Legacy.pretensaoClt);
		double pj = getNumber(legacy, Legacy.pretensaoPj);
		int disponibility = (int) getNumber(legacy, Legacy.disponibilidade);
		CcpJsonRepresentation resume = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityResume.Fields.email, email)
				.put(VisEntityResume.Fields.resumeType, resumeType)
				.put(VisEntityResume.Fields.ddd, ddds)
				.put(VisEntityResume.Fields.experience, startYear)
				.put(VisEntityResume.Fields.desiredJob, desiredJob)
				.put(VisEntityResume.Fields.lastJob, lastJob)
				.put(VisEntityResume.Fields.clt, clt)
				.put(VisEntityResume.Fields.pj, pj)
				.put(VisEntityResume.Fields.negotiableClaim, negotiable)
				.put(VisEntityResume.Fields.pcd, pcd)
				.put(VisEntityResume.Fields.disponibility, disponibility)
				.put(VisEntityResume.Fields.skill, skills);
		return resume;
	}

	/** A number of the legacy index, which keeps them as numbers or as texts; 0 when absent or not a number. */
	static double getNumber(CcpJsonRepresentation legacy, CcpJsonFieldName field) {
		String text = legacy.getAsString(field);
		String withPoint = text.replace(',', '.');
		try {
			return Double.parseDouble(withPoint);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/** Home office when that is all the candidate accepts, otherwise the DDD informed; a DDD 0 is no region at all. */
	private static List<Integer> getDdds(CcpJsonRepresentation legacy) {
		int ddd = (int) getNumber(legacy, Legacy.ddd);
		boolean homeOffice = legacy.getOrDefault(Legacy.homeoffice, () -> false);
		boolean onlyHomeOffice = homeOffice && ddd == 0;
		if(onlyHomeOffice) {
			return List.of(VisBusinessBuildResumeRow.HOME_OFFICE_DDD);
		}
		return List.of(ddd);
	}

	/** The skills of the text of the resume, as the screen finds them; only the type 1 has skills. */
	private static List<CcpJsonRepresentation> getSkills(CcpJsonRepresentation legacy, int resumeType) {
		boolean noSkills = resumeType != RESUME_TYPE_WITH_SKILLS;
		if(noSkills) {
			return List.of();
		}
		CcpJsonRepresentation resumeFile = legacy.getInnerJson(LegacyResumeFile.curriculo);
		String text = resumeFile.getAsString(LegacyResumeFile.t);
		CcpJsonRepresentation textInput = CcpOtherConstants.EMPTY_JSON
				.put(CcpJsonCommonsFields.text, text)
				.put(SkillsInput.excludedSkill, List.of());
		CcpJsonRepresentation skillsOfTheText = VisServiceSkills.GetSkillsFromText.execute(textInput);
		boolean noSkillFound = false == skillsOfTheText.containsAllFields(VisEntityResume.Fields.skill);
		if(noSkillFound) {
			return List.of();
		}
		String skillsFromText = VisResumeSkillCategories.skillsFromText.name();
		List<CcpJsonRepresentation> foundSkills = skillsOfTheText.getAsJsonList(VisEntityResume.Fields.skill);
		List<CcpJsonRepresentation> skills = new ArrayList<>();
		for (CcpJsonRepresentation foundSkill : foundSkills) {
			CcpJsonRepresentation skill = foundSkill.put(VisJsonFieldsSkills.category, skillsFromText);
			skills.add(skill);
		}
		return skills;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation legacy = json.getInnerJson(VisRecruiterResumesFields.legacyCandidate);
		String email = json.getAsString(JnJsonCommonsFields.email);
		String frontendUrl = json.getAsString(VisRecruiterResumesFields.frontendUrl);
		CcpJsonRepresentation resume = toResume(legacy);
		CcpJsonRepresentation resumesJson = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.resumes, List.of(resume));
		CcpJsonRepresentation withRankings = VisBusinessLoadSkillRankings.INSTANCE.execute(resumesJson);
		CcpJsonRepresentation rankings = withRankings.getInnerJson(VisRecruiterResumesFields.rankings);
		long now = System.currentTimeMillis();
		CcpJsonRepresentation rowInput = resume
				.put(VisRecruiterResumesFields.rankings, rankings)
				.put(VisRecruiterResumesFields.insertion, now)
				.put(VisRecruiterResumesFields.lastChange, now)
				.put(JnJsonCommonsFields.language, LANGUAGE);
		CcpJsonRepresentation builtRow = VisBusinessBuildResumeRow.INSTANCE.execute(rowInput);
		List<String> whatMatters = getWhatMatters(builtRow, legacy);
		List<String> otherInformation = getOtherInformation(builtRow, legacy);
		String neverViewed = VisRecruiterResumesTexts.neverViewed.getMessage(LANGUAGE);
		String viewResume = VisRecruiterResumesTexts.viewResume.getMessage(LANGUAGE);
		CcpJsonRepresentation row = builtRow
				.put(VisRecruiterResumesFields.whatMatters, whatMatters)
				.put(VisRecruiterResumesFields.otherInformation, otherInformation)
				.put(VisRecruiterResumesFields.lastView, neverViewed)
				.put(VisRecruiterResumesFields.viewed, false)
				.put(VisRecruiterResumesFields.viewLabel, viewResume);
		CcpJsonRepresentation tableInput = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.resumes, List.of(row))
				.put(JnJsonCommonsFields.email, email)
				.put(VisRecruiterResumesFields.frontendUrl, frontendUrl)
				.put(JnJsonCommonsFields.language, LANGUAGE);
		CcpJsonRepresentation withTable = VisBusinessRenderResumesTable.INSTANCE.execute(tableInput);
		String resumesTable = withTable.getAsString(VisRecruiterResumesFields.resumesTable);
		CcpJsonRepresentation jsonWithTable = json.put(VisRecruiterResumesFields.resumesTable, resumesTable);
		return jsonWithTable;
	}

	/** The claims, and the negotiation and the experience only when the candidate informed them. */
	private static List<String> getWhatMatters(CcpJsonRepresentation row, CcpJsonRepresentation legacy) {
		List<String> builtLines = row.getAsStringList(VisRecruiterResumesFields.whatMatters);
		List<String> claims = builtLines.subList(0, CLAIMS_LINES);
		List<String> lines = new ArrayList<>(claims);
		boolean hasNegotiation = legacy.containsAllFields(Legacy.pretensaoNegociavel);
		if(hasNegotiation) {
			String negotiableLine = builtLines.get(NEGOTIABLE_LINE);
			lines.add(negotiableLine);
		}
		int experienceYear = (int) getNumber(legacy, Legacy.experiencia);
		boolean hasExperience = experienceYear > FIRST_YEAR_OF_EXPERIENCE;
		if(hasExperience) {
			String experienceLine = builtLines.get(EXPERIENCE_LINE);
			lines.add(experienceLine);
		}
		return lines;
	}

	/** The disponibility only when the candidate informed a number of days, and the line of the person with disability. */
	private static List<String> getOtherInformation(CcpJsonRepresentation row, CcpJsonRepresentation legacy) {
		List<String> builtLines = row.getAsStringList(VisRecruiterResumesFields.otherInformation);
		String pcdLine = builtLines.get(PCD_LINE);
		List<String> lines = new ArrayList<>();
		boolean hasDisponibility = getNumber(legacy, Legacy.disponibilidade) > 0;
		if(hasDisponibility) {
			String disponibilityLine = builtLines.get(DISPONIBILITY_LINE);
			lines.add(disponibilityLine);
		}
		lines.add(pcdLine);
		return lines;
	}
}
