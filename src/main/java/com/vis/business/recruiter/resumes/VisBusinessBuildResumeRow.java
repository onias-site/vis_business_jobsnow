package com.vis.business.recruiter.resumes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpNumberDecorator;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTextDecorator;
import com.ccp.decorators.CcpTimeDecorator;
import com.ccp.json.fields.validation.CcpJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;
import com.vis.entities.VisEntityResume;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonFieldsSkills;
import com.vis.json.fields.validation.VisLanguages;
import com.vis.json.fields.validation.VisResumeSkillCategories;
import com.vis.json.transformers.VisSeniorityType;

/**
 * Turns a stored resume into a row of the table of resumes sent to the recruiters: the lines of the four first columns
 * (what matters, languages and skills, role, other information), the dates of the fifth one, the alias suggested to
 * the recruiter who opens it and the keys that sort the rows. The rest of the fifth column (the last view and the link)
 * depends on the recruiter, and is added when the e-mail of each one is assembled.
 *
 * <p>Input: the resume as stored in {@code vis_resume} (the e-mail already hashed, {@code experience} as the year the
 * experience began), {@code rankings} (each skill and its position in {@code vis_skill}, the lower the better),
 * {@code insertion} and {@code lastChange} (the oldest and the newest timestamps of the resume in the history; without
 * them the row has no date lines) and
 * {@code language} (of the texts).
 */
public class VisBusinessBuildResumeRow implements CcpBusiness {

	/** The single instance. */
	public static final VisBusinessBuildResumeRow INSTANCE = new VisBusinessBuildResumeRow();

	private VisBusinessBuildResumeRow() {}

	/** How many languages and skills go into the suggested alias. */
	static final int SKILLS_IN_THE_ALIAS = 5;

	/** How many regions go into the suggested alias (and are shown before the link "+N regions"). */
	public static final int VISIBLE_REGIONS = 3;

	/** The longest alias accepted by {@code vis_viewed_resume}. */
	static final int ALIAS_MAX_LENGTH = 200;

	/** The DDD of the resume that accepts only home office. */
	static final int HOME_OFFICE_DDD = 10;

	/** The level of a spoken language that counts as fluent (1 is intermediate). */
	static final int FLUENT_LEVEL = 2;

	/** The months from which a resume does not consider temporary jobs. */
	static final int MONTHS_OF_A_PERMANENT_JOB = 12;

	/** A missing salary expectation goes after every informed one in the sorting. */
	static final double MISSING_SALARY = Double.MAX_VALUE;

	/** The texts that are not messages. */
	static enum Constants implements CcpJsonFieldName {
		DATE_PATTERN { public String getValue() { return "dd/MM/yyyy HH:mm"; } },
		ALIAS_SEPARATOR { public String getValue() { return "_"; } },
	}

	/**
	 * The languages that go to the top of the column when fluent, in this order: the names are compared without accents
	 * and in lower case, in Portuguese, English and Spanish.
	 */
	static enum TopLanguages implements CcpJsonFieldName {
		ingles, english,
		espanhol, spanish, espanol,
		;
		/**
		 * The position of the language at the top of the column: English before Spanish.
		 * @param languageName the name of the language
		 * @return 0 for English, 1 for Spanish, nothing for any other
		 */
		static Optional<Integer> positionOf(String languageName) {
			CcpStringDecorator nameDecorator = new CcpStringDecorator(languageName);
			CcpTextDecorator nameText = nameDecorator.text();
			CcpTextDecorator withoutAccents = nameText.stripAccents();
			String normalizedName = withoutAccents.content.trim().toLowerCase();
			boolean isEnglish = ingles.name().equals(normalizedName) || english.name().equals(normalizedName);
			if(isEnglish) {
				return Optional.of(0);
			}
			boolean isSpanish = espanhol.name().equals(normalizedName) || spanish.name().equals(normalizedName) || espanol.name().equals(normalizedName);
			if(isSpanish) {
				return Optional.of(1);
			}
			return Optional.empty();
		}
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String languageName = json.getAsString(JnJsonCommonsFields.language);
		JnLanguage language = JnLanguage.valueOf(languageName);
		String resumeId = VisEntityResume.ENTITY.calculateId(json);

