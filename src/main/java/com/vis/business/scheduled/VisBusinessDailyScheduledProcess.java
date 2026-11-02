package com.vis.business.scheduled;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.business.database.JnBusinessDailyDatabaseMaintenance;
import com.jn.business.email.suppression.JnBusinessLoadEmailSuppressions;
import com.jn.business.email.suppression.JnBusinessLoadEmailSuppressionsOrSaveAWarning;
import com.vis.business.recruiter.resumes.VisBusinessSendResumesToRecruiters;

/**
 * The scheduled process that runs once a day ({@code CcpCronTasksController}, topic
 * {@code com.vis.business.scheduled.VisBusinessDailyScheduledProcess}): first the suppressions of the e-mail provider
 * ({@link JnBusinessLoadEmailSuppressions}), so the e-mail of the day already skips them, then the e-mail of the resumes
 * to the recruiters ({@link VisBusinessSendResumesToRecruiters}, which skips the weekend by itself), then the purge of the
 * expired disposable records and the backup of the database ({@link JnBusinessDailyDatabaseMaintenance}), so the backup
 * already holds what the e-mail wrote ({@code vis_recruiter_resumes}).
 * <p>
 * Input: the one of the processes ({@code startTimeInSeconds}, {@code recruiters}, {@code bucketName} and
 * {@code entityNamePatterns}, all optional).
 * Output: the one of the maintenance plus {@code resumesToRecruiters} (the counters of the e-mail) and
 * {@code emailSuppressions} (the counters of the load).
 */
public class VisBusinessDailyScheduledProcess implements CcpBusiness {

	/** The fields of the output. */
	public static enum Fields implements CcpJsonFieldName {
		/** The counters of the e-mail of the resumes to the recruiters. */
		resumesToRecruiters,
		/** The counters of the load of the suppressions of the e-mail provider, or the warning of its failure. */
		emailSuppressions,
	}

	/** The single instance: the business keeps no state. */
	public static final VisBusinessDailyScheduledProcess INSTANCE = new VisBusinessDailyScheduledProcess();

	private VisBusinessDailyScheduledProcess() {}

	/**
	 * Loads the suppressions, sends the resumes to the recruiters and then purges and backs up the database.
	 * @param json the input of the processes
	 * @return the output of the maintenance plus the counters of the e-mail and of the load
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation emailSuppressions = JnBusinessLoadEmailSuppressionsOrSaveAWarning.INSTANCE.execute(json);
		CcpJsonRepresentation resumesToRecruiters = VisBusinessSendResumesToRecruiters.INSTANCE.execute(json);
		CcpJsonRepresentation maintenance = JnBusinessDailyDatabaseMaintenance.INSTANCE.execute(json);
		CcpJsonRepresentation maintenanceWithResumes = maintenance.put(Fields.resumesToRecruiters, resumesToRecruiters);
		CcpJsonRepresentation maintenanceWithSuppressions = maintenanceWithResumes.put(Fields.emailSuppressions, emailSuppressions);
		return maintenanceWithSuppressions;
	}
}
