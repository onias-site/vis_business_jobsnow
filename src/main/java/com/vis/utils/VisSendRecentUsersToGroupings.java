package com.vis.utils;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.jn.entities.JnEntityDisposableRecord;
import com.jn.entities.JnEntityLoginSessionValidation;
import com.jn.mensageria.JnFunctionMensageriaSender;
import com.vis.schedulling.VisBusinessGroupResumeViewsByRecruiter;
import com.vis.schedulling.VisBusinessGroupResumeViewsByResume;
import com.vis.schedulling.VisBusinessGroupResumesOpinionsByRecruiter;
import com.vis.schedulling.VisBusinessGroupResumesOpinionsByResume;
public class VisSendRecentUsersToGroupings implements Consumer<List<CcpJsonRepresentation>> {
	enum JsonFieldNames implements CcpJsonFieldName{
		masters
	}
	
	private VisSendRecentUsersToGroupings() {}
	
	public final static VisSendRecentUsersToGroupings INSTANCE = new VisSendRecentUsersToGroupings();

	public void accept(List<CcpJsonRepresentation> records) {
		List<String> emails = records.stream()
		.map(rec ->	rec.getAsString(JnEntityDisposableRecord.Fields.id))
		.map(id -> new CcpJsonRepresentation(id))
		.map(json -> json.getAsString(JnEntityLoginSessionValidation.Fields.email))
		.collect(Collectors.toList());
		
		CcpJsonRepresentation message = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.masters, emails);
		
		new JnFunctionMensageriaSender(VisBusinessGroupResumesOpinionsByRecruiter.INSTANCE).sendToMensageria(message);
		VisBusinessGroupResumesOpinionsByResume.INSTANCE.sendToMensageria(message);
		VisBusinessGroupResumeViewsByRecruiter.INSTANCE.sendToMensageria(message);
		VisBusinessGroupResumeViewsByResume.INSTANCE.sendToMensageria(message);
	}

}
