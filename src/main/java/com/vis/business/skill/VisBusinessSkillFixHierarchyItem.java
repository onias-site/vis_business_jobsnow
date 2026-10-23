package com.vis.business.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntitySkill;
import com.vis.entities.VisEntitySkillFixHierarchyItemApproved;
import com.vis.json.fields.validation.VisSkillFixHierarchyTypes;

/**
 * Callback of the transfer of a {@code VisEntitySkillFixHierarchyItemPending} record to
 * {@code VisEntitySkillFixHierarchyItemApproved}, executed after the transfer has actually happened, that is,
 * when the support bot operator approves the item. Applies the approved item to {@link VisEntitySkill}: in the
 * record of its {@code skill}, adds its {@code parent} to the {@code parent} array (association, {@code add}) or
 * removes it from there (dissociation, {@code remove}).
 *
 * <p>Applies the same change to the items of the skill in the lookup the resume screen reads
 * ({@link VisSkillWordsGroups}), in the groups of the skill and of each synonym: up to 2026-10-08 only
 * {@link VisEntitySkill} changed, and the implicit knowledge of the resume screen never showed an approved fix.
 *
 * <p>A skill that has no {@link VisEntitySkill} record is not created there, because the record cannot be created
 * without its {@code ranking}; its own word in the lookup still changes.
 */
public class VisBusinessSkillFixHierarchyItem implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisBusinessSkillFixHierarchyItem INSTANCE = new VisBusinessSkillFixHierarchyItem();

	private VisBusinessSkillFixHierarchyItem() {}

	/**
	 * Runs the business described in the class documentation.
	 * @param json the input
	 * @return the result
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		String skill = json.getAsString(VisEntitySkillFixHierarchyItemApproved.Fields.skill);
		String parent = json.getAsString(VisEntitySkillFixHierarchyItemApproved.Fields.parent);
		VisSkillFixHierarchyTypes type = json.getAsEnum(VisEntitySkillFixHierarchyItemApproved.Fields.type, VisSkillFixHierarchyTypes.class);
		Consumer<Set<String>> changeParents = parents -> type.execute(parents, parent);

		List<String> words = new ArrayList<>();
		words.add(skill);

		CcpJsonRepresentation skillKey = CcpOtherConstants.EMPTY_JSON.put(VisEntitySkill.Fields.skill, skill);

		CcpBusiness changeOnlyTheLookup = new VisBusinessChangeOnlyTheSkillLookup(skill, words, changeParents);

		CcpBusiness changeTheSkillAndTheLookup = new VisBusinessChangeTheSkillAndTheLookup(skill, words, changeParents);

		// a single read by id decides the branch and brings the stored skill
		new CcpGetEntityId(skillKey)
		.toBeginProcedureAnd()
			.ifThisIdIsNotPresentInEntity(VisEntitySkill.ENTITY).executeAction(changeOnlyTheLookup).and()
			.ifThisIdIsPresentInEntity(VisEntitySkill.ENTITY).executeAction(changeTheSkillAndTheLookup)
			.andFinallyReturningTheseFields(JsonFieldNames.inexistentField)
		.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
		;

		return json;
	}

	/** The procedure answers nothing: the business returns the json it received. */
	enum JsonFieldNames implements CcpJsonFieldName{
		/** The {@code inexistentField} field. */
		inexistentField
	}

}
