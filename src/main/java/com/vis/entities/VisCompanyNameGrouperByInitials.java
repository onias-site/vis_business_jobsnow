package com.vis.entities;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTextDecorator;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Consumer of the records of the legacy {@code old_recruiters} index of
 * {@link VisEntityGroupCompaniesByTheirFirstThreeInitials}: the company name is the first part of the domain of the
 * recruiter e-mail (capitalized, at least 3 letters), grouped by its first three letters.
 */
class VisCompanyNameGrouperByInitials implements Consumer<CcpJsonRepresentation> {

	/** Receives the companies grouped by their first three letters. */
	private final Map<String, Set<String>> groupedCompanies;

	/**
	 * Keeps where the companies go.
	 * @param groupedCompanies receives the companies grouped by their first three letters
	 */
	VisCompanyNameGrouperByInitials(Map<String, Set<String>> groupedCompanies) {
		this.groupedCompanies = groupedCompanies;
	}

	/**
	 * Adds the company of the recruiter e-mail to its group.
	 * @param json the record of {@code old_recruiters}
	 */
	public void accept(CcpJsonRepresentation json) {
		String recruiterEmail = json.getAsString(VisJsonCommonsFields.id);
		String[] emailParts = recruiterEmail.split("@");
		boolean isNotAnEmail = emailParts.length != 2;
		if(isNotAnEmail) {
			return;
		}

		String domain = emailParts[1];

		String[] domainParts = domain.split("\\.");
		String upperCaseCompanyName = domainParts[0].toUpperCase();

		String companyName = upperCaseCompanyName.trim();
		int companyNameLength = companyName.length();
		boolean companyNameIsTooShort = companyNameLength < 3;

		if(companyNameIsTooShort) {
			return;
		}
		CcpStringDecorator companyNameDecorator = new CcpStringDecorator(companyName);
		CcpTextDecorator companyNameText = companyNameDecorator.text();
		var capitalizedText = companyNameText.capitalize();

		String capitalizedCompanyName = capitalizedText.content;

		String initials = companyName.substring(0, 3);
		Set<String> companiesWithTheseInitials = this.groupedCompanies.computeIfAbsent(initials, key -> new LinkedHashSet<>());
		companiesWithTheseInitials.add(capitalizedCompanyName);
	}
}
