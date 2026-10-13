package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.List;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.fields.CcpJsonTransformersDefaultEntityField;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityVirtualHashGrouper;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Field transformer applied to the {@code hash} of the VisEntityResume entity while it is persisted: it calculates,
 * with {@code VisResumeHashCombinations}, every search the resume satisfies and keeps the id of each one in the
 * {@code hash} list. Whatever the input brought in the field is replaced.
 */
public class VisJsonTransformerResumeHashes implements CcpJsonTransformersDefaultEntityField {

	/**
	 * Replaces the {@code hash} of the resume with the ids of its combinations.
	 * @param json the resume
	 * @return the resume with its hashes
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		// the transformers also run on deletes and transfers, whose json may carry only the key of the resume
		boolean isNotAWholeResume = false == json.containsAllFields(VisEntityResume.Fields.resumeType, VisEntityResume.Fields.experience, VisEntityResume.Fields.ddd);

		if(isNotAWholeResume) {
			return json;
		}

		VisResumeHashCombinations hashCombinations = new VisResumeHashCombinations();
		List<CcpJsonRepresentation> combinations = hashCombinations.apply(json);

		List<String> hashes = new ArrayList<>();
		for (CcpJsonRepresentation combination : combinations) {
			String hash = VisEntityVirtualHashGrouper.ENTITY.calculateId(combination);
			hashes.add(hash);
		}

		CcpJsonRepresentation jsonWithHashes = json.put(VisEntityResume.Fields.hash, hashes);
		return jsonWithHashes;
	}

	/**
	 * Tells that the result can not be part of a primary key: it is a list.
	 * @return {@code false}
	 */
	public boolean canBePrimaryKey() {
		return false;
	}

	/**
	 * Returns the name of the transformed field.
	 * @return {@code hash}
	 */
	public String name() {
		String fieldName = VisJsonCommonsFields.hash.name();
		return fieldName;
	}
}
