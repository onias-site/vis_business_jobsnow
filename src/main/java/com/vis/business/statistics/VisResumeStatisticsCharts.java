package com.vis.business.statistics;

import java.util.ArrayList;
import java.util.List;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpTimeDecorator;
import com.vis.entities.VisEntityResume;
import com.vis.json.fields.validation.VisLanguages;
import com.vis.json.transformers.VisSeniorityType;

/**
 * The pie charts of the statistics of the resumes of a region: each item tells, for a resume (a child of the group of
 * its ddd, with the fields of {@code VisEntityVirtualDddGrouper}), the slices it counts in. The keys are codes, not
 * texts: the screen gives each one its label.
 */
public enum VisResumeStatisticsCharts implements CcpJsonFieldName {
	/** The type of the resume: its code ({@code 1} to {@code 4}). */
	resumeType {
		public List<String> getKeys(CcpJsonRepresentation resume) {
			List<String> keys = getIntegerKey(resume, VisEntityResume.Fields.resumeType);
			return keys;
		}
	},
	/**
	 * The spoken languages: one slice per language and level ({@code <name>|<level>}, level 2 fluent and 1 intermediate),
	 * so the fluent and the intermediate English are different slices.
	 */
	spokenLanguage {
		public List<String> getKeys(CcpJsonRepresentation resume) {
			boolean noLanguage = false == resume.containsAllFields(VisEntityResume.Fields.spokenLanguage);

			if(noLanguage) {
				return List.of();
			}

			List<CcpJsonRepresentation> languages = resume.getAsJsonList(VisEntityResume.Fields.spokenLanguage);
			List<String> keys = new ArrayList<>();
			String separator = Values.LANGUAGE_SEPARATOR.getValue();
			for (CcpJsonRepresentation language : languages) {
				String name = language.getAsString(VisLanguages.name);
				Integer level = language.getAsIntegerNumber(VisLanguages.level);
				String key = name + separator + level;
				keys.add(key);
			}
			return keys;
		}
	},
	/** The skills the resume declares to have ({@code skillsFromResume}). */
	skill {
		public List<String> getKeys(CcpJsonRepresentation resume) {
			boolean noSkill = false == resume.containsAllFields(VisEntityResume.Fields.skill);

			if(noSkill) {
				return List.of();
			}

			List<String> keys = resume.getAsStringList(VisEntityResume.Fields.skill);
			return keys;
		}
	},
	/** Person with disability: {@code true} or {@code false}. */
	pcd {
		public List<String> getKeys(CcpJsonRepresentation resume) {
			List<String> keys = getBooleanKey(resume, VisEntityResume.Fields.pcd);
			return keys;
		}
	},
	/** Available to travel: {@code true} or {@code false}. */
	travel {
		public List<String> getKeys(CcpJsonRepresentation resume) {
			List<String> keys = getBooleanKey(resume, VisEntityResume.Fields.travel);
			return keys;
		}
	},
	/** The seniority, one of {@link VisSeniorityType}, from the year the experience started. */
	seniority {
		public List<String> getKeys(CcpJsonRepresentation resume) {
			VisSeniorityType seniorityType = getSeniority(resume);
			String key = seniorityType.name();
			return List.of(key);
		}
	},
	/** The availability: {@code immediate} (0 days) or {@code notImmediate}. */
	disponibility {
		public List<String> getKeys(CcpJsonRepresentation resume) {
			List<String> keys = getZeroOrMoreKey(resume, VisEntityResume.Fields.disponibility, Values.immediate, Values.notImmediate);
			return keys;
		}
	},
	/** The temporary jobs: {@code doesNotAccept} (0 months) or {@code accepts}. */
	temporallyJob {
		public List<String> getKeys(CcpJsonRepresentation resume) {
			List<String> keys = getZeroOrMoreKey(resume, VisEntityResume.Fields.temporallyJobTime, Values.doesNotAccept, Values.accepts);
			return keys;
		}
	},
	;

