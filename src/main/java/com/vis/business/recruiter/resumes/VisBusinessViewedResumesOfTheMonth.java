package com.vis.business.recruiter.resumes;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.business.grouper.JnBusinessGetGroup;
import com.jn.entities.JnEntityGrouper;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnGroupers;
import com.vis.entities.VisEntityViewedResume;
import com.vis.entities.VisEntityViewedResumesOfTheMonth;

/**
 * The resumes a recruiter viewed in a month, read by id (OQL): the id of the group of the recruiter in the month
 * ({@code VisEntityViewedResumesOfTheMonth}, its period given in {@code disposablePeriod}) and then the group itself in
 * {@code jn_grouper}, whose children the saves of {@code vis_viewed_resume} keep up to date. No query runs.
 *
 * <p>Input: {@code email} (the recruiter) and {@code month} ({@code yyyyMM}). Output: {@code month} and
 * {@code viewedResume}, the list of the JSON texts of the views ({@code linkedin}, {@code alias} and {@code date} of the
 * last view of each resume), empty when the recruiter viewed nothing in the month.
 */
public class VisBusinessViewedResumesOfTheMonth implements CcpBusiness {

	/** The input of this business, with its validation rules. */
	public static enum Fields implements CcpJsonFieldName {
		/** The recruiter. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		email,

		/** The month of the views, as {@code yyyyMM} (the format of the monthly disposable entities). */
		@CcpJsonFieldTypeString(regexValidation = "^[0-9]{4}(0[1-9]|1[0-2])$")
		@CcpJsonFieldValidatorRequired
		month,
	}

	/** The single instance. */
	public static final VisBusinessViewedResumesOfTheMonth INSTANCE = new VisBusinessViewedResumesOfTheMonth();

	private VisBusinessViewedResumesOfTheMonth() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	/**
	 * The key, in {@code jn_grouper}, of the group of the views of the recruiter in the month.
	 * @param json {@code email} (the recruiter) and {@code month} ({@code yyyyMM})
	 * @return the key: parent id, parent name and child name
	 */
	static CcpJsonRepresentation getGroupKeyOfTheMonth(CcpJsonRepresentation json) {
		String month = json.getAsString(Fields.month);
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

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String month = json.getAsString(Fields.month);
		CcpJsonRepresentation groupKey = getGroupKeyOfTheMonth(json);
		CcpJsonRepresentation group = JnBusinessGetGroup.INSTANCE.execute(groupKey);
		List<String> viewedResume = group.getAsStringList(JnEntityGrouper.Fields.child);
		CcpJsonRepresentation responseWithMonth = CcpOtherConstants.EMPTY_JSON.put(Fields.month, month);
		CcpJsonRepresentation response = responseWithMonth.put(VisEntityViewedResumesOfTheMonth.Fields.viewedResume, viewedResume);
		return response;
	}
}
