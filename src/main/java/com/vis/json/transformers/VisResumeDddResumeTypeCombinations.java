package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityDecoratorTypes;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeRestriction;
import com.vis.entities.VisEntityVirtualDddResumeTypeGrouper;

/**
 * Produces the groups of {@code VisEntityVirtualDddResumeTypeGrouper} of a resume: one per ddd of the resume, each with
 * the area of the resume (resumeType), both as integers, so the input and the stored record give the same ids. These
 * groups feed the count of the resumes of the filters of a position, so a resume the recruiters may not see is in none
 * of them (since 2026-11-22): the one whose LinkedIn the support confirmed as invalid ({@code invalidLinkedinAddress})
 * and the one with a restriction to correct ({@code vis_resume_restriction}). Those states change without a save of the
 * resume, so {@code VisBusinessRefreshResumeDddResumeTypeGroups} takes the resume out of its groups, or puts it back,
 * when they change; it removes with {@link #getAllCombinations}, which ignores them.
 */
public class VisResumeDddResumeTypeCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisResumeDddResumeTypeCombinations INSTANCE = new VisResumeDddResumeTypeCombinations();

	private VisResumeDddResumeTypeCombinations() {}

	/**
	 * One combination per ddd of the resume, all with the area of the resume; none for a resume the recruiters may not see.
	 * @param json the resume, its e-mail already hashed
	 * @return the combinations, {@code {"ddd": <ddd>, "resumeType": <area>}}
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		boolean hidden = isHiddenFromTheRecruiters(json);

		if(hidden) {
			return new ArrayList<>();
		}

		List<CcpJsonRepresentation> combinations = getAllCombinations(json);
		return combinations;
	}

	/**
	 * Whether the resume is out of the groups: its LinkedIn confirmed as invalid or a restriction to correct.
	 * @param json the resume, its e-mail already hashed
	 * @return {@code true} when the recruiters may not see it
	 */
	public static boolean isHiddenFromTheRecruiters(CcpJsonRepresentation json) {
		boolean invalidLinkedinAddress = json.getOrDefault(VisEntityResume.Fields.invalidLinkedinAddress, () -> false);

		if(invalidLinkedinAddress) {
			return true;
		}

		String hashedEmail = json.getAsString(VisEntityResume.Fields.email);
		CcpJsonRepresentation restrictionKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityResumeRestriction.Fields.email, hashedEmail);
		// the restriction keeps the e-mail hashed as the resume does: read without the fields transformer
		CcpEntity restrictionByHashedEmail = CcpEntityFactory.getCustomEntity(VisEntityResumeRestriction.ENTITY, CcpEntityDecoratorTypes.FieldsTransformer);
		boolean restricted = restrictionByHashedEmail.exists(restrictionKey);
		return restricted;
	}

	/**
	 * Every group of the resume, whatever its state: the groups a resume may be in.
	 * @param json the resume
	 * @return the combinations, {@code {"ddd": <ddd>, "resumeType": <area>}}
	 */
	public static List<CcpJsonRepresentation> getAllCombinations(CcpJsonRepresentation json) {
		List<CcpJsonRepresentation> combinations = new ArrayList<>();
		boolean withoutResumeType = false == json.containsAllFields(VisEntityResume.Fields.resumeType);

		if(withoutResumeType) {
			return combinations;
		}

		Integer resumeType = json.getAsIntegerNumber(VisEntityResume.Fields.resumeType);
		List<Object> ddds = json.getAsObjectList(VisEntityResume.Fields.ddd);
		for (Object ddd : ddds) {
			CcpJsonRepresentation rawCombination = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualDddResumeTypeGrouper.Fields.ddd, ddd);
			Integer dddAsInteger = rawCombination.getAsIntegerNumber(VisEntityVirtualDddResumeTypeGrouper.Fields.ddd);
			CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON
					.put(VisEntityVirtualDddResumeTypeGrouper.Fields.ddd, dddAsInteger)
					.put(VisEntityVirtualDddResumeTypeGrouper.Fields.resumeType, resumeType);
			combinations.add(combination);
		}
		return combinations;
	}
}
