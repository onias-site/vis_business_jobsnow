package com.vis.business.resume;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.vis.entities.VisEntityResume;

/**
 * Takes the resume out of its page of the group of {@code JnEntityGrouper} of each hash it has stored; a page left with
 * no child is deleted, and a page that does not exist is not created. Runs before the resume is saved again (its new
 * hashes go in afterwards, through {@code VisBusinessResumeAddToGroupers}), after it is deactivated (moved to the twin,
 * where it is read from) and before it is deleted everywhere (when it can still be read). Each page is changed by an
 * atomic script, so a concurrent writer of the same page loses nothing.
 */
public class VisBusinessResumeRemoveFromGroupers implements CcpBusiness {

	/**
	 * Removes the resume from the groups of its stored hashes; a resume found nowhere is returned as is.
	 * @param json the resume (at least its key)
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation storedResume = VisResumeGroupers.getStoredResume(json);
		List<String> storedHashes = storedResume.getAsStringList(VisEntityResume.Fields.hash);
		boolean hasNoHashes = storedHashes.isEmpty();

		if(hasNoHashes) {
			return json;
		}

		String resumeEmail = storedResume.getAsString(VisEntityResume.Fields.email);
		int childPage = VisResumeGroupers.getChildPage(resumeEmail);
		CcpJsonRepresentation params = VisResumeGroupers.getChildKeyParams(resumeEmail);

		VisResumeGroupers.writeGroups(storedHashes, hash -> {
			CcpJsonRepresentation groupKey = VisResumeGroupers.getGroupKey(hash, childPage);
			// the script also runs when the page does not exist, to decide not to create it
			CcpBulkItem item = VisResumeGroupers.getScriptedItem(groupKey, VisResumeGroupers.Scripts.removeChild, params, CcpOtherConstants.EMPTY_JSON, true);
			return item;
		});
		return json;
	}
}
