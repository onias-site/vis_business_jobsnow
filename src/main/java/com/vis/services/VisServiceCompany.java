package com.vis.services;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.services.JnService;
import com.vis.entities.VisEntityGroupCompaniesByTheirFirstThreeInitials;

public enum VisServiceCompany implements JnService {

	SearchCompaniesByTheirFirstThreeInitials{

		@Override
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			
			var search = json.getAsString(VisServiceCompany.FieldsToSearchCompaniesByTheirFirstThreeInitials.search);	
			var threeInitials = search.substring(0, 3);
			var querySearch = json.put(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.firstThreeInitials, threeInitials);
			CcpBusiness retrievesEmptyCompaniesList = query -> query.put(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.companies, Arrays.asList(search));
			CcpJsonRepresentation searchResult = VisEntityGroupCompaniesByTheirFirstThreeInitials.ENTITY.getOneByIdOrHandleItIfThisIdWasNotFound(querySearch, retrievesEmptyCompaniesList);
			var jsonPiece = searchResult.getJsonPiece(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.companies);
			
			var typedJustThreeCharacters = search.equals(threeInitials);
			
			if(typedJustThreeCharacters) {
				return jsonPiece;
			}

			var companies = jsonPiece.getAsStringList(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.companies);
			var filteredCompanies = companies.stream().filter(x -> x.toUpperCase().startsWith(search.toUpperCase())).collect(Collectors.toList());
			if(filteredCompanies.isEmpty()) {
				filteredCompanies = Arrays.asList(search);
			}
			var searchResultWithFilteredCompanies = jsonPiece.put(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.companies, filteredCompanies);
			return searchResultWithFilteredCompanies;
		}

		public Class<?> getJsonValidationClass() {
			return FieldsToSearchCompaniesByTheirFirstThreeInitials.class;
		}
		
	}
	;
	public static enum FieldsToSearchCompaniesByTheirFirstThreeInitials implements CcpJsonFieldName{
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 20)
		@CcpJsonFieldValidatorRequired
		search
	}
}
