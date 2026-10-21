package com.vis.business.recruiter.resumes;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * The metropolitan region of each DDD accepted by the {@code ddd} of the resume (the same names of the legacy front
 * end, {@code JnRegion.js}). The DDD 10 is not a region: it is the resume that accepts only home office, and has no item
 * here. The names are proper names, the same in every language.
 */
public enum VisMetropolitanRegions implements CcpJsonFieldName {
	DDD_11 { public String getValue() { return "São Paulo – SP"; } },
	DDD_12 { public String getValue() { return "São José dos Campos – SP"; } },
	DDD_13 { public String getValue() { return "Santos – SP"; } },
	DDD_14 { public String getValue() { return "Bauru – SP"; } },
	DDD_15 { public String getValue() { return "Sorocaba – SP"; } },
	DDD_16 { public String getValue() { return "Ribeirão Preto – SP"; } },
	DDD_17 { public String getValue() { return "São José do Rio Preto – SP"; } },
	DDD_18 { public String getValue() { return "Presidente Prudente – SP"; } },
	DDD_19 { public String getValue() { return "Campinas – SP"; } },
	DDD_21 { public String getValue() { return "Rio de Janeiro – RJ"; } },
	DDD_22 { public String getValue() { return "Campos dos Goytacazes – RJ"; } },
	DDD_24 { public String getValue() { return "Volta Redonda – RJ"; } },
	DDD_27 { public String getValue() { return "Vila Velha/Vitória – ES"; } },
	DDD_28 { public String getValue() { return "Cachoeiro de Itapemirim – ES"; } },
	DDD_31 { public String getValue() { return "Belo Horizonte – MG"; } },
	DDD_32 { public String getValue() { return "Juiz de Fora – MG"; } },
	DDD_33 { public String getValue() { return "Governador Valadares – MG"; } },
	DDD_34 { public String getValue() { return "Uberlândia – MG"; } },
	DDD_35 { public String getValue() { return "Poços de Caldas – MG"; } },
	DDD_37 { public String getValue() { return "Divinópolis – MG"; } },
	DDD_38 { public String getValue() { return "Montes Claros – MG"; } },
	DDD_41 { public String getValue() { return "Curitiba – PR"; } },
	DDD_42 { public String getValue() { return "Ponta Grossa – PR"; } },
	DDD_43 { public String getValue() { return "Londrina – PR"; } },
	DDD_44 { public String getValue() { return "Maringá – PR"; } },
	DDD_45 { public String getValue() { return "Foz do Iguaçu – PR"; } },
	DDD_46 { public String getValue() { return "Francisco Beltrão/Pato Branco – PR"; } },
	DDD_47 { public String getValue() { return "Joinville – SC"; } },
	DDD_48 { public String getValue() { return "Florianópolis – SC"; } },
	DDD_49 { public String getValue() { return "Chapecó – SC"; } },
	DDD_51 { public String getValue() { return "Porto Alegre – RS"; } },
	DDD_53 { public String getValue() { return "Pelotas – RS"; } },
	DDD_54 { public String getValue() { return "Caxias do Sul – RS"; } },
	DDD_55 { public String getValue() { return "Santa Maria – RS"; } },
	DDD_61 { public String getValue() { return "Brasília – DF"; } },
	DDD_62 { public String getValue() { return "Goiânia – GO"; } },
	DDD_63 { public String getValue() { return "Palmas – TO"; } },
	DDD_64 { public String getValue() { return "Rio Verde – GO"; } },
	DDD_65 { public String getValue() { return "Cuiabá – MT"; } },
	DDD_66 { public String getValue() { return "Rondonópolis – MT"; } },
	DDD_67 { public String getValue() { return "Campo Grande – MS"; } },
	DDD_68 { public String getValue() { return "Rio Branco – AC"; } },
	DDD_69 { public String getValue() { return "Porto Velho – RO"; } },
	DDD_71 { public String getValue() { return "Salvador – BA"; } },
	DDD_73 { public String getValue() { return "Ilhéus – BA"; } },
	DDD_74 { public String getValue() { return "Juazeiro – BA"; } },
	DDD_75 { public String getValue() { return "Feira de Santana – BA"; } },
	DDD_77 { public String getValue() { return "Barreiras – BA"; } },
	DDD_79 { public String getValue() { return "Aracaju – SE"; } },
	DDD_81 { public String getValue() { return "Recife – PE"; } },
	DDD_82 { public String getValue() { return "Maceió – AL"; } },
	DDD_83 { public String getValue() { return "João Pessoa – PB"; } },
	DDD_84 { public String getValue() { return "Natal – RN"; } },
	DDD_85 { public String getValue() { return "Fortaleza – CE"; } },
	DDD_86 { public String getValue() { return "Teresina – PI"; } },
	DDD_87 { public String getValue() { return "Petrolina – PE"; } },
	DDD_88 { public String getValue() { return "Juazeiro do Norte – CE"; } },
	DDD_89 { public String getValue() { return "Picos – PI"; } },
	DDD_91 { public String getValue() { return "Belém – PA"; } },
	DDD_92 { public String getValue() { return "Manaus – AM"; } },
	DDD_93 { public String getValue() { return "Santarém – PA"; } },
	DDD_94 { public String getValue() { return "Marabá – PA"; } },
	DDD_95 { public String getValue() { return "Boa Vista – RR"; } },
	DDD_96 { public String getValue() { return "Macapá – AP"; } },
	DDD_97 { public String getValue() { return "Coari – AM"; } },
	DDD_98 { public String getValue() { return "São Luís – MA"; } },
	DDD_99 { public String getValue() { return "Imperatriz – MA"; } },
	;

	/** The prefix of the names of the items, followed by the DDD. */
	static enum Prefix implements CcpJsonFieldName {
		DDD_,
	}

	/**
	 * Finds the region of a DDD.
	 * @param ddd the DDD of the resume
	 * @return the region, or nothing for the DDD 10 (home office) or an unknown one
	 */
	public static Optional<VisMetropolitanRegions> fromDdd(int ddd) {
		String itemName = Prefix.DDD_.name() + ddd;
		VisMetropolitanRegions[] allRegions = values();
		Stream<VisMetropolitanRegions> regions = Arrays.stream(allRegions);
		Optional<VisMetropolitanRegions> region = regions.filter(x -> x.name().equals(itemName)).findFirst();
		return region;
	}
}
