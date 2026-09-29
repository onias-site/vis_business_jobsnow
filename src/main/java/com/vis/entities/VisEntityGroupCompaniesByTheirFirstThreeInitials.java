
package com.vis.entities;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.query.CcpQueryExecutor;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.ccp.decorators.CcpTextDecorator;
import java.util.stream.Stream;

/**
 * Represents the grouping of company names by the first three letters of the e-mail domain.
 * Allows fast company lookups by prefix. Cached for 1 hour. Includes the initial data load logic.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.class)
public class VisEntityGroupCompaniesByTheirFirstThreeInitials implements CcpEntityConfigurator {

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityGroupCompaniesByTheirFirstThreeInitials.class).entityInstance;
	
	public static enum Fields implements CcpJsonFieldName{
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(exactLength = 3)
		firstThreeInitials, 
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 30)
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonFieldValidatorRequired
		companies,
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp,
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date,
		;
		
	}
	
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		
		try {
			CcpQueryExecutor queryExecutor = CcpDependencyInjection.getDependency(CcpQueryExecutor.class);
			CcpQueryOptions query = CcpQueryOptions.INSTANCE.matchAll();
			
			Consumer<CcpJsonRepresentation> consumer = json -> {
				String recruiterEmail = json.getAsString(VisJsonCommonsFields.id);
					String[] emailParts = recruiterEmail.split("@");
					boolean isNotAnEmail = emailParts.length != 2;
					if(isNotAnEmail) {
						return;
					}
					
					
				String domain = emailParts[1];
				
				String[] domainParts = domain.split("\\.");			
				String upperCaseCompanyName = domainParts[0].toUpperCase();

				String companyName = upperCaseCompanyName.trim();
				int companyNameLength = companyName.length();
				boolean companyNameIsTooShort = companyNameLength < 3;

				if(companyNameIsTooShort) {
					return;
				}
				CcpStringDecorator companyNameDecorator = new CcpStringDecorator(companyName);
				CcpTextDecorator companyNameText = companyNameDecorator.text();
				var capitalizedText = companyNameText.capitalize();

				String capitalizedCompanyName = capitalizedText.content;
				
				String initials = companyName.substring(0, 3);
				CcpFieldName initialsFieldName = new CcpFieldName(initials);

				LinkedHashSet<String> companiesWithTheseInitials = groupedCompanies.getOrDefault(initialsFieldName, () -> new LinkedHashSet<>());
				companiesWithTheseInitials.add(capitalizedCompanyName);
				CcpFieldName sameInitialsFieldName = new CcpFieldName(initials);
				groupedCompanies = groupedCompanies.put(sameInitialsFieldName, companiesWithTheseInitials);
			};
			queryExecutor.consumeQueryResult(query, new String[] {"old_recruiters"}, "1s", 10000, consumer, "id");
			Set<String> allInitials = groupedCompanies.fieldSet();
			Stream<String> initialsStream = allInitials.stream();
			var bulkItemsStream = initialsStream.map(initials -> this.toBulkItem(initials));

			List<CcpBulkItem> bulkItems = bulkItemsStream.collect(Collectors.toList());
			
			return bulkItems;
		
		} catch (Exception e) {
			return new ArrayList<>();
		}
		
	}
	
	private CcpBulkItem toBulkItem(String initials) {
		CcpFieldName groupFieldName = new CcpFieldName(initials);
		Set<String> companies = groupedCompanies.getAsObject(groupFieldName);
		CcpJsonRepresentation jsonWithInitials = CcpOtherConstants.EMPTY_JSON
		.put(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.firstThreeInitials, initials);
	
		CcpJsonRepresentation json = jsonWithInitials
		.put(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.companies, companies);
		String recordId = ENTITY.calculateId(json);
		CcpBulkItem item = new CcpBulkItem(json, CcpBulkEntityOperationType.create, ENTITY, recordId);
		return item;
	}
	static CcpJsonRepresentation groupedCompanies = CcpOtherConstants.EMPTY_JSON;

}
