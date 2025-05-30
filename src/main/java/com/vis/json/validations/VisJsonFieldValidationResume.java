package com.vis.json.validations;

import com.ccp.validation.annotations.CcpAllowedValues;
import com.ccp.validation.annotations.CcpObjectNumbers;
import com.ccp.validation.annotations.CcpObjectTextSize;
import com.ccp.validation.annotations.CcpSimpleArray;
import com.ccp.validation.annotations.CcpSimpleObject;
import com.ccp.validation.annotations.CcpJsonFieldsValidation;
import com.ccp.validation.annotations.CcpYear;
import com.ccp.validation.enums.CcpAllowedValuesValidations;
import com.ccp.validation.enums.CcpObjectNumberValidations;
import com.ccp.validation.enums.CcpObjectTextSizeValidations;
import com.ccp.validation.enums.CcpSimpleArrayValidations;
import com.ccp.validation.enums.CcpSimpleObjectValidations;
import com.ccp.validation.enums.YearValidations;

@CcpJsonFieldsValidation(

		simpleObject = {
				@CcpSimpleObject(rule = CcpSimpleObjectValidations.requiredFields, 
						fields = { 
						"companiesNotAllowed", 
						"ddd", 
						"desiredJob", 
						"disponibility", 
						"disabilities",
						"experience",
						"lastJob", 
						"name",
						"fileName",
						"observations", 
						"resumeBase64", 
						"email"
						}),
				@CcpSimpleObject(rule = CcpSimpleObjectValidations.requiredAtLeastOne, 
				fields = { "clt", "pj" }),
				@CcpSimpleObject(rule = CcpSimpleObjectValidations.nonRepeatedLists, 
				fields = { "ddd", "disabilities", "companiesNotAllowed" }),
				@CcpSimpleObject(rule = CcpSimpleObjectValidations.integerFields, 
				fields = { "disponibility" , "experience"}), }

		, allowedValues = {
				@CcpAllowedValues(rule = CcpAllowedValuesValidations.arrayWithAllowedNumbers, 
						fields = { "ddd" },//LATER COMO INTERNACIONALIZAR ISSO???
						allowedValues = {"10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "21", "22", "24", "27", "28", "31", "32", "33", "34", 
								"35", "37", "38", "41", "42", "43", "44", "45", "46", "47", "48", "49", "51", "53", "54", "55", "61", "62", "63", 
								"64", "65", "66", "67", "68", "69", "71", "73", "74", "75", "77", "79", "81", "82", "83", "84", "85", "86", "87", "88", 
								"89", "91", "92", "93", "94", "95", "96", "97", "98", "99"}),
				@CcpAllowedValues(rule = CcpAllowedValuesValidations.arrayWithAllowedTexts, 
				fields = { "disabilities" },
						allowedValues = { "física/motora", "intelectual/mental", "visual", "auditiva" })
				//LATER COMO INTERNACIONALIZAR ISSO???

		},

		objectTextSize = {
				@CcpObjectTextSize(rule = CcpObjectTextSizeValidations.equalsOrLessThan, 
						fields = { "desiredJob", "lastJob" }, bound = 100),
				
				@CcpObjectTextSize(rule = CcpObjectTextSizeValidations.equalsOrLessThan, 
				fields = { "observations" }, bound = 500),
		@CcpObjectTextSize(rule = CcpObjectTextSizeValidations.equalsOrLessThan, 
				fields = { "resumeBase64" }, bound = 4_200_000),
		@CcpObjectTextSize(rule = CcpObjectTextSizeValidations.equalsOrGreaterThan, 
				fields = { "resumeBase64" }, bound = 512),
		@CcpObjectTextSize(rule = CcpObjectTextSizeValidations.equalsOrGreaterThan, 
		fields = {"name"}, bound = 3), //bound = 10), 
		@CcpObjectTextSize(rule = CcpObjectTextSizeValidations.equalsOrLessThan, 
				fields = {"name"}, bound = 100), //bound = 50), 
		@CcpObjectTextSize(rule = CcpObjectTextSizeValidations.equalsOrLessThan, 
		fields = {"email"}, bound = 41), //bound = 50), 
		},

		simpleArray = { @CcpSimpleArray(rule = CcpSimpleArrayValidations.notEmptyArray, fields = { "ddd"}),
				@CcpSimpleArray(rule = CcpSimpleArrayValidations.integerItems, fields = { "ddd" }), }

		, year = { @CcpYear(rule = YearValidations.equalsOrLessThan, fields = { "experience" }, bound = 70),
				@CcpYear(rule = YearValidations.equalsOrGreaterThan, fields = { "experience" }, bound = 0),

		},

		objectNumbers = {
				@CcpObjectNumbers(rule = CcpObjectNumberValidations.equalsOrGreaterThan, bound = 1000, 
						fields = { "btc","clt", "pj" }),
				@CcpObjectNumbers(rule = CcpObjectNumberValidations.equalsOrLessThan, bound = 100000, 
						fields = { "btc", "clt", "pj" }) }

)
public class VisJsonFieldValidationResume {

}
