package com.vis.business.skill;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;

/**
 * Callback of the transfer of a {@code VisEntitySkillFixHierarchyItemPending} record to
 * {@code VisEntitySkillFixHierarchyItemApproved}, executed after the transfer has actually happened.
 * The implementation is marked as TODO: it returns the input JSON unchanged.
 */
public class VisBusinessSkillFixHierarchyItem implements CcpBusiness {

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		return json;
	}

}
