package com.vis.business.recruiter.resumes;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.business.grouper.JnBusinessGetGroupOnBothSides;
import com.jn.entities.JnEntityGrouper;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnGroupers;
import com.vis.entities.VisEntityViewedResume;
import com.vis.entities.VisEntityViewedResumesOfTheMonth;
import com.vis.entities.VisEntityViewedResumesOfTheRecruiter;

/**
 * The resumes a recruiter viewed in a month, read by id (OQL): the id of the group of the recruiter in the month
 * ({@code VisEntityViewedResumesOfTheMonth}, its period given in {@code disposablePeriod}) and then the group itself in
 * {@code jn_grouper} and in {@code jn_inactive_grouper}, in a single read, whose children the writes of
 * {@code vis_viewed_resume} keep up to date (the main views in {@code jn_grouper}, the ones moved to the twin,
 * {@code vis_inactive_viewed_resume}, in {@code jn_inactive_grouper}, since 2026-11-15). No query runs.
 *
 * <p>Input: {@code email} (the recruiter) and {@code month} ({@code yyyyMM}). Output: {@code month},
 * {@code viewedResume}, the list of the JSON texts of the views of the main entity ({@code linkedinHash}, {@code alias}
 * and {@code date} of the last view of each resume), and {@code unwantedResume}, the same for the views of the twin (the
 * tab "Indesejados" of the screen); each list is empty when that side has nothing in the month. The LinkedIn never leaves in this
 * list: its hash is the key of the details ({@link VisBusinessViewedResumeDetails}), which check whether the candidate
 * still lets the company of the recruiter see the resume.
 *
 * <p>The month {@code all} (since 2026-11-15, the option "Listar todos" of the screen) reads, the same way, the group of
 * every view of the recruiter, whatever the month ({@code VisEntityViewedResumesOfTheRecruiter}); so do the details, the
 * edit and the moves, which find the view by {@link #getViewOfTheMonth(CcpJsonRepresentation)}.
 */
public class VisBusinessViewedResumesOfTheMonth implements CcpBusiness {

	/** The input of this business, with its validation rules. */
	public static enum Fields implements CcpJsonFieldName {
		/** The recruiter. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		email,

		/**
		 * The month of the views, as {@code yyyyMM} (the format of the monthly disposable entities), or {@code all} for every
		 * view of the recruiter, whatever the month.
		 */
		@CcpJsonFieldTypeString(regexValidation = "^([0-9]{4}(0[1-9]|1[0-2])|all)$")
		@CcpJsonFieldValidatorRequired
		month,
	}

	/** The texts of this business. */
	enum Constants implements CcpJsonFieldName {
		/** The month that stands for every month: the group of every view of the recruiter. */
		ALL_MONTHS {
			public String getValue() {
				return "all";
			}
		},
	}

	/** The single instance. */
	public static final VisBusinessViewedResumesOfTheMonth INSTANCE = new VisBusinessViewedResumesOfTheMonth();

	private VisBusinessViewedResumesOfTheMonth() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	/**
	 * The key, in {@code jn_grouper}, of the group of the views of the recruiter in the month, or of every view of the
	 * recruiter when the month is {@code all}.
	 * @param json {@code email} (the recruiter) and {@code month} ({@code yyyyMM} or {@code all})
	 * @return the key: parent id, parent name and child name
	 */
	static CcpJsonRepresentation getGroupKeyOfTheMonth(CcpJsonRepresentation json) {
		String month = json.getAsString(Fields.month);
		String allMonths = Constants.ALL_MONTHS.getValue();
		boolean everyMonth = allMonths.equals(month);

		if(everyMonth) {
			return getGroupKeyOfTheRecruiter(json);
		}

		CcpJsonRepresentation recruiter = json.getJsonPiece(VisEntityViewedResumesOfTheMonth.Fields.email);
		CcpJsonRepresentation recruiterWithHashedEmail = VisEntityViewedResumesOfTheMonth.ENTITY.getJsonWithHandledPrimaryKey(recruiter);
		CcpEntityMetaData monthDetails = VisEntityViewedResumesOfTheMonth.ENTITY.getEntityMetaData();
		// the transformer also keeps the original e-mail: only the hash calculates the id
		CcpJsonRepresentation recruiterKey = monthDetails.getOnlyExistingFields(recruiterWithHashedEmail);
		CcpJsonRepresentation recruiterKeyInTheMonth = recruiterKey.put(JnJsonCommonsFields.disposablePeriod, month);
		String parentId = VisEntityViewedResumesOfTheMonth.ENTITY.calculateId(recruiterKeyInTheMonth);
		CcpEntityMetaData viewDetails = VisEntityViewedResume.ENTITY.getEntityMetaData();
		CcpJsonRepresentation groupKey = JnGroupers.getGroupKey(monthDetails.entityName, viewDetails.entityName, parentId);
		return groupKey;
	}

