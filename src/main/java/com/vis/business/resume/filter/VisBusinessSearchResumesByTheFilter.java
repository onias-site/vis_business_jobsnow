package com.vis.business.resume.filter;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.cache.CcpCacheDecorator;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityResumeFilter;
import com.vis.json.fields.validation.VisJsonFieldsResumeFilter;

/**
 * The resumes of the filters chosen in the screen {@code curriculos/buscar} or in the position form (since 2026-11-25):
 * the id of the filters in {@code vis_resume_filter} ({@code calculateId} of {@code {json: filters}}) is the key of the
 * cache of their count. Found in the cache, the count is answered from there, with no search and no write. Otherwise the
 * resumes are counted with the search of the position form, the count is cached for 1 hour under the id and the filters
 * are saved (discarded day by day; the same filters in any order are one record of the day); the save is asynchronous
 * (the entity is disposable, and the REST APIs have no bulk), and the insertion, finding the count already cached, does
 * not search again ({@code VisBusinessCacheTheResumesOfTheFilter}).
 * The answer is the count plus the id, which the screen puts in its address.
 */
public class VisBusinessSearchResumesByTheFilter implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public static final VisBusinessSearchResumesByTheFilter INSTANCE = new VisBusinessSearchResumesByTheFilter();

	private VisBusinessSearchResumesByTheFilter() {}

	public Class<?> getJsonValidationClass() {
		return VisJsonFieldsResumeFilter.class;
	}

	/**
	 * Counts the resumes of the filters, from the cache when they were counted in the last hour.
	 * @param filters the filters, as the fields of the position
	 * @return the id of the filters, the ids of the resumes found, how many they are and the averages
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation filters) {
		CcpJsonRepresentation record = CcpOtherConstants.EMPTY_JSON.put(VisEntityResumeFilter.Fields.json, filters);
		CcpJsonRepresentation handledRecord = VisEntityResumeFilter.ENTITY.getJsonWithHandledPrimaryKey(record);
		String filterId = VisEntityResumeFilter.ENTITY.calculateId(handledRecord);
		CcpCacheDecorator cache = VisBusinessCountTheResumesOfTheFilter.getCache(filterId);
		CcpJsonRepresentation cachedCount = cache.getOrDefault(CcpOtherConstants.EMPTY_JSON);
		boolean countedInTheLastHour = false == cachedCount.isEmpty();

		if(countedInTheLastHour) {
			return cachedCount.put(JnJsonCommonsFields.id, filterId);
		}

		CcpJsonRepresentation count = VisBusinessCountTheResumesOfTheFilter.INSTANCE.execute(filters);
		cache.put(count, VisBusinessCountTheResumesOfTheFilter.CACHE_SECONDS);
		VisEntityResumeFilter.ENTITY.save(record);
		CcpJsonRepresentation countWithTheId = count.put(JnJsonCommonsFields.id, filterId);
		return countWithTheId;
	}
}
