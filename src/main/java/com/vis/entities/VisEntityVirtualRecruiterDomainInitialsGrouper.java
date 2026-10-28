package com.vis.entities;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTextDecorator;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;

/**
 * Virtual entity whose id identifies a group of recruiters of {@code JnEntityGrouper}: the first three letters, in upper
 * case, of the domain of the e-mail of the recruiter. A professional {@link VisEntityRecruiter} is a child of the group
 * of the initials of its domain (a non-professional domain, a public webmail provider, puts the recruiter in no group),
 * so the search of companies by name reads, by id, the recruiters whose company starts like the typed text. Until
 * 2026-10-27 the entity {@code vis_group_companies_by_their_first_three_initials} kept the company names of each group.
 * Cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualRecruiterDomainInitialsGrouper.Fields.class)
public class VisEntityVirtualRecruiterDomainInitialsGrouper {

	/** The entity {@code vis_virtual_recruiter_domain_initials_grouper}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualRecruiterDomainInitialsGrouper.class).entityInstance;

	/** How many letters the initials have. */
	public static final int INITIALS_LENGTH = 3;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code firstThreeInitials} field: the primary key, the first three letters of a domain in upper case. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(exactLength = 3)
		firstThreeInitials,
		;
	}

	/** The texts of this class. */
	enum Constants implements CcpJsonFieldName{
		/** The pattern that splits a domain in its parts. */
		DOMAIN_SEPARATOR { public String getValue() { return "\\."; } },
	}

	/**
	 * The combination of the group of a text: its first three letters, in upper case.
	 * @param text a domain, or a company name typed in the search, of at least {@link #INITIALS_LENGTH} characters
	 * @return the combination
	 */
	public static CcpJsonRepresentation getCombination(String text) {
		String upperCaseText = text.toUpperCase();
		String firstThreeInitials = upperCaseText.substring(0, INITIALS_LENGTH);
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(Fields.firstThreeInitials, firstThreeInitials);
		return combination;
	}

	/**
	 * The parent id of the group of a text, the {@code parentId} of its group in {@code JnEntityGrouper}.
	 * @param text a domain, or a company name typed in the search, of at least {@link #INITIALS_LENGTH} characters
	 * @return the parent id
	 */
	public static String getParentId(String text) {
		CcpJsonRepresentation combination = getCombination(text);
		String parentId = ENTITY.calculateId(combination);
		return parentId;
	}

	/**
	 * The name of the company of a domain: its first part, capitalized ({@code empresa.com.br} is {@code Empresa}).
	 * @param domain the domain of the e-mail of a recruiter
	 * @return the company name
	 */
	public static String getCompanyName(String domain) {
		String domainSeparator = Constants.DOMAIN_SEPARATOR.getValue();
		String[] domainParts = domain.split(domainSeparator);
		String firstPart = domainParts[0];
		String trimmedFirstPart = firstPart.trim();
		String upperCaseCompanyName = trimmedFirstPart.toUpperCase();
		CcpStringDecorator companyNameDecorator = new CcpStringDecorator(upperCaseCompanyName);
		CcpTextDecorator companyNameText = companyNameDecorator.text();
		CcpTextDecorator capitalizedText = companyNameText.capitalize();
		String companyName = capitalizedText.content;
		return companyName;
	}
}
