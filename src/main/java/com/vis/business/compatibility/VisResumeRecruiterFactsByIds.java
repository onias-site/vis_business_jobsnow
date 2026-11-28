package com.vis.business.compatibility;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.crud.CcpUnionAllExecutor;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityDecoratorTypes;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.vis.business.recruiter.resumes.VisRecruiterResumesFields;
import com.vis.entities.VisEntityRecruiterUnsubscribed;
import com.vis.entities.VisEntityResumeRestriction;
import com.vis.entities.VisEntityViewedResume;

/**
 * The facts of the rules between recruiters and resumes read by id (OQL, MGIE): the unsubscription of each recruiter,
 * the restriction of each resume and the view of each pair in the twin {@code vis_inactive_viewed_resume}, all in a
 * single read of the ids. The ids come from the hashes through the entities without their fields transformer, since
 * the e-mails and the LinkedIn addresses are already hashed.
 */
public class VisResumeRecruiterFactsByIds implements VisResumeRecruiterFacts {

	/** The unsubscriptions, whose id comes from the e-mail already hashed. */
	private static final CcpEntity UNSUBSCRIPTIONS_BY_HASH = CcpEntityFactory.getCustomEntity(VisEntityRecruiterUnsubscribed.ENTITY, CcpEntityDecoratorTypes.FieldsTransformer);

	/** The restrictions, whose id comes from the e-mail of the resume already hashed. */
	private static final CcpEntity RESTRICTIONS_BY_HASH = CcpEntityFactory.getCustomEntity(VisEntityResumeRestriction.ENTITY, CcpEntityDecoratorTypes.FieldsTransformer);

	/** The views, whose id (the same on both sides) comes from the e-mail and the LinkedIn already hashed. */
	private static final CcpEntity VIEWS_BY_HASHES = CcpEntityFactory.getCustomEntity(VisEntityViewedResume.ENTITY, CcpEntityDecoratorTypes.FieldsTransformer);

	/** What the read found. */
	private final CcpSelectUnionAll found;

	private VisResumeRecruiterFactsByIds(CcpSelectUnionAll found) {
		this.found = found;
	}

	/**
	 * Reads, at once, the facts of every pair recruiter + resume.
	 * @param recruiters the recruiters, as {@link VisResumeRecruiterCompatibility#getRecruiter(String)} gives them
	 * @param resumes the resumes, as {@link VisResumeRecruiterCompatibility#getResume(CcpJsonRepresentation)} gives them
	 * @return the facts
	 */
	public static VisResumeRecruiterFacts load(List<CcpJsonRepresentation> recruiters, List<CcpJsonRepresentation> resumes) {
		Set<String> ids = new LinkedHashSet<>();
		for (CcpJsonRepresentation recruiter : recruiters) {
			String recruiterEmailHash = recruiter.getAsString(VisRecruiterResumesFields.emailHash);
			String unsubscriptionId = getUnsubscriptionId(recruiterEmailHash);
			ids.add(unsubscriptionId);
			for (CcpJsonRepresentation resume : resumes) {
				String linkedinHash = resume.getAsString(VisRecruiterResumesFields.linkedinHash);
				String viewId = getViewId(recruiterEmailHash, linkedinHash);
				ids.add(viewId);
			}
		}
		for (CcpJsonRepresentation resume : resumes) {
			String resumeEmailHash = resume.getAsString(VisRecruiterResumesFields.emailHash);
			String restrictionId = getRestrictionId(resumeEmailHash);
			ids.add(restrictionId);
		}
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpUnionAllExecutor unionAllExecutor = crud.getUnionAllExecutor();
		CcpEntity inactiveViews = VisEntityViewedResume.ENTITY.getTwinEntity();
		CcpSelectUnionAll found = unionAllExecutor.unionAllByIds(ids, VisEntityRecruiterUnsubscribed.ENTITY, VisEntityResumeRestriction.ENTITY, inactiveViews);
		VisResumeRecruiterFactsByIds facts = new VisResumeRecruiterFactsByIds(found);
		return facts;
	}

	public boolean isUnsubscribed(String recruiterEmailHash) {
		String unsubscriptionId = getUnsubscriptionId(recruiterEmailHash);
		String entityName = VisEntityRecruiterUnsubscribed.ENTITY.getEntityMetaData().entityName;
		boolean unsubscribed = this.found.isPresent(entityName, unsubscriptionId);
		return unsubscribed;
	}

	public boolean isRestricted(String resumeEmailHash) {
		String restrictionId = getRestrictionId(resumeEmailHash);
		String entityName = VisEntityResumeRestriction.ENTITY.getEntityMetaData().entityName;
		boolean restricted = this.found.isPresent(entityName, restrictionId);
		return restricted;
	}

	public boolean isUnwanted(String recruiterEmailHash, String linkedinHash) {
		String viewId = getViewId(recruiterEmailHash, linkedinHash);
		CcpEntity inactiveViews = VisEntityViewedResume.ENTITY.getTwinEntity();
		String entityName = inactiveViews.getEntityMetaData().entityName;
		boolean unwanted = this.found.isPresent(entityName, viewId);
		return unwanted;
	}

	private static String getUnsubscriptionId(String recruiterEmailHash) {
		CcpJsonRepresentation key = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiterUnsubscribed.Fields.email, recruiterEmailHash);
		String id = UNSUBSCRIPTIONS_BY_HASH.calculateId(key);
		return id;
	}

	private static String getRestrictionId(String resumeEmailHash) {
		CcpJsonRepresentation key = CcpOtherConstants.EMPTY_JSON.put(VisEntityResumeRestriction.Fields.email, resumeEmailHash);
		String id = RESTRICTIONS_BY_HASH.calculateId(key);
		return id;
	}

	private static String getViewId(String recruiterEmailHash, String linkedinHash) {
		CcpJsonRepresentation key = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityViewedResume.Fields.email, recruiterEmailHash)
				.put(VisEntityViewedResume.Fields.linkedin, linkedinHash);
		String id = VIEWS_BY_HASHES.calculateId(key);
		return id;
	}
}
