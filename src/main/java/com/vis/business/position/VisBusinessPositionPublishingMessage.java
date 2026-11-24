package com.vis.business.position;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.utils.JnLanguage;
import com.vis.business.recruiter.resumes.VisBusinessSendResumesToRecruiters;
import com.vis.business.recruiter.resumes.VisRecruiterResumesFields;
import com.vis.business.recruiter.resumes.VisRecruiterResumesTexts;
import com.vis.entities.VisEntityPosition;
import com.vis.json.fields.validation.VisJsonFieldsPositionContact;

/**
 * The message of a position in the groups of instant messages (since 2026-11-24), html of Telegram: the header in bold
 * with the link to the resume form, the summary of the position (title, area, seniorities, regions or home office and the
 * contacts; the salary ranges stay out) and the footer in bold with the link to the positions of the recruiter. The texts
 * are system messages ({@link VisRecruiterResumesTexts}); what the recruiter typed is escaped. Up to
 * {@value VisBusinessPublishPositionsInTheGroups#TELEGRAM_LIMIT} characters it is one piece; longer, two: the header and
 * the summary (cut at the limit), then the footer, sent as an answer to the first.
 */
public class VisBusinessPositionPublishingMessage implements CcpBusiness {

	/** The output of this business. */
	public static enum Fields implements CcpJsonFieldName {
		/** The pieces of the message, each one a message of its own. */
		pieces,
	}

	/** The texts of this class. */
	static enum Constants implements CcpJsonFieldName {
		HOME_OFFICE_DDD { public String getValue() { return "10"; } },
		LINE_BREAK { public String getValue() { return "\n"; } },
		PARAGRAPH { public String getValue() { return "\n\n"; } },
		COMMA { public String getValue() { return ", "; } },
		AMPERSAND { public String getValue() { return "&"; } },
		AMPERSAND_ESCAPED { public String getValue() { return "&amp;"; } },
		LESS_THAN { public String getValue() { return "<"; } },
		LESS_THAN_ESCAPED { public String getValue() { return "&lt;"; } },
		GREATER_THAN { public String getValue() { return ">"; } },
		GREATER_THAN_ESCAPED { public String getValue() { return "&gt;"; } },
		DECIMAL_ZERO { public String getValue() { return ".0"; } },
	}

	/** The language of the groups of positions. */
	static final JnLanguage LANGUAGE = JnLanguage.portuguese;

	/** The single instance: the class keeps no state. */
	public static final VisBusinessPositionPublishingMessage INSTANCE = new VisBusinessPositionPublishingMessage();

