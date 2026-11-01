package com.vis.business.telegram;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpStringDecorator;

/**
 * The contact of a position of Telegram, the best one its text has: an e-mail; or a link out of Telegram, the channels of
 * the JobsNow and the social networks; or a link of WhatsApp; or a number told as WhatsApp; or a phone number.
 */
public final class VisTelegramContact {

	/** The texts of the rules. */
	enum Constants implements CcpJsonFieldName {
		/** A link. */
		LINK { public String getValue() { return "(?i)\\b(?:https?://|www\\.)[^\\s<>\"']{1,1900}"; } },
		/** The links that are not the contact of a position. */
		IGNORED_LINK { public String getValue() { return "(?i)t\\.me/|telegram|ccpjobsnow|youtube|youtu\\.be|instagram|facebook|twitter"; } },
		/** The links of WhatsApp. */
		WHATSAPP_LINK { public String getValue() { return "(?i)wa\\.me/|api\\.whatsapp|chat\\.whatsapp"; } },
		/** A number right after the mention to WhatsApp. */
		NUMBER_AFTER_WHATSAPP { public String getValue() { return "(?i)whats[^\\d]{0,20}(\\+?\\d[\\d\\s().-]{8,17}\\d)"; } },
		/** A number right before the mention to WhatsApp. */
		NUMBER_BEFORE_WHATSAPP { public String getValue() { return "(?i)(\\+?\\d[\\d\\s().-]{8,17}\\d)[^\\d]{0,20}whats"; } },
		/** A phone number with its DDD. */
		PHONE { public String getValue() { return "\\(?\\b\\d{2}\\)?[\\s.-]?9?\\d{4}[\\s.-]?\\d{4}\\b"; } },
		/** What takes the place of a link. */
		SPACE { public String getValue() { return " "; } },
		/** What is not a digit. */
		NOT_A_DIGIT { public String getValue() { return "\\D"; } },
		/** What ends a link by accident, as the punctuation of the phrase. */
		END_OF_THE_PHRASE { public String getValue() { return "[.,;:!?)\\]}]+$"; } },
	}

	/** Finds the links. */
	private static final Pattern LINK = Pattern.compile(Constants.LINK.getValue());

	/** Finds the links that do not count. */
	private static final Pattern IGNORED_LINK = Pattern.compile(Constants.IGNORED_LINK.getValue());

	/** Finds the links of WhatsApp. */
	private static final Pattern WHATSAPP_LINK = Pattern.compile(Constants.WHATSAPP_LINK.getValue());

	/** Finds the number after the mention to WhatsApp. */
	private static final Pattern NUMBER_AFTER_WHATSAPP = Pattern.compile(Constants.NUMBER_AFTER_WHATSAPP.getValue());

	/** Finds the number before the mention to WhatsApp. */
	private static final Pattern NUMBER_BEFORE_WHATSAPP = Pattern.compile(Constants.NUMBER_BEFORE_WHATSAPP.getValue());

	/** Finds the phone numbers. */
	private static final Pattern PHONE = Pattern.compile(Constants.PHONE.getValue());

	/** Utility class; not instantiable. */
	private VisTelegramContact() {}

	/**
	 * The contact of the position.
	 * @param text the text of the position
	 * @return the contact, empty when the text has none
	 */
	public static String find(String text) {
		CcpStringDecorator textDecorator = new CcpStringDecorator(text);
		CcpEmailDecorator emailsOfTheText = textDecorator.email();
		Set<String> emails = emailsOfTheText.findAllInText();
		boolean hasAnEmail = false == emails.isEmpty();

		if(hasAnEmail) {
			return emails.iterator().next();
		}

		String whatsAppLink = "";
		Matcher links = LINK.matcher(text);
		while (links.find()) {
			String foundLink = links.group();
			String link = foundLink.replaceAll(Constants.END_OF_THE_PHRASE.getValue(), "");
			Matcher ignored = IGNORED_LINK.matcher(link);
			boolean ignoredLink = ignored.find();

			if(ignoredLink) {
				continue;
			}

			Matcher whatsApp = WHATSAPP_LINK.matcher(link);
			boolean notFromWhatsApp = false == whatsApp.find();

			if(notFromWhatsApp) {
				return link;
			}

			whatsAppLink = whatsAppLink.isEmpty() ? link : whatsAppLink;
		}

		boolean hasAWhatsAppLink = false == whatsAppLink.isEmpty();

		if(hasAWhatsAppLink) {
			return whatsAppLink;
		}

		String number = findNumber(text);
		return number;
	}

	/**
	 * The text with no links, whose addresses say nothing about the skills of the position.
	 * @param text the text of the position
	 * @return the text without its links
	 */
	public static String removeLinks(String text) {
		Matcher links = LINK.matcher(text);
		String withoutLinks = links.replaceAll(Constants.SPACE.getValue());
		return withoutLinks;
	}

	/**
	 * The number told as WhatsApp, or else a phone number, only its digits so that its formats are the same contact.
	 * @param text the text of the position
	 * @return the digits, empty when the text has no number
	 */
	private static String findNumber(String text) {
		Matcher numberAfter = NUMBER_AFTER_WHATSAPP.matcher(text);

		if(numberAfter.find()) {
			return numberAfter.group(1).replaceAll(Constants.NOT_A_DIGIT.getValue(), "");
		}

		Matcher numberBefore = NUMBER_BEFORE_WHATSAPP.matcher(text);

		if(numberBefore.find()) {
			return numberBefore.group(1).replaceAll(Constants.NOT_A_DIGIT.getValue(), "");
		}

		Matcher phone = PHONE.matcher(text);

		if(phone.find()) {
			return phone.group().replaceAll(Constants.NOT_A_DIGIT.getValue(), "");
		}

		return "";
	}
}
