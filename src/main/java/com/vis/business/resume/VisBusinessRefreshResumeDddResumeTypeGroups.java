package com.vis.business.resume;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityGrouperChild;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.decorators.engine.JnGrouperChildEntityOnWrite;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnGroupers;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityVirtualDddResumeTypeGrouper;
import com.vis.json.transformers.VisResumeDddResumeTypeCombinations;

/**
 * Puts the resume of an e-mail in the groups of ddd and area ({@code vis_virtual_ddd_resume_type_grouper}) as its state
 * asks, without saving the resume again (since 2026-11-22): out of them while the recruiters may not see it (a
 * restriction to correct, or its LinkedIn confirmed as invalid), back in them when it may. Those groups feed the count of
 * the resumes of the filters of a position, and the states change without a save of the resume: the restriction is
 * created and deleted by its own entity (after its save and its delete), and the support marks the LinkedIn by a partial
 * update. The resume leaves the groups of every ddd of it and comes back with the same child the decorator of
 * {@code @CcpEntityGrouperChild} writes, on the side where it is (main or twin): running again changes nothing.
 */
public class VisBusinessRefreshResumeDddResumeTypeGroups implements CcpBusiness {

	/** The input: the record that names the resume (a restriction, for one). */
	public static enum Fields implements CcpJsonFieldName {
		/** The e-mail of the resume, readable or already hashed (the side effects of the restriction get it hashed). */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 7, maxLength = 100)
		email,
	}

	/** The text that only a readable e-mail has. */
	enum Constants implements CcpJsonFieldName {
		AT_SIGN {
			public String getValue() {
				return "@";
			}
		},
	}

	/** The single instance: the class keeps no state. */
	public static final VisBusinessRefreshResumeDddResumeTypeGroups INSTANCE = new VisBusinessRefreshResumeDddResumeTypeGroups();

	private VisBusinessRefreshResumeDddResumeTypeGroups() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	/**
	 * Finds the resume of the e-mail, on either side, and refreshes its groups; an e-mail with no resume changes nothing.
	 * @param json the record that names the resume
	 * @return the record, as it came
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String email = json.getAsString(Fields.email);
		String atSign = Constants.AT_SIGN.getValue();
		boolean readableEmail = email.contains(atSign);
		CcpJsonRepresentation withHashedEmail = readableEmail ? JnJsonTransformersFieldsEntityDefault.email.execute(json) : json;
		String hashedEmail = withHashedEmail.getAsString(Fields.email);
		CcpJsonRepresentation resumeKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityResume.Fields.email, hashedEmail);
		CcpJsonRepresentation[] resumeKeys = new CcpJsonRepresentation[] {resumeKey};
		CcpEntity twinEntity = VisEntityResume.ENTITY.getTwinEntity();
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpSelectUnionAll resumeOnEitherSide = crud.unionAll(resumeKeys, JnDeleteKeysFromCache.INSTANCE, VisEntityResume.ENTITY, twinEntity);
		boolean active = VisEntityResume.ENTITY.isPresentInThisUnionAll(resumeOnEitherSide, resumeKey);
		boolean inactive = twinEntity.isPresentInThisUnionAll(resumeOnEitherSide, resumeKey);
		boolean noResume = false == active && false == inactive;

		if(noResume) {
			return json;
		}

		CcpEntity sideEntity = active ? VisEntityResume.ENTITY : twinEntity;
		Supplier<CcpJsonRepresentation> resumeKeySupplier = resumeKey.getJsonSupplier();
		CcpJsonRepresentation storedResume = sideEntity.getRecordFromUnionAll(resumeOnEitherSide, resumeKeySupplier);
		refresh(storedResume, inactive);
		return json;
	}

	/**
	 * Takes the resume out of the groups of every ddd of it and puts it back when the recruiters may see it.
	 * @param storedResume the resume as stored
	 * @param twinSide whether the resume is inactive (its groups are in {@code jn_inactive_grouper})
	 */
	public static void refresh(CcpJsonRepresentation storedResume, boolean twinSide) {
		CcpEntityMetaData parentDetails = VisEntityVirtualDddResumeTypeGrouper.ENTITY.getEntityMetaData();
		CcpEntityMetaData childDetails = VisEntityResume.ENTITY.getEntityMetaData();
		CcpEntity grouper = JnGroupers.getGrouper(twinSide);
		List<CcpJsonRepresentation> combinations = VisResumeDddResumeTypeCombinations.getAllCombinations(storedResume);
		Map<String, CcpJsonRepresentation> parents = new LinkedHashMap<>();
		for (CcpJsonRepresentation combination : combinations) {
			String parentId = VisEntityVirtualDddResumeTypeGrouper.ENTITY.calculateId(combination);
			parents.put(parentId, combination);
		}
		List<String> parentIds = List.copyOf(parents.keySet());
		String childId = VisEntityResume.ENTITY.calculateId(storedResume);
		// the child is a JSON text: the text "id":"<id>" is in the children of this resume only
		CcpJsonRepresentation idAlone = CcpOtherConstants.EMPTY_JSON.put(JnJsonCommonsFields.id, childId);
		String idAloneText = idAlone.asUgglyJson();
		int closingBrace = idAloneText.length() - 1;
		String childKey = idAloneText.substring(1, closingBrace);
		List<String> childKeys = Arrays.asList(childKey);
		JnGroupers.removeChildByKeys(grouper, parentDetails.entityName, childDetails.entityName, childKeys, parentIds);
		boolean hidden = VisResumeDddResumeTypeCombinations.isHiddenFromTheRecruiters(storedResume);

		if(hidden) {
			return;
		}

		CcpEntityGrouperChild grouping = getGrouping();
		String child = JnGrouperChildEntityOnWrite.getChild(VisEntityResume.ENTITY, grouping, storedResume);
		JnGroupers.addChild(grouper, parentDetails.entityName, childDetails.entityName, child, parents);
	}

	/** The grouping of the resume in the groups of ddd and area, as {@code VisEntityResume} declares it. */
	private static CcpEntityGrouperChild getGrouping() {
		CcpEntityGrouperChild[] groupings = VisEntityResume.class.getAnnotationsByType(CcpEntityGrouperChild.class);
		for (CcpEntityGrouperChild grouping : groupings) {
			Class<?>[] parentClasses = grouping.value();
			List<Class<?>> parentClassesList = Arrays.asList(parentClasses);
			boolean dddAndArea = parentClassesList.contains(VisEntityVirtualDddResumeTypeGrouper.class);

			if(dddAndArea) {
				return grouping;
			}
		}
		throw new VisErrorResumeWithoutDddAndAreaGrouping();
	}

	/** Raised when {@code VisEntityResume} no longer declares the grouping of ddd and area. */
	@SuppressWarnings("serial")
	private static class VisErrorResumeWithoutDddAndAreaGrouping extends RuntimeException {
		private VisErrorResumeWithoutDddAndAreaGrouping() {
			super(VisEntityResume.class.getName() + " declares no @CcpEntityGrouperChild of " + VisEntityVirtualDddResumeTypeGrouper.class.getName());
		}
	}
}
