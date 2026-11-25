package com.vis.business.position;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpTimeDecorator;
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
	 * @param json the input, with {@code email}, {@code title} and {@code yearMonth}
	 * @return {@code {recruiter, title, yearMonth}}
	 */
	public static CcpJsonRepresentation getKey(CcpJsonRepresentation json) {
		String title = json.getAsString(VisPositionKeyFields.title);
		CcpJsonRepresentation key = getKey(json, title);
		return key;
	}

	/**
	 * The primary key of the position of the logged-in user with another title, in the same month of creation: the old
	 * title of a renaming, or a numbered title of a restoration.
	 * @param json the input, with {@code email} and {@code yearMonth}
	 * @param title the title of the key
	 * @return {@code {recruiter, title, yearMonth}}
	 */
	public static CcpJsonRepresentation getKey(CcpJsonRepresentation json, String title) {
		String email = json.getAsString(VisPositionKeyFields.email);
		String yearMonth = json.getAsString(VisPositionKeyFields.yearMonth);
		CcpJsonRepresentation key = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityPosition.Fields.recruiter, email)
				.put(VisEntityPosition.Fields.title, title)
				.put(VisEntityPosition.Fields.yearMonth, yearMonth);
		return key;
	}

	/**
	 * The month of the creation of a new position, {@code yyyyMM}, in the America/Sao_Paulo time zone.
	 * @return the current month
	 */
	public static String getCurrentYearMonth() {
		CcpTimeDecorator now = new CcpTimeDecorator();
		String pattern = Constants.YEAR_MONTH_PATTERN.getValue();
		String yearMonth = now.getFormattedDateTime(pattern);
		return yearMonth;
	}

	/** The texts of this class. */
	static enum Constants implements CcpJsonFieldName {
		YEAR_MONTH_PATTERN { public String getValue() { return "yyyyMM"; } },
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
		CcpJsonRepresentation position = fromStored(jsonOfTheProcedure, email, VisEntityPosition.ENTITY);
		return position;
	}

	/**
	 * The stored position found by a presence check in the given side ({@code vis_position} or the twin
	 * {@code vis_inactive_position}), ready to be written again.
	 * @param jsonOfTheProcedure the json of the procedure, holding the stored position under {@code _entities}
	 * @param email the e-mail of the logged-in user
	 * @param side the entity of the presence check
	 * @return the position, only with the fields of the entity, the recruiter as the e-mail
	 */
	public static CcpJsonRepresentation fromStored(CcpJsonRepresentation jsonOfTheProcedure, String email, CcpEntity side) {
		CcpJsonRepresentation stored = jsonOfTheProcedure.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, side);
		VisEntityPosition.Fields[] fields = VisEntityPosition.Fields.values();
		CcpJsonRepresentation storedFields = stored.getJsonPiece(fields);
		CcpJsonRepresentation position = storedFields.put(VisEntityPosition.Fields.recruiter, email);
		return position;
	}
}
