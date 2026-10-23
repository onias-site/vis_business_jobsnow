package com.vis.business.skill;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntityOperationType;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntitySkillPending;
import com.vis.entities.VisEntitySkillSuggestionDailyLimit;
import com.vis.entities.VisEntitySkillSynonym;

/**
 * Action of {@code VisServiceSkillSuggestion.SuggestSkill} once every check passed: saves the suggestion as pending,
 * records each of its synonyms in {@link VisEntitySkillSynonym} (pointing to the suggested skill, in a single bulk) and
 * spends the day of the candidate in {@link VisEntitySkillSuggestionDailyLimit}.
 */
public class VisBusinessSaveSkillSuggestionWithSynonyms implements CcpBusiness {

	/** The suggestion, as it came. */
	private final CcpJsonRepresentation json;

	/**
	 * Keeps the suggestion.
	 * @param json the suggestion, as it came
	 */
	public VisBusinessSaveSkillSuggestionWithSynonyms(CcpJsonRepresentation json) {
		this.json = json;
	}

	/**
	 * Saves the suggestion, its synonyms and the daily limit.
	 * @param suggestion the json of the procedure
	 * @return the result of saving the suggestion
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation suggestion) {
		CcpBusiness saveAsPending = CcpEntityOperationType.save.getOperationCallback(VisEntitySkillPending.ENTITY);
		CcpJsonRepresentation savedSuggestion = saveAsPending.execute(suggestion);
		this.saveSynonyms();
		// written here, and not by the pending entity, because that one writes through the messaging: the next
		// request of the candidate must already find the day spent
		String email = this.json.getAsString(VisEntitySkillPending.Fields.email);
		CcpJsonRepresentation dailyLimit = CcpOtherConstants.EMPTY_JSON.put(VisEntitySkillSuggestionDailyLimit.Fields.email, email);
		VisEntitySkillSuggestionDailyLimit.ENTITY.save(dailyLimit);
		return savedSuggestion;
	}

	/**
	 * Records each synonym of the suggestion in {@link VisEntitySkillSynonym}, pointing to the suggested skill, in a
	 * single bulk. A suggestion without synonyms writes nothing.
	 */
	private void saveSynonyms() {
		String skill = this.json.getAsString(VisEntitySkillPending.Fields.skill);
		List<String> synonyms = this.json.getAsStringList(VisEntitySkillPending.Fields.synonym);
		List<CcpBulkItem> synonymItems = new ArrayList<>();
		for (String synonym : synonyms) {
			CcpJsonRepresentation synonymWithItsSkill = CcpOtherConstants.EMPTY_JSON
					.put(VisEntitySkillSynonym.Fields.skill, synonym);
			CcpJsonRepresentation synonymRecord = synonymWithItsSkill
					.put(VisEntitySkillSynonym.Fields.synonym, skill);
			List<CcpBulkItem> bulkItems = VisEntitySkillSynonym.ENTITY.toBulkItems(synonymRecord, CcpBulkEntityOperationType.create);
			synonymItems.addAll(bulkItems);
		}
		JnExecuteBulkOperation.INSTANCE.executeBulk(synonymItems, JnDeleteKeysFromCache.INSTANCE);
	}
}
