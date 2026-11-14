package com.vis.services;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.services.JnService;
import com.vis.business.statistics.VisBusinessResumeStatisticsOfTheRegion;
import com.vis.business.statistics.VisBusinessResumeTotals;

/**
 * The statistics of the active resumes (supply against demand), read by id from the groups of ddd (OQL). They are public.
 */
public enum VisServiceResumeStatistics implements JnService {
	/** The statistics of the resumes of a region. */
	GetResumeStatisticsOfTheRegion{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessResumeStatisticsOfTheRegion.INSTANCE.execute(json);
			return response;
		}

		public Class<?> getJsonValidationClass() {
			return VisBusinessResumeStatisticsOfTheRegion.Fields.class;
		}
	},
	/** The counters by ddd, by day and by month of insertion: the titles of the folders and the line chart. */
	GetResumeTotals{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessResumeTotals.INSTANCE.execute(json);
			return response;
		}

		public Class<?> getJsonValidationClass() {
			return VisBusinessResumeTotals.Fields.class;
		}
	},
	;
}
