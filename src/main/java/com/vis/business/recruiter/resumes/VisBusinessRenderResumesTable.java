package com.vis.business.recruiter.resumes;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTextDecorator;
import com.ccp.decorators.CcpUrlDecorator;
import com.ccp.json.fields.validation.CcpJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;
import com.vis.messages.VisEmailHtml;

/**
 * Renders the table of resumes of the e-mail of a recruiter: one row per resume, five columns, each value of a column
 * in a line of its own. The regions after the third one and the languages and skills after the first
 * {@value #VISIBLE_LANGUAGES_AND_SKILLS} stay out, counted by a link ("+N regions", "see the other N skills") whose
 * {@code title} lists them (shown when the mouse rests on it) and that goes
 * where "View resume" goes: the resume in the front end, signed for this recruiter, or the form of the resume in the
 * preview of the candidate.
 *
 * <p>Input: {@code resumes} (the rows, with the fields of the recruiter: {@code lastView}, {@code viewed},
 * {@code viewLabel} and {@code token}), {@code email} (the recruiter), {@code frontendUrl} and {@code language}.
 * Output: the input with {@code resumesTable}.
 */
public class VisBusinessRenderResumesTable implements CcpBusiness {

	/** The single instance. */
	public static final VisBusinessRenderResumesTable INSTANCE = new VisBusinessRenderResumesTable();

	private VisBusinessRenderResumesTable() {}

	/**
	 * How many lines the column of languages and skills shows: the first ones, already in the order of the row (fluent
	 * English and Spanish, the skills by ranking, the other languages). The whole list stays in the row.
	 */
	static final int VISIBLE_LANGUAGES_AND_SKILLS = 15;

	/** The pieces of HTML of the table; {@code {value}} is the escaped text. */
	static enum Html implements CcpJsonFieldName {
		TABLE { public String getValue() { return "<table id=\"customers\"><tr>{value}</tr>{rows}</table>"; } },
		HEADER { public String getValue() { return "<th style=\"width: 20%; text-align: center;\">{value}</th>"; } },
		ROW { public String getValue() { return "<tr>{value}</tr>"; } },
		CELL { public String getValue() { return "<td>{value}</td>"; } },
		LINE { public String getValue() { return "<p>{value}</p>"; } },
		LINK { public String getValue() { return "<p><a href=\"{link}\" target=\"_blank\">{value}</a></p>"; } },
		TITLED_LINK { public String getValue() { return "<p><a href=\"{link}\" title=\"{title}\" target=\"_blank\">{value}</a></p>"; } },
		NO_LINES { public String getValue() { return ""; } },
		TITLE_SEPARATOR { public String getValue() { return ", "; } },
		QUOTE { public String getValue() { return "\""; } },
		ESCAPED_QUOTE { public String getValue() { return "&quot;"; } },
		VIEW_PATH { public String getValue() { return "{frontendUrl}/recrutador/ver-curriculo?email={email}&resume={resumeId}&token={token}"; } },
		FORM_PATH { public String getValue() { return "{frontendUrl}/meus-dados/sobre-mim"; } },
	}

	/** The fields of the pieces of HTML. */
	static enum HtmlFields implements CcpJsonFieldName {
		rows,
		link,
		title,
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String languageName = json.getAsString(JnJsonCommonsFields.language);
		JnLanguage language = JnLanguage.valueOf(languageName);
		String email = json.getAsString(JnJsonCommonsFields.email);
		String frontendUrl = json.getAsString(VisRecruiterResumesFields.frontendUrl);

		VisRecruiterResumesTexts[] columns = {
				VisRecruiterResumesTexts.columnWhatMatters,
				VisRecruiterResumesTexts.columnLanguagesAndSkills,
				VisRecruiterResumesTexts.columnJob,
				VisRecruiterResumesTexts.columnOtherInformation,
				VisRecruiterResumesTexts.columnDatesAndViews,
		};
		StringBuilder headers = new StringBuilder();
		for (VisRecruiterResumesTexts column : columns) {
			String columnName = column.getMessage(language);
			String header = piece(Html.HEADER, columnName);
			headers.append(header);
		}

		List<CcpJsonRepresentation> resumes = json.getAsJsonList(VisRecruiterResumesFields.resumes);
		StringBuilder rows = new StringBuilder();
		for (CcpJsonRepresentation resume : resumes) {
			String row = this.getRow(resume, language, email, frontendUrl);
			rows.append(row);
		}

