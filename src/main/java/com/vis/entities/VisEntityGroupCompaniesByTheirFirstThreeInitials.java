
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
 * Representa o agrupamento de nomes de empresas pelas três primeiras letras do domínio de e-mail.
 * Permite buscas rápidas de empresas por prefixo. Possui cache de 1 hora. Inclui lógica de carga inicial de dados.
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
				CcpFieldName ccpFieldName = new CcpFieldName("id");
				String x = json.getAsString(ccpFieldName);
					String[] split = x.split("@");
					boolean lengthDiferente = split.length != 2;
					if(lengthDiferente) {
						return;
					}
					
					
				String domain = split[1];
				
				String[] split1 = domain.split("\\.");			
				String toUpperCase = split1[0].toUpperCase();

				String companyName = toUpperCase.trim();
				int companyNameLength = companyName.length();
				boolean companyNameLengthMenor = companyNameLength < 3;

				if(companyNameLengthMenor) {
					return;
				}
				CcpStringDecorator ccpStringDecorator = new CcpStringDecorator(companyName);
				CcpTextDecorator ccpStringDecoratorText = ccpStringDecorator.text();
				var capitalize = ccpStringDecoratorText.capitalize();

				String capitalizedCompanyName = capitalize.content;
				
				String initials = companyName.substring(0, 3);
				CcpFieldName ccpFieldName2 = new CcpFieldName(initials);

				LinkedHashSet<String> orDefault = groupedCompanies.getOrDefault(ccpFieldName2, () -> new LinkedHashSet<>());
				orDefault.add(capitalizedCompanyName);
				CcpFieldName ccpFieldName3 = new CcpFieldName(initials);
				groupedCompanies = groupedCompanies.put(ccpFieldName3, orDefault);
			};
			queryExecutor.consumeQueryResult(query, new String[] {"old_recruiters"}, "1s", 10000, consumer, "id");
			Set<String> fieldSet = groupedCompanies.fieldSet();
			Stream<String> stream = fieldSet.stream();
			var streamMap = stream.map(initials -> this.toBulkItem(initials));

			List<CcpBulkItem> collect = streamMap.collect(Collectors.toList());
			
			return collect;
		
		} catch (Exception e) {
			return new ArrayList<>();
		}
		
	}
	
	private CcpBulkItem toBulkItem(String initials) {
		CcpFieldName ccpFieldName4 = new CcpFieldName(initials);
		Set<String> companies = groupedCompanies.getAsObject(ccpFieldName4);
		CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON
		.put(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.firstThreeInitials, initials);
	
		CcpJsonRepresentation json = put
		.put(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.companies, companies);
		String calculateId = ENTITY.calculateId(json);
		CcpBulkItem item = new CcpBulkItem(json, CcpBulkEntityOperationType.create, ENTITY, calculateId);
		return item;
	}
	static CcpJsonRepresentation groupedCompanies = CcpOtherConstants.EMPTY_JSON;

}
