package com.vis.json.transformers;

import java.util.List;

import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.db.utils.entity.fields.CcpJsonTransformersDefaultEntityField;
import com.jn.entities.JnEntitySystemParameter;
import com.vis.entities.VisEntityPosition;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Field transformer applied to the {@code recruiterDomain} of the VisEntityPosition entity (since 2026-11-24): the domain
 * of the e-mail of the recruiter who owns the position, taken from the e-mail as it came (in {@code originalRecruiter}
 * when the transformer of the {@code recruiter} already ran, in {@code recruiter} when it did not). A non-professional
 * domain (the {@code NONPROFESSIONALDOMAINS} record of {@code jn_system_parameter}: gmail and the like) names no company,
 * so the field is removed. When the e-mail is no longer readable (the hash already calculated, the record coming back from
 * the queue) the json is returned as is, keeping the domain already calculated.
 */
public class VisJsonTransformerPositionRecruiterDomain implements CcpJsonTransformersDefaultEntityField {
	/** The single instance: the class keeps no state. */
	public final static VisJsonTransformerPositionRecruiterDomain INSTANCE = new VisJsonTransformerPositionRecruiterDomain();

	private VisJsonTransformerPositionRecruiterDomain() {}

	/**
	 * Puts in {@code recruiterDomain} the domain of the e-mail of the recruiter, or removes it for a non-professional one.
	 * @param json the position
	 * @return the position with the domain of its recruiter
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		boolean hashAlreadyCalculated = json.containsAllFields(VisJsonTransformerRecruiterHash.JsonFieldNames.originalRecruiter);
		boolean noRecruiter = false == hashAlreadyCalculated && false == json.containsAllFields(VisJsonCommonsFields.recruiter);

		if(noRecruiter) {
			return json;
		}

		String recruiter = hashAlreadyCalculated ? json.getAsString(VisJsonTransformerRecruiterHash.JsonFieldNames.originalRecruiter) : json.getAsString(VisJsonCommonsFields.recruiter);
		CcpStringDecorator recruiterText = new CcpStringDecorator(recruiter);
		CcpEmailDecorator recruiterEmail = recruiterText.email();
		boolean notReadable = false == recruiterEmail.isValid();

		if(notReadable) {
			return json;
		}

		String domain = recruiterEmail.getDomain();
		String lowerCaseDomain = domain.toLowerCase();
		// cached for an hour by the entity
		List<String> nonProfessionalDomains = JnEntitySystemParameter.getValues(JnEntitySystemParameter.NON_PROFESSIONAL_DOMAINS);
		boolean nonProfessional = nonProfessionalDomains.contains(lowerCaseDomain);

		if(nonProfessional) {
			return json.removeFields(VisEntityPosition.Fields.recruiterDomain);
		}

		CcpJsonRepresentation jsonWithDomain = json.put(VisEntityPosition.Fields.recruiterDomain, lowerCaseDomain);
		return jsonWithDomain;
	}

	/**
	 * Tells that the result can not be part of a primary key: it depends on a list kept in the database.
	 * @return {@code false}
	 */
	public boolean canBePrimaryKey() {
		return false;
	}

	/**
	 * Returns the name of the transformed field.
	 * @return {@code recruiterDomain}
	 */
	public String name() {
		String fieldName = VisEntityPosition.Fields.recruiterDomain.name();
		return fieldName;
	}
}
