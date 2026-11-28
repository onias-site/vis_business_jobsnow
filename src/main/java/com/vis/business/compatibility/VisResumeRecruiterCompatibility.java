package com.vis.business.compatibility;

import java.util.ArrayList;
import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTextDecorator;
import com.jn.entities.JnEntitySystemParameter;
import com.vis.business.recruiter.resumes.VisBusinessSendResumesToRecruiters;
import com.vis.business.recruiter.resumes.VisRecruiterResumesFields;
import com.vis.entities.VisEntityRecruiter;
import com.vis.entities.VisEntityResume;
import com.vis.json.transformers.VisJsonTransformerRecruiterDomain;
import com.vis.json.transformers.VisJsonTransformerRecruiterProfessional;

/**
 * Whether a recruiter and a resume are compatible (since 2026-11-28): the single door to the rules of
 * {@link VisResumeRecruiterRule}, used by every flow that sends a resume to a recruiter or shows a position to a
 * candidate. It builds the recruiter and the resume the rules read, applies them and, for the flows that read by id,
 * loads their facts ({@link VisResumeRecruiterFactsByIds}).
 */
public class VisResumeRecruiterCompatibility {

	/** The texts of this class. */
	static enum Constants implements CcpJsonFieldName {
		NOT_LETTERS_OR_DIGITS { public String getValue() { return "[^a-z0-9]"; } },
		DOMAIN_PARTS_SEPARATORS { public String getValue() { return "[.\\-]"; } },
		EMPTY { public String getValue() { return ""; } },
	}

	/** The shortest name of a company looked for inside the domain written without dots and hyphens. */
	public static final int SHORTEST_COMPANY_NAME = 4;

	private VisResumeRecruiterCompatibility() {}

