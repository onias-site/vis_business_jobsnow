package com.vis.commons.json.validations;

import com.ccp.validation.annotations.AllowedValues;
import com.ccp.validation.annotations.ObjectNumbers;
import com.ccp.validation.annotations.ObjectTextSize;
import com.ccp.validation.annotations.SimpleArray;
import com.ccp.validation.annotations.SimpleObject;
import com.ccp.validation.annotations.CcpJsonFieldsValidation;
import com.ccp.validation.annotations.Year;
import com.ccp.validation.enums.AllowedValuesValidations;
import com.ccp.validation.enums.ObjectNumberValidations;
import com.ccp.validation.enums.ObjectTextSizeValidations;
import com.ccp.validation.enums.SimpleArrayValidations;
import com.ccp.validation.enums.SimpleObjectValidations;
import com.ccp.validation.enums.YearValidations;

@CcpJsonFieldsValidation(

		simpleObject = {
				@SimpleObject(rule = SimpleObjectValidations.requiredFields, 
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
				@SimpleObject(rule = SimpleObjectValidations.requiredAtLeastOne, 
				fields = { "clt", "pj" }),
				@SimpleObject(rule = SimpleObjectValidations.nonRepeatedLists, 
				fields = { "ddd", "disabilities", "companiesNotAllowed" }),
				@SimpleObject(rule = SimpleObjectValidations.integerFields, 
				fields = { "disponibility" , "experience"}), }

		, allowedValues = {
				@AllowedValues(rule = AllowedValuesValidations.arrayWithAllowedNumbers, 
						fields = { "ddd" },//LATER COMO INTERNACIONALIZAR ISSO???
						allowedValues = {"10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "21", "22", "24", "27", "28", "31", "32", "33", "34", 
								"35", "37", "38", "41", "42", "43", "44", "45", "46", "47", "48", "49", "51", "53", "54", "55", "61", "62", "63", 
								"64", "65", "66", "67", "68", "69", "71", "73", "74", "75", "77", "79", "81", "82", "83", "84", "85", "86", "87", "88", 
								"89", "91", "92", "93", "94", "95", "96", "97", "98", "99"}),
				@AllowedValues(rule = AllowedValuesValidations.arrayWithAllowedTexts, 
				fields = { "disabilities" },
						allowedValues = { "física/motora", "intelectual/mental", "visual", "auditiva" })
				//LATER COMO INTERNACIONALIZAR ISSO???

		},

		objectTextSize = {
				@ObjectTextSize(rule = ObjectTextSizeValidations.equalsOrLessThan, 
						fields = { "desiredJob", "lastJob" }, bound = 100),
				
				@ObjectTextSize(rule = ObjectTextSizeValidations.equalsOrLessThan, 
				fields = { "observations" }, bound = 500),
		@ObjectTextSize(rule = ObjectTextSizeValidations.equalsOrLessThan, 
				fields = { "resumeBase64" }, bound = 4_200_000),
		@ObjectTextSize(rule = ObjectTextSizeValidations.equalsOrGreaterThan, 
				fields = { "resumeBase64" }, bound = 512),
		@ObjectTextSize(rule = ObjectTextSizeValidations.equalsOrGreaterThan, 
		fields = {"name"}, bound = 3), //bound = 10), 
		@ObjectTextSize(rule = ObjectTextSizeValidations.equalsOrLessThan, 
				fields = {"name"}, bound = 100), //bound = 50), 
		@ObjectTextSize(rule = ObjectTextSizeValidations.equalsOrLessThan, 
		fields = {"email"}, bound = 41), //bound = 50), 
		},

		simpleArray = { @SimpleArray(rule = SimpleArrayValidations.notEmptyArray, fields = { "ddd"}),
				@SimpleArray(rule = SimpleArrayValidations.integerItems, fields = { "ddd" }), }

		, year = { @Year(rule = YearValidations.equalsOrLessThan, fields = { "experience" }, bound = 70),
				@Year(rule = YearValidations.equalsOrGreaterThan, fields = { "experience" }, bound = 0),

		},

		objectNumbers = {
				@ObjectNumbers(rule = ObjectNumberValidations.equalsOrGreaterThan, bound = 1000, 
						fields = { "btc","clt", "pj" }),
				@ObjectNumbers(rule = ObjectNumberValidations.equalsOrLessThan, bound = 100000, 
						fields = { "btc", "clt", "pj" }) }

)
public class VisJsonValidationResume {

}
