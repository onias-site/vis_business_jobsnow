package com.vis.business.scheduled;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.business.database.JnBusinessDailyDatabaseMaintenance;
import com.vis.business.recruiter.resumes.VisBusinessSendResumesToRecruiters;

/**
 * The scheduled process that runs once a day ({@code CcpCronTasksController}, topic
 * {@code com.vis.business.scheduled.VisBusinessDailyScheduledProcess}): first the e-mail of the resumes to the recruiters
 * ({@link VisBusinessSendResumesToRecruiters}, which skips the weekend by itself), then the purge of the expired disposable
 * records and the backup of the database ({@link JnBusinessDailyDatabaseMaintenance}), so the backup already holds what the
 * e-mail wrote ({@code vis_recruiter_resumes}).
 * <p>
 * The suppressions of the e-mail provider are not loaded here: the hourly process of the login token
 * ({@code JsBusinessDailyLoginTokenProcess}) loads them every hour, so the e-mail of the day already skips them.
 * <p>
 * The e-mail can be locked by the input ({@code resumesToRecruitersLocked} true): the process then does all the rest and
 * sends nothing to the recruiters. The Cloud Run Job {@code jobsnow-processo-diario} gives it in the input of its image
 * ({@code processo-diario.Dockerfile}) until the e-mail is released.
 * <p>
 * Input: the one of the processes ({@code recruiters}, {@code bucketName}, {@code entityNamePatterns} and
 * {@code resumesToRecruitersLocked}, all optional).
 * Output: the one of the maintenance plus {@code resumesToRecruiters} (the counters of the e-mail, or
 * {@code {"locked": true}} when the e-mail is locked).
 */
public class VisBusinessDailyScheduledProcess implements CcpBusiness {

	/** The fields of the input and of the output. */
	public static enum Fields implements CcpJsonFieldName {
		/** The counters of the e-mail of the resumes to the recruiters. */
		resumesToRecruiters,
		/** Input: true locks the e-mail of the resumes to the recruiters. */
		resumesToRecruitersLocked,
		/** Output, inside {@code resumesToRecruiters}: the e-mail was locked. */
		locked,
	}

	/** The single instance: the business keeps no state. */
	public static final VisBusinessDailyScheduledProcess INSTANCE = new VisBusinessDailyScheduledProcess();

	private VisBusinessDailyScheduledProcess() {}

	/**
	 * Sends the resumes to the recruiters and then purges and backs up the database.
	 * @param json the input of the processes
	 * @return the output of the maintenance plus the counters of the e-mail
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation resumesToRecruiters = this.sendResumesToRecruiters(json);
		CcpJsonRepresentation maintenance = JnBusinessDailyDatabaseMaintenance.INSTANCE.execute(json);
		CcpJsonRepresentation maintenanceWithResumes = maintenance.put(Fields.resumesToRecruiters, resumesToRecruiters);
		return maintenanceWithResumes;
	}

	/**
	 * Sends the resumes to the recruiters, unless the input locks the e-mail.
	 * @param json the input of the processes
	 * @return the counters of the e-mail, or {@code {"locked": true}}
	 */
	private CcpJsonRepresentation sendResumesToRecruiters(CcpJsonRepresentation json) {
		boolean locked = json.getAsBoolean(Fields.resumesToRecruitersLocked);
		if(locked) {
			return CcpOtherConstants.EMPTY_JSON.put(Fields.locked, true);
		}
		CcpJsonRepresentation resumesToRecruiters = VisBusinessSendResumesToRecruiters.INSTANCE.execute(json);
		return resumesToRecruiters;
	}
}
