package com.clubchretien.abonnement.core.countries.services;


import com.clubchretien.abonnement.core.countries.models.CountryWs;
import com.clubchretien.abonnement.core.countries.payloads.params.CountryParams;
import com.clubchretien.abonnement.xss.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class CountryDataCenter {

	public static Collection<CountryWs> countries() {
		List<CountryWs> countries = new ArrayList<>();
		String[] isoCountries = Locale.getISOCountries();
		for (String iso : isoCountries) {
			CountryWs country = CountryWs.From(iso);
			countries.add(country);
		}
		countries = countries.stream()
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(CountryWs::getName))
				.collect(Collectors.toList());
		return countries;
	}

	public static Collection<CountryWs> allByPhoneCode(String phoneCode) {
		try {
			Integer code = Integer.parseInt(phoneCode);
			return countries().stream()
					.filter(country -> country.getPhone() != null && country.getPhone().getCode() != null)
					.filter(country -> country.getPhone().getCode().equals(code))
					.collect(Collectors.toList());
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return new ArrayList<>();
	}

	public static CountryWs findByPhoneCode(String phoneCode) {
		try {
			Integer code = Integer.parseInt(phoneCode);
			return countries().stream()
					.filter(country -> country.getPhone() != null && country.getPhone().getCode() != null)
					.filter(country -> country.getPhone().getCode().equals(code))
					.findFirst().orElse(CountryWs.Default());
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return CountryWs.Default();
	}


	public static CountryWs find(String name) {
		return countries().stream()
				.filter(country -> country.getName().equalsIgnoreCase(Utils.StripAccent(name)))
				.findFirst().orElse(CountryWs.Default());
	}

	public static Collection<CountryWs> all(CountryParams params) {
		return filter(countries(), params);
	}

	private static Collection<CountryWs> filter(Collection<CountryWs> countries, CountryParams params) {
		if (!params.hasData()) {
			return countries;
		}
		return countries.stream()
				.filter(country -> params.getSearch() == null || country.contain(params.getSearch()))
				.filter(country -> params.getCode() == null
						|| country.getIso().equalsIgnoreCase(params.getCode())
						|| country.getCode().equalsIgnoreCase(params.getCode())
				)
				.collect(Collectors.toList());
	}

}
