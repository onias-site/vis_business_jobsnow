package com.vis.business.resume.filter;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.cache.CcpCacheDecorator;
import com.vis.business.position.VisBusinessFiltersOfThePosition;
import com.vis.business.position.VisBusinessResumesOfTheFilters;
import com.vis.json.fields.validation.VisJsonFieldsResumeFilter;

/**
 * Counts the resumes of the filters of a search of resumes (since 2026-11-25), the same search of the position form: the
 * filters, kept as the fields of the position, become the body of {@code VisBusinessResumesOfTheFilters}
 * ({@code VisBusinessFiltersOfThePosition}, plus the domain of the logged recruiter, which the position takes from the
 * e-mail that receives its resumes), and the answer is the ids of the resumes found, how many they are and the averages
 * of their claims and years of experience.
 * <p>
 * The answer is cached for 1 hour under the id of the filters in {@code vis_resume_filter} ({@link #getCache}): the
 * screens that ask the same filters again read it from there, with no second search.
 */
public class VisBusinessCountTheResumesOfTheFilter implements CcpBusiness {

	/** The texts of this class. */
	static enum Constants implements CcpJsonFieldName {
		/** The prefix of the key of the cache of the count, followed by the id of the filters. */
		CACHE_KEY_PREFIX { public String getValue() { return "resumes.of.the.filter."; } },
	}

	/** How long the count stays in the cache: 1 hour, as the record of the filters. */
	public static final int CACHE_SECONDS = 3600;

	/** The single instance: the class keeps no state. */
	public static final VisBusinessCountTheResumesOfTheFilter INSTANCE = new VisBusinessCountTheResumesOfTheFilter();

	private VisBusinessCountTheResumesOfTheFilter() {}

	public Class<?> getJsonValidationClass() {
		return VisJsonFieldsResumeFilter.class;
	}

	/**
	 * Counts the resumes of the filters.
	 * @param filters the filters, as the fields of the position
	 * @return the ids of the resumes found, how many they are and the averages
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation filters) {
		CcpJsonRepresentation filtersOfThePosition = VisBusinessFiltersOfThePosition.INSTANCE.execute(filters);
		boolean noRecruiterDomain = false == filters.containsAllFields(VisJsonFieldsResumeFilter.recruiterDomain);
		CcpJsonRepresentation searchFilters = noRecruiterDomain ? filtersOfThePosition : filtersOfThePosition.put(VisBusinessResumesOfTheFilters.Fields.recruiterDomain, filters.getAsString(VisJsonFieldsResumeFilter.recruiterDomain));
		CcpJsonRepresentation count = VisBusinessResumesOfTheFilters.INSTANCE.execute(searchFilters);
		return count;
	}

	/**
	 * The cache of the count of the filters of this id.
	 * @param filterId the id of the filters in {@code vis_resume_filter}
	 * @return the cache
	 */
	public static CcpCacheDecorator getCache(String filterId) {
		String prefix = Constants.CACHE_KEY_PREFIX.getValue();
		String key = prefix + filterId;
		CcpCacheDecorator cache = new CcpCacheDecorator(key);
		return cache;
	}
}
