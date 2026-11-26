package com.vis.business.resume.filter;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.cache.CcpCacheDecorator;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityResumeFilter;

/**
 * After the insertion of the filters of a search of resumes in {@code vis_resume_filter} (since 2026-11-25): keeps for 1
 * hour in the cache, under the id of the record, the answer of the search of the position form over them
 * ({@code VisBusinessCountTheResumesOfTheFilter}), where the screens that ask the same filters read it. The insertion
 * runs in the consumer of the queue of the entity, after the API that answered the screen already counted and cached the
 * resumes: the search runs here only when the cache does not have the count, so the same filters are never searched twice.
 */
public class VisBusinessCacheTheResumesOfTheFilter implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public static final VisBusinessCacheTheResumesOfTheFilter INSTANCE = new VisBusinessCacheTheResumesOfTheFilter();

	private VisBusinessCacheTheResumesOfTheFilter() {}

	/**
	 * Counts the resumes of the filters inserted and caches the count, when the cache does not have it.
	 * @param record the record of {@code vis_resume_filter}
	 * @return the record, unchanged
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation record) {
		CcpJsonRepresentation handledRecord = VisEntityResumeFilter.ENTITY.getJsonWithHandledPrimaryKey(record);
		String filterId = VisEntityResumeFilter.ENTITY.calculateId(handledRecord);
		CcpCacheDecorator cache = VisBusinessCountTheResumesOfTheFilter.getCache(filterId);
		boolean alreadyCounted = cache.isPresentInTheCache();

		if(alreadyCounted) {
			return record;
		}

		CcpJsonRepresentation filters = record.getInnerJson(JnJsonCommonsFields.json);
		CcpJsonRepresentation count = VisBusinessCountTheResumesOfTheFilter.INSTANCE.execute(filters);
		cache.put(count, VisBusinessCountTheResumesOfTheFilter.CACHE_SECONDS);
		return record;
	}
}