	private VisBusinessPositionPublishingMessage() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation position) {
		CcpJsonRepresentation linkAddresses = VisBusinessSendResumesToRecruiters.getLinkAddresses();
		String frontendUrl = linkAddresses.getAsString(VisRecruiterResumesFields.frontendUrl);
		CcpJsonRepresentation linkValues = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.frontendUrl, frontendUrl);
		String header = VisRecruiterResumesTexts.positionPublishingHeader.getMessage(LANGUAGE, linkValues);
		String footer = VisRecruiterResumesTexts.positionPublishingFooter.getMessage(LANGUAGE, linkValues);
		String summary = getSummary(position);
		String paragraph = Constants.PARAGRAPH.getValue();
		String headerAndSummary = header + paragraph + summary;
		String whole = headerAndSummary + paragraph + footer;
		int limit = VisBusinessPublishPositionsInTheGroups.TELEGRAM_LIMIT;
		boolean fitsInOne = whole.length() <= limit;
		if(fitsInOne) {
			return CcpOtherConstants.EMPTY_JSON.put(Fields.pieces, List.of(whole));
		}
		int firstPieceLength = Math.min(limit, headerAndSummary.length());
		String firstPiece = headerAndSummary.substring(0, firstPieceLength);
		CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON.put(Fields.pieces, List.of(firstPiece, footer));
		return response;
	}

	private static String getSummary(CcpJsonRepresentation position) {
		List<String> lines = new ArrayList<>();
		String title = position.getAsString(VisEntityPosition.Fields.title);
		String escapedTitle = escape(title);
		String titleLine = getLine(VisRecruiterResumesTexts.positionPublishingTitle, escapedTitle);
		lines.add(titleLine);
		Double resumeTypeNumber = position.getAsDoubleNumber(VisEntityPosition.Fields.resumeType);
		int resumeType = resumeTypeNumber.intValue();
		VisRecruiterResumesTexts resumeTypeText = VisRecruiterResumesTexts.of(VisRecruiterResumesTexts.Prefixes.resumeType_, resumeType);
		String area = resumeTypeText.getMessage(LANGUAGE);
		String areaLine = getLine(VisRecruiterResumesTexts.positionPublishingArea, area);
		lines.add(areaLine);
		String seniorities = getSeniorities(position);
		String seniorityLine = getLine(VisRecruiterResumesTexts.positionPublishingSeniority, seniorities);
		lines.add(seniorityLine);
		String regions = getRegions(position);
		String regionsLine = getLine(VisRecruiterResumesTexts.positionPublishingRegions, regions);
		lines.add(regionsLine);
		String contactsTitle = VisRecruiterResumesTexts.positionPublishingContacts.getMessage(LANGUAGE);
		lines.add(contactsTitle);
		List<CcpJsonRepresentation> contacts = position.getAsJsonList(VisEntityPosition.Fields.contact);
		for (CcpJsonRepresentation contact : contacts) {
			String type = contact.getAsString(VisJsonFieldsPositionContact.type);
			String value = contact.getAsString(VisJsonFieldsPositionContact.value);
			String escapedValue = escape(value);
			VisRecruiterResumesTexts contactText = VisRecruiterResumesTexts.of(VisRecruiterResumesTexts.Prefixes.positionPublishingContact_, type);
			String contactLine = getLine(contactText, escapedValue);
			lines.add(contactLine);
		}
		String lineBreak = Constants.LINE_BREAK.getValue();
		String summary = String.join(lineBreak, lines);
		return summary;
	}

	// the positions saved before 2026-11-23 keep a single seniority as a text
	private static String getSeniorities(CcpJsonRepresentation position) {
		Object seniority = position.getOrDefault(VisEntityPosition.Fields.seniority, () -> List.of());
		boolean singleSeniority = seniority instanceof String;
		List<String> seniorities = singleSeniority ? List.of((String) seniority) : position.getAsStringList(VisEntityPosition.Fields.seniority);
		List<String> names = new ArrayList<>();
		for (String seniorityName : seniorities) {
			VisRecruiterResumesTexts seniorityText = VisRecruiterResumesTexts.of(VisRecruiterResumesTexts.Prefixes.seniority_, seniorityName);
			String name = seniorityText.getMessage(LANGUAGE);
			names.add(name);
		}
		String comma = Constants.COMMA.getValue();
		String joined = String.join(comma, names);
		return joined;
	}

	// the ddds of the position; the home office among them comes first, by its name
	private static String getRegions(CcpJsonRepresentation position) {
		List<String> ddds = position.getAsStringList(VisEntityPosition.Fields.ddd);
		String decimalZero = Constants.DECIMAL_ZERO.getValue();
		String homeOfficeDdd = Constants.HOME_OFFICE_DDD.getValue();
		List<String> regions = new ArrayList<>();
		List<String> otherDdds = new ArrayList<>();
		for (String ddd : ddds) {
			String wholeDdd = ddd.replace(decimalZero, "");
			boolean homeOffice = homeOfficeDdd.equals(wholeDdd);
			if(homeOffice) {
				String homeOfficeName = VisRecruiterResumesTexts.positionPublishingHomeOffice.getMessage(LANGUAGE);
				regions.add(homeOfficeName);
				continue;
			}
			otherDdds.add(wholeDdd);
		}
		regions.addAll(otherDdds);
		String comma = Constants.COMMA.getValue();
		String joined = String.join(comma, regions);
		return joined;
	}

	private static String getLine(VisRecruiterResumesTexts text, String value) {
		CcpJsonRepresentation values = CcpOtherConstants.EMPTY_JSON.put(VisJsonFieldsPositionContact.value, value);
		String line = text.getMessage(LANGUAGE, values);
		return line;
	}

	// what the recruiter typed cannot open a tag in the html of Telegram
	private static String escape(String text) {
		String withoutAmpersand = text.replace(Constants.AMPERSAND.getValue(), Constants.AMPERSAND_ESCAPED.getValue());
		String withoutLessThan = withoutAmpersand.replace(Constants.LESS_THAN.getValue(), Constants.LESS_THAN_ESCAPED.getValue());
		String escaped = withoutLessThan.replace(Constants.GREATER_THAN.getValue(), Constants.GREATER_THAN_ESCAPED.getValue());
		return escaped;
	}
}
