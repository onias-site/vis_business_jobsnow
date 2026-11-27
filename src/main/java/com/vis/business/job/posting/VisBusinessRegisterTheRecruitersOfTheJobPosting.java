package com.vis.business.job.posting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.json.validations.global.engine.CcpJsonValidationError;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.business.recruiter.VisBusinessPrepareRecruiter;
import com.vis.entities.VisEntityJobPosting;
import com.vis.entities.VisEntityRecruiter;

/**
 * Fourth business before the save of {@code vis_job_posting} (since 2026-11-26): the valid e-mails of the text of the job
 * posting become recruiters of {@code vis_recruiter}, prepared as {@code /saveRecruiter} prepares them (lower case, no
 * accents, source {@code support}, refused when the entity refuses them). An e-mail already among the recruiters stays as
 * it is, and one among the inactive recruiters (the twin: unsubscribed, eliminated or who stopped viewing) is not brought
 * back, as the loads of Telegram do; the others are inserted. The two entities are read once, for every e-mail (MGIE).
 * The e-mails go on in {@link Fields#recruiterEmails}, for the e-mail of the resumes
 * ({@link VisBusinessSendTheResumesOfTheJobPosting}).
 */
public class VisBusinessRegisterTheRecruitersOfTheJobPosting implements CcpBusiness {

	/** The output of this business besides the record. */
	public static enum Fields implements CcpJsonFieldName {
		/** The e-mails of the text that are recruiters, prepared. */
		recruiterEmails,
	}

	/** The single instance: the class keeps no state. */
	public static final VisBusinessRegisterTheRecruitersOfTheJobPosting INSTANCE = new VisBusinessRegisterTheRecruitersOfTheJobPosting();

	private VisBusinessRegisterTheRecruitersOfTheJobPosting() {}

	/**
	 * Registers the recruiters of the text.
	 * @param json the record, with {@code description}
	 * @return the record with {@code recruiterEmails}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String description = json.getAsString(VisEntityJobPosting.Fields.description);
		Map<String, CcpJsonRepresentation> recruitersByEmail = getRecruiters(description);
		boolean noEmail = recruitersByEmail.isEmpty();

		if(noEmail) {
			return json.put(Fields.recruiterEmails, List.of());
		}

		List<CcpJsonRepresentation> recruiters = new ArrayList<>(recruitersByEmail.values());
		CcpJsonRepresentation[] recruitersArray = recruiters.toArray(new CcpJsonRepresentation[0]);
		CcpEntity twinEntity = VisEntityRecruiter.ENTITY.getTwinEntity();
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpSelectUnionAll found = crud.unionAll(recruitersArray, JnDeleteKeysFromCache.INSTANCE, VisEntityRecruiter.ENTITY, twinEntity);
		for (CcpJsonRepresentation recruiter : recruiters) {
			boolean active = VisEntityRecruiter.ENTITY.isPresentInThisUnionAll(found, recruiter);

			if(active) {
				continue;
			}

			boolean inactive = twinEntity.isPresentInThisUnionAll(found, recruiter);

			if(inactive) {
				continue;
			}

			VisEntityRecruiter.ENTITY.save(recruiter);
		}
		List<String> emails = new ArrayList<>(recruitersByEmail.keySet());
		CcpJsonRepresentation withTheEmails = json.put(Fields.recruiterEmails, emails);
		return withTheEmails;
	}

	/**
	 * The recruiters of the valid e-mails of the text, prepared, by their prepared e-mail (an e-mail written twice, once).
	 * @param description the text of the job posting
	 * @return the recruiters
	 */
	private static Map<String, CcpJsonRepresentation> getRecruiters(String description) {
		CcpStringDecorator text = new CcpStringDecorator(description);
		CcpEmailDecorator emailsOfTheText = text.email();
		Set<String> emails = emailsOfTheText.findAllInText();
		String supportSource = VisEntityRecruiter.Sources.support.getValue();
		Map<String, CcpJsonRepresentation> recruitersByEmail = new LinkedHashMap<>();
		for (String email : emails) {
			CcpJsonRepresentation recruiterEmail = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiter.Fields.email, email);
			CcpJsonRepresentation recruiterInput = recruiterEmail.put(VisEntityRecruiter.Fields.source, supportSource);
			try {
				CcpJsonRepresentation recruiter = VisBusinessPrepareRecruiter.INSTANCE.execute(recruiterInput);
				String preparedEmail = recruiter.getAsString(VisEntityRecruiter.Fields.email);
				recruitersByEmail.putIfAbsent(preparedEmail, recruiter);
			} catch (CcpJsonValidationError refusedByTheEntity) {
				// an address the entity refuses is not a recruiter
				continue;
			}
		}
		return recruitersByEmail;
	}
}
