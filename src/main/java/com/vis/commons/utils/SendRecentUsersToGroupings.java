package com.vis.commons.utils;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.vis.schedulling.VisBusinessGroupResumeViewsByRecruiter;
import com.ccp.vis.schedulling.VisBusinessGroupResumeViewsByResume;
import com.ccp.vis.schedulling.VisBusinessGroupResumesOpinionsByRecruiter;
import com.ccp.vis.schedulling.VisBusinessGroupResumesOpinionsByResume;
import com.jn.entities.JnEntityDisposableRecord;
import com.jn.entities.JnEntityLoginSessionValidation;
import com.jn.mensageria.JnMensageriaSender;

public class SendRecentUsersToGroupings implements Consumer<List<CcpJsonRepresentation>> {
	
	private SendRecentUsersToGroupings() {}
	
	public final static SendRecentUsersToGroupings INSTANCE = new SendRecentUsersToGroupings();

	public void accept(List<CcpJsonRepresentation> records) {
		List<String> emails = records.stream()
		.map(rec ->	rec.getAsString(JnEntityDisposableRecord.Fields.id.name()))
		.map(id -> new CcpJsonRepresentation(id))
		.map(json -> json.getAsString(JnEntityLoginSessionValidation.Fields.email.name()))
		.collect(Collectors.toList());
		
		CcpJsonRepresentation message = CcpOtherConstants.EMPTY_JSON.put("masters", emails);
		
		new JnMensageriaSender(VisBusinessGroupResumesOpinionsByRecruiter.INSTANCE).send(message);
		VisBusinessGroupResumesOpinionsByResume.INSTANCE.sendToMensageria(message);
		VisBusinessGroupResumeViewsByRecruiter.INSTANCE.sendToMensageria(message);
		VisBusinessGroupResumeViewsByResume.INSTANCE.sendToMensageria(message);
	}

}
