package com.vis.json.transformers;

import java.util.List;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.entities.JnEntityLoginEmail;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * The parent of a position among the e-mails of the users ({@link JnEntityLoginEmail}): the e-mail of the logged-in user
 * who registered it, kept in the {@code recruiter} field of the position, so the group of each user keeps every position
 * they registered (oneToMany: a position has one recruiter). As it came the value is the e-mail, which the parent
 * transforms into its SHA-1; as stored it is already the SHA-1.
 */
public class VisPositionRecruiterLoginEmailCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> {

	/** The single instance. */
	public static final VisPositionRecruiterLoginEmailCombinations INSTANCE = new VisPositionRecruiterLoginEmailCombinations();

	private VisPositionRecruiterLoginEmailCombinations() {}

	/**
	 * The only combination: the recruiter of the position as the e-mail of the user.
	 * @param json the position
	 * @return the combination
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		String recruiter = json.getAsString(VisJsonCommonsFields.recruiter);
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(JnEntityLoginEmail.Fields.email, recruiter);
		List<CcpJsonRepresentation> combinations = List.of(combination);
		return combinations;
	}
}
