package com.vis.utils;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.mensageria.JnFunctionMensageriaSender;
import com.vis.schedulling.VisBusinessGroupResumeViewsByRecruiter;
import com.vis.schedulling.VisBusinessGroupResumeViewsByResume;
import com.vis.schedulling.VisBusinessGroupResumesOpinionsByRecruiter;
import com.vis.schedulling.VisBusinessGroupResumesOpinionsByResume;
import java.util.stream.Stream;

import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Consumer of a list of recent session records that extracts the users' e-mails and sends them to the
 * four asynchronous grouping processes (opinions by recruiter, opinions by resume, views by recruiter,
 * views by resume), triggering the corresponding messaging senders.
 */
public class VisSendRecentUsersToGroupings implements Consumer<List<CcpJsonRepresentation>> {
	
	private VisSendRecentUsersToGroupings() {}
	
	public final static VisSendRecentUsersToGroupings INSTANCE = new VisSendRecentUsersToGroupings();

	public void accept(List<CcpJsonRepresentation> records) {
		Stream<CcpJsonRepresentation> recordsStream = records.stream();
		var idsStream = recordsStream
		.map(rec ->	rec.getAsString(JnJsonCommonsFields.id));
		var idsAsJsonStream = idsStream
		.map(id -> new CcpJsonRepresentation(id));
		var emailsStream = idsAsJsonStream
		.map(json -> json.getAsString(JnJsonCommonsFields.email));
		List<String> emails = emailsStream
		.collect(Collectors.toList());
		
		CcpJsonRepresentation message = CcpOtherConstants.EMPTY_JSON.put(VisJsonCommonsFields.masters, emails);
		JnFunctionMensageriaSender mensageriaSender = new JnFunctionMensageriaSender(VisBusinessGroupResumesOpinionsByRecruiter.INSTANCE);

		mensageriaSender.sendToMensageria(message);
		VisBusinessGroupResumesOpinionsByResume.INSTANCE.sendToMensageria(message);
		VisBusinessGroupResumeViewsByRecruiter.INSTANCE.sendToMensageria(message);
		VisBusinessGroupResumeViewsByResume.INSTANCE.sendToMensageria(message);
	}

}
