package com.vis.business.resume.filter;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityResumeFilterDescription;

/**
 * The text of the position shared with the link of a search of resumes (since 2026-11-26): "Copiar este link para enviar
 * para alguém" of the screen {@code curriculos/buscar}, with the text filled in and the user logged in, saves it in
 * {@code vis_resume_filter_description} under the id of the filters and the e-mail of the session; the link then gets
 * {@code &email=<e-mail>}, and whoever opens it gets the text back ({@code VisBusinessGetTheResumeFilter}). Saving again
 * the same filters replaces the text. One write by id, synchronous: the entity has no bulk side effect.
 */
public class VisBusinessSaveTheResumeFilterDescription implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public static final VisBusinessSaveTheResumeFilterDescription INSTANCE = new VisBusinessSaveTheResumeFilterDescription();

	private VisBusinessSaveTheResumeFilterDescription() {}

	public Class<?> getJsonValidationClass() {
		return VisEntityResumeFilterDescription.Fields.class;
	}

	/**
	 * Saves the text of the position under the id of the filters and the e-mail of the session.
	 * @param json the values of the session, the id of the filters and the text
	 * @return the id of the filters
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		VisEntityResumeFilterDescription.ENTITY.save(json);
		String filterId = json.getAsString(VisEntityResumeFilterDescription.Fields.filterId);
		CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON.put(VisEntityResumeFilterDescription.Fields.filterId, filterId);
		return response;
	}
}
