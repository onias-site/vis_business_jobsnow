package com.vis.json.fields.validation;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.vis.entities.VisEntityPosition;

/**
 * The filters of a search of resumes (the field {@code json} of {@code vis_resume_filter}, since 2026-11-25): the fields
 * of {@code VisEntityPosition} that filter the resumes counted by the position form, with the same validations of the
 * position, but only the area ({@code resumeType}) is required, so the user filters with as much freedom as possible.
 * Each field copies its type from the class where the position takes it (the copy is of one level only: a field of the
 * position that copies from {@code VisJsonCommonsFields} is copied here from there too); the lists repeat their sizes,
 * which the validation reads from the field itself.
 */
public enum VisJsonFieldsResumeFilter implements CcpJsonFieldName {

	/** The area of the resumes, the only required filter. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	resumeType,

	/** The regions: 1 to 68 ddds (10 is the home office, every ddd); absent, every ddd. */
	@CcpJsonFieldValidatorArray(minSize = 1, maxSize = 68)
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	ddd,

	/** Days for the candidate to start: the resumes that start in up to these days; absent or 0, no filter. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	disponibility,

	/** Months of the temporary job: the resumes that accept up to these months; absent or 0, no filter. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	temporallyJobTime,

	/** The seniorities: the resumes whose years of experience fall in the range of any of them. */
	@CcpJsonFieldValidatorArray(minSize = 1, maxSize = 6)
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	seniority,

	/** The skills, as the position keeps them: the ones of the category {@code skillsFromText} are required in the resumes. */
	@CcpJsonFieldValidatorArray
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	skill,

	/** The languages, each one with its minimum level. */
	@CcpJsonFieldValidatorArray
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	spokenLanguage,

	/** Checked, only the resumes that negotiate the salary claim. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	negotiableClaim,

	/** Checked, only the resumes of people with disabilities. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	pcd,

	/** Checked, only the resumes available to travel. */
	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	travel,

	/** The range of the CLT claim, {@code {min, max}} from 1,000 to 100,000. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	clt,

	/** The range of the PJ claim, {@code {min, max}} from 1,000 to 100,000. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	pj,

	/** The domain of the e-mail of the logged recruiter: the resumes that forbid that company are left out. */
	@CcpJsonCopyFieldValidationsFrom(VisEntityPosition.Fields.class)
	recruiterDomain,
	;
}
