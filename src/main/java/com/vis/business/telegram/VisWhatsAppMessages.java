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
import com.vis.json.fields.validation.VisJsonSkillStatisticsFields;

/**
 * Reads the messages of a text file exported from a group of WhatsApp, whose positions enter the same load of the ones of
 * Telegram. Each message starts in a line {@code dd/MM/yyyy HH:mm - author: text} and goes on until the next one; a line
 * with no author is a notice of the group and is left out. The author is left out too: their name or phone is not
 * the contact of the position. Every position of these groups is remote, so each message comes with the {@code ddd} of
 * home office and no rule of region runs over it.
 */
public final class VisWhatsAppMessages {

	/** The texts of the export. */
	enum Constants implements CcpJsonFieldName {
		/** The start of a message: day, month and year. */
		START_OF_THE_MESSAGE { public String getValue() { return "(?m)^(\\d{2})/(\\d{2})/(\\d{4}) \\d{2}:\\d{2} - "; } },
		/** What ends the author, in the first line of a message. */
		END_OF_THE_AUTHOR { public String getValue() { return ": "; } },
		/** The end of a line. */
		NEW_LINE { public String getValue() { return "\n"; } },
		/** The invisible mark of direction that the export puts in the texts. */
		DIRECTION_MARK { public String getValue() { return "\u200e"; } },
	}

	/** Finds the start of each message. */
	private static final Pattern START_OF_THE_MESSAGE = Pattern.compile(Constants.START_OF_THE_MESSAGE.getValue());

	/** Utility class; not instantiable. */
	private VisWhatsAppMessages() {}

	/**
	 * The messages of the file that have an author and a text.
	 * @param content the content of the file
	 * @return each message: {@code text}, {@code disposablePeriod} (the day of the message, {@code ddMMyyyy}),
	 * {@code yearMonth} ({@code yyyyMM}) and {@code ddd} (home office)
	 */
	public static List<CcpJsonRepresentation> read(String content) {
		List<CcpJsonRepresentation> messages = new ArrayList<>();
		Matcher start = START_OF_THE_MESSAGE.matcher(content);
		boolean hasAMessage = start.find();
		while (hasAMessage) {
			String day = start.group(1);
			String month = start.group(2);
			String year = start.group(3);
			int startOfTheMessage = start.end();
			hasAMessage = start.find();
			int endOfTheMessage = hasAMessage ? start.start() : content.length();
			String message = content.substring(startOfTheMessage, endOfTheMessage);
			String text = getTextWithoutAuthor(message);
			boolean noText = text.isEmpty();

			if(noText) {
				continue;
			}

			CcpJsonRepresentation messageWithText = CcpOtherConstants.EMPTY_JSON.put(CcpJsonCommonsFields.text, text);
			CcpJsonRepresentation messageWithPeriod = messageWithText.put(JnJsonCommonsFields.disposablePeriod, day + month + year);
			CcpJsonRepresentation messageWithYearMonth = messageWithPeriod.put(VisEntityTelegramPosition.Fields.yearMonth, year + month);
			CcpJsonRepresentation messageWithRegion = messageWithYearMonth.put(VisEntityTelegramPosition.Fields.ddd, VisJsonSkillStatisticsFields.HOME_OFFICE);
			messages.add(messageWithRegion);
		}
		return messages;
	}

	/**
	 * The text of a message, with no author.
	 * @param message the message, from the end of its date on
	 * @return the text, empty for a notice of the group
	 */
	private static String getTextWithoutAuthor(String message) {
		String newLine = Constants.NEW_LINE.getValue();
		int foundEndOfTheLine = message.indexOf(newLine);
		int endOfTheFirstLine = foundEndOfTheLine < 0 ? message.length() : foundEndOfTheLine;
		String endOfTheAuthorMark = Constants.END_OF_THE_AUTHOR.getValue();
		int endOfTheAuthor = message.indexOf(endOfTheAuthorMark);
		boolean noAuthor = endOfTheAuthor < 0 || endOfTheAuthor >= endOfTheFirstLine;

		if(noAuthor) {
			return "";
		}

		int startOfTheText = endOfTheAuthor + endOfTheAuthorMark.length();
		String textWithMarks = message.substring(startOfTheText);
		String text = textWithMarks.replace(Constants.DIRECTION_MARK.getValue(), "");
		return text.trim();
	}
}
