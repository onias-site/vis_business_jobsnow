package com.vis.business.position;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityPosition;

/**
 * Step of "Inativar" and of "Reativar" of the screen "Meu Recrutamento - Vagas": saves the stored position, found by the
 * presence check of the position, with a new end of the expiration. A min after the new max goes back to the max, so the
 * range stays in order.
 */
public class VisBusinessChangePositionExpiration implements CcpBusiness {

	/** The ends of the range of the expiration. */
	enum RangeFields implements CcpJsonFieldName {
		/** The beginning of the position. */
		min,
		/** The expiration of the position. */
		max
	}

	/** The e-mail of the logged-in user, the recruiter of the position. */
	private final String email;

	/** The new end of the expiration (milliseconds). */
	private final Long max;

	/**
	 * Keeps the e-mail and the new end.
	 * @param email the e-mail of the logged-in user
	 * @param max the new end of the expiration (milliseconds)
	 */
	public VisBusinessChangePositionExpiration(String email, Long max) {
		this.email = email;
		this.max = max;
	}

	/**
	 * Saves the position with the new expiration.
	 * @param json the json of the procedure, holding the stored position under {@code _entities}
	 * @return the json with the new {@code expiration}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation position = VisPositionOfTheRecruiter.fromStored(json, this.email);
		CcpJsonRepresentation expiration = position.getInnerJson(VisEntityPosition.Fields.expiration);
		Double storedMin = expiration.getAsDoubleNumber(RangeFields.min);
		long storedMinValue = storedMin.longValue();
		long min = Math.min(storedMinValue, this.max);
		CcpJsonRepresentation expirationWithMin = expiration.put(RangeFields.min, min);
		CcpJsonRepresentation newExpiration = expirationWithMin.put(RangeFields.max, this.max);
		CcpJsonRepresentation positionWithNewExpiration = position.put(VisEntityPosition.Fields.expiration, newExpiration);
		VisEntityPosition.ENTITY.save(positionWithNewExpiration);
		CcpJsonRepresentation jsonWithNewExpiration = json.put(VisEntityPosition.Fields.expiration, newExpiration);
		return jsonWithNewExpiration;
	}
}
