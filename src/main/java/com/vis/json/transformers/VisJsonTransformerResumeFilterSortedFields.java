package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.fields.CcpJsonTransformersDefaultEntityField;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * Field transformer of the {@code json} of {@code vis_resume_filter} (since 2026-11-25), the primary key of the entity:
 * keeps the filters in one canonical form, so the same filters give the same id whatever the order the screen sends them
 * in and whoever parsed them (the id is the hash of the text of the filters). Every map, at any depth, is sorted by its
 * keys; every list, at any depth, is sorted by the text of its items, since every list of the filters is a set (the
 * required skills {@code [JAVA, SQL]} and {@code [SQL, JAVA]} are the same filter, as are the ddds, the seniorities and
 * the languages); and every number gets the same type (an integer parsed by the REST API prints as {@code 1} where the
 * same number parsed from a JSON text prints as {@code 1.0}). A record with no filters is returned as is.
 */
public class VisJsonTransformerResumeFilterSortedFields implements CcpJsonTransformersDefaultEntityField {
	/** The single instance: the class keeps no state. */
	public final static VisJsonTransformerResumeFilterSortedFields INSTANCE = new VisJsonTransformerResumeFilterSortedFields();

	private VisJsonTransformerResumeFilterSortedFields() {}

	/**
	 * Puts the filters in their canonical form.
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
		Object sortedFilters = this.canonical(parsedFilters.content);
		CcpJsonRepresentation recordWithSortedFilters = record.put(JnJsonCommonsFields.json, sortedFilters);
		return recordWithSortedFilters;
	}

	/**
	 * The canonical form of a value of the filters: maps sorted by key, lists sorted by the text of their items, both at
	 * any depth; any other value as it is.
	 * @param value a value of the filters
	 * @return the value in its canonical form
	 */
	private Object canonical(Object value) {
		if(value instanceof Map<?, ?> map) {
			return this.canonicalMap(map);
		}
		if(value instanceof Collection<?> collection) {
			return this.canonicalList(collection);
		}
		return value;
	}

	private TreeMap<String, Object> canonicalMap(Map<?, ?> map) {
		TreeMap<String, Object> sortedMap = new TreeMap<>();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			Object key = entry.getKey();
			String keyText = String.valueOf(key);
			Object entryValue = entry.getValue();
			Object canonicalValue = this.canonical(entryValue);
			sortedMap.put(keyText, canonicalValue);
		}
		return sortedMap;
	}

	private List<Object> canonicalList(Collection<?> collection) {
		List<Object> sortedList = new ArrayList<>();
		for (Object item : collection) {
			Object canonicalItem = this.canonical(item);
			sortedList.add(canonicalItem);
		}
		// the text of a canonical item is itself canonical: equal items print the same
		Comparator<Object> byText = Comparator.comparing(String::valueOf);
		sortedList.sort(byText);
		return sortedList;
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
