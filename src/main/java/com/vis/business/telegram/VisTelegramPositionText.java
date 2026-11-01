package com.vis.business.telegram;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTextDecorator;

/**
 * The part of a message of Telegram that talks about the position. The channels of the JobsNow put a header (up to
 * {@code #jnVaga}, to {@code [vaga]} or to the line of the warning {@code 2) Se esta vaga}), lines of their own among the
 * text and a footer around the position: what they say (other e-mails, links and technologies) is not about it. The
 * same phrase may be a header or a footer ({@code ENCAMINHE ESTA VAGA}), so the header goes first. The hashtag of the
 * ads comes misspelled in most messages ({@code #jnPopaganda}).
 */
public final class VisTelegramPositionText {

	/** The texts of the channels. */
	enum Constants implements CcpJsonFieldName {
		/** The end of the header. */
		END_OF_THE_HEADER { public String getValue() { return "(?i)#jnVaga|\\[vaga\\]|2\\) Se esta vaga[^\n]*"; } },
		/** The lines of the channels that may come after the header. */
		LINES_OF_THE_CHANNELS {
			public String getValue() {
				return "(?im)^\\s*\\[(?:PUBLIQUE ESTA VAGA|RECRUTADOR, RECEBA|ESTE AN.{1,2}NCIO N.{1,2}O SERVIU)[^\n]*$";
			}
		},
		/** The start of each kind of footer. */
		START_OF_THE_FOOTER {
			public String getValue() {
				return "(?i)#jnDica|#jnPropaganda|#jnPopaganda|\\{textoDoPublicador\\}|Vaga Publicada por|Essa vaga n.{1,2}o serviu"
						+ "|={5,}|ENCAMINHE ESTA VAGA|Divulgue conosco|Inclus.{1,2}o desta vaga|Esta vaga expira em";
			}
		},
		/** The marks that separate words and that the removal of the accents would take out, gluing the words around them. */
		WORD_SEPARATORS { public String getValue() { return "[^\\p{L}\\p{M}\\p{N}\\s.,+'´`#-]"; } },
		/** What stands for a mark that separates words. */
		WORD_SEPARATOR { public String getValue() { return " - "; } },
	}

	/** Finds the end of the header. */
	private static final Pattern END_OF_THE_HEADER = Pattern.compile(Constants.END_OF_THE_HEADER.getValue());

	/** Finds the lines of the channels. */
	private static final Pattern LINES_OF_THE_CHANNELS = Pattern.compile(Constants.LINES_OF_THE_CHANNELS.getValue());

	/** Finds the start of the footer. */
	private static final Pattern START_OF_THE_FOOTER = Pattern.compile(Constants.START_OF_THE_FOOTER.getValue());

	/** Finds the marks that separate words. */
	private static final Pattern WORD_SEPARATORS = Pattern.compile(Constants.WORD_SEPARATORS.getValue());

	/** Utility class; not instantiable. */
	private VisTelegramPositionText() {}

	/**
	 * Takes out the header and the footer.
	 * @param text the text of the message
	 * @return the text of the position, which may come out empty (a message with a footer only)
	 */
	public static String cut(String text) {
		Matcher endOfTheHeader = END_OF_THE_HEADER.matcher(text);
		boolean hasHeader = endOfTheHeader.find();
		int startOfThePosition = hasHeader ? endOfTheHeader.end() : 0;
		String textWithoutHeader = text.substring(startOfThePosition);
		Matcher linesOfTheChannels = LINES_OF_THE_CHANNELS.matcher(textWithoutHeader);
		String textOfThePosition = linesOfTheChannels.replaceAll("");

		Matcher startOfTheFooter = START_OF_THE_FOOTER.matcher(textOfThePosition);
		boolean noFooter = false == startOfTheFooter.find();

		if(noFooter) {
			return textOfThePosition.trim();
		}

		int endOfThePosition = startOfTheFooter.start();
		String withoutFooter = textOfThePosition.substring(0, endOfThePosition);
		return withoutFooter.trim();
	}

	/**
	 * The text as the rules of seniority and region read it: upper case and no accents, with a hyphen in place of the
	 * marks that separate words, so {@code BARUERI/SP} and {@code PL/SR} stay two words each.
	 * @param text the text
	 * @return the normalized text
	 */
	public static String normalize(String text) {
		Matcher wordSeparators = WORD_SEPARATORS.matcher(text);
		String wordSeparator = Constants.WORD_SEPARATOR.getValue();
		String textWithHyphens = wordSeparators.replaceAll(wordSeparator);
		CcpStringDecorator textDecorator = new CcpStringDecorator(textWithHyphens);
		CcpTextDecorator textOfTheDecorator = textDecorator.text();
		CcpTextDecorator withoutAccents = textOfTheDecorator.stripAccents();
		return withoutAccents.content.toUpperCase();
	}
}
