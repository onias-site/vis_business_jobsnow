package com.vis.commons.cache.tasks;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.file.bucket.CcpFileBucketOperation;
import com.vis.commons.entities.VisEntityResume;
import com.vis.commons.utils.VisCommonsUtils;

public class ReadResumeContent implements Function<CcpJsonRepresentation, String>{

	public static final ReadResumeContent INSTANCE = new ReadResumeContent();
	
	private ReadResumeContent() {}
	
	public String apply(CcpJsonRepresentation json) {
		String email = json.getAsString(VisEntityResume.Fields.email.name());
		String folder = "resumes/" + email;
		String file = "" + json.getAsLongNumber(VisEntityResume.Fields.timestamp.name());
		String tenant = VisCommonsUtils.getTenant();
		String execute = CcpFileBucketOperation.get.execute(tenant, folder, file);
		return execute;
	}

}
