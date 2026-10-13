package com.vis.business.resume;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.jn.entities.JnEntityGrouper;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityVirtualHashGrouper;
import com.vis.json.transformers.VisResumeHashCombinations;

/**
 * Puts the resume in the group of {@code JnEntityGrouper} of each of its hashes, in its page of the group: the parent
 * is the hash and the values it came from ({@code parentJson}), the child is the resume with all of its data except the
 * hash list. Runs after the resume is saved and after it is reactivated (moved back from the twin); the groups of the
 * hashes it had before were left by {@code VisBusinessResumeRemoveFromGroupers}. Each page is changed by an atomic script
 * that replaces a version already there, so a repeated run does not duplicate the resume and a concurrent writer of the
 * same page loses nothing.
 */
public class VisBusinessResumeAddToGroupers implements CcpBusiness {

	/**
	 * Adds (or replaces) the resume in the groups of its hashes.
	 * @param json the resume: the saved one, already transformed, or the key of the reactivated one
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		// a reactivation brings only the key: the resume is read from the main entity, where it came back to
		boolean hasHashes = json.containsAllFields(VisEntityResume.Fields.hash);
		CcpJsonRepresentation resume = hasHashes ? json : VisResumeGroupers.getStoredResume(json);
		boolean noResume = false == resume.containsAllFields(VisEntityResume.Fields.hash);

		if(noResume) {
			return json;
		}

		CcpEntityMetaData resumeMetaData = VisEntityResume.ENTITY.getEntityMetaData();
		CcpJsonRepresentation resumeFields = resumeMetaData.getOnlyExistingFields(resume);
		CcpJsonRepresentation child = resumeFields.removeFields(VisEntityResume.Fields.hash);
		String resumeEmail = child.getAsString(VisEntityResume.Fields.email);
		int childPage = VisResumeGroupers.getChildPage(resumeEmail);
		CcpJsonRepresentation childKeyParams = VisResumeGroupers.getChildKeyParams(resumeEmail);
		CcpJsonRepresentation paramsWithChild = childKeyParams.put(VisResumeGroupers.Scripts.child, child);

		VisResumeHashCombinations hashCombinations = new VisResumeHashCombinations();
		List<CcpJsonRepresentation> combinations = hashCombinations.apply(resume);
		Map<String, CcpJsonRepresentation> parentJsonByHash = new LinkedHashMap<>();
		for (CcpJsonRepresentation combination : combinations) {
			String hash = VisEntityVirtualHashGrouper.ENTITY.calculateId(combination);
			parentJsonByHash.put(hash, combination);
		}
		List<String> hashes = new ArrayList<>(parentJsonByHash.keySet());
		List<CcpJsonRepresentation> children = Arrays.asList(child);

		VisResumeGroupers.writeGroups(hashes, hash -> {
			CcpJsonRepresentation groupKey = VisResumeGroupers.getGroupKey(hash, childPage);
			CcpJsonRepresentation parentJson = parentJsonByHash.get(hash);
			CcpJsonRepresentation params = paramsWithChild.put(VisResumeGroupers.Scripts.parentJson, parentJson);
			// the page that does not exist yet is created as is, with the resume as its only child
			CcpJsonRepresentation newPageWithParentJson = groupKey.put(JnEntityGrouper.Fields.parentJson, parentJson);
			CcpJsonRepresentation newPageWithChildren = newPageWithParentJson.put(JnEntityGrouper.Fields.childJson, children);
			CcpJsonRepresentation newPage = newPageWithChildren.put(JnEntityGrouper.Fields.childCount, 1);
			CcpBulkItem item = VisResumeGroupers.getScriptedItem(groupKey, VisResumeGroupers.Scripts.addChild, params, newPage, false);
			return item;
		});
		return json;
	}
}
