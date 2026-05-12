package com.clubchretien.abonnement.core.countries.models;

import com.clubchretien.abonnement.core.constants.DefaultConstants;
import com.clubchretien.abonnement.xss.Utils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Locale;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class CountryWs {

	public static final String ENTITY = "country";

	public static final String FIELD_CODE = "code";
	public static final String COLUMN_CODE = ENTITY + "." + FIELD_CODE;

	@NotNull
	@NotBlank
	@ToString.Include
	@EqualsAndHashCode.Include
	private String code;

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	@ToString.Include
	private String iso;

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	@ToString.Include
	private String name;

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private String formatted;

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	@ToString.Include
	private PhoneWs phone;

//	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
//	@ToString.Include
//	private CurrencyWs currency;
//
//	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
//	@ToString.Include
//	private List<CurrencyWs> currencies;

	public static CountryWs From(Locale locale) {
		CountryWs country = CountryWs.builder()
				.iso(locale.getISO3Country())
				.code(locale.getCountry())
				.name(locale.getDisplayCountry())
				.formatted(locale.getDisplayCountry(locale))
				.build();
		CountryWs.Trim(country);
		if (country.isEmpty()) return null;
//		country.setCurrency(currencyOf(country, locale));
//		country.setCurrencies(currenciesOf(country));
		return country;
	}

//	public static CurrencyWs currencyOf(CountryWs country) {
//		return CurrencyWs.FromCountryCode(country.getCode());
//	}
//
//	public static List<CurrencyWs> currenciesOf(CountryWs country) {
//		return new ArrayList<>(CurrencyDataCenter.currenciesByCountryCode(country.getCode()));
//	}
//
//	public static CurrencyWs currencyOf(CountryWs country, Locale locale) {
//		CurrencyWs currency = CurrencyWs.FromCountryCode(country.getCode());
//		currency.update(locale);
//		return currency;
//	}

	public static CountryWs From(String code, String language) {
		Locale locale = new Locale(language, code);
		return From(locale);
	}

	public static CountryWs From(String code, Locale l) {
		Locale locale = new Locale(l.getLanguage(), code);
		return From(locale);
	}

	public static CountryWs From(String code) {
		if (code == null) {
			return CountryWs.Default();
		}
		Locale locale = new Locale(
				LocaleContextHolder.getLocale().getLanguage(), code
		);
		return From(locale);
	}

	public static CountryWs Trim(CountryWs country) {
		country.setCode(Utils.Trim(country.getCode()));
		country.setIso(Utils.Trim(country.getIso()));
		country.setName(Utils.Trim(country.getName()));
		country.setFormatted(Utils.Trim(country.getFormatted()));
		return country;
	}

	public static CountryWs Default() {
		return CountryWs.From(DefaultConstants.COUNTRY_CODE);
	}

	public void update(CountryWs request) {
		if (request.getCode() != null && !request.getCode().equals(this.getCode())) {
			this.setCode(request.getCode());
		}
		if (request.getIso() != null && !request.getIso().equals(this.getIso())) {
			this.setIso(request.getIso());
		}
		if (request.getName() != null && !request.getName().equals(this.getName())) {
			this.setName(request.getName());
		}
//		this.getCurrency().update(this.locale());
	}

	private Locale locale() {
		if (this.getCode() == null) {
			return LocaleContextHolder.getLocale();
		}
		return new Locale(LocaleContextHolder.getLocale().getLanguage(), this.getCode());
	}

	public String getIso() {
		if (iso == null && this.locale() != null) {
			iso = Objects.requireNonNull(this.locale()).getISO3Country();
		}
		return iso;
	}

	public String getName() {
		if (name == null && this.locale() != null) {
			name = Objects.requireNonNull(this.locale()).getDisplayCountry();
		}
		return name;
	}

	public String getFormatted() {
		if (formatted == null) {
			formatted = this.locale().getDisplayCountry(this.locale());
		}
		return formatted;
	}

	public PhoneWs getPhone() {
		if (phone == null && this.getCode() != null) {
			phone = PhoneWs.From(this);
		}
		return phone;
	}

//	public CurrencyWs getCurrency() {
//		if (currency == null && this.getCode() != null) {
//			currency = currencyOf(this);
//		}
//		return currency;
//	}

	@JsonIgnore
	public boolean isSame(CountryWs model) {
		return (
				(this.getCode() == null || this.getCode().equals(model.getCode()))
		);
	}

	@JsonIgnore
	public boolean isEmpty() {
		Trim(this);
		return (
				(this.getCode() == null) || (this.getIso() == null)
						|| (this.getName() == null) || (this.getFormatted() == null)
		);
	}

	public boolean contain(String search) {
		if (isEmpty()) {
			return false;
		}
		String container = this.getCode() + this.getIso() + this.getName()
				+ this.getFormatted() + this.getPhone().getCode()
//				+ this.getCurrency().getCode() + this.getCurrency().getFormatted()
				;
		return StringUtils.containsIgnoreCase(container, search);
	}
}
