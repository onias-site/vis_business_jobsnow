package com.vis.business.recruiter.resumes;

import com.ccp.decorators.CcpHashDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.hash.CcpHashAlgorithm;
import com.jn.utils.JnSystemProperties;

/**
 * The links of the e-mail of resumes that act without a login, signed so that nobody can act on behalf of another
 * recruiter by just writing their e-mail in the link: the token is the SHA-1 of the purpose of the link, its values
 * and a secret of the system properties ({@code recruiterLinksSecret}), with the secret at the end, so that the hash
 * cannot be extended to sign other values.
 */
public enum VisRecruiterLinks implements CcpJsonFieldName {
	/** Opens a resume of the e-mail: signs the e-mail of the recruiter and the id of the resume. */
	view,
	/** Stops the e-mails to the recruiter: signs the e-mail of the recruiter. */
	unsubscribe,
	;

	/** The separator of the values signed. */
	static enum Separator implements CcpJsonFieldName {
		PIPE { public String getValue() { return "|"; } },
	}

	/**
	 * Signs the values of a link.
	 * @param values the values of the link (the e-mail of the recruiter, and the id of the resume for {@link #view})
	 * @return the token
	 * @throws VisErrorRecruiterLinksSecretIsMissing when the system properties have no secret
	 */
	public String sign(String... values) {
		String secret = getSecret();
		String separator = Separator.PIPE.getValue();
		String joinedValues = String.join(separator, values);
		String purpose = this.name();
		String textToSign = purpose + separator + joinedValues + separator + secret;
		CcpStringDecorator textDecorator = new CcpStringDecorator(textToSign);
		CcpHashDecorator hash = textDecorator.hash();
		String token = hash.asString(CcpHashAlgorithm.SHA1);
		return token;
	}

	/**
	 * Tells whether the token was signed by this system for these values.
	 * @param token the token of the link
	 * @param values the values of the link, in the order of {@link #sign(String...)}
	 * @return {@code true} when it matches
	 */
	public boolean matches(String token, String... values) {
		String expectedToken = this.sign(values);
		boolean matches = expectedToken.equals(token);
		return matches;
	}

	private static String getSecret() {
		CcpJsonRepresentation systemProperties = JnSystemProperties.INSTANCE.systemProperties;
		String secret = systemProperties.getOrDefault(VisRecruiterResumesFields.recruiterLinksSecret, () -> "");
		boolean noSecret = secret.isBlank();
		if(noSecret) {
			throw new VisErrorRecruiterLinksSecretIsMissing();
		}
		return secret;
	}

	/** The system properties have no {@code recruiterLinksSecret}, so the links of the e-mail cannot be signed. */
	static class VisErrorRecruiterLinksSecretIsMissing extends RuntimeException {
		private static final long serialVersionUID = 1L;

		VisErrorRecruiterLinksSecretIsMissing() {
			super("The system properties have no recruiterLinksSecret: the links of the e-mail of resumes cannot be signed");
		}
	}
}
