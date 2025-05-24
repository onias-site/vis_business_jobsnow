package com.vis.commons.business.resume;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.file.bucket.CcpFileBucket;
import com.vis.commons.entities.VisEntityResume;
import com.vis.commons.utils.VisCommonsUtils;

public class VisCommonsBusinessSaveResumeInBucket implements Function<CcpJsonRepresentation, CcpJsonRepresentation> {

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpFileBucket dependency = CcpDependencyInjection.getDependency(CcpFileBucket.class);
		
		String[] fields = new String[] {"fileName", "resumeText", "originalEmail", "name", "observations", "resumeBase64"};
	
		String fileContent = json.getJsonPiece(fields).asUgglyJson();
		String fileName = "" + json.getAsLongNumber(VisEntityResume.Fields.timestamp.name());
		String folderName = json.getAsString(VisEntityResume.Fields.email.name());
		String tenant = VisCommonsUtils.getTenant();
		String bucketName = "resumes/" + folderName;

		dependency.save(tenant, bucketName, fileName, fileContent);
		
		return json;
	}

}