		String headersHtml = headers.toString();
		String rowsHtml = rows.toString();
		CcpJsonRepresentation tableValues = CcpOtherConstants.EMPTY_JSON
				.put(CcpJsonCommonsFields.value, headersHtml)
				.put(HtmlFields.rows, rowsHtml);
		String table = resolve(Html.TABLE, tableValues);
		CcpJsonRepresentation jsonWithTable = json.put(VisRecruiterResumesFields.resumesTable, table);
		return jsonWithTable;
	}

	private String getRow(CcpJsonRepresentation resume, JnLanguage language, String email, String frontendUrl) {
		String resumeLink = getResumeLink(resume, email, frontendUrl);

		List<String> whatMatters = resume.getAsStringList(VisRecruiterResumesFields.whatMatters);
		List<String> regions = resume.getAsStringList(VisRecruiterResumesFields.regions);
		String whatMattersLines = lines(whatMatters);
		String regionsLines = getFirstLines(regions, VisBusinessBuildResumeRow.VISIBLE_REGIONS, VisRecruiterResumesTexts.moreRegions, language, resumeLink);
		String whatMattersCell = cell(whatMattersLines + regionsLines);

		List<String> languagesAndSkills = resume.getAsStringList(VisRecruiterResumesFields.languagesAndSkills);
		String languagesAndSkillsLines = getFirstLines(languagesAndSkills, VISIBLE_LANGUAGES_AND_SKILLS, VisRecruiterResumesTexts.moreSkills, language, resumeLink);
		String languagesAndSkillsCell = cell(languagesAndSkillsLines);

		List<String> job = resume.getAsStringList(VisRecruiterResumesFields.job);
		String jobLines = lines(job);
		String jobCell = cell(jobLines);

		List<String> otherInformation = resume.getAsStringList(VisRecruiterResumesFields.otherInformation);
		String otherInformationLines = lines(otherInformation);
		String otherInformationCell = cell(otherInformationLines);

		String datesCell = getDatesAndViewsCell(resume, resumeLink);

		String cells = whatMattersCell + languagesAndSkillsCell + jobCell + otherInformationCell + datesCell;
		String row = resolveRaw(Html.ROW, cells);
		return row;
	}