		List<String> whatMatters = this.getWhatMatters(json, language);
		List<String> regions = getRegions(json, language);
		List<String> languages = getTopLanguagesAndOthers(json, language, true);
		List<String> otherLanguages = getTopLanguagesAndOthers(json, language, false);
		List<String> skills = getSkillsByRanking(json);
		List<String> languagesAndSkills = new ArrayList<>(languages);
		languagesAndSkills.addAll(skills);
		languagesAndSkills.addAll(otherLanguages);
		List<String> job = this.getJob(json, language);
		List<String> otherInformation = this.getOtherInformation(json, language);

		CcpJsonRepresentation withDefaultAlias = VisBusinessDefaultResumeAlias.INSTANCE.execute(json);
		String defaultAlias = withDefaultAlias.getAsString(VisRecruiterResumesFields.defaultAlias);
		List<Object> sortKeys = getSortKeys(json);

		CcpJsonRepresentation rowWithId = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.resumeId, resumeId);
		CcpJsonRepresentation rowWithWhatMatters = rowWithId.put(VisRecruiterResumesFields.whatMatters, whatMatters);
		CcpJsonRepresentation rowWithRegions = rowWithWhatMatters.put(VisRecruiterResumesFields.regions, regions);
		CcpJsonRepresentation rowWithLanguagesAndSkills = rowWithRegions.put(VisRecruiterResumesFields.languagesAndSkills, languagesAndSkills);
		CcpJsonRepresentation rowWithJob = rowWithLanguagesAndSkills.put(VisRecruiterResumesFields.job, job);
		CcpJsonRepresentation rowWithOtherInformation = rowWithJob.put(VisRecruiterResumesFields.otherInformation, otherInformation);
		CcpJsonRepresentation rowWithDefaultAlias = rowWithOtherInformation.put(VisRecruiterResumesFields.defaultAlias, defaultAlias);
		CcpJsonRepresentation rowWithSortKeys = rowWithDefaultAlias.put(VisRecruiterResumesFields.sortKeys, sortKeys);
		CcpJsonRepresentation row = putDateLines(rowWithSortKeys, json, language);
		return row;
	}

	/**
	 * The lines of insertion and of last change, only when the input brings both timestamps: they come from the history
	 * of the resume, which only a query reads (the e-mail of resumes reads it; the details of a viewed resume, an online
	 * request, do not, OQL).
	 */
	private static CcpJsonRepresentation putDateLines(CcpJsonRepresentation row, CcpJsonRepresentation json, JnLanguage language) {
		boolean noDates = false == json.containsAllFields(VisRecruiterResumesFields.insertion, VisRecruiterResumesFields.lastChange);

		if(noDates) {
			return row;
		}

		Long insertionTime = json.getAsLongNumber(VisRecruiterResumesFields.insertion);
		Long lastChangeTime = json.getAsLongNumber(VisRecruiterResumesFields.lastChange);
		String insertionLine = getDateLine(VisRecruiterResumesTexts.insertionDate, insertionTime, language);
		String lastChangeLine = getDateLine(VisRecruiterResumesTexts.lastChangeDate, lastChangeTime, language);
		CcpJsonRepresentation rowWithInsertion = row.put(VisRecruiterResumesFields.insertion, insertionLine);
		CcpJsonRepresentation rowWithLastChange = rowWithInsertion.put(VisRecruiterResumesFields.lastChange, lastChangeLine);
		return rowWithLastChange;
	}

	private List<String> getWhatMatters(CcpJsonRepresentation json, JnLanguage language) {
		String clt = getSalary(json, VisEntityResume.Fields.clt, language);
		String pj = getSalary(json, VisEntityResume.Fields.pj, language);
		String cltLine = getLine(VisRecruiterResumesTexts.cltClaim, language, withValue(clt));
		String pjLine = getLine(VisRecruiterResumesTexts.pjClaim, language, withValue(pj));

		String negotiable = getYesOrNo(json, VisEntityResume.Fields.negotiableClaim, language);
		String negotiableLine = getLine(VisRecruiterResumesTexts.negotiableClaim, language, withValue(negotiable));

		int years = getYearsOfExperience(json);
		String seniority = getSeniority(years, language);
		CcpJsonRepresentation experienceValues = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.years, years)
				.put(VisRecruiterResumesFields.level, seniority);
		String experienceLine = getLine(VisRecruiterResumesTexts.experience, language, experienceValues);

		List<String> whatMatters = List.of(cltLine, pjLine, negotiableLine, experienceLine);
		return whatMatters;
	}

	/**
	 * The metropolitan regions of the resume, in the order of the DDD; a resume with only the DDD 10 accepts only home
	 * office.
	 */
	static List<String> getRegions(CcpJsonRepresentation json, JnLanguage language) {
		List<Object> ddds = json.getAsObjectList(VisEntityResume.Fields.ddd);
		List<Integer> sortedDdds = ddds.stream().map(x -> ((Number) x).intValue()).sorted().distinct().collect(Collectors.toList());
		boolean homeOfficeOnly = sortedDdds.equals(List.of(HOME_OFFICE_DDD));

		if(homeOfficeOnly) {
			String homeOffice = VisRecruiterResumesTexts.homeOfficeOnly.getMessage(language);
			return List.of(homeOffice);
		}

		List<String> regions = new ArrayList<>();
		for (Integer ddd : sortedDdds) {
			Optional<VisMetropolitanRegions> region = VisMetropolitanRegions.fromDdd(ddd);
			boolean notARegion = region.isEmpty();
			if(notARegion) {
				continue;
			}
			String regionName = region.get().getValue();
			regions.add(regionName);
		}
		return regions;
	}

	/**
	 * The spoken languages: the fluent English and Spanish (in this order) when {@code top}, every other one otherwise
	 * (the other languages, at any level, and English or Spanish at the intermediate level).
	 */
	static List<String> getTopLanguagesAndOthers(CcpJsonRepresentation json, JnLanguage language, boolean top) {
		boolean noLanguage = false == json.containsAllFields(VisEntityResume.Fields.spokenLanguage);
		if(noLanguage) {
			return List.of();
		}
		List<CcpJsonRepresentation> spokenLanguages = json.getAsJsonList(VisEntityResume.Fields.spokenLanguage);
		List<CcpJsonRepresentation> selectedLanguages = new ArrayList<>();
		for (CcpJsonRepresentation spokenLanguage : spokenLanguages) {
			boolean isTop = isFluentTopLanguage(spokenLanguage);
			boolean selected = isTop == top;
			if(selected) {
				selectedLanguages.add(spokenLanguage);
			}
		}
		Comparator<CcpJsonRepresentation> byTopPosition = Comparator.comparing(x -> getTopPosition(x));
		if(top) {
			selectedLanguages.sort(byTopPosition);
		}
		List<String> lines = new ArrayList<>();
		for (CcpJsonRepresentation spokenLanguage : selectedLanguages) {
			String name = spokenLanguage.getAsString(VisLanguages.name);
			Integer level = spokenLanguage.getAsIntegerNumber(VisLanguages.level);
			boolean fluent = level == FLUENT_LEVEL;
			VisRecruiterResumesTexts text = fluent ? VisRecruiterResumesTexts.languageFluent : VisRecruiterResumesTexts.languageIntermediate;
			CcpJsonRepresentation values = CcpOtherConstants.EMPTY_JSON.put(JnJsonCommonsFields.language, name);
			String line = getLine(text, language, values);
			lines.add(line);
		}
		return lines;
	}

	private static boolean isFluentTopLanguage(CcpJsonRepresentation spokenLanguage) {
		Integer level = spokenLanguage.getAsIntegerNumber(VisLanguages.level);
		boolean fluent = level == FLUENT_LEVEL;
		if(false == fluent) {
			return false;
		}
		String name = spokenLanguage.getAsString(VisLanguages.name);
		Optional<Integer> topPosition = TopLanguages.positionOf(name);
		boolean isTop = topPosition.isPresent();
		return isTop;
	}

	private static Integer getTopPosition(CcpJsonRepresentation spokenLanguage) {
		String name = spokenLanguage.getAsString(VisLanguages.name);
		Optional<Integer> topPosition = TopLanguages.positionOf(name);
		Integer position = topPosition.orElse(Integer.MAX_VALUE);
		return position;
	}

	/**
	 * The skills the candidate wrote in the resume (the category {@code skillsFromText}), by their ranking in
	 * {@code vis_skill}; a skill without ranking goes to the end.
	 */
	static List<String> getSkillsByRanking(CcpJsonRepresentation json) {
		boolean noSkill = false == json.containsAllFields(VisEntityResume.Fields.skill);
		if(noSkill) {
			return List.of();
		}
		CcpJsonRepresentation rankings = json.getInnerJson(VisRecruiterResumesFields.rankings);
		List<CcpJsonRepresentation> resumeSkills = json.getAsJsonList(VisEntityResume.Fields.skill);
		String skillsFromText = VisResumeSkillCategories.skillsFromText.name();
		Set<String> skills = new LinkedHashSet<>();
		for (CcpJsonRepresentation resumeSkill : resumeSkills) {
			String category = resumeSkill.getAsString(VisJsonFieldsSkills.category);
			boolean writtenInTheResume = skillsFromText.equals(category);
			if(false == writtenInTheResume) {
				continue;
			}
			String skill = resumeSkill.getAsString(VisJsonFieldsSkills.skill);
			skills.add(skill);
		}
		List<String> sortedSkills = new ArrayList<>(skills);
		sortedSkills.sort(Comparator.comparing(skill -> getRanking(rankings, skill)));
		return sortedSkills;
	}

	private static Double getRanking(CcpJsonRepresentation rankings, String skill) {
		CcpFieldName skillField = new CcpFieldName(skill);
		boolean noRanking = false == rankings.containsAllFields(skillField);
		if(noRanking) {
			return Double.MAX_VALUE;
		}
		Double ranking = rankings.getAsDoubleNumber(skillField);
		return ranking;
	}

	private List<String> getJob(CcpJsonRepresentation json, JnLanguage language) {
		Integer resumeTypeNumber = json.getAsIntegerNumber(VisEntityResume.Fields.resumeType);
		VisRecruiterResumesTexts resumeTypeText = VisRecruiterResumesTexts.of(VisRecruiterResumesTexts.Prefixes.resumeType_, resumeTypeNumber);
		String resumeTypeName = resumeTypeText.getMessage(language);
		String resumeTypeLine = getLine(VisRecruiterResumesTexts.resumeType, language, withValue(resumeTypeName));

		String lastJob = getTextOrNotInformed(json, VisEntityResume.Fields.lastJob, language);
		String lastJobLine = getLine(VisRecruiterResumesTexts.lastJob, language, withValue(lastJob));
		String desiredJob = getTextOrNotInformed(json, VisEntityResume.Fields.desiredJob, language);
		String desiredJobLine = getLine(VisRecruiterResumesTexts.desiredJob, language, withValue(desiredJob));

		List<String> job = List.of(resumeTypeLine, lastJobLine, desiredJobLine);
		return job;
	}

	private List<String> getOtherInformation(CcpJsonRepresentation json, JnLanguage language) {
		Double disponibilityNumber = getNumberOrZero(json, VisEntityResume.Fields.disponibility);
		int disponibility = disponibilityNumber.intValue();
		boolean immediate = disponibility <= 0;
		CcpJsonRepresentation disponibilityValues = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.days, disponibility);
		String disponibilityLine = immediate
				? VisRecruiterResumesTexts.immediateDisponibility.getMessage(language)
				: getLine(VisRecruiterResumesTexts.disponibilityInDays, language, disponibilityValues);

		Double temporallyJobTimeNumber = getNumberOrZero(json, VisEntityResume.Fields.temporallyJobTime);
		int temporallyJobTime = temporallyJobTimeNumber.intValue();
		boolean noTemporaryJobs = temporallyJobTime <= 0 || temporallyJobTime >= MONTHS_OF_A_PERMANENT_JOB;
		CcpJsonRepresentation temporaryJobValues = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.months, temporallyJobTime);
		String temporaryJobLine = noTemporaryJobs
				? VisRecruiterResumesTexts.noTemporaryJobs.getMessage(language)
				: getLine(VisRecruiterResumesTexts.temporaryJobMinimum, language, temporaryJobValues);

		String travel = getYesOrNo(json, VisEntityResume.Fields.travel, language);
		String travelLine = getLine(VisRecruiterResumesTexts.travel, language, withValue(travel));
		String pcd = getYesOrNo(json, VisEntityResume.Fields.pcd, language);
		String pcdLine = getLine(VisRecruiterResumesTexts.pcd, language, withValue(pcd));

		List<String> otherInformation = List.of(disponibilityLine, temporaryJobLine, travelLine, pcdLine);
		return otherInformation;
	}

	/** Type of resume, CLT, PJ and years of experience: the order of the rows of the table. */
	private static List<Object> getSortKeys(CcpJsonRepresentation json) {
		Integer resumeType = json.getAsIntegerNumber(VisEntityResume.Fields.resumeType);
		boolean hasClt = json.containsAllFields(VisEntityResume.Fields.clt);
		Double clt = hasClt ? json.getAsDoubleNumber(VisEntityResume.Fields.clt) : MISSING_SALARY;
		boolean hasPj = json.containsAllFields(VisEntityResume.Fields.pj);
		Double pj = hasPj ? json.getAsDoubleNumber(VisEntityResume.Fields.pj) : MISSING_SALARY;
		int years = getYearsOfExperience(json);
		List<Object> sortKeys = List.of(resumeType, clt.doubleValue(), pj.doubleValue(), years);
		return sortKeys;
	}

	/**
	 * The years of experience: the resume stores the year the experience began
	 * ({@code VisJsonTransformerExperienceStartYear}).
	 */
	static int getYearsOfExperience(CcpJsonRepresentation json) {
		Integer experienceStartYear = json.getAsIntegerNumber(VisJsonCommonsFields.experience);
		CcpTimeDecorator now = new CcpTimeDecorator();
		int currentYear = now.getYear();
		int years = Math.max(0, currentYear - experienceStartYear);
		return years;
	}

	private static String getSeniority(int years, JnLanguage language) {
		CcpJsonRepresentation yearsJson = CcpOtherConstants.EMPTY_JSON.put(VisJsonCommonsFields.experience, years);
		VisSeniorityType[] seniorities = VisSeniorityType.values();
		for (VisSeniorityType seniority : seniorities) {
			boolean matches = seniority.matches(VisJsonCommonsFields.experience, yearsJson);
			if(false == matches) {
				continue;
			}
			VisRecruiterResumesTexts seniorityText = VisRecruiterResumesTexts.of(VisRecruiterResumesTexts.Prefixes.seniority_, seniority.name());
			String seniorityName = seniorityText.getMessage(language);
			return seniorityName;
		}
		VisRecruiterResumesTexts specialist = VisRecruiterResumesTexts.of(VisRecruiterResumesTexts.Prefixes.seniority_, VisSeniorityType.especialista.name());
		String specialistName = specialist.getMessage(language);
		return specialistName;
	}

	/** A number of the resume, or zero when absent: the stored numbers come back as {@code Double}, so they cannot be
	 * read with a typed default. */
	private static Double getNumberOrZero(CcpJsonRepresentation json, CcpJsonFieldName field) {
		boolean absent = false == json.containsAllFields(field);
		if(absent) {
			return 0d;
		}
		Double number = json.getAsDoubleNumber(field);
		return number;
	}

	static String getSalary(CcpJsonRepresentation json, CcpJsonFieldName field, JnLanguage language) {
		Double salary = getNumberOrZero(json, field);
		boolean notInformed = salary <= 0;
		if(notInformed) {
			String notInformedText = VisRecruiterResumesTexts.notInformed.getMessage(language);
			return notInformedText;
		}
		String salaryText = "" + salary;
		CcpNumberDecorator salaryNumber = new CcpNumberDecorator(salaryText);
		String brazilianMoney = salaryNumber.toBrazilianCurrency();
		return brazilianMoney;
	}

	private static String getYesOrNo(CcpJsonRepresentation json, CcpJsonFieldName field, JnLanguage language) {
		boolean present = json.containsAllFields(field);
		boolean yes = present && json.getAsBoolean(field);
		VisRecruiterResumesTexts answer = yes ? VisRecruiterResumesTexts.yes : VisRecruiterResumesTexts.no;
		String answerText = answer.getMessage(language);
		return answerText;
	}

	private static String getTextOrNotInformed(CcpJsonRepresentation json, CcpJsonFieldName field, JnLanguage language) {
		String text = json.getOrDefault(field, () -> "");
		boolean notInformed = text.isBlank();
		if(notInformed) {
			String notInformedText = VisRecruiterResumesTexts.notInformed.getMessage(language);
			return notInformedText;
		}
		return text;
	}

	static String getDateLine(VisRecruiterResumesTexts text, Long time, JnLanguage language) {
		CcpTimeDecorator timeDecorator = new CcpTimeDecorator(time);
		String datePattern = Constants.DATE_PATTERN.getValue();
		String formattedDate = timeDecorator.getFormattedDateTime(datePattern);
		CcpJsonRepresentation values = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.date, formattedDate);
		String line = getLine(text, language, values);
		return line;
	}

	static String getLine(VisRecruiterResumesTexts text, JnLanguage language, CcpJsonRepresentation values) {
		String line = text.getMessage(language, values);
		return line;
	}

	/** The json with the single {@code value} of a text. */
	static CcpJsonRepresentation withValue(String value) {
		CcpJsonRepresentation values = CcpOtherConstants.EMPTY_JSON.put(CcpJsonCommonsFields.value, value);
		return values;
	}
}
