package com.vis.entities;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;

/**
 * Virtual entity whose id identifies the groups of {@code JnEntityGrouper} that count the suggestions of the candidates
 * by status ({@code pending}, {@code approved}, {@code rejected}, {@code VisSuggestionStatuses}): the charts of the
 * screen Curiosities about the suggestions (since 2026-11-20). Its only field names the counting:
 * <ul>
 * <li>{@code skillSuggestion}: the suggestions of a new skill, child {@code vis_skill_pending};</li>
 * <li>{@code add} and {@code remove}: the items of the suggestions of association and of dissociation of the hierarchy of
 * the skills ({@code type} of the item), child {@code vis_skill_fix_hierarchy_item_pending}.</li>
 * </ul>
 * The counters are corrected by the side effects of the writes and transfers of those entities
 * ({@code VisSuggestionStatusCounters}). Cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualSuggestionStatusCounter.Fields.class)
public class VisEntityVirtualSuggestionStatusCounter {

	/** The entity {@code vis_virtual_suggestion_status_counter}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualSuggestionStatusCounter.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code counter} field: the primary key, the name of the counting. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 50)
		counter,
		;
	}

	/**
	 * The combination of the group of a counting.
	 * @param counterName {@code skillSuggestion}, {@code add} or {@code remove}
	 * @return {@code {"counter": counterName}}
	 */
	public static CcpJsonRepresentation getCombination(String counterName) {
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(Fields.counter, counterName);
		return combination;
	}
}
