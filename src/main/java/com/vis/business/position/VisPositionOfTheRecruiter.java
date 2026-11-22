package com.vis.business.position;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntityPosition;

/**
 * The position of the logged-in user as the entity saves it: the {@code recruiter} is always the e-mail of the session
 * (as it came, so the parents of the groups are calculated from the e-mail), never a value of the input or the SHA-1 kept
 * in the stored record.
 */
public class VisPositionOfTheRecruiter {

	private VisPositionOfTheRecruiter() {}

	/**
	 * The primary key of the position.
	 * @param json the input, with {@code email} and {@code title}
	 * @return {@code {recruiter, title}}
	 */
	public static CcpJsonRepresentation getKey(CcpJsonRepresentation json) {
		String email = json.getAsString(VisPositionKeyFields.email);
		String title = json.getAsString(VisPositionKeyFields.title);
		CcpJsonRepresentation key = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityPosition.Fields.recruiter, email)
				.put(VisEntityPosition.Fields.title, title);
		return key;
	}

	/**
	 * The position filled in by the user.
	 * @param json the input, with {@code email} and the fields of the position
	 * @return the position, only with the fields of the entity
	 */
	public static CcpJsonRepresentation fromInput(CcpJsonRepresentation json) {
		String email = json.getAsString(VisPositionKeyFields.email);
		VisEntityPosition.Fields[] fields = VisEntityPosition.Fields.values();
		CcpJsonRepresentation positionFields = json.getJsonPiece(fields);
		CcpJsonRepresentation position = positionFields.put(VisEntityPosition.Fields.recruiter, email);
		return position;
	}

	/**
	 * The stored position, found by a presence check of the position ({@code _entities}), ready to be written again.
	 * @param jsonOfTheProcedure the json of the procedure, holding the stored position
	 * @param email the e-mail of the logged-in user
	 * @return the position, only with the fields of the entity, the recruiter as the e-mail
	 */
	public static CcpJsonRepresentation fromStored(CcpJsonRepresentation jsonOfTheProcedure, String email) {
		CcpJsonRepresentation stored = jsonOfTheProcedure.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, VisEntityPosition.ENTITY);
		VisEntityPosition.Fields[] fields = VisEntityPosition.Fields.values();
		CcpJsonRepresentation storedFields = stored.getJsonPiece(fields);
		CcpJsonRepresentation position = storedFields.put(VisEntityPosition.Fields.recruiter, email);
		return position;
	}
}
