package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.mensageria.JnBusinessSendToMensageria;
import com.vis.entities.VisEntityGroupResumeViewsByRecruiter;
import com.vis.entities.VisEntityResumeFreeView;
import com.vis.utils.VisUtils;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Scheduled task that groups the resume views by recruiter, using VisEntityResumeFreeView
 * as the data source and VisEntityGroupResumeViewsByRecruiter as the target of the paginated grouping.
 * Delegates the logic to the VisUtils.groupDetailsByMasters utility.
 */
public class VisBusinessGroupResumeViewsByRecruiter implements JnBusinessSendToMensageria{
		

	/** Singleton; use {@link #INSTANCE}. */
	private VisBusinessGroupResumeViewsByRecruiter() {}
	
	/** The single instance. */
	public static final VisBusinessGroupResumeViewsByRecruiter INSTANCE = new VisBusinessGroupResumeViewsByRecruiter();
	
	/**
	 * Runs the business described in the class documentation.
	 * @param json the input
	 * @return the result
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation groupingResult = VisUtils.groupDetailsByMasters(
				json, 
				VisEntityResumeFreeView.ENTITY, 
				VisEntityGroupResumeViewsByRecruiter.ENTITY, 
				VisJsonCommonsFields.email, 
				JnJsonCommonsFields.timestamp
				);
		
		return groupingResult;
	}

}
