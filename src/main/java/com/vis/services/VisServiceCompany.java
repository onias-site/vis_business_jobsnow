package com.vis.services;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.services.JnService;
import com.vis.entities.VisEntityGroupCompaniesByTheirFirstThreeInitials;

/**
 * Company data access service. Exposes the operations related to searching companies by name.
 * Each constant is a service endpoint.
 */ 
public enum VisServiceCompany implements JnService {

	/**
	 * Suggests company names for the typed text: reads the group of its first three letters and keeps the names that start
	 * with the text (case insensitive); when nothing matches, the text itself is the only suggestion.
	 */
	SearchCompaniesByTheirFirstThreeInitials{

		/**
		 * Runs the search.
		 * @param json the request with {@code search}
		 * @return {@code companies}: the suggested names
		 */
		@Override
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			
			var search = json.getAsString(VisServiceCompany.FieldsToSearchCompaniesByTheirFirstThreeInitials.search);	
			var threeInitials = search.substring(0, 3);
			var querySearch = json.put(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.firstThreeInitials, threeInitials);
			CcpBusiness retrievesEmptyCompaniesList = query -> query.put(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.companies, Arrays.asList(search));
			CcpEntityMetaData entityMetaData = VisEntityGroupCompaniesByTheirFirstThreeInitials.ENTITY.getEntityMetaData();
			CcpJsonRepresentation searchResult = entityMetaData.getOneByIdOrHandleItIfThisIdWasNotFound(querySearch, retrievesEmptyCompaniesList);
			var jsonPiece = searchResult.getJsonPiece(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.companies);
			
			var typedJustThreeCharacters = search.equals(threeInitials);
			
			if(typedJustThreeCharacters) {
				return jsonPiece;
			}

			var companies = jsonPiece.getAsStringList(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.companies);
			Stream<String> companiesStream = companies.stream();
			var companiesStartingWithSearch = companiesStream.filter(x -> x.toUpperCase().startsWith(search.toUpperCase()));
			var filteredCompanies = companiesStartingWithSearch.collect(Collectors.toList());
			var noCompanyFound = filteredCompanies.isEmpty();
			if(noCompanyFound) {
				filteredCompanies = Arrays.asList(search);
			}
			var searchResultWithFilteredCompanies = jsonPiece.put(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.companies, filteredCompanies);
			return searchResultWithFilteredCompanies;
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
	/** Input fields of the company search. */
	public static enum FieldsToSearchCompaniesByTheirFirstThreeInitials implements CcpJsonFieldName{
		/** The {@code search} field: text, required. */
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 20)
		@CcpJsonFieldValidatorRequired
		search
	}
}
