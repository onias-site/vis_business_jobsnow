package com.vis.exceptions;

import java.util.List;

@SuppressWarnings("serial")
public class VisErrorBusinessRequiredSkillsMissingInResume extends RuntimeException{

	public final List<String> requiredSkillsNotFoundInResume;

	public VisErrorBusinessRequiredSkillsMissingInResume(List<String> requiredSkillsNotFoundInResume) {
		this.requiredSkillsNotFoundInResume = requiredSkillsNotFoundInResume;
	}
	
	
	
}
