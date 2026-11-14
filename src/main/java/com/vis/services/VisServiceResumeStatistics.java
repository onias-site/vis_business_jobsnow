package com.vis.services;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.services.JnService;
import com.vis.business.statistics.VisBusinessResumeStatisticsOfTheRegion;
import com.vis.business.statistics.VisBusinessResumeTotalsByRegion;

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
	/** The totals of every region, for the titles of the folders. */
	GetResumeTotalsByRegion{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessResumeTotalsByRegion.INSTANCE.execute(json);
			return response;
		}

		public Class<?> getJsonValidationClass() {
			return VisBusinessResumeTotalsByRegion.Fields.class;
		}
	},
	;
}