	/**
	 * The recruiter the rules read, from their e-mail alone: the hash, the domain and the kind of the e-mail come from the
	 * same transformers that write them in {@code vis_recruiter}.
	 * @param email the e-mail of the recruiter
	 * @return the recruiter of the rules
	 */
	public static CcpJsonRepresentation getRecruiter(String email) {
		CcpJsonRepresentation recruiter = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiter.Fields.email, email);
		CcpJsonRepresentation withDomain = VisJsonTransformerRecruiterDomain.INSTANCE.execute(recruiter);
		CcpJsonRepresentation withProfessional = VisJsonTransformerRecruiterProfessional.INSTANCE.execute(withDomain);
		String emailHash = VisBusinessSendResumesToRecruiters.getEmailHash(email);
		String domain = withProfessional.getAsString(VisEntityRecruiter.Fields.domain);
		boolean professional = withProfessional.getAsBoolean(VisEntityRecruiter.Fields.professional);
		CcpJsonRepresentation recruiterOfTheRules = getRecruiter(emailHash, domain, professional);
		return recruiterOfTheRules;
	}

	/**
	 * The recruiter the rules read, of whom only the hash of the e-mail and the domain are known (the owner of a position).
	 * @param emailHash the e-mail of the recruiter, hashed
	 * @param domain the domain of the e-mail, empty when unknown
	 * @return the recruiter of the rules
	 */
	public static CcpJsonRepresentation getRecruiterOfTheDomain(String emailHash, String domain) {
		List<String> nonProfessionalDomains = JnEntitySystemParameter.getValues(JnEntitySystemParameter.NON_PROFESSIONAL_DOMAINS);
		boolean professional = false == domain.isEmpty() && false == nonProfessionalDomains.contains(domain);
		CcpJsonRepresentation recruiterOfTheRules = getRecruiter(emailHash, domain, professional);
		return recruiterOfTheRules;
	}

	private static CcpJsonRepresentation getRecruiter(String emailHash, String domain, boolean professional) {
		CcpJsonRepresentation recruiterOfTheRules = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.emailHash, emailHash)
				.put(VisEntityRecruiter.Fields.domain, domain)
				.put(VisEntityRecruiter.Fields.professional, professional);
		return recruiterOfTheRules;
	}

	/**
	 * The resume the rules read: the hashes of its e-mail and of its LinkedIn, the companies it forbids and whether the
	 * support confirmed its LinkedIn as invalid.
	 * @param storedResume the resume, as stored (the e-mail hashed)
	 * @return the resume of the rules
	 */
	public static CcpJsonRepresentation getResume(CcpJsonRepresentation storedResume) {
		String emailHash = storedResume.getAsString(VisEntityResume.Fields.email);
		String linkedinAddress = storedResume.getAsString(VisEntityResume.Fields.linkedinAddress);
		String linkedinHash = VisBusinessSendResumesToRecruiters.getLinkedinHash(linkedinAddress);
		List<String> notAllowedCompanies = storedResume.getOrDefault(VisEntityResume.Fields.notAllowedCompany, () -> List.of());
		// the flag may come as a text
		boolean invalidLinkedin = storedResume.containsAllFields(VisEntityResume.Fields.invalidLinkedinAddress) && storedResume.getAsBoolean(VisEntityResume.Fields.invalidLinkedinAddress);
		CcpJsonRepresentation resumeOfTheRules = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.emailHash, emailHash)
				.put(VisRecruiterResumesFields.linkedinHash, linkedinHash)
				.put(VisEntityResume.Fields.notAllowedCompany, notAllowedCompanies)
				.put(VisEntityResume.Fields.invalidLinkedinAddress, invalidLinkedin);
		return resumeOfTheRules;
	}

	/**
	 * Whether the resume may be sent to the recruiter: no rule broken.
	 * @param recruiter the recruiter of the rules
	 * @param resume the resume of the rules
	 * @param facts what the rules read from the database
	 * @return {@code true} when the resume may go
	 */
	public static boolean mayReceive(CcpJsonRepresentation recruiter, CcpJsonRepresentation resume, VisResumeRecruiterFacts facts) {
		VisResumeRecruiterRule[] rules = VisResumeRecruiterRule.values();
		for (VisResumeRecruiterRule rule : rules) {
			boolean broken = rule.isBroken(recruiter, resume, facts);
			if(broken) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Whether the resume may be shown to the recruiter on a screen they opened: no rule of the screens broken.
	 * @param recruiter the recruiter of the rules
	 * @param resume the resume of the rules
	 * @param facts what the rules read from the database
	 * @return {@code true} when the resume may be shown
	 */
	public static boolean mayBeShown(CcpJsonRepresentation recruiter, CcpJsonRepresentation resume, VisResumeRecruiterFacts facts) {
		VisResumeRecruiterRule[] rules = VisResumeRecruiterRule.values();
		for (VisResumeRecruiterRule rule : rules) {
			boolean otherKind = false == rule.holdsOnTheScreensOfTheRecruiter();
			if(otherKind) {
				continue;
			}
			boolean broken = rule.isBroken(recruiter, resume, facts);
			if(broken) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Whether the resume goes to no recruiter at all, by a rule of the resume alone.
	 * @param resume the resume of the rules
	 * @param facts what the rules read from the database
	 * @return {@code true} when the owner must correct the resume first
	 */
	public static boolean isTheResumeBlocked(CcpJsonRepresentation resume, VisResumeRecruiterFacts facts) {
		VisResumeRecruiterRule[] rules = VisResumeRecruiterRule.values();
		for (VisResumeRecruiterRule rule : rules) {
			boolean otherKind = false == rule.concernsOnlyTheResume();
			if(otherKind) {
				continue;
			}
			boolean broken = rule.isBroken(CcpOtherConstants.EMPTY_JSON, resume, facts);
			if(broken) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether the recruiter receives no resume at all, by a rule of the recruiter alone.
	 * @param recruiter the recruiter of the rules (only the hash of the e-mail is read)
	 * @param facts what the rules read from the database
	 * @return {@code true} when nothing goes to the recruiter
	 */
	public static boolean receivesNothing(CcpJsonRepresentation recruiter, VisResumeRecruiterFacts facts) {
		VisResumeRecruiterRule[] rules = VisResumeRecruiterRule.values();
		for (VisResumeRecruiterRule rule : rules) {
			boolean otherKind = false == rule.concernsOnlyTheRecruiter();
			if(otherKind) {
				continue;
			}
			boolean broken = rule.isBroken(recruiter, CcpOtherConstants.EMPTY_JSON, facts);
			if(broken) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The resumes that may be sent to the recruiter, their facts read by id.
	 * @param recruiterEmail the e-mail of the recruiter
	 * @param storedResumes the resumes, as stored
	 * @return the resumes, as stored, that may go
	 */
	public static List<CcpJsonRepresentation> getResumesTheRecruiterMayReceive(String recruiterEmail, List<CcpJsonRepresentation> storedResumes) {
		List<CcpJsonRepresentation> resumes = getCompatibleResumes(recruiterEmail, storedResumes, false);
		return resumes;
	}

	/**
	 * The resumes that may be shown to the recruiter on a screen they opened, their facts read by id.
	 * @param recruiterEmail the e-mail of the recruiter
	 * @param storedResumes the resumes, as stored
	 * @return the resumes, as stored, that may be shown
	 */
	public static List<CcpJsonRepresentation> getResumesTheRecruiterMayBeShown(String recruiterEmail, List<CcpJsonRepresentation> storedResumes) {
		List<CcpJsonRepresentation> resumes = getCompatibleResumes(recruiterEmail, storedResumes, true);
		return resumes;
	}

	private static List<CcpJsonRepresentation> getCompatibleResumes(String recruiterEmail, List<CcpJsonRepresentation> storedResumes, boolean onTheScreens) {
		List<CcpJsonRepresentation> compatibleResumes = new ArrayList<>();
		boolean noResume = storedResumes.isEmpty();
		if(noResume) {
			return compatibleResumes;
		}
		CcpJsonRepresentation recruiter = getRecruiter(recruiterEmail);
		List<CcpJsonRepresentation> resumes = new ArrayList<>();
		for (CcpJsonRepresentation storedResume : storedResumes) {
			CcpJsonRepresentation resume = getResume(storedResume);
			resumes.add(resume);
		}
		List<CcpJsonRepresentation> recruiters = List.of(recruiter);
		VisResumeRecruiterFacts facts = VisResumeRecruiterFactsByIds.load(recruiters, resumes);
		for (int index = 0; index < resumes.size(); index++) {
			CcpJsonRepresentation resume = resumes.get(index);
			boolean compatible = onTheScreens ? mayBeShown(recruiter, resume, facts) : mayReceive(recruiter, resume, facts);
			if(false == compatible) {
				continue;
			}
			CcpJsonRepresentation storedResume = storedResumes.get(index);
			compatibleResumes.add(storedResume);
		}
		return compatibleResumes;
	}

	/**
	 * Tells whether the resume forbids the company of the recruiter, from their e-mail alone (no read of
	 * {@code vis_recruiter}). A recruiter of a non-professional domain (gmail and the like) has no company to forbid.
	 * @param recruiterEmail the e-mail of the recruiter
	 * @param notAllowedCompanies the companies the resume forbids, as they are now
	 * @return {@code true} when the recruiter must not see the resume
	 */
	public static boolean isTheCompanyOfTheRecruiterForbidden(String recruiterEmail, List<String> notAllowedCompanies) {
		boolean nothingForbidden = notAllowedCompanies.isEmpty();
		if(nothingForbidden) {
			return false;
		}
		CcpJsonRepresentation recruiter = getRecruiter(recruiterEmail);
		boolean forbidden = isTheCompanyOfTheRecruiterForbidden(recruiter, notAllowedCompanies);
		return forbidden;
	}

	static boolean isTheCompanyOfTheRecruiterForbidden(CcpJsonRepresentation recruiter, List<String> notAllowedCompanies) {
		boolean nothingForbidden = notAllowedCompanies.isEmpty();
		if(nothingForbidden) {
			return false;
		}
		boolean personalEmail = false == recruiter.getAsBoolean(VisEntityRecruiter.Fields.professional);
		if(personalEmail) {
			return false;
		}
		String domain = recruiter.getAsString(VisEntityRecruiter.Fields.domain);
		boolean forbidden = isForbiddenCompany(domain, notAllowedCompanies);
		return forbidden;
	}

	/**
	 * Tells whether one of the companies is the one of the domain: the name, without accents, spaces and symbols, is a
	 * part of the domain (split by dots and hyphens) or, when it has at least {@value #SHORTEST_COMPANY_NAME} letters, is
	 * inside the domain written without dots and hyphens ({@code Itaú} forbids {@code itau.com.br} and
	 * {@code itau-unibanco.com.br}).
	 * @param domain the domain of the recruiter
	 * @param notAllowedCompanies the companies the resume forbids
	 * @return {@code true} when the domain is of a forbidden company
	 */
	public static boolean isForbiddenCompany(String domain, List<String> notAllowedCompanies) {
		String lowerCaseDomain = domain.toLowerCase();
		String partsSeparators = Constants.DOMAIN_PARTS_SEPARATORS.getValue();
		String[] domainParts = lowerCaseDomain.split(partsSeparators);
		List<String> domainPartsList = List.of(domainParts);
		String nothing = Constants.EMPTY.getValue();
		String joinedDomain = String.join(nothing, domainParts);
		for (String company : notAllowedCompanies) {
			String normalizedCompany = normalize(company);
			boolean empty = normalizedCompany.isEmpty();
			if(empty) {
				continue;
			}
			boolean isAPartOfTheDomain = domainPartsList.contains(normalizedCompany);
			if(isAPartOfTheDomain) {
				return true;
			}
			boolean longEnough = normalizedCompany.length() >= SHORTEST_COMPANY_NAME;
			boolean insideTheDomain = longEnough && joinedDomain.contains(normalizedCompany);
			if(insideTheDomain) {
				return true;
			}
		}
		return false;
	}

	private static String normalize(String text) {
		CcpStringDecorator textDecorator = new CcpStringDecorator(text);
		CcpTextDecorator textText = textDecorator.text();
		CcpTextDecorator withoutAccents = textText.stripAccents();
		String lowerCase = withoutAccents.content.toLowerCase();
		String notLettersOrDigits = Constants.NOT_LETTERS_OR_DIGITS.getValue();
		String nothing = Constants.EMPTY.getValue();
		String normalized = lowerCase.replaceAll(notLettersOrDigits, nothing);
		return normalized;
	}
}
