package com.vis.business.recruiter.resumes;

import java.util.ArrayList;
import java.util.List;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.jn.messages.JnSystemMessage;
import com.jn.utils.JnLanguage;

/**
 * The texts of the table of resumes sent to the recruiters (the e-mail and the "see all" screen): one record of
 * {@code jn_system_message} per item and language, seeded by {@link #getSeeds()}. The {@code {...}} are filled by
 * {@link VisRecruiterResumesFields}.
 */
public enum VisRecruiterResumesTexts implements JnSystemMessage {
	columnWhatMatters("O que importa", "What matters"),
	columnLanguagesAndSkills("Idiomas e habilidades", "Languages and skills"),
	columnJob("Função / papel / emprego / ocupação", "Role / position / job / occupation"),
	columnOtherInformation("Outras Informações", "Other information"),
	columnDatesAndViews("Datas e visualização", "Dates and views"),
	cltClaim("Pretensão Salarial CLT: {value}", "Salary expectation (CLT): {value}"),
	pjClaim("Pretensão Salarial PJ: {value}", "Salary expectation (contractor): {value}"),
	notInformed("Não Informada", "Not informed"),
	negotiableClaim("Pretensões salariais negociáveis: {value}", "Negotiable salary expectations: {value}"),
	yes("Sim", "Yes"),
	no("Não", "No"),
	experience("Experiência em anos: {years} ({level})", "Years of experience: {years} ({level})"),
	seniority_especialista("Especialista", "Specialist"),
	seniority_senior("Senior", "Senior"),
	seniority_pleno("Pleno", "Mid-level"),
	seniority_junior("Junior", "Junior"),
	seniority_estag("Trainee/Estágio", "Trainee/Intern"),
	homeOfficeOnly("Aceita somente homeoffice", "Accepts home office only"),
	moreRegions("+{count} regiões", "+{count} regions"),
	resumeType("Tipo do currículo: {value}", "Type of resume: {value}"),
	resumeType_1("Tecnologia da Informação", "Information Technology"),
	resumeType_2("Recrutamento e seleção", "Recruitment and selection"),
	resumeType_3("Nível Superior", "Higher education"),
	resumeType_4("Profissão de nível operacional", "Operational-level profession"),
	lastJob("Última função / papel / emprego / ocupação exercida: {value}", "Last role / position / job / occupation: {value}"),
	desiredJob("Função / papel / emprego / ocupação desejada: {value}", "Desired role / position / job / occupation: {value}"),
	disponibilityInDays("Disponibilidade para novas oportunidades em {days} dias", "Available for new opportunities in {days} days"),
	immediateDisponibility("Disponibilidade imediata para ingressar em novas oportunidades", "Immediately available for new opportunities"),
	noTemporaryJobs("Este perfil não avalia oportunidades temporárias", "This profile does not consider temporary opportunities"),
	temporaryJobMinimum("Mínimo de tempo de projeto aceito por este perfil: {months} meses", "Minimum project length accepted by this profile: {months} months"),
	travel("Disponibilidade para viagens: {value}", "Available to travel: {value}"),
	pcd("Pessoa com deficiência (PCD): {value}", "Person with disability: {value}"),
	languageFluent("{language} fluente", "{language}, fluent"),
	languageIntermediate("{language} intermediário", "{language}, intermediate"),
	lastView("Sua última visualização: {date}", "Your last view: {date}"),
	neverViewed("nunca", "never"),
	viewResume("Ver currículo", "View resume"),
	insertionDate("Data de inserção: {date}", "Added on: {date}"),
	lastChangeDate("Última alteração: {date}", "Last changed on: {date}"),
	aliasYears("{years} Anos", "{years} Years"),
	seeAll("Ver todos", "See all"),
	seeAllParagraph("Estes são os {shown} primeiros de {total} currículos. {seeAllLink}", "These are the first {shown} of {total} resumes. {seeAllLink}"),
	;

	/** The text in Portuguese. */
	private final String portuguese;

	/** The text in English. */
	private final String english;

	/**
	 * Keeps the texts of the item.
	 * @param portuguese the text in Portuguese
	 * @param english the text in English
	 */
	private VisRecruiterResumesTexts(String portuguese, String english) {
		this.portuguese = portuguese;
		this.english = english;
	}

	/** The prefixes of the items chosen by a value of the resume (the seniority and the type of the resume). */
	public static enum Prefixes implements CcpJsonFieldName {
		seniority_,
		resumeType_,
	}

	/**
	 * Finds the item of a value of the resume: {@code seniority_} + the name of the seniority, {@code resumeType_} + the
	 * type of the resume.
	 * @param prefix the prefix of the item
	 * @param value the value of the resume
	 * @return the item
	 */
	public static VisRecruiterResumesTexts of(Prefixes prefix, Object value) {
		String itemName = prefix.name() + value;
		VisRecruiterResumesTexts text = valueOf(itemName);
		return text;
	}

	/**
	 * The seeds of every text, in Portuguese and in English.
	 * @return the records of {@code jn_system_message}
	 */
	public static List<CcpBulkItem> getSeeds() {
		List<CcpBulkItem> seeds = new ArrayList<>();
		VisRecruiterResumesTexts[] texts = values();
		for (VisRecruiterResumesTexts text : texts) {
			List<CcpBulkItem> portugueseSeed = text.toBulkItems(JnLanguage.portuguese, text.portuguese);
			List<CcpBulkItem> englishSeed = text.toBulkItems(JnLanguage.english, text.english);
			seeds.addAll(portugueseSeed);
			seeds.addAll(englishSeed);
		}
		return seeds;
	}
}