	/**
	 * The key, in {@code jn_grouper}, of the group of every view of the recruiter, whatever the month.
	 * @param json {@code email} (the recruiter)
	 * @return the key: parent id, parent name and child name
	 */
	static CcpJsonRepresentation getGroupKeyOfTheRecruiter(CcpJsonRepresentation json) {
		CcpJsonRepresentation recruiter = json.getJsonPiece(VisEntityViewedResumesOfTheRecruiter.Fields.email);
		CcpJsonRepresentation recruiterWithHashedEmail = VisEntityViewedResumesOfTheRecruiter.ENTITY.getJsonWithHandledPrimaryKey(recruiter);
		CcpEntityMetaData recruiterDetails = VisEntityViewedResumesOfTheRecruiter.ENTITY.getEntityMetaData();
		// the transformer also keeps the original e-mail: only the hash calculates the id
		CcpJsonRepresentation recruiterKey = recruiterDetails.getOnlyExistingFields(recruiterWithHashedEmail);
		String parentId = VisEntityViewedResumesOfTheRecruiter.ENTITY.calculateId(recruiterKey);
		CcpEntityMetaData viewDetails = VisEntityViewedResume.ENTITY.getEntityMetaData();
		CcpJsonRepresentation groupKey = JnGroupers.getGroupKey(recruiterDetails.entityName, viewDetails.entityName, parentId);
		return groupKey;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String month = json.getAsString(Fields.month);
		CcpJsonRepresentation groupKey = getGroupKeyOfTheMonth(json);
		CcpJsonRepresentation groupOnBothSides = JnBusinessGetGroupOnBothSides.INSTANCE.execute(groupKey);
		List<String> mainViews = getStoredViews(groupOnBothSides, false);
		List<String> twinViews = getStoredViews(groupOnBothSides, true);
		List<String> viewedResume = getViewsToShow(mainViews);
		List<String> unwantedResume = getViewsToShow(twinViews);
		CcpJsonRepresentation responseWithMonth = CcpOtherConstants.EMPTY_JSON.put(Fields.month, month);
		CcpJsonRepresentation responseWithViewed = responseWithMonth.put(VisEntityViewedResumesOfTheMonth.Fields.viewedResume, viewedResume);
		CcpJsonRepresentation response = responseWithViewed.put(VisRecruiterResumesFields.unwantedResume, unwantedResume);
		return response;
	}

	/**
	 * The children of a side of the group of the month.
	 * @param groupOnBothSides the group read on both sides of the grouper, by the name of each side
	 * @param twinSide whether the twin side ({@code jn_inactive_grouper}) is wanted
	 * @return the JSON texts of the views, empty when that side has no group
	 */
	static List<String> getStoredViews(CcpJsonRepresentation groupOnBothSides, boolean twinSide) {
		CcpEntity grouper = JnGroupers.getGrouper(twinSide);
		CcpJsonRepresentation group = groupOnBothSides.getInnerJson(grouper);
		List<String> storedViews = group.getAsStringList(JnEntityGrouper.Fields.child);
		return storedViews;
	}

	/**
	 * The view of the recruiter in the month whose LinkedIn has the given hash, on either side of the group of the month,
	 * as the group keeps it ({@code linkedin} as it came, {@code alias}, {@code date}, {@code ddd} and {@code comment}).
	 * @param json {@code email} (the recruiter), {@code month} ({@code yyyyMM}) and {@code linkedinHash}
	 * @return the view, parsed
	 * @throws CcpErrorFlowDisturb {@code 404} when no view of the recruiter in the month has a LinkedIn of this hash
	 */
	static CcpJsonRepresentation getViewOfTheMonth(CcpJsonRepresentation json) {
		String linkedinHash = json.getAsString(VisRecruiterResumesFields.linkedinHash);
		CcpJsonRepresentation groupKey = getGroupKeyOfTheMonth(json);
		CcpJsonRepresentation groupOnBothSides = JnBusinessGetGroupOnBothSides.INSTANCE.execute(groupKey);
		List<String> mainViews = getStoredViews(groupOnBothSides, false);
		List<String> twinViews = getStoredViews(groupOnBothSides, true);
		List<String> views = new ArrayList<>(mainViews);
		views.addAll(twinViews);
		for (String viewText : views) {
			CcpJsonRepresentation view = new CcpJsonRepresentation(viewText);
			String viewedLinkedin = view.getOrDefault(VisEntityViewedResume.Fields.linkedin, () -> "");
			boolean noLinkedin = viewedLinkedin.isEmpty();

			if(noLinkedin) {
				continue;
			}

			String viewedLinkedinHash = VisBusinessSendResumesToRecruiters.getLinkedinHash(viewedLinkedin);
			boolean anotherLinkedin = false == linkedinHash.equals(viewedLinkedinHash);

			if(anotherLinkedin) {
				continue;
			}

			return view;
		}
		throw new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.NOT_FOUND);
	}

	/**
	 * The views as the screen shows them: the LinkedIn replaced by its hash.
	 * @param storedViews the JSON texts of the views, as the group keeps them
	 * @return the JSON texts with {@code linkedinHash} in place of {@code linkedin}
	 */
	private static List<String> getViewsToShow(List<String> storedViews) {
		List<String> viewedResume = new ArrayList<>();
		for (String storedViewText : storedViews) {
			CcpJsonRepresentation storedView = new CcpJsonRepresentation(storedViewText);
			String linkedin = storedView.getAsString(VisEntityViewedResume.Fields.linkedin);
			String linkedinHash = VisBusinessSendResumesToRecruiters.getLinkedinHash(linkedin);
			CcpJsonRepresentation viewWithoutLinkedin = storedView.removeFields(VisEntityViewedResume.Fields.linkedin);
			CcpJsonRepresentation viewToShow = viewWithoutLinkedin.put(VisRecruiterResumesFields.linkedinHash, linkedinHash);
			String viewToShowText = viewToShow.asUgglyJson();
			viewedResume.add(viewToShowText);
		}
		return viewedResume;
	}
}
