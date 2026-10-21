package com.vis.json.transformers;

import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.db.utils.entity.fields.CcpJsonTransformersDefaultEntityField;
import com.jn.entities.JnEntitySystemMessage;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;
import com.vis.entities.VisEntityRecruiter;

/**
 * Field transformer applied to the {@code professional} of the VisEntityRecruiter entity while it is persisted: the
 * e-mail is professional when its domain (the text after the {@code @}) is not in the list of non-professional domains
 * (public webmail providers such as {@code gmail.com}), which is the {@code message} of the {@code jn_system_message}
 * record {@code NONPROFESSIONALDOMAINS} in Portuguese. The domain is taken from the e-mail itself, so this transformer
 * does not depend on the one of the {@code domain} field having run before. A json with no e-mail is returned as is.
 */
public class VisJsonTransformerRecruiterProfessional implements CcpJsonTransformersDefaultEntityField {

	/**
	 * Puts in {@code professional} whether the domain of the e-mail is out of the non-professional domains.
	 * @param json the recruiter
	 * @return the recruiter with the flag
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		boolean noEmail = false == json.containsAllFields(VisEntityRecruiter.Fields.email);

		if(noEmail) {
			return json;
		}

		String email = json.getAsString(VisEntityRecruiter.Fields.email);
		CcpStringDecorator emailText = new CcpStringDecorator(email);
		CcpEmailDecorator emailDecorator = emailText.email();
		String domain = emailDecorator.getDomain();

		CcpJsonRepresentation systemMessageName = CcpOtherConstants.EMPTY_JSON
				.put(JnEntitySystemMessage.Fields.systemMessageName, JnEntitySystemMessage.NON_PROFESSIONAL_DOMAINS);
		CcpJsonRepresentation systemMessageId = systemMessageName.put(JnJsonCommonsFields.language, JnLanguage.portuguese);
		// cached for an hour by the entity: a load of thousands of recruiters reads the database once
		CcpJsonRepresentation nonProfessionalDomainsMessage = JnEntitySystemMessage.ENTITY.getOneById(systemMessageId);
		List<String> nonProfessionalDomains = nonProfessionalDomainsMessage.getAsStringList(JnJsonCommonsFields.message);

		boolean professional = false == nonProfessionalDomains.contains(domain);
		CcpJsonRepresentation jsonWithProfessional = json.put(VisEntityRecruiter.Fields.professional, professional);
		return jsonWithProfessional;
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
	 * @return {@code professional}
	 */
	public String name() {
		String fieldName = VisEntityRecruiter.Fields.professional.name();
		return fieldName;
	}
}