	/**
	 * The first lines of a list and, when there are more, a link that counts the others ("+N regions", "see the other N
	 * skills"). The others stay out of the e-mail: the providers (Gmail among them) drop {@code <details>}, so a folded
	 * list would show open.
	 * @param texts the whole list, already in its order
	 * @param visibleLimit how many lines are shown
	 * @param moreText the text of the link, with {@code {count}}
	 * @param language the language of the text
	 * @param resumeLink where the link goes, the same address of "View resume"
	 * @return the HTML of the lines
	 */
	static String getFirstLines(List<String> texts, int visibleLimit, VisRecruiterResumesTexts moreText, JnLanguage language, String resumeLink) {
		int textsCount = texts.size();
		int visibleCount = Math.min(visibleLimit, textsCount);
		List<String> visibleTexts = texts.subList(0, visibleCount);
		String visibleLines = lines(visibleTexts);
		boolean nothingLeftOut = textsCount <= visibleLimit;

		if(nothingLeftOut) {
			return visibleLines;
		}

		List<String> leftOutTexts = texts.subList(visibleCount, textsCount);
		int leftOutCount = leftOutTexts.size();
		CcpJsonRepresentation countValues = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.count, leftOutCount);
		String more = moreText.getMessage(language, countValues);
		String separator = Html.TITLE_SEPARATOR.getValue();
		String leftOutList = String.join(separator, leftOutTexts);
		String moreLink = titledLink(resumeLink, more, leftOutList);
		return visibleLines + moreLink;
	}

	/**
	 * A line with a link whose {@code title} holds the texts left out of the e-mail: the e-mail runs no script, but the
	 * reader of a computer sees the title when the mouse rests on the link.
	 * @param address where the link goes
	 * @param text the text of the link
	 * @param title the texts left out, already joined
	 * @return the HTML of the line
	 */
	private static String titledLink(String address, String text, String title) {
		String escapedText = VisEmailHtml.escape(text);
		String escapedTitle = VisEmailHtml.escape(title);
		String quote = Html.QUOTE.getValue();
		String escapedQuote = Html.ESCAPED_QUOTE.getValue();
		String attributeTitle = escapedTitle.replace(quote, escapedQuote);
		CcpJsonRepresentation anchorValues = CcpOtherConstants.EMPTY_JSON
				.put(HtmlFields.link, address)
				.put(HtmlFields.title, attributeTitle)
				.put(CcpJsonCommonsFields.value, escapedText);
		String linkLine = resolve(Html.TITLED_LINK, anchorValues);
		return linkLine;
	}

	/** Last view, link to the resume, date added and date of the last change. */
	private static String getDatesAndViewsCell(CcpJsonRepresentation resume, String resumeLink) {
		String lastView = resume.getAsString(VisRecruiterResumesFields.lastView);
		String lastViewLine = piece(Html.LINE, lastView);
		String viewLine = getViewLine(resume, resumeLink);

		String dateLines = getDateLines(resume);
		String datesCell = cell(lastViewLine + viewLine + dateLines);
		return datesCell;
	}

	/**
	 * The lines of the date added and of the last change, when the row has them: they come from the history of the
	 * resume, which only a query reads, so the e-mail of the saved resume has none.
	 */
	private static String getDateLines(CcpJsonRepresentation resume) {
		boolean noDates = false == resume.containsAllFields(VisRecruiterResumesFields.insertion, VisRecruiterResumesFields.lastChange);
		if(noDates) {
			return Html.NO_LINES.getValue();
		}
		String insertion = resume.getAsString(VisRecruiterResumesFields.insertion);
		String lastChange = resume.getAsString(VisRecruiterResumesFields.lastChange);
		String insertionLine = piece(Html.LINE, insertion);
		String lastChangeLine = piece(Html.LINE, lastChange);
		return insertionLine + lastChangeLine;
	}

	/** The link to the resume; a row with no token is a preview for the candidate, so its label is only a text. */
	private static String getViewLine(CcpJsonRepresentation resume, String resumeLink) {
		String viewLabel = resume.getAsString(VisRecruiterResumesFields.viewLabel);
		boolean preview = false == resume.containsAllFields(VisRecruiterResumesFields.token);
		if(preview) {
			return piece(Html.LINE, viewLabel);
		}
		String viewLine = link(resumeLink, viewLabel);
		return viewLine;
	}

	/**
	 * Where the links of the row go: the resume signed for this recruiter; a row with no token is a preview for the
	 * candidate, whose links go to the form of the resume.
	 */
	private static String getResumeLink(CcpJsonRepresentation resume, String email, String frontendUrl) {
		boolean preview = false == resume.containsAllFields(VisRecruiterResumesFields.token);
		CcpJsonRepresentation frontendValues = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.frontendUrl, frontendUrl);
		if(preview) {
			return resolve(Html.FORM_PATH, frontendValues);
		}
		String resumeId = resume.getAsString(VisRecruiterResumesFields.resumeId);
		String token = resume.getAsString(VisRecruiterResumesFields.token);
		CcpStringDecorator emailDecorator = new CcpStringDecorator(email);
		CcpUrlDecorator emailUrl = emailDecorator.url();
		String encodedEmail = emailUrl.asEnconded();
		CcpJsonRepresentation linkValues = frontendValues
				.put(JnJsonCommonsFields.email, encodedEmail)
				.put(VisRecruiterResumesFields.resumeId, resumeId)
				.put(VisRecruiterResumesFields.token, token);
		String link = resolve(Html.VIEW_PATH, linkValues);
		return link;
	}

	/** A line with a link; the text is escaped. */
	private static String link(String address, String text) {
		String escapedText = VisEmailHtml.escape(text);
		CcpJsonRepresentation anchorValues = CcpOtherConstants.EMPTY_JSON
				.put(HtmlFields.link, address)
				.put(CcpJsonCommonsFields.value, escapedText);
		String linkLine = resolve(Html.LINK, anchorValues);
		return linkLine;
	}

	private static String lines(List<String> texts) {
		StringBuilder lines = new StringBuilder();
		for (String text : texts) {
			String line = piece(Html.LINE, text);
			lines.append(line);
		}
		String linesHtml = lines.toString();
		return linesHtml;
	}

	private static String cell(String linesHtml) {
		String cell = resolveRaw(Html.CELL, linesHtml);
		return cell;
	}

	/** A piece with an escaped text. */
	private static String piece(Html html, String text) {
		String escapedText = VisEmailHtml.escape(text);
		String piece = resolveRaw(html, escapedText);
		return piece;
	}

	/** A piece with HTML already assembled. */
	private static String resolveRaw(Html html, String innerHtml) {
		CcpJsonRepresentation values = CcpOtherConstants.EMPTY_JSON.put(CcpJsonCommonsFields.value, innerHtml);
		String piece = resolve(html, values);
		return piece;
	}

	private static String resolve(Html html, CcpJsonRepresentation values) {
		String template = html.getValue();
		CcpStringDecorator templateDecorator = new CcpStringDecorator(template);
		CcpTextDecorator templateText = templateDecorator.text();
		CcpTextDecorator resolved = templateText.resolveTemplate(values);
		return resolved.content;
	}
}
