package com.vis.business.telegram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.ccp.decorators.CcpFileDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.vis.json.fields.validation.VisJsonSkillStatisticsFields;

/**
 * The region of a position of Telegram, by rules and a local table (no internet, no reading of the database), in this
 * order: a remote position with no mention to a hybrid or presencial work is home office; a city followed by its state
 * ({@code BARUERI/SP}, {@code BARUERI - SP}, {@code BARUERI, SP}, {@code BARUERI (SP)}); the first big city or known place
 * mentioned without the state; a remote position is home office after all; and no DDD at all.
 * The table is {@code documentation/vis/telegram/ddd-by-city.json}: the municipalities of each DDD, and the words that tell
 * a city with no state. Names that are common words or surnames (Santos, Natal, Americana, Franca, Vitória) are not among
 * these words: they count only when followed by the state. An instance is immutable, so the threads of the load share it.
 */
public final class VisTelegramDdd {

	/** The texts of the rules and the fields of the table. */
	enum Constants implements CcpJsonFieldName {
		/** The table, by its path from any module of the workspace. */
		TABLE_PATH { public String getValue() { return "../ccp_rest-api-tests_jobsnow/documentation/vis/telegram/ddd-by-city.json"; } },
		/** A remote position. */
		REMOTE { public String getValue() { return "\\b(?:HOME ?OFFICE|REMOTO|REMOTA|REMOTOS|REMOTAS|REMOTE|TELETRABALHO)\\b"; } },
		/** What takes out the remote from a position. */
		NOT_ONLY_REMOTE { public String getValue() { return "\\b(?:HIBRIDO|HIBRIDA|HYBRID|PRESENCIAL)\\b"; } },
		/** A city followed by its state. */
		CITY_AND_STATE {
			public String getValue() {
				return "([A-Z][A-Z' ]*[A-Z])\\s*[/,(\u2013\u2014-]\\s*(AC|AL|AM|AP|BA|CE|DF|ES|GO|MA|MG|MS|MT|PA|PB|PE|PI|PR|RJ|RN|RO|RR|RS|SC|SE|SP|TO)\\b";
			}
		},
		/** What joins the name of a city to its state in the keys of the table. */
		KEY_SEPARATOR { public String getValue() { return "|"; } },
		/** The apostrophe of some names of the table. */
		APOSTROPHE { public String getValue() { return "'"; } },
		/** What takes the place of the apostrophe. */
		NOTHING { public String getValue() { return ""; } },
		/** What separates the words of a name in the table. */
		SPACE { public String getValue() { return " "; } },
		/** What separates the words of a name in the text. */
		SPACES { public String getValue() { return " +"; } },
		/** The start of the expression of the words that tell a city. */
		WORDS_START { public String getValue() { return "\\b(?:"; } },
		/** The end of the expression of the words that tell a city. */
		WORDS_END { public String getValue() { return ")\\b"; } },
		/** The DDDs of the table. */
		cities,
		/** A DDD. */
		ddd,
		/** The state of a DDD. */
		state,
		/** The words of the table that tell a city with no state. */
		wordsWithoutState,
		/** A word of the table. */
		word,
	}

	/** The most words the name of a city has. */
	private static final int LONGEST_CITY_NAME = 7;

	/** Finds the mention to a remote position. */
	private static final Pattern REMOTE = Pattern.compile(Constants.REMOTE.getValue());

	/** Finds what takes out the remote from a position. */
	private static final Pattern NOT_ONLY_REMOTE = Pattern.compile(Constants.NOT_ONLY_REMOTE.getValue());

	/** Finds a city followed by its state. */
	private static final Pattern CITY_AND_STATE = Pattern.compile(Constants.CITY_AND_STATE.getValue());

	/** The DDD by the name of the city and its state. */
	private final Map<String, Integer> dddByCityAndState;

	/** The DDD by the word that tells it with no state. */
	private final Map<String, Integer> dddByWord;

	/** Finds the words that tell a DDD with no state. */
	private final Pattern wordsWithoutState;

	/**
	 * Reads the table from the workspace.
	 */
	public VisTelegramDdd() {
		this(getTable());
	}

