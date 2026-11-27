package com.vis.services;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.services.JnService;
import com.vis.business.resume.filter.VisBusinessGetTheResumeFilter;
import com.vis.business.resume.filter.VisBusinessSaveTheResumeFilterDescription;
import com.vis.business.resume.filter.VisBusinessSearchResumesByTheFilter;
import com.vis.entities.VisEntityResumeFilterDescription;
import com.vis.json.fields.validation.VisJsonFieldsResumeFilter;

/**
 * The search of resumes by filters (screen {@code curriculos/buscar} and the position form, since 2026-11-25): every set
 * of filters chosen is a record of {@code vis_resume_filter}, whose id keys the cache of the count of its resumes and goes
 * back to the screen, which reads the filters again by it. Public, with no session, but the save of the text of the
 * position shared with the link (since 2026-11-26), which needs the session.
 */
public enum VisServiceResumeFilter implements JnService {

	/** The filters chosen: the count of their resumes (from the cache when counted in the last hour) and their id. */
	SearchResumes {
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessSearchResumesByTheFilter.INSTANCE.execute(json);
			return response;
		}

		public Class<?> getJsonValidationClass() {
			return VisJsonFieldsResumeFilter.class;
		}
	},

	/**
	 * The screen opened with the id of the filters in its address: the filters of that id, 404 when they expired; with
	 * the e-mail of the link too, plus the text of the position that e-mail shared with them.
	 */
	GetResumeFilter {
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessGetTheResumeFilter.INSTANCE.execute(json);
			return response;
		}

		public Class<?> getJsonValidationClass() {
			return VisBusinessGetTheResumeFilter.Fields.class;
		}
	},

	/** The link shared with the text of the position: the text, under the id of the filters and the e-mail of the session. */
	SaveResumeFilterDescription {
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessSaveTheResumeFilterDescription.INSTANCE.execute(json);
			return response;
		}

		public Class<?> getJsonValidationClass() {
			return VisEntityResumeFilterDescription.Fields.class;
		}
	},
	;
}
