package com.vis.cache;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.file.bucket.CcpFileBucketOperation;
import com.vis.entities.VisEntityResume;
import com.vis.utils.VisUtils;

public class VisBusinessReadResumeContent implements Function<CcpJsonRepresentation, String>{

	public static final VisBusinessReadResumeContent INSTANCE = new VisBusinessReadResumeContent();
	
	private VisBusinessReadResumeContent() {}
	
	public String apply(CcpJsonRepresentation json) {
		String email = json.getAsString(VisEntityResume.Fields.email);
		String folder = "resumes/" + email;
		String file = "" + json.getAsLongNumber(VisEntityResume.Fields.timestamp);
		String tenant = VisUtils.getTenant();
		String execute = CcpFileBucketOperation.get.execute(tenant, folder, file);
		return execute;
	}

}
