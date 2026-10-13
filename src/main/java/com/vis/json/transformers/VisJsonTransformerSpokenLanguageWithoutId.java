package com.vis.json.transformers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.fields.CcpJsonTransformersDefaultEntityField;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Field transformer applied to the {@code spokenLanguage} of the VisEntityResume entity while it is persisted. The front
 * end sends each language with its {@code id}, which the backend ignores: it is removed from every item before the
 * write. A json with no spoken language is returned as is.
 */
public class VisJsonTransformerSpokenLanguageWithoutId implements CcpJsonTransformersDefaultEntityField {

	/** Fields removed by the transformer. */
	enum JsonFieldNames implements CcpJsonFieldName {
		/** The {@code id} field of each language. */
		id
	}

	/**
	 * Removes the {@code id} of each spoken language.
	 * @param json the resume
	 * @return the resume with the spoken languages without id
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		boolean noSpokenLanguage = false == json.containsAllFields(VisJsonCommonsFields.spokenLanguage);

		if(noSpokenLanguage) {
			return json;
		}

		List<CcpJsonRepresentation> spokenLanguages = json.getAsJsonList(VisJsonCommonsFields.spokenLanguage);
		Stream<CcpJsonRepresentation> spokenLanguagesStream = spokenLanguages.stream();
		var spokenLanguagesWithoutIdStream = spokenLanguagesStream.map(spokenLanguage -> spokenLanguage.removeFields(JsonFieldNames.id));
		List<CcpJsonRepresentation> spokenLanguagesWithoutId = spokenLanguagesWithoutIdStream.collect(Collectors.toList());
		CcpJsonRepresentation jsonWithSpokenLanguagesWithoutId = json.put(VisJsonCommonsFields.spokenLanguage, spokenLanguagesWithoutId);
		return jsonWithSpokenLanguagesWithoutId;
	}

	/**
	 * Tells that the result can not be part of a primary key: it is a list of languages.
	 * @return {@code false}
	 */
	public boolean canBePrimaryKey() {
		return false;
	}

	/**
	 * Returns the name of the transformed field.
	 * @return {@code spokenLanguage}
	 */
	public String name() {
		String fieldName = VisJsonCommonsFields.spokenLanguage.name();
		return fieldName;
	}
}
