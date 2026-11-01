package com.vis.business.telegram;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.json.fields.validation.CcpJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityTelegramPosition;

/**
 * Reads the messages of an HTML file exported from Telegram. Each message starts at its date, printed in the
 * {@code title} of a {@code div}, and has its text (when it has one) in the {@code div} of the class {@code text} that
 * comes before the next date. The date is the one printed, whatever the time zone: it is the day the channel saw.
 */
public final class VisTelegramMessages {

	/** The texts of the export. */
	enum Constants implements CcpJsonFieldName {
		/** The date of a message: day, month and year. */
		DATE_OF_THE_MESSAGE { public String getValue() { return "<div class=\"pull_right date details\" title=\"(\\d\\d)\\.(\\d\\d)\\.(\\d{4}) "; } },
		/** Where the text of a message starts. */
		TEXT_START { public String getValue() { return "<div class=\"text\">"; } },
		/** Where the text of a message ends. */
		TEXT_END { public String getValue() { return "</div>"; } },
		/** A line break. */
		LINE_BREAK { public String getValue() { return "<br\\s*/?>"; } },
		/** A link, whose address the text keeps (the contact of the position may be only there). */
		LINK { public String getValue() { return "<a [^>]*href=\"(https?:[^\"]*)\"[^>]*>"; } },
		/** The link kept in the text. */
		LINK_REPLACEMENT { public String getValue() { return " $1 "; } },
		/** Any other tag. */
		TAG { public String getValue() { return "<[^>]+>"; } },
		/** What separates the words of the text. */
		SPACE { public String getValue() { return " "; } },
		/** The end of a line. */
		NEW_LINE { public String getValue() { return "\n"; } },
		/** An escaped ampersand. */
		ESCAPED_AMPERSAND { public String getValue() { return "&amp;"; } },
		/** An ampersand. */
		AMPERSAND { public String getValue() { return "&"; } },
		/** An escaped quote. */
		ESCAPED_QUOTE { public String getValue() { return "&quot;"; } },
		/** A quote. */
		QUOTE { public String getValue() { return "\""; } },
		/** An escaped apostrophe. */
		ESCAPED_APOSTROPHE { public String getValue() { return "&#39;"; } },
		/** An apostrophe. */
		APOSTROPHE { public String getValue() { return "'"; } },
		/** An escaped less than. */
		ESCAPED_LESS_THAN { public String getValue() { return "&lt;"; } },
		/** A less than. */
		LESS_THAN { public String getValue() { return "<"; } },
		/** An escaped greater than. */
		ESCAPED_GREATER_THAN { public String getValue() { return "&gt;"; } },
		/** A greater than. */
		GREATER_THAN { public String getValue() { return ">"; } },
	}

	/** Finds the date of each message. */
	private static final Pattern DATE_OF_THE_MESSAGE = Pattern.compile(Constants.DATE_OF_THE_MESSAGE.getValue());

	/** Finds the line breaks. */
	private static final Pattern LINE_BREAK = Pattern.compile(Constants.LINE_BREAK.getValue());

	/** Finds the links. */
	private static final Pattern LINK = Pattern.compile(Constants.LINK.getValue());

	/** Finds the tags. */
	private static final Pattern TAG = Pattern.compile(Constants.TAG.getValue());

	/** Utility class; not instantiable. */
	private VisTelegramMessages() {}

	/**
	 * The messages of the file that have a text.
	 * @param html the content of the file
	 * @return each message: {@code text}, {@code disposablePeriod} (the day of the message, {@code ddMMyyyy}) and
	 * {@code yearMonth} ({@code yyyyMM})
	 */
	public static List<CcpJsonRepresentation> read(String html) {
		List<CcpJsonRepresentation> messages = new ArrayList<>();
		Matcher date = DATE_OF_THE_MESSAGE.matcher(html);
		boolean hasADate = date.find();
		while (hasADate) {
			String day = date.group(1);
			String month = date.group(2);
			String year = date.group(3);
			int startOfTheMessage = date.end();
			hasADate = date.find();
			int endOfTheMessage = hasADate ? date.start() : html.length();
			String text = getText(html, startOfTheMessage, endOfTheMessage);
			boolean noText = text.isEmpty();

			if(noText) {
				continue;
			}

			CcpJsonRepresentation messageWithText = CcpOtherConstants.EMPTY_JSON.put(CcpJsonCommonsFields.text, text);
			CcpJsonRepresentation messageWithPeriod = messageWithText.put(JnJsonCommonsFields.disposablePeriod, day + month + year);
			CcpJsonRepresentation message = messageWithPeriod.put(VisEntityTelegramPosition.Fields.yearMonth, year + month);
			messages.add(message);
		}
		return messages;
	}

	/**
	 * The text of a message, with no tags.
	 * @param html the content of the file
	 * @param startOfTheMessage where the message starts
	 * @param endOfTheMessage where the message ends
	 * @return the text, empty when the message has none
	 */
	private static String getText(String html, int startOfTheMessage, int endOfTheMessage) {
		String textStart = Constants.TEXT_START.getValue();
		int foundTextStart = html.indexOf(textStart, startOfTheMessage);
		boolean noText = foundTextStart < 0 || foundTextStart >= endOfTheMessage;

		if(noText) {
			return "";
		}

		int startOfTheText = foundTextStart + textStart.length();
		String textEnd = Constants.TEXT_END.getValue();
		int foundTextEnd = html.indexOf(textEnd, startOfTheText);
		int endOfTheText = foundTextEnd < 0 ? endOfTheMessage : Math.min(foundTextEnd, endOfTheMessage);
		String htmlOfTheText = html.substring(startOfTheText, endOfTheText);
		String text = getTextWithoutTags(htmlOfTheText);
		return text.trim();
	}

	/**
	 * Turns the HTML of a text into plain text.
	 * @param htmlOfTheText the HTML
	 * @return the plain text
	 */
	private static String getTextWithoutTags(String htmlOfTheText) {
		Matcher lineBreaks = LINE_BREAK.matcher(htmlOfTheText);
		String withLines = lineBreaks.replaceAll(Constants.NEW_LINE.getValue());
		Matcher links = LINK.matcher(withLines);
		String withLinks = links.replaceAll(Constants.LINK_REPLACEMENT.getValue());
		Matcher tags = TAG.matcher(withLinks);
		String withoutTags = tags.replaceAll(Constants.SPACE.getValue());
		String withQuotes = withoutTags.replace(Constants.ESCAPED_QUOTE.getValue(), Constants.QUOTE.getValue());
		String withApostrophes = withQuotes.replace(Constants.ESCAPED_APOSTROPHE.getValue(), Constants.APOSTROPHE.getValue());
		String withLessThan = withApostrophes.replace(Constants.ESCAPED_LESS_THAN.getValue(), Constants.LESS_THAN.getValue());
		String withGreaterThan = withLessThan.replace(Constants.ESCAPED_GREATER_THAN.getValue(), Constants.GREATER_THAN.getValue());
		String plainText = withGreaterThan.replace(Constants.ESCAPED_AMPERSAND.getValue(), Constants.AMPERSAND.getValue());
		return plainText;
	}
}
