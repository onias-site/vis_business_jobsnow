package com.vis.business.job.posting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.utils.JnGroupers;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityVirtualSkillGrouper;

/**
 * Third business before the save of {@code vis_job_posting} (since 2026-11-26; the second one until 2026-11-27): the largest set of the skills of the
 * text ({@code requestedSkills}) that some resume has, directly or indirectly. The groups of the skills in
 * {@code jn_grouper} ({@code vis_virtual_skill_grouper}, the active resumes only) are read by id in one round trip, and
 * each resume is counted by how many of the skills it has. The resumes with the most skills give the answer: when they
 * have different sets of the same size, the set that more of them share wins (the first found on a tie). So when a
 * single resume has every skill of the text, the answer is every skill; when no resume has all of them, it is the most
 * skills that at least one resume has. No resume with any skill of the text, no skill.
 */
public class VisBusinessFindTheMostCoveredSkills implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public static final VisBusinessFindTheMostCoveredSkills INSTANCE = new VisBusinessFindTheMostCoveredSkills();

	private VisBusinessFindTheMostCoveredSkills() {}

	/** The fields this business adds to the record, read by the next one and not saved. */
	public static enum Fields implements CcpJsonFieldName {
		/** The largest set of the skills of the text that some resume has, in the order of the text. */
		possibleSkills,
	}

	/**
	 * Crosses the groups of the skills of the text.
	 * @param json the record, with {@code requestedSkills}
	 * @return the record with {@code possibleSkills}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		List<String> requestedSkills = json.getAsStringList(VisBusinessFindTheSkillsOfTheJobPosting.Fields.requestedSkills);
		Map<String, String> skillByParentId = new LinkedHashMap<>();
		for (String requestedSkill : requestedSkills) {
			CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualSkillGrouper.Fields.skill, requestedSkill);
			String parentId = VisEntityVirtualSkillGrouper.ENTITY.calculateId(combination);
			skillByParentId.put(parentId, requestedSkill);
		}
		boolean noSkill = skillByParentId.isEmpty();

		if(noSkill) {
			return json.put(Fields.possibleSkills, List.of());
		}

		String parentName = VisEntityVirtualSkillGrouper.ENTITY.getEntityMetaData().entityName;
		String childName = VisEntityResume.ENTITY.getEntityMetaData().entityName;
		Set<String> parentIds = skillByParentId.keySet();
		Map<String, List<String>> resumesByParentId = JnGroupers.getChildrenOfGroups(parentName, childName, parentIds);
		Map<String, Set<String>> skillsByResume = getSkillsByResume(skillByParentId, resumesByParentId);
		List<String> possibleSkills = getMostCoveredSkills(skillsByResume, requestedSkills);
		CcpJsonRepresentation jsonWithThePossibleSkills = json.put(Fields.possibleSkills, possibleSkills);
		return jsonWithThePossibleSkills;
	}

	/**
	 * The skills of the text each resume has.
	 * @param skillByParentId the skill of each group
	 * @param resumesByParentId the resumes of each group found
	 * @return the skills by resume
	 */
	private static Map<String, Set<String>> getSkillsByResume(Map<String, String> skillByParentId, Map<String, List<String>> resumesByParentId) {
		Map<String, Set<String>> skillsByResume = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> group : resumesByParentId.entrySet()) {
			String parentId = group.getKey();
			String skill = skillByParentId.get(parentId);
			List<String> resumes = group.getValue();
			for (String resume : resumes) {
				Set<String> skillsOfTheResume = skillsByResume.computeIfAbsent(resume, newResume -> new LinkedHashSet<>());
				skillsOfTheResume.add(skill);
			}
		}
		return skillsByResume;
	}

	/**
	 * The largest set of skills some resume has; on a tie of size, the set more resumes share.
	 * @param skillsByResume the skills of the text each resume has
	 * @param requestedSkills the skills of the text, which give the order of the answer
	 * @return the skills, in the order of the text; none when no resume has any
	 */
	private static List<String> getMostCoveredSkills(Map<String, Set<String>> skillsByResume, List<String> requestedSkills) {
		int largestSize = 0;
		for (Set<String> skillsOfTheResume : skillsByResume.values()) {
			int size = skillsOfTheResume.size();
			largestSize = Math.max(largestSize, size);
		}
		Map<Set<String>, Integer> resumesBySet = new LinkedHashMap<>();
		for (Set<String> skillsOfTheResume : skillsByResume.values()) {
			int size = skillsOfTheResume.size();
			boolean smaller = size < largestSize;

			if(smaller) {
				continue;
			}

			resumesBySet.merge(skillsOfTheResume, 1, Integer::sum);
		}
		Set<String> mostSharedSet = Set.of();
		int mostResumes = 0;
		for (Map.Entry<Set<String>, Integer> entry : resumesBySet.entrySet()) {
			Integer resumes = entry.getValue();
			boolean fewer = resumes <= mostResumes;

			if(fewer) {
				continue;
			}

			mostResumes = resumes;
			mostSharedSet = entry.getKey();
		}
		List<String> possibleSkills = new ArrayList<>();
		for (String requestedSkill : requestedSkills) {
			boolean covered = mostSharedSet.contains(requestedSkill);

			if(false == covered) {
				continue;
			}

			possibleSkills.add(requestedSkill);
		}
		return possibleSkills;
	}
}
