package com.vis.business.skill;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntitySkill;
import com.vis.entities.VisEntitySkillApproved;

/**
 * Callback of the transfer of a {@code VisEntitySkillPending} suggestion to {@link VisEntitySkillApproved},
 * executed after the transfer has actually happened, that is, when the support bot operator approves the skill.
 * Puts the skill in {@link VisEntitySkill}: a new skill gets the suggested synonyms and the last {@code ranking} (the
 * number of skills plus one, since the ranking goes from the most to the least frequent in the resumes); a skill that
 * is already there (approved meanwhile from another candidate's suggestion) only gets the synonyms it did not have.
 *
 * <p>Saving the skill also puts it in the groups of the initials of the skill and of each synonym
 * ({@link VisSkillWordsByInitials}), the lookup the reading of skills from a resume uses: the decorator of
 * {@code @CcpEntityGrouperChild} of {@link VisEntitySkill} does it. Without it the approved skill would never be found
 * in a resume, and a new suggestion of one of its synonyms would not be refused.
 *
 * <p>Runs in the review of the support bot, never in an online request, so counting the skills is allowed.
 */
public class VisBusinessSkillSuggestionApply implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessSkillSuggestionApply INSTANCE = new VisBusinessSkillSuggestionApply();

	private VisBusinessSkillSuggestionApply() {}

	/**
	 * Runs the business described in the class documentation.
	 * @param json the approved suggestion
	 * @return the same JSON
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		String skill = json.getAsString(VisEntitySkillApproved.Fields.skill);
		List<String> suggestedSynonyms = json.getAsStringList(VisEntitySkillApproved.Fields.synonym);
		this.saveSkill(skill, suggestedSynonyms);

		return json;
	}

	/**
	 * Creates the skill in {@link VisEntitySkill}, or adds the synonyms it did not have.
	 * @param skill the skill
	 * @param suggestedSynonyms the synonyms suggested with it
	 */
	private void saveSkill(String skill, List<String> suggestedSynonyms) {

		CcpJsonRepresentation skillKey = CcpOtherConstants.EMPTY_JSON.put(VisEntitySkill.Fields.skill, skill);

		CcpBusiness createTheSkill = new VisBusinessCreateApprovedSkill(skillKey, suggestedSynonyms);

		CcpBusiness addTheMissingSynonyms = new VisBusinessAddMissingSynonymsToSkill(suggestedSynonyms);

		// a single read by id decides the branch and brings the stored skill
		new CcpGetEntityId(skillKey)
		.toBeginProcedureAnd()
			.ifThisIdIsNotPresentInEntity(VisEntitySkill.ENTITY).executeAction(createTheSkill).and()
			.ifThisIdIsPresentInEntity(VisEntitySkill.ENTITY).executeAction(addTheMissingSynonyms)
			.andFinallyReturningTheseFields(JsonFieldNames.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;
	}

	/** The procedure answers nothing: the business returns the json it received. */
	enum JsonFieldNames implements CcpJsonFieldName{
		/** The {@code inexistentField} field. */
		inexistentField
	}

}
