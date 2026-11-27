package com.vis.business.resume.filter;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.bulk.CcpErrorBulkEntityRecordNotFound;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityResumeFilter;
import com.vis.entities.VisEntityResumeFilterDescription;

/**
 * The filters of an id of {@code vis_resume_filter} (since 2026-11-25): the screen {@code curriculos/buscar} opened with
 * the id in its address fills the filters in with them. The id is the one the search answered
 * ({@code VisBusinessSearchResumesByTheFilter}), the hash of the filters, with no day (since 2026-11-26): the read finds
 * the document of the day or, on the next day, its copy in {@code jn_disposable_record}, valid for 24 hours after the
 * last search of the filters ({@code JnDisposableEntity}).
 * <p>
 * With the e-mail too (since 2026-11-26, the link shared with the text of the position, {@code &email=}), the filters and
 * the text the user of that e-mail shared ({@code vis_resume_filter_description}) are read in one union all, and the text
 * goes back in {@code description}, which the screen puts in its field. An e-mail that is not valid (typed wrong in the
 * link) is ignored, as if the link had none: only the filters are read. No text of that e-mail for the filters, the
 * answer is the filters alone.
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
		/**
		 * Optional: the e-mail of the user who shared the link with the text of the position. Not validated here: an
		 * invalid one is ignored, the filters are answered without the text.
		 */
		email,
		;
	}

	/** The single instance: the class keeps no state. */
	public static final VisBusinessGetTheResumeFilter INSTANCE = new VisBusinessGetTheResumeFilter();

	private VisBusinessGetTheResumeFilter() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	/**
	 * Reads the filters of the id and, with a valid e-mail, the text of the position shared with them.
	 * @param json the id and, optionally, the e-mail
	 * @return the filters, as the screen sent them to the search, plus {@code description} when the text was found
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String filterId = json.getAsString(Fields.id);
		CcpJsonRepresentation idOfTheFilters = CcpOtherConstants.EMPTY_JSON.put(VisEntityResumeFilter.Fields.id, filterId);
		boolean withoutTheEmail = false == json.containsAllFields(Fields.email);

		if(withoutTheEmail) {
			return getTheFilters(json, idOfTheFilters);
		}

		String email = json.getAsString(Fields.email);
		CcpStringDecorator emailText = new CcpStringDecorator(email);
		CcpEmailDecorator emailDecorator = emailText.email();
		boolean invalidEmail = false == emailDecorator.isValid();

		// an e-mail typed wrong in the link: as if the link had no e-mail
		if(invalidEmail) {
			return getTheFilters(json, idOfTheFilters);
		}

		CcpJsonRepresentation keyWithTheFilterId = idOfTheFilters.put(VisEntityResumeFilterDescription.Fields.filterId, filterId);
		CcpJsonRepresentation key = keyWithTheFilterId.put(VisEntityResumeFilterDescription.Fields.email, email);
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpSelectUnionAll unionAll = crud.unionAll(key, JnDeleteKeysFromCache.INSTANCE, VisEntityResumeFilter.ENTITY, VisEntityResumeFilterDescription.ENTITY);
		boolean noFilters = false == VisEntityResumeFilter.ENTITY.isPresentInThisUnionAll(unionAll, key);

		if(noFilters) {
			throw new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.NOT_FOUND);
		}

		CcpJsonRepresentation record = VisEntityResumeFilter.ENTITY.getRecordFromUnionAll(unionAll, () -> key);
		CcpJsonRepresentation filters = record.getInnerJson(JnJsonCommonsFields.json);
		boolean noDescription = false == VisEntityResumeFilterDescription.ENTITY.isPresentInThisUnionAll(unionAll, key);

		if(noDescription) {
			return filters;
		}

		CcpJsonRepresentation descriptionRecord = VisEntityResumeFilterDescription.ENTITY.getRecordFromUnionAll(unionAll, () -> key);
		String description = descriptionRecord.getAsString(VisEntityResumeFilterDescription.Fields.description);
		CcpJsonRepresentation filtersWithTheDescription = filters.put(VisEntityResumeFilterDescription.Fields.description, description);
		return filtersWithTheDescription;
	}

	/**
	 * The filters alone, one read by id.
	 * @param json the input, for the error
	 * @param idOfTheFilters the id of the filters
	 * @return the filters
	 */
	private static CcpJsonRepresentation getTheFilters(CcpJsonRepresentation json, CcpJsonRepresentation idOfTheFilters) {
		try {
			CcpJsonRepresentation record = VisEntityResumeFilter.ENTITY.getOneById(idOfTheFilters);
			CcpJsonRepresentation filters = record.getInnerJson(JnJsonCommonsFields.json);
			return filters;
		} catch (CcpErrorBulkEntityRecordNotFound e) {
			throw new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.NOT_FOUND);
		}
	}
}