	/** The keys that are not values of the resume. */
	public static enum Values implements CcpJsonFieldName {
		immediate,
		notImmediate,
		accepts,
		doesNotAccept,
		notInformed,
		LANGUAGE_SEPARATOR {
			public String getValue() {
				return "|";
			}
		},
	}

	/**
	 * The slices the resume counts in.
	 * @param resume the child of the group of the ddd, parsed
	 * @return the keys of the slices, empty when it counts in none
	 */
	public abstract List<String> getKeys(CcpJsonRepresentation resume);

	/**
	 * The seniority of the resume: its years of experience (the current year minus the year the experience started)
	 * classified by {@link VisSeniorityType}; {@code especialista} above every range, {@code estag} without the year.
	 * @param resume the child of the group of the ddd, parsed
	 * @return the seniority
	 */
	public static VisSeniorityType getSeniority(CcpJsonRepresentation resume) {
		boolean noExperience = false == resume.containsAllFields(VisEntityResume.Fields.experience);

		if(noExperience) {
			return VisSeniorityType.estag;
		}

		int years = getYearsOfExperience(resume);
		CcpJsonRepresentation yearsJson = resume.put(VisEntityResume.Fields.experience, years);
		VisSeniorityType[] seniorities = VisSeniorityType.values();
		for (VisSeniorityType seniorityType : seniorities) {
			boolean matches = seniorityType.matches(VisEntityResume.Fields.experience, yearsJson);

			if(matches) {
				return seniorityType;
			}
		}
		return VisSeniorityType.especialista;
	}

	/**
	 * The years of experience of the resume: the current year minus the year the experience started, never negative.
	 * @param resume the child of the group of the ddd, parsed, with the field {@code experience}
	 * @return the years of experience
	 */
	public static int getYearsOfExperience(CcpJsonRepresentation resume) {
		Integer experienceStartYear = resume.getAsIntegerNumber(VisEntityResume.Fields.experience);
		CcpTimeDecorator now = new CcpTimeDecorator();
		int currentYear = now.getYear();
		int years = Math.max(0, currentYear - experienceStartYear);
		return years;
	}

	/**
	 * A salary of the resume, when informed.
	 * @param resume the child of the group of the ddd, parsed
	 * @param field {@code clt} or {@code pj}
	 * @return the salary, 0 when it is not informed
	 */
	public static double getSalary(CcpJsonRepresentation resume, CcpJsonFieldName field) {
		boolean notInformed = false == resume.containsAllFields(field);

		if(notInformed) {
			return 0d;
		}

		Double salary = resume.getAsDoubleNumber(field);
		double positiveSalary = Math.max(0d, salary);
		return positiveSalary;
	}

	private static List<String> getIntegerKey(CcpJsonRepresentation resume, CcpJsonFieldName field) {
		boolean absent = false == resume.containsAllFields(field);

		if(absent) {
			String notInformed = Values.notInformed.name();
			return List.of(notInformed);
		}

		Integer value = resume.getAsIntegerNumber(field);
		String key = String.valueOf(value);
		return List.of(key);
	}

	private static List<String> getBooleanKey(CcpJsonRepresentation resume, CcpJsonFieldName field) {
		boolean absent = false == resume.containsAllFields(field);

		if(absent) {
			String notInformed = Values.notInformed.name();
			return List.of(notInformed);
		}

		boolean value = resume.getAsBoolean(field);
		String key = String.valueOf(value);
		return List.of(key);
	}

	private static List<String> getZeroOrMoreKey(CcpJsonRepresentation resume, CcpJsonFieldName field, Values zero, Values more) {
		boolean absent = false == resume.containsAllFields(field);

		if(absent) {
			String notInformed = Values.notInformed.name();
			return List.of(notInformed);
		}

		Integer value = resume.getAsIntegerNumber(field);
		Values slice = value > 0 ? more : zero;
		String key = slice.name();
		return List.of(key);
	}
}
