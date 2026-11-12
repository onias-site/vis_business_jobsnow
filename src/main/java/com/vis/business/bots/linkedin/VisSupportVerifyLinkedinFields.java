package com.vis.business.bots.linkedin;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * Fields of the {@code verifyLinkedinAddress} command and of its texts. The first parameter of the command, the LinkedIn
 * address, is {@code VisJsonCommonsFields.linkedinAddress}.
 */
public enum VisSupportVerifyLinkedinFields implements CcpJsonFieldName {
	/** The second parameter of the command: the id of the resume, in {@code vis_resume} or {@code vis_inactive_resume}. */
	resumeEmailSha1,
	/** The third parameter of the command: the e-mail of the recruiter who reported the address, as typed. */
	recruiterEmail,
	/** The options offered to the operator. */
	options,
}
