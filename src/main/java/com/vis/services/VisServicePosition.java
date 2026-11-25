package com.vis.services;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.services.JnService;
import com.vis.business.position.VisBusinessCreatePosition;
import com.vis.business.position.VisBusinessDeletePosition;
import com.vis.business.position.VisBusinessEditPosition;
import com.vis.business.position.VisBusinessInactivatePosition;
import com.vis.business.position.VisBusinessReactivatePosition;
import com.vis.business.position.VisBusinessRestorePosition;
import com.vis.business.position.VisBusinessResumesOfTheFilters;
import com.vis.business.position.VisBusinessInstantMessageGroupsOfThePosition;
import com.vis.business.position.VisPositionKeyFields;
import com.vis.business.position.VisPositionOfTheRecruiterFields;

/**
 * The positions of the logged-in user (screen "Meu Recrutamento - Vagas", since 2026-11-21): create, edit, delete
 * (move to the twin), inactivate (expire one minute before now), reactivate (a new end of the expiration) and restore (back from the twin, since
 * 2026-11-24). The list
 * of the screen is the group of the e-mail of the user in {@code jn_grouper}, read by id through the endpoint of the
 * groups of js. Every write is asynchronous and every presence check is one read by id.
 */
public enum VisServicePosition implements JnService {

	/** "Criar vaga": a new position; a title the user already has answers 409. */
	CreatePosition {
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessCreatePosition.INSTANCE.execute(json);
			return response;
		}

		public Class<?> getJsonValidationClass() {
			return VisPositionOfTheRecruiterFields.class;
		}
	},

	/** "Editar": saves again a position of the user, whose title does not change; an unknown title answers 404. */
	EditPosition {
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessEditPosition.INSTANCE.execute(json);
			return response;
		}

		public Class<?> getJsonValidationClass() {
			return VisPositionOfTheRecruiterFields.class;
		}
	},

	/** "Excluir": moves a position of the user to the twin; an unknown title answers 404. */
	DeletePosition {
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessDeletePosition.INSTANCE.execute(json);
			return response;
		}

		public Class<?> getJsonValidationClass() {
			return VisPositionKeyFields.class;
		}
	},

	/** "Inativar": the position of the user expires one minute before now; an unknown title answers 404. */
	InactivatePosition {
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessInactivatePosition.INSTANCE.execute(json);
			return response;
		}

		public Class<?> getJsonValidationClass() {
			return VisPositionKeyFields.class;
		}
	},

	/** "Reativar": the position of the user gets a new end of the expiration; an unknown title answers 404. */
	ReactivatePosition {
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessReactivatePosition.INSTANCE.execute(json);
			return response;
		}

		public Class<?> getJsonValidationClass() {
			return VisBusinessReactivatePosition.Fields.class;
		}
	},

	/** "Restaurar": a deleted position of the user goes back, valid for one week; an unknown title answers 404. */
	RestorePosition {
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessRestorePosition.INSTANCE.execute(json);
			return response;
		}

		public Class<?> getJsonValidationClass() {
			return VisPositionKeyFields.class;
		}
	},

	/** The tabs of the position form: the resumes that satisfy its filters, counted inside Elasticsearch. */
	GetResumesOfTheFilters {
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessResumesOfTheFilters.INSTANCE.execute(json);
			return response;
		}

		public Class<?> getJsonValidationClass() {
			return VisBusinessResumesOfTheFilters.Fields.class;
		}
	},

	/** The modal of the groups of the position form: the instant message groups of its seniorities, area and ddds. */
	GetInstantMessageGroups {
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessInstantMessageGroupsOfThePosition.INSTANCE.execute(json);
			return response;
		}

		public Class<?> getJsonValidationClass() {
			return VisBusinessInstantMessageGroupsOfThePosition.Fields.class;
		}
	},
	;
}
