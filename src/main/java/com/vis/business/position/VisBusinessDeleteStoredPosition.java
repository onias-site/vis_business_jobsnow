package com.vis.business.position;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityPosition;

/**
 * Step of "Excluir" of the screen "Meu Recrutamento - Vagas": deletes the stored position, found by the presence check
 * of the position, which moves it to the twin {@code vis_inactive_position} (and its children to the twin groups).
 */
public class VisBusinessDeleteStoredPosition implements CcpBusiness {

	/** The e-mail of the logged-in user, the recruiter of the position. */
	private final String email;

	/**
	 * Keeps the e-mail.
	 * @param email the e-mail of the logged-in user
	 */
	public VisBusinessDeleteStoredPosition(String email) {
		this.email = email;
	}

	/**
	 * Deletes the position.
	 * @param json the json of the procedure, holding the stored position under {@code _entities}
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation position = VisPositionOfTheRecruiter.fromStored(json, this.email);
		VisEntityPosition.ENTITY.delete(position);
		return json;
	}
}
