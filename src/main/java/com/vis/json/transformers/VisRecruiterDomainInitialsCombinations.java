package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityRecruiter;
import com.vis.entities.VisEntityVirtualRecruiterDomainInitialsGrouper;

/**
 * Produces the group of {@code VisEntityVirtualRecruiterDomainInitialsGrouper} a recruiter belongs to: the first three
 * letters (upper case) of the domain of its e-mail. A recruiter whose domain is non-professional (the {@code professional}
 * flag, calculated from the {@code NONPROFESSIONALDOMAINS} record of {@code jn_system_message}, is {@code false}) belongs
 * to no group.
 */
public class VisRecruiterDomainInitialsCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisRecruiterDomainInitialsCombinations INSTANCE = new VisRecruiterDomainInitialsCombinations();

	private VisRecruiterDomainInitialsCombinations() {}

	/**
	 * The combination of the initials of the domain, when it is a professional one.
	 * @param json the record of {@link VisEntityRecruiter}, its fields already transformed
	 * @return the combination, or none for a non-professional domain
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		List<CcpJsonRepresentation> combinations = new ArrayList<>();
		boolean nonProfessional = false == json.getAsBoolean(VisEntityRecruiter.Fields.professional);

		if(nonProfessional) {
			return combinations;
		}

		String domain = json.getAsString(VisEntityRecruiter.Fields.domain);
		CcpJsonRepresentation combination = VisEntityVirtualRecruiterDomainInitialsGrouper.getCombination(domain);
		combinations.add(combination);
		return combinations;
	}
}
