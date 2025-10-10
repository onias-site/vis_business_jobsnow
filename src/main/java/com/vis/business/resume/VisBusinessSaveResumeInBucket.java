package com.vis.business.resume;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.file.bucket.CcpFileBucket;
import com.ccp.especifications.mensageria.receiver.CcpBusiness;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.vis.entities.VisEntityResume;
import com.vis.utils.VisUtils;

public class VisBusinessSaveResumeInBucket implements CcpBusiness {
	
	public static enum JsonFieldNames implements CcpJsonFieldName{
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 5, maxLength = 100)
		fileName, 
		@CcpJsonFieldTypeString(minLength = 512, maxLength = 10_485_760)
		resumeText, 
		originalEmail, //TODO
		@CcpJsonFieldTypeString(maxLength = 5000)
		observations, 
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 512, maxLength = 10_485_760)
		resumeBase64,
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
