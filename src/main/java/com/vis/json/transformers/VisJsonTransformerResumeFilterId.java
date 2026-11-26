package com.vis.json.transformers;

import com.ccp.decorators.CcpHashDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.db.utils.entity.fields.CcpJsonTransformersDefaultEntityField;
import com.ccp.hash.CcpHashAlgorithm;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * Field transformer of the {@code id} of {@code vis_resume_filter} (since 2026-11-26), the primary key of the entity: the
 * SHA-1 of the text of the filters ({@code json}) in their canonical form ({@code VisJsonTransformerResumeFilterSortedFields}:
 * the keys of the filters and of every inner json in alphabetical order, the lists sorted and the numbers of one type),
 * so the same filters always have the same id, on any day. The id holds no day: the link of a search made at 23:59 still
 * opens the next day, from the copy of the record in {@code jn_disposable_record}, valid for 24 hours after the save.
 * A record with no filters (a read by the id) is returned as is.
 */
public class VisJsonTransformerResumeFilterId implements CcpJsonTransformersDefaultEntityField {
	/** The single instance: the class keeps no state. */
	public final static VisJsonTransformerResumeFilterId INSTANCE = new VisJsonTransformerResumeFilterId();

	private VisJsonTransformerResumeFilterId() {}

	/**
	 * Puts in the record the id of its filters.
	 * @param record the record of {@code vis_resume_filter}
	 * @return the record with the id of the filters, or as is when it has no filters
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation record) {
		boolean noFilters = false == record.containsAllFields(JnJsonCommonsFields.json);

		if(noFilters) {
			return record;
		}

		CcpJsonRepresentation recordWithSortedFilters = VisJsonTransformerResumeFilterSortedFields.INSTANCE.execute(record);
		CcpJsonRepresentation sortedFilters = recordWithSortedFilters.getInnerJson(JnJsonCommonsFields.json);
		String sortedFiltersText = sortedFilters.asUgglyJson();
		CcpStringDecorator sortedFiltersDecorator = new CcpStringDecorator(sortedFiltersText);
		CcpHashDecorator hashDecorator = sortedFiltersDecorator.hash();
		String filterId = hashDecorator.asString(CcpHashAlgorithm.SHA1);
		CcpJsonRepresentation recordWithTheId = record.put(JnJsonCommonsFields.id, filterId);
		return recordWithTheId;
	}

	/**
	 * Tells that the result can be part of a primary key: the same filters always give the same hash.
	 * @return {@code true}
	 */
	public boolean canBePrimaryKey() {
		return true;
	}

	/**
	 * Returns the name of the transformed field.
	 * @return {@code id}
	 */
	public String name() {
		String fieldName = JnJsonCommonsFields.id.name();
		return fieldName;
	}
}
