package com.vis.business.telegram;

import java.util.Comparator;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntitySkill;

/**
 * The order of the ranking of the skills: the most {@code occurrences} first, then the most {@code childOcurrences}, then
 * the current {@code ranking}.
 */
public class VisSkillRankingComparator implements Comparator<CcpJsonRepresentation> {

	/** The single instance. */
	public static final VisSkillRankingComparator INSTANCE = new VisSkillRankingComparator();

	private VisSkillRankingComparator() {}

	/**
	 * Compares two skills.
	 * @param first a skill
	 * @param second another skill
	 * @return the order between them
	 */
	public int compare(CcpJsonRepresentation first, CcpJsonRepresentation second) {
		Integer firstOccurrences = first.getAsIntegerNumber(VisTelegramLoadFields.occurrences);
		Integer secondOccurrences = second.getAsIntegerNumber(VisTelegramLoadFields.occurrences);
		boolean differentOccurrences = false == firstOccurrences.equals(secondOccurrences);

		if(differentOccurrences) {
			return secondOccurrences.compareTo(firstOccurrences);
		}

		Integer firstChildOcurrences = first.getAsIntegerNumber(VisEntitySkill.Fields.childOcurrences);
		Integer secondChildOcurrences = second.getAsIntegerNumber(VisEntitySkill.Fields.childOcurrences);
		boolean differentChildOcurrences = false == firstChildOcurrences.equals(secondChildOcurrences);

		if(differentChildOcurrences) {
			return secondChildOcurrences.compareTo(firstChildOcurrences);
		}

		Integer firstRanking = first.getAsIntegerNumber(VisEntitySkill.Fields.ranking);
		Integer secondRanking = second.getAsIntegerNumber(VisEntitySkill.Fields.ranking);
		return firstRanking.compareTo(secondRanking);
	}
}
