package com.vis.business.telegram;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFileDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.json.fields.validation.CcpJsonCommonsFields;
import com.jn.entities.decorators.engine.JnEntityGrouperParents;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityStatisSkillLineChart;
import com.vis.entities.VisEntityTelegramPosition;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.transformers.VisSeniorityType;
import com.vis.services.VisSkillsInText;

/**
 * Turns a message of Telegram into a position of {@link VisEntityTelegramPosition}, ready for the bulk (with its
 * {@code disposablePeriod} and the ids of its groups). A message with no contact or no known skill is no position.
 * The words of {@code documentation/vis/telegram/words-that-are-not-skills.json} are common words in these texts that
 * would mean a skill ({@code TEMPO}, from "tempo integral"), so they go in the {@code excludedSkill} of the reading of
 * the skills. An instance is immutable, so the threads of the load share it.
 */
public class VisBusinessReadTelegramPosition implements CcpBusiness {

	/** The texts of the reading. */
	enum Constants implements CcpJsonFieldName {
		/** The list of the words that are not skills, by its path from any module of the workspace. */
		EXCLUDED_WORDS_PATH { public String getValue() { return "../ccp_rest-api-tests_jobsnow/documentation/vis/telegram/words-that-are-not-skills.json"; } },
	}

	/** The words that are not skills in these texts. */
	private final CcpJsonRepresentation excludedWords;

	/** Gives the words of the skills. */
	private final Function<Collection<String>, List<CcpJsonRepresentation>> skillWords;

	/** Tells the region of the position. */
	private final VisTelegramDdd regions;

	/** The groups of the position. */
	private final JnEntityGrouperParents lineChartParents;

	/**
	 * Takes what the reading needs.
	 * @param skillWords gives the words of the skills, by the words of the text
	 * @param regions tells the region of the position
	 */
	public VisBusinessReadTelegramPosition(Function<Collection<String>, List<CcpJsonRepresentation>> skillWords, VisTelegramDdd regions) {
		String excludedWordsPath = Constants.EXCLUDED_WORDS_PATH.getValue();
		CcpStringDecorator excludedWordsPathDecorator = new CcpStringDecorator(excludedWordsPath);
		CcpFileDecorator excludedWordsFile = excludedWordsPathDecorator.file();
		this.excludedWords = excludedWordsFile.asSingleJson();
		this.skillWords = skillWords;
		this.regions = regions;
		this.lineChartParents = new JnEntityGrouperParents(VisEntityStatisSkillLineChart.class, VisEntityTelegramPosition.class);
	}

	/**
	 * Reads the position of the message.
	 * @param json the message, as {@link VisTelegramMessages#read} gives it; with a {@code ddd}, its region is that one
	 * @return the position; or a JSON with only {@code noContact} or only {@code noSkill} when the message is not a position
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String textOfTheMessage = json.getAsString(CcpJsonCommonsFields.text);
		String text = VisTelegramPositionText.cut(textOfTheMessage);
		String contact = VisTelegramContact.find(text);
		boolean noContact = contact.isEmpty();

		if(noContact) {
			return CcpOtherConstants.EMPTY_JSON.put(VisTelegramLoadFields.noContact, 1);
		}

		String skills = this.getSkills(text);
		boolean noSkill = skills.isEmpty();

		if(noSkill) {
			return CcpOtherConstants.EMPTY_JSON.put(VisTelegramLoadFields.noSkill, 1);
		}

		String normalizedText = VisTelegramPositionText.normalize(text);
		VisSeniorityType seniority = VisTelegramSeniority.find(normalizedText);
		boolean regionAlreadyKnown = json.containsAllFields(VisEntityTelegramPosition.Fields.ddd);
		int ddd = regionAlreadyKnown ? json.getAsIntegerNumber(VisEntityTelegramPosition.Fields.ddd) : this.regions.find(normalizedText);
		String period = json.getAsString(JnJsonCommonsFields.disposablePeriod);
		String yearMonth = json.getAsString(VisEntityTelegramPosition.Fields.yearMonth);

		CcpJsonRepresentation positionWithContact = CcpOtherConstants.EMPTY_JSON.put(VisEntityTelegramPosition.Fields.contact, contact);
		CcpJsonRepresentation positionWithSkills = positionWithContact.put(VisEntityTelegramPosition.Fields.skill, skills);
		CcpJsonRepresentation positionWithSeniority = positionWithSkills.put(VisEntityTelegramPosition.Fields.seniority, seniority.name());
		CcpJsonRepresentation positionWithDdd = positionWithSeniority.put(VisEntityTelegramPosition.Fields.ddd, ddd);
		CcpJsonRepresentation positionWithYearMonth = positionWithDdd.put(VisEntityTelegramPosition.Fields.yearMonth, yearMonth);
		CcpJsonRepresentation positionWithPeriod = positionWithYearMonth.put(JnJsonCommonsFields.disposablePeriod, period);
		Map<String, CcpJsonRepresentation> parents = this.lineChartParents.getParents(positionWithPeriod);
		CcpJsonRepresentation position = this.lineChartParents.putParentIds(positionWithPeriod, parents);
		return position;
	}

	/**
	 * The skills of the text, as the {@code skill} field keeps them.
	 * @param text the text of the position
	 * @return the skills in upper case, with no repetition, in alphabetical order and joined; empty when there are none
	 */
	private String getSkills(String text) {
		String textWithoutLinks = VisTelegramContact.removeLinks(text);
		CcpJsonRepresentation textToSearch = this.excludedWords.put(CcpJsonCommonsFields.text, textWithoutLinks);
		CcpJsonRepresentation skillsInTheText = VisSkillsInText.find(textToSearch, this.skillWords);
		List<CcpJsonRepresentation> skills = skillsInTheText.getAsJsonList(VisJsonCommonsFields.skill);
		Set<String> skillNames = new TreeSet<>();
		for (CcpJsonRepresentation skill : skills) {
			String skillName = skill.getAsString(VisJsonCommonsFields.skill);
			String upperCaseSkillName = skillName.toUpperCase();
			skillNames.add(upperCaseSkillName);
		}
		String skillsSeparator = VisEntityTelegramPosition.Constants.SKILLS_SEPARATOR.getValue();
		String joinedSkills = String.join(skillsSeparator, skillNames);
		return joinedSkills;
	}
}
