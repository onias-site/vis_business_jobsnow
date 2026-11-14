package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityResume;

/**
 * The keys of the counter of records by ddd ({@code counterKeyClass} of a {@code @CcpEntityGrouperChildCounter} counted
 * by {@code JnGrouperChildCurrentCounterEntityBeforeWrite}): each ddd the stored record lists (the resume and the view
 * of a resume keep them in {@code ddd}), as an integer; no key when the record has none.
 */
public class VisDddCounterKeys implements Function<CcpJsonRepresentation, List<String>>{
	/** The single instance: the class keeps no state. */
	public final static VisDddCounterKeys INSTANCE = new VisDddCounterKeys();

	private VisDddCounterKeys() {}

	/**
	 * The ddds of the record.
	 * @param json the record as stored
	 * @return one key per ddd, empty when the record has none
	 */
	public List<String> apply(CcpJsonRepresentation json) {
		boolean noDdd = false == json.containsField(VisEntityResume.Fields.ddd);

		if(noDdd) {
			return List.of();
		}

		List<Object> ddds = json.getAsObjectList(VisEntityResume.Fields.ddd);
		List<String> keys = new ArrayList<>();
		for (Object ddd : ddds) {
			// a number read from the database may come as 11.0
			CcpJsonRepresentation dddJson = CcpOtherConstants.EMPTY_JSON.put(VisEntityResume.Fields.ddd, ddd);
			Integer dddAsInteger = dddJson.getAsIntegerNumber(VisEntityResume.Fields.ddd);
			String key = String.valueOf(dddAsInteger);
			keys.add(key);
		}
		return keys;
	}
}
