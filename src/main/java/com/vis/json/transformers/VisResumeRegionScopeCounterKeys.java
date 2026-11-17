package com.vis.json.transformers;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.business.recruiter.resumes.VisMetropolitanRegions;
import com.vis.entities.VisEntityResume;

/**
 * The key of the counter of the resumes by the reach of their ddds ({@code counterKeyClass} of a
 * {@code @CcpEntityGrouperChildCounter} counted by {@code JnGrouperChildCurrentCounterEntityBeforeWrite}): one key per
 * resume, from the distinct ddds it stores. The ddd 10 (home office) is always alone ({@code VisJsonValidatorHomeOfficeAlone});
 * every metropolitan region ({@link VisMetropolitanRegions}) chosen is the option "qualquer estado / cidade" of the
 * screen of the resume. No key when the resume has no ddd.
 */
public class VisResumeRegionScopeCounterKeys implements Function<CcpJsonRepresentation, List<String>>{

	/** The keys of the counter. */
	public static enum Scopes implements CcpJsonFieldName {
		/** Only the ddd 10: accepts only home office. */
		homeOfficeOnly,
		/** A single metropolitan region. */
		oneRegion,
		/** More than one metropolitan region, but not all of them. */
		severalRegions,
		/** Every metropolitan region. */
		anyRegion,
	}

	/** The single instance: the class keeps no state. */
	public final static VisResumeRegionScopeCounterKeys INSTANCE = new VisResumeRegionScopeCounterKeys();

	/** The ddd of the resume that accepts only home office. */
	private static final int HOME_OFFICE_DDD = 10;

	private VisResumeRegionScopeCounterKeys() {}

	/**
	 * The reach of the ddds of the resume.
	 * @param json the resume as stored
	 * @return its key, empty when it has no ddd
	 */
	public List<String> apply(CcpJsonRepresentation json) {
		Set<Integer> ddds = getDistinctDdds(json);
		boolean noDdd = ddds.isEmpty();

		if(noDdd) {
			return List.of();
		}

		Scopes scope = getScope(ddds);
		String key = scope.getValue();
		return List.of(key);
	}

	private static Scopes getScope(Set<Integer> ddds) {
		boolean homeOfficeOnly = ddds.contains(HOME_OFFICE_DDD);

		if(homeOfficeOnly) {
			return Scopes.homeOfficeOnly;
		}

		int regions = ddds.size();
		boolean oneRegion = regions == 1;

		if(oneRegion) {
			return Scopes.oneRegion;
		}

		VisMetropolitanRegions[] allRegions = VisMetropolitanRegions.values();
		boolean anyRegion = regions >= allRegions.length;

		if(anyRegion) {
			return Scopes.anyRegion;
		}

		return Scopes.severalRegions;
	}

	private static Set<Integer> getDistinctDdds(CcpJsonRepresentation json) {
		Set<Integer> ddds = new LinkedHashSet<>();
		boolean noDdd = false == json.containsField(VisEntityResume.Fields.ddd);

		if(noDdd) {
			return ddds;
		}

		List<Object> storedDdds = json.getAsObjectList(VisEntityResume.Fields.ddd);
		for (Object ddd : storedDdds) {
			// a number read from the database may come as 11.0
			CcpJsonRepresentation dddJson = CcpOtherConstants.EMPTY_JSON.put(VisEntityResume.Fields.ddd, ddd);
			Integer dddAsInteger = dddJson.getAsIntegerNumber(VisEntityResume.Fields.ddd);
			ddds.add(dddAsInteger);
		}
		return ddds;
	}
}
