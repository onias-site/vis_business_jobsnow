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
 * in a line of its own. The regions after the third one are folded under "+N regions"; a resume the recruiter never
 * viewed has its last view in red (as in the legacy e-mail); the link of the last column opens the resume in the front
 * end, signed for this recruiter.
 *
 * <p>Input: {@code resumes} (the rows, with the fields of the recruiter: {@code lastView}, {@code viewed},
 * {@code viewLabel} and {@code token}), {@code email} (the recruiter), {@code frontendUrl} and {@code language}.
 * Output: the input with {@code resumesTable}.
 */
public class VisBusinessRenderResumesTable implements CcpBusiness {

	/** The single instance. */
	public static final VisBusinessRenderResumesTable INSTANCE = new VisBusinessRenderResumesTable();

	private VisBusinessRenderResumesTable() {}

	/** The pieces of HTML of the table; {@code {value}} is the escaped text. */
	static enum Html implements CcpJsonFieldName {
		TABLE { public String getValue() { return "<table id=\"customers\"><tr>{value}</tr>{rows}</table>"; } },
		HEADER { public String getValue() { return "<th style=\"width: 20%; text-align: center;\">{value}</th>"; } },
		ROW { public String getValue() { return "<tr>{value}</tr>"; } },
		CELL { public String getValue() { return "<td>{value}</td>"; } },
		LINE { public String getValue() { return "<p>{value}</p>"; } },
		NOT_VIEWED_LINE { public String getValue() { return "<p><strong style=\"color: white; background-color: red;\">{value}</strong></p>"; } },
		FOLDED_LINES { public String getValue() { return "<details><summary>{count}</summary>{value}</details>"; } },
		LINK { public String getValue() { return "<p><a href=\"{link}\" target=\"_blank\">{value}</a></p>"; } },
		VIEW_PATH { public String getValue() { return "{frontendUrl}/recrutador/ver-curriculo?email={email}&resume={resumeId}&token={token}"; } },
	}

	/** The fields of the pieces of HTML. */
	static enum HtmlFields implements CcpJsonFieldName {
		rows,
		link,
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
		List<String> whatMatters = resume.getAsStringList(VisRecruiterResumesFields.whatMatters);
		List<String> regions = resume.getAsStringList(VisRecruiterResumesFields.regions);
		String whatMattersLines = lines(whatMatters);
		String regionsLines = getRegionsLines(regions, language);
		String whatMattersCell = cell(whatMattersLines + regionsLines);

		List<String> languagesAndSkills = resume.getAsStringList(VisRecruiterResumesFields.languagesAndSkills);
		String languagesAndSkillsLines = lines(languagesAndSkills);
		String languagesAndSkillsCell = cell(languagesAndSkillsLines);

		List<String> job = resume.getAsStringList(VisRecruiterResumesFields.job);
		String jobLines = lines(job);
		String jobCell = cell(jobLines);

		List<String> otherInformation = resume.getAsStringList(VisRecruiterResumesFields.otherInformation);
		String otherInformationLines = lines(otherInformation);
		String otherInformationCell = cell(otherInformationLines);

		String datesCell = this.getDatesAndViewsCell(resume, email, frontendUrl);

		String cells = whatMattersCell + languagesAndSkillsCell + jobCell + otherInformationCell + datesCell;
		String row = resolveRaw(Html.ROW, cells);
		return row;
	}

	/** The three first regions; the others folded under "+N regions". */
	private static String getRegionsLines(List<String> regions, JnLanguage language) {
		int visibleCount = Math.min(VisBusinessBuildResumeRow.VISIBLE_REGIONS, regions.size());
		List<String> visibleRegions = regions.subList(0, visibleCount);
		String visibleLines = lines(visibleRegions);
		boolean nothingFolded = regions.size() <= VisBusinessBuildResumeRow.VISIBLE_REGIONS;

		if(nothingFolded) {
			return visibleLines;
		}

		int regionsCount = regions.size();
		List<String> foldedRegions = regions.subList(visibleCount, regionsCount);
		String foldedLines = lines(foldedRegions);
		int foldedCount = foldedRegions.size();
		CcpJsonRepresentation countValues = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.count, foldedCount);
		String moreRegions = VisRecruiterResumesTexts.moreRegions.getMessage(language, countValues);
		String escapedMoreRegions = VisEmailHtml.escape(moreRegions);
		CcpJsonRepresentation foldedValues = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.count, escapedMoreRegions)
				.put(CcpJsonCommonsFields.value, foldedLines);
		String folded = resolve(Html.FOLDED_LINES, foldedValues);
		return visibleLines + folded;
	}

	/** Last view (in red when never viewed), link to the resume, date added and date of the last change. */
	private String getDatesAndViewsCell(CcpJsonRepresentation resume, String email, String frontendUrl) {
		String lastView = resume.getAsString(VisRecruiterResumesFields.lastView);
		boolean viewed = resume.getAsBoolean(VisRecruiterResumesFields.viewed);
		Html lastViewPiece = viewed ? Html.LINE : Html.NOT_VIEWED_LINE;
		String lastViewLine = piece(lastViewPiece, lastView);

		String resumeId = resume.getAsString(VisRecruiterResumesFields.resumeId);
		String token = resume.getAsString(VisRecruiterResumesFields.token);
		CcpStringDecorator emailDecorator = new CcpStringDecorator(email);
		CcpUrlDecorator emailUrl = emailDecorator.url();
		String encodedEmail = emailUrl.asEnconded();
		CcpJsonRepresentation linkValues = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.frontendUrl, frontendUrl)
				.put(JnJsonCommonsFields.email, encodedEmail)
				.put(VisRecruiterResumesFields.resumeId, resumeId)
				.put(VisRecruiterResumesFields.token, token);
		String link = resolve(Html.VIEW_PATH, linkValues);
		String viewLabel = resume.getAsString(VisRecruiterResumesFields.viewLabel);
		String escapedViewLabel = VisEmailHtml.escape(viewLabel);
		CcpJsonRepresentation anchorValues = CcpOtherConstants.EMPTY_JSON
				.put(HtmlFields.link, link)
				.put(CcpJsonCommonsFields.value, escapedViewLabel);
		String viewLine = resolve(Html.LINK, anchorValues);

		String insertion = resume.getAsString(VisRecruiterResumesFields.insertion);
		String lastChange = resume.getAsString(VisRecruiterResumesFields.lastChange);
		String insertionLine = piece(Html.LINE, insertion);
		String lastChangeLine = piece(Html.LINE, lastChange);

		String datesCell = cell(lastViewLine + viewLine + insertionLine + lastChangeLine);
		return datesCell;
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
