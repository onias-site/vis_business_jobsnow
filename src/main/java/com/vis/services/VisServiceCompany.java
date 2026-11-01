package com.vis.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.services.JnService;
import com.jn.utils.JnGroupers;
import com.vis.entities.VisEntityRecruiter;
import com.vis.entities.VisEntityVirtualRecruiterDomainInitialsGrouper;

/**
 * Company data access service. Exposes the operations related to searching companies by name.
 * Each constant is a service endpoint.
 */
public enum VisServiceCompany implements JnService {

	/**
	 * Suggests company names for the typed text: reads, in {@code jn_grouper}, the group of the domains that start with
	 * the first three letters of the text and keeps the company names (the first part of the domain, capitalized) that
	 * start with the text (case insensitive), in alphabetical order; when nothing matches, the text itself is the only
	 * suggestion. Until 2026-10-27 the names came from the entity
	 * {@code vis_group_companies_by_their_first_three_initials}.
	 */
	SearchCompaniesByTheirFirstThreeInitials{

		/**
		 * Runs the search.
		 * @param json the request with {@code search}
		 * @return {@code companies}: the suggested names
		 */
		@Override
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			String search = json.getAsString(VisServiceCompany.FieldsToSearchCompaniesByTheirFirstThreeInitials.search);
			String upperCaseSearch = search.toUpperCase();
			List<String> domains = getDomainsOfTheGroup(search);

			Set<String> companies = new TreeSet<>();
			for (String domain : domains) {
				String companyName = VisEntityVirtualRecruiterDomainInitialsGrouper.getCompanyName(domain);
				String upperCaseCompanyName = companyName.toUpperCase();
				boolean anotherCompany = false == upperCaseCompanyName.startsWith(upperCaseSearch);

				if(anotherCompany) {
					continue;
				}

				companies.add(companyName);
			}

			boolean noCompanyFound = companies.isEmpty();

			if(noCompanyFound) {
				List<String> onlyTheSearch = Arrays.asList(search);
				return CcpOtherConstants.EMPTY_JSON.put(FieldsToSearchCompaniesByTheirFirstThreeInitials.companies, onlyTheSearch);
			}

			List<String> companiesList = new ArrayList<>(companies);
			CcpJsonRepresentation searchResult = CcpOtherConstants.EMPTY_JSON.put(FieldsToSearchCompaniesByTheirFirstThreeInitials.companies, companiesList);
			return searchResult;
		}

		/**
		 * Validates the input with {@link FieldsToSearchCompaniesByTheirFirstThreeInitials}.
		 * @return the validation class
		 */
		public Class<?> getJsonValidationClass() {
			return FieldsToSearchCompaniesByTheirFirstThreeInitials.class;
		}

	}
	;

	/**
	 * The domains of the group of the first three letters of the text: one read by id.
	 * @param search the typed text, of at least three characters
	 * @return the domains of the group, none when the group does not exist
	 */
	private static List<String> getDomainsOfTheGroup(String search) {
		CcpEntityMetaData parentDetails = VisEntityVirtualRecruiterDomainInitialsGrouper.ENTITY.getEntityMetaData();
		CcpEntityMetaData recruiterDetails = VisEntityRecruiter.ENTITY.getEntityMetaData();
		String parentId = VisEntityVirtualRecruiterDomainInitialsGrouper.getParentId(search);
		List<String> domains = JnGroupers.getChildren(false, parentDetails.entityName, recruiterDetails.entityName, parentId);
		return domains;
	}

	/** Input and output fields of the company search. */
	public static enum FieldsToSearchCompaniesByTheirFirstThreeInitials implements CcpJsonFieldName{
		/** The {@code search} field: text, required. */
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 50)
		@CcpJsonFieldValidatorRequired
		search,
		/** The {@code companies} field of the output: the suggested names. */
		companies,
	}
}
