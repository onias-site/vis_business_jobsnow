package com.vis.business.skill;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntitySkillFixHierarchyItemApproved;
import com.vis.entities.VisEntitySkillFixHierarchyItemPending;
import com.vis.entities.VisEntitySkillFixHierarchyPending;

/**
 * Runs after every save of a {@link VisEntitySkillFixHierarchyPending} request, insert or update: splits it into
 * one {@link VisEntitySkillFixHierarchyItemPending} record per skill of the {@code skill} array, created in a
 * single bulk operation. An item that already exists as pending, as rejected (the twin) or in
 * {@link VisEntitySkillFixHierarchyItemApproved} is not written: its decision was already taken, and the
 * {@code fixSkillHierarchy} command of the support bot reports that decision without asking the operator again.
 *
 * <p>Up to 2026-09-30 the split lived in the notice of the new request, which runs only on insert: a skill added
 * to a request already pending never became an item, and the support bot skipped it without asking the operator.
 */
public class VisBusinessSkillFixHierarchyCreateItems implements CcpBusiness {

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		List<String> skills = json.getAsStringList(VisEntitySkillFixHierarchyPending.Fields.skill);
		Stream<String> skillsStream = skills.stream();
		Stream<String> distinctSkillsStream = skillsStream.distinct();
		List<String> distinctSkills = distinctSkillsStream.collect(Collectors.toList());
		Stream<String> distinctSkillsToItemsStream = distinctSkills.stream();
		Stream<CcpJsonRepresentation> itemsStream = distinctSkillsToItemsStream.map(skill -> json.put(VisEntitySkillFixHierarchyItemPending.Fields.skill, skill));
		CcpJsonRepresentation[] items = itemsStream.toArray(CcpJsonRepresentation[]::new);

		CcpEntity rejectedItemEntity = VisEntitySkillFixHierarchyItemPending.ENTITY.getTwinEntity();
		CcpEntity[] entitiesThatPreventCreation = {rejectedItemEntity, VisEntitySkillFixHierarchyItemApproved.ENTITY};
		JnExecuteBulkOperation.INSTANCE.executeCreateBulk(VisEntitySkillFixHierarchyItemPending.ENTITY, entitiesThatPreventCreation, JnDeleteKeysFromCache.INSTANCE, items);

		return json;
	}
}
