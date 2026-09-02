package com.vis.services;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.services.JnService;
import com.vis.entities.VisEntityGroupCompaniesByTheirFirstThreeInitials;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import java.util.stream.Stream;

/**
 * Serviço de acesso a dados de empresas. Expõe operações relacionadas à busca de empresas pelo nome.
 * Cada constante é um endpoint de serviço.
 */
public enum VisServiceCompany implements JnService {

	SearchCompaniesByTheirFirstThreeInitials{

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
			Stream<String> stream = companies.stream();
			var filter = stream.filter(x -> x.toUpperCase().startsWith(search.toUpperCase()));
			var filteredCompanies = filter.collect(Collectors.toList());
			var filteredCompaniesEmpty = filteredCompanies.isEmpty();
			if(filteredCompaniesEmpty) {
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
