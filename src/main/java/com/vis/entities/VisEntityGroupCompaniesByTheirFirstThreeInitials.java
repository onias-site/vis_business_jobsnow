
package com.vis.entities;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.query.CcpQueryExecutor;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.annotations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;

@CcpEntityCache(3600)
@CcpEntitySpecifications(
		entityFieldsTransformers = JnJsonTransformersFieldsEntityDefault.class,
		entityValidation = VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.class,
		afterDeleteRecord = {},
		beforeSaveRecord = {},
		afterSaveRecord = {},
		flow = {}
)
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
		CcpQueryExecutor queryExecutor = CcpDependencyInjection.getDependency(CcpQueryExecutor.class);
		CcpQueryOptions query = CcpQueryOptions.INSTANCE.matchAll();
		
		Consumer<CcpJsonRepresentation> consumer = json -> {
			String x = json.getDynamicVersion().getAsString("id");
				String[] split = x.split("@");
				if(split.length != 2) {
					return;
				}
				
				
			String domain = split[1];
			
			String[] split1 = domain.split("\\.");			

			String companyName = split1[0].toUpperCase().trim();
			
			if(companyName.length() < 3) {
				return;
			}
			
			String capitalizedCompanyName = new CcpStringDecorator(companyName).text().capitalize().content;
			
			String initials = companyName.substring(0, 3);
			
			LinkedHashSet<String> orDefault = groupedCompanies.getDynamicVersion().getOrDefault(initials, new LinkedHashSet<>());
			orDefault.add(capitalizedCompanyName);
			groupedCompanies = groupedCompanies.getDynamicVersion().put(initials, orDefault);
		};
		queryExecutor.consumeQueryResult(query, new String[] {"old_recruiters"}, "1s", 10000, consumer, "id");

		List<CcpBulkItem> collect = groupedCompanies.fieldSet().stream().map(initials -> this.toBulkItem(initials)).collect(Collectors.toList());
		
		return collect;
	}
	
	private CcpBulkItem toBulkItem(String initials) {
		Set<String> companies = groupedCompanies.getDynamicVersion().getAsObject(initials);
		
		CcpJsonRepresentation json = CcpOtherConstants.EMPTY_JSON
		.put(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.firstThreeInitials, initials)
		.put(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.companies, companies);
		
		CcpBulkItem item = new CcpBulkItem(json, CcpBulkEntityOperationType.create, ENTITY);
		return item;
	}
	static CcpJsonRepresentation groupedCompanies = CcpOtherConstants.EMPTY_JSON;

}
