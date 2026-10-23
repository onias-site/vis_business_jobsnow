
package com.vis.entities;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
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
import java.util.stream.Stream;

/**
 * Represents the grouping of company names by the first three letters of the e-mail domain.
 * Allows fast company lookups by prefix. Cached for 1 hour. Includes the initial data load logic.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.class)
public class VisEntityGroupCompaniesByTheirFirstThreeInitials implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityGroupCompaniesByTheirFirstThreeInitials INSTANCE = new VisEntityGroupCompaniesByTheirFirstThreeInitials();

	private VisEntityGroupCompaniesByTheirFirstThreeInitials() {}

	/** The entity {@code vis_group_companies_by_their_first_three_initials}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityGroupCompaniesByTheirFirstThreeInitials.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code firstThreeInitials} field: part of the primary key, text. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(exactLength = 3)
		firstThreeInitials,
		/** The {@code companies} field: text, list, required. */
		@CcpJsonFieldTypeString(minLength = 3, maxLength = 30)
		@CcpJsonFieldValidatorArray(minSize = 1)
		@CcpJsonFieldValidatorRequired
		companies,
		/** The {@code timestamp} field: validated as in {@code JnJsonCommonsFields}. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		timestamp,
		/** The {@code date} field: validated as in {@code JnJsonCommonsFields}. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		date,
		;

	}

	/**
	 * Seeds the groups from the legacy {@code old_recruiters} index: the company name is the first part of the domain of the
	 * recruiter e-mail (capitalized, at least 3 letters), grouped by its first three letters. Until 2026-10-22 the groups
	 * were kept in a static field, so a second call added its companies to the ones of the first, and any failure (a
	 * missing legacy index included) was swallowed and resulted in no seed; now a failure reaches the caller, as in
	 * {@link VisEntityRecruiter}.
	 * @return the seed records
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		CcpQueryExecutor queryExecutor = CcpDependencyInjection.getDependency(CcpQueryExecutor.class);
		CcpQueryOptions query = CcpQueryOptions.INSTANCE.matchAll();
		Map<String, Set<String>> groupedCompanies = new LinkedHashMap<>();

		Consumer<CcpJsonRepresentation> consumer = new VisCompanyNameGrouperByInitials(groupedCompanies);
		queryExecutor.consumeQueryResult(query, new String[] {"old_recruiters"}, "1s", 10000, consumer, "id");
		Set<Entry<String, Set<String>>> groups = groupedCompanies.entrySet();
		Stream<Entry<String, Set<String>>> groupsStream = groups.stream();
		var bulkItemsStream = groupsStream.map(group -> this.toBulkItem(group.getKey(), group.getValue()));

		List<CcpBulkItem> bulkItems = bulkItemsStream.collect(Collectors.toList());

		return bulkItems;
	}

	/**
	 * Builds the {@code create} item of one group.
	 * @param initials the first three letters
	 * @param companies the companies whose names start with them
	 * @return the bulk item
	 */
	private CcpBulkItem toBulkItem(String initials, Set<String> companies) {
		CcpJsonRepresentation jsonWithInitials = CcpOtherConstants.EMPTY_JSON
		.put(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.firstThreeInitials, initials);

		CcpJsonRepresentation json = jsonWithInitials
		.put(VisEntityGroupCompaniesByTheirFirstThreeInitials.Fields.companies, companies);
		String recordId = ENTITY.calculateId(json);
		CcpBulkItem item = new CcpBulkItem(json, CcpBulkEntityOperationType.create, ENTITY, recordId);
		return item;
	}

}