	/**
	 * Takes the table.
	 * @param table the content of the table
	 */
	public VisTelegramDdd(CcpJsonRepresentation table) {
		Map<String, Integer> citiesAndStates = new HashMap<>();
		List<CcpJsonRepresentation> ddds = table.getAsJsonList(Constants.cities);
		String keySeparator = Constants.KEY_SEPARATOR.getValue();
		for (CcpJsonRepresentation ddd : ddds) {
			Integer dddNumber = ddd.getAsIntegerNumber(Constants.ddd);
			String state = ddd.getAsString(Constants.state);
			List<String> citiesOfTheDdd = ddd.getAsStringList(Constants.cities);
			for (String city : citiesOfTheDdd) {
				// the normalized text has no apostrophe: D'OESTE is read as DOESTE
				String cityAsInTheText = city.replace(Constants.APOSTROPHE.getValue(), Constants.NOTHING.getValue());
				citiesAndStates.put(cityAsInTheText + keySeparator + state, dddNumber);
			}
		}

		Map<String, Integer> words = new HashMap<>();
		List<CcpJsonRepresentation> wordsOfTheTable = table.getAsJsonList(Constants.wordsWithoutState);
		for (CcpJsonRepresentation wordOfTheTable : wordsOfTheTable) {
			String word = wordOfTheTable.getAsString(Constants.word);
			Integer dddOfTheWord = wordOfTheTable.getAsIntegerNumber(Constants.ddd);
			words.put(word, dddOfTheWord);
		}

		// the longest first: SAO JOSE DOS CAMPOS must win over a shorter name that starts the same way
		List<String> wordsByLength = new ArrayList<>(words.keySet());
		wordsByLength.sort(Comparator.comparingInt(String::length).reversed());
		String alternatives = wordsByLength.stream().map(Pattern::quote).collect(Collectors.joining(keySeparator));
		this.dddByCityAndState = citiesAndStates;
		this.dddByWord = words;
		this.wordsWithoutState = Pattern.compile(Constants.WORDS_START.getValue() + alternatives + Constants.WORDS_END.getValue());
	}

	/**
	 * The content of the table.
	 * @return the table
	 */
	private static CcpJsonRepresentation getTable() {
		String tablePath = Constants.TABLE_PATH.getValue();
		CcpStringDecorator tablePathDecorator = new CcpStringDecorator(tablePath);
		CcpFileDecorator tableFile = tablePathDecorator.file();
		CcpJsonRepresentation table = tableFile.asSingleJson();
		return table;
	}

	/**
	 * The region of the position.
	 * @param normalizedText the text of the position, as {@link VisTelegramPositionText#normalize} leaves it
	 * @return the DDD, {@link VisJsonSkillStatisticsFields#HOME_OFFICE} or {@link VisJsonSkillStatisticsFields#NO_DDD}
	 */
	public int find(String normalizedText) {
		Matcher remoteMention = REMOTE.matcher(normalizedText);
		boolean remote = remoteMention.find();
		Matcher notOnlyRemoteMention = NOT_ONLY_REMOTE.matcher(normalizedText);
		boolean onlyRemote = remote && false == notOnlyRemoteMention.find();

		if(onlyRemote) {
			return VisJsonSkillStatisticsFields.HOME_OFFICE;
		}

		int dddOfTheCityAndState = this.findCityAndState(normalizedText);
		boolean cityAndStateFound = dddOfTheCityAndState != VisJsonSkillStatisticsFields.NO_DDD;

		if(cityAndStateFound) {
			return dddOfTheCityAndState;
		}

		Matcher word = this.wordsWithoutState.matcher(normalizedText);

		if(word.find()) {
			String foundWord = word.group();
			return this.dddByWord.get(foundWord);
		}

		return remote ? VisJsonSkillStatisticsFields.HOME_OFFICE : VisJsonSkillStatisticsFields.NO_DDD;
	}

	/**
	 * The DDD of the first city followed by its state. What comes before the state may have more words than the city
	 * ({@code VAGA EM BARUERI/SP}): the longest ending of those words that is a municipality of that state counts.
	 * @param normalizedText the text of the position
	 * @return the DDD, or {@link VisJsonSkillStatisticsFields#NO_DDD}
	 */
	private int findCityAndState(String normalizedText) {
		String space = Constants.SPACE.getValue();
		String keySeparator = Constants.KEY_SEPARATOR.getValue();
		Matcher cityAndState = CITY_AND_STATE.matcher(normalizedText);
		while (cityAndState.find()) {
			String wordsBeforeTheState = cityAndState.group(1);
			String state = cityAndState.group(2);
			String[] wordsArray = wordsBeforeTheState.trim().split(Constants.SPACES.getValue());
			List<String> words = Arrays.asList(wordsArray);
			int wordsCount = words.size();
			int firstWord = Math.max(0, wordsCount - LONGEST_CITY_NAME);
			for (int start = firstWord; start < wordsCount; start++) {
				List<String> wordsOfTheCity = words.subList(start, wordsCount);
				String city = String.join(space, wordsOfTheCity);
				Integer ddd = this.dddByCityAndState.get(city + keySeparator + state);
				boolean notACity = null == ddd;

				if(notACity) {
					continue;
				}

				return ddd;
			}
		}
		return VisJsonSkillStatisticsFields.NO_DDD;
	}
}
