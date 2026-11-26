package com.vis.json.transformers;

import java.util.TreeMap;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.fields.CcpJsonTransformersDefaultEntityField;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * Field transformer of the {@code json} of {@code vis_resume_filter} (since 2026-11-25), the primary key of the entity:
 * keeps the filters sorted by name and with every number of the same type, so the same filters give the same id whatever
 * the order the screen sends them in and whoever parsed them (the id is the hash of the text of the filters, the text of
 * a map follows the order of its keys, and an integer parsed by the REST API prints as {@code 1} where the same number
 * parsed from a JSON text prints as {@code 1.0}). A record with no filters is returned as is.
 */
public class VisJsonTransformerResumeFilterSortedFields implements CcpJsonTransformersDefaultEntityField {
	/** The single instance: the class keeps no state. */
	public final static VisJsonTransformerResumeFilterSortedFields INSTANCE = new VisJsonTransformerResumeFilterSortedFields();

	private VisJsonTransformerResumeFilterSortedFields() {}

	/**
	 * Sorts the filters by name.
	 * @param record the record of {@code vis_resume_filter}
	 * @return the record with the filters sorted
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation record) {
		boolean noFilters = false == record.containsAllFields(JnJsonCommonsFields.json);

		if(noFilters) {
			return record;
		}

		CcpJsonRepresentation filters = record.getInnerJson(JnJsonCommonsFields.json);
		// through the text and back, every number becomes the same type (1 and 1.0 are the same filter)
		String filtersText = filters.asUgglyJson();
		CcpJsonRepresentation parsedFilters = new CcpJsonRepresentation(filtersText);
		TreeMap<String, Object> sortedFilters = new TreeMap<>(parsedFilters.content);
		CcpJsonRepresentation recordWithSortedFilters = record.put(JnJsonCommonsFields.json, sortedFilters);
		return recordWithSortedFilters;
	}

	/**
	 * Tells that the result can be part of a primary key: it only changes the order of the filters.
	 * @return {@code true}
	 */
	public boolean canBePrimaryKey() {
		return true;
	}

	/**
	 * Returns the name of the transformed field.
	 * @return {@code json}
	 */
	public String name() {
		String fieldName = JnJsonCommonsFields.json.name();
		return fieldName;
	}
}
