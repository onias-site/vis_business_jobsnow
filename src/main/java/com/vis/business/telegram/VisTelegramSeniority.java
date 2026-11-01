package com.vis.business.telegram;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ccp.decorators.CcpJsonFieldName;
import com.vis.json.transformers.VisSeniorityType;

/**
 * The seniority of a position of Telegram: the first one its text mentions, or {@link VisSeniorityType#unknown}.
 */
public final class VisTelegramSeniority {

	/** The ways of mentioning each seniority, in a normalized text. */
	enum Constants implements CcpJsonFieldName {
		/** The internships. */
		INTERNSHIP { public String getValue() { return "\\b(?:ESTAGIO|ESTAGIARIO|ESTAGIARIA|INTERN|INTERNSHIP|TRAINEE|APRENDIZ)\\b"; } },
		/** The juniors. */
		JUNIOR { public String getValue() { return "\\b(?:JUNIOR|JR)\\b"; } },
		/** The plenos ({@code PL/SQL} is a technology). */
		PLENO { public String getValue() { return "\\b(?:PLENO|PLENA|PL)\\b(?![/ -]*SQL)"; } },
		/** The seniors. */
		SENIOR { public String getValue() { return "\\b(?:SENIOR|SR)\\b"; } },
		/** The specialists. */
		SPECIALIST { public String getValue() { return "\\b(?:ESPECIALISTA|SPECIALIST|STAFF)\\b"; } },
	}

	/** How each seniority is found. */
	private static final Map<VisSeniorityType, Pattern> MENTIONS = getMentions();

	/** Utility class; not instantiable. */
	private VisTelegramSeniority() {}

	/**
	 * How each seniority is found.
	 * @return the mentions, by seniority
	 */
	private static Map<VisSeniorityType, Pattern> getMentions() {
		Map<VisSeniorityType, Pattern> mentions = new LinkedHashMap<>();
		mentions.put(VisSeniorityType.estag, Pattern.compile(Constants.INTERNSHIP.getValue()));
		mentions.put(VisSeniorityType.junior, Pattern.compile(Constants.JUNIOR.getValue()));
		mentions.put(VisSeniorityType.pleno, Pattern.compile(Constants.PLENO.getValue()));
		mentions.put(VisSeniorityType.senior, Pattern.compile(Constants.SENIOR.getValue()));
		mentions.put(VisSeniorityType.especialista, Pattern.compile(Constants.SPECIALIST.getValue()));
		return mentions;
	}

	/**
	 * The seniority of the position.
	 * @param normalizedText the text of the position, as {@link VisTelegramPositionText#normalize} leaves it
	 * @return the seniority mentioned first
	 */
	public static VisSeniorityType find(String normalizedText) {
		VisSeniorityType firstSeniority = VisSeniorityType.unknown;
		int firstMention = Integer.MAX_VALUE;
		Set<VisSeniorityType> seniorities = MENTIONS.keySet();
		for (VisSeniorityType seniority : seniorities) {
			Pattern mentionOfTheSeniority = MENTIONS.get(seniority);
			Matcher mention = mentionOfTheSeniority.matcher(normalizedText);
			boolean notMentionedBefore = false == mention.find() || mention.start() >= firstMention;

			if(notMentionedBefore) {
				continue;
			}

			firstMention = mention.start();
			firstSeniority = seniority;
		}
		return firstSeniority;
	}
}
