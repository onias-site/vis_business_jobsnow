package com.vis.business.resume.filter;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpErrorBulkEntityRecordNotFound;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityResumeFilter;

/**
 * The filters of an id of {@code vis_resume_filter} (since 2026-11-25), one read by id: the screen
 * {@code curriculos/buscar} opened with the id in its address fills the filters in with them. The id is the one the
 * search answered ({@code VisBusinessSearchResumesByTheFilter}), the hash of the filters, with no day (since 2026-11-26):
 * the read finds the document of the day or, on the next day, its copy in {@code jn_disposable_record}, valid for 24 hours
 * after the last search of the filters ({@code JnDisposableEntity}).
 *
 * @throws CcpErrorFlowDisturb {@code 404} when there is no record of this id (the copy expired or it never existed): the
 * screen then does nothing
 */
public class VisBusinessGetTheResumeFilter implements CcpBusiness {

	/** The input of the business. */
	public static enum Fields implements CcpJsonFieldName {
		/**
		 * The id of the filters: the SHA-1 the search answered, up to 40 hexadecimal characters, with the sign of the
		 * hash of the Java when negative ({@code CcpHashDecorator}: 41 characters).
		 */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 41, regexValidation = "^-?[0-9a-fA-F]+$")
		id,
		;
	}

	/** The single instance: the class keeps no state. */
	public static final VisBusinessGetTheResumeFilter INSTANCE = new VisBusinessGetTheResumeFilter();

	private VisBusinessGetTheResumeFilter() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	/**
	 * Reads the filters of the id.
	 * @param json the id
	 * @return the filters, as the screen sent them to the search
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String filterId = json.getAsString(Fields.id);
		CcpJsonRepresentation idOfTheFilters = CcpOtherConstants.EMPTY_JSON.put(VisEntityResumeFilter.Fields.id, filterId);
		try {
			CcpJsonRepresentation record = VisEntityResumeFilter.ENTITY.getOneById(idOfTheFilters);
			CcpJsonRepresentation filters = record.getInnerJson(JnJsonCommonsFields.json);
			return filters;
		} catch (CcpErrorBulkEntityRecordNotFound e) {
			throw new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.NOT_FOUND);
		}
	}
}
