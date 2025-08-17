package com.vis.business.resume;

import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.file.bucket.CcpFileBucket;
import com.vis.entities.VisEntityResume;
import com.vis.utils.VisUtils;

public class VisBusinessSaveResumeInBucket implements Function<CcpJsonRepresentation, CcpJsonRepresentation> {
	enum JsonFieldNames implements CcpJsonFieldName{
		fileName, resumeText, originalEmail, name, observations, resumeBase64
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpFileBucket bucket = CcpDependencyInjection.getDependency(CcpFileBucket.class);
		
	
		String fileContent = json.getJsonPiece(JsonFieldNames.values()).asUgglyJson();
		String fileName = "" + json.getAsLongNumber(VisEntityResume.Fields.timestamp);
		String folderName = json.getAsString(VisEntityResume.Fields.email);
		String tenant = VisUtils.getTenant();
		String bucketName = "resumes/" + folderName;

		bucket.save(tenant, bucketName, fileName, fileContent);
		
		return json;
	}

}
