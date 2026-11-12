package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityResume;

/**
 * Produces the keys a resume is counted under in the group of the forbidden consultancies: the domains of its
 * {@code notAllowedCompany}, as typed, each once. The quotes and backslashes are dropped, because the counters are kept
 * as a JSON text whose keys hold none; a value left empty does not count.
 */
public class VisResumeNotAllowedCompanyCounterKeys implements Function<CcpJsonRepresentation, List<String>>{
	/** The single instance: the class keeps no state. */
	public final static VisResumeNotAllowedCompanyCounterKeys INSTANCE = new VisResumeNotAllowedCompanyCounterKeys();

	private VisResumeNotAllowedCompanyCounterKeys() {}

	/** The texts of this class. */
	enum Constants implements CcpJsonFieldName {
		/** The characters a key of the counters cannot hold: quotes and backslashes. */
		FORBIDDEN_CHARACTERS { public String getValue() { return "[\"\\\\]"; } },
		/** What takes the place of a forbidden character. */
		NOTHING { public String getValue() { return ""; } },
	}

	/**
	 * The domains the resume forbids.
	 * @param json the resume
	 * @return the keys, none when the resume forbids no domain
	 */
	public List<String> apply(CcpJsonRepresentation json) {
		List<String> notAllowedCompanies = json.getOrDefault(VisEntityResume.Fields.notAllowedCompany, () -> List.of());
		String forbiddenCharacters = Constants.FORBIDDEN_CHARACTERS.getValue();
		String nothing = Constants.NOTHING.getValue();
		Set<String> keys = new LinkedHashSet<>();
		for (String notAllowedCompany : notAllowedCompanies) {
			String key = notAllowedCompany.replaceAll(forbiddenCharacters, nothing);
			boolean empty = key.isEmpty();

			if(empty) {
				continue;
			}

			keys.add(key);
		}
		return new ArrayList<>(keys);
	}
}
