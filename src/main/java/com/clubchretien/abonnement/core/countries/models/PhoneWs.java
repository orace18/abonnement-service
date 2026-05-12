package com.clubchretien.abonnement.core.countries.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.owomi.commons.utils.StringUtils;
import lombok.*;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class PhoneWs {

	public static final String ENTITY = "phone";

	public static final String FIELD_ISO = "iso";
	public static final String FIELD_CODE = "code";
	public static final String FIELD_NAME = "name";
	public static final String COLUMN_CODE = ENTITY + "." + FIELD_CODE;
	public static final String COLUMN_ISO = ENTITY + "." + FIELD_ISO;
	public static final String COLUMN_NAME = ENTITY + "." + FIELD_NAME;

	@NotNull
	@ToString.Include
	@EqualsAndHashCode.Include
	private Integer code;

	@NotNull
	@EqualsAndHashCode.Include
	private Long example;

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	@ToString.Include
	private String formatted;

	public static PhoneWs From(CountryWs country) {
		return From(country.getCode());
	}

	public static PhoneWs From(String country) {
		Integer code = PhoneNumberUtil.getInstance().getCountryCodeForRegion(country);
		Phonenumber.PhoneNumber phone = PhoneNumberUtil.getInstance().getExampleNumber(country);
		long example = 0L;
		if (phone != null) {
			example = phone.getNationalNumber();
		}
		return PhoneWs.builder().code(code).example(example).build();
	}

	public static PhoneWs FromRawTelephone(String rawInput) {
		Integer countryCode = CountryCode(rawInput);
		Long example = PhoneNumberWithoutCountryCodeAsInteger(rawInput);
		return PhoneWs.builder().code(countryCode).example(example).build();
	}

	public static String PhoneNumberWithoutCountryCode(String rawPhoneNumber) {
		try {
			PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
			// Parse the raw phone number
			Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(rawPhoneNumber, null);
			// Get the formatted phone number without the country code
			String number = phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
			return StringUtils.StripSpace(number);
		} catch (Exception e) {
			// Handle parsing exceptions
			e.printStackTrace();
		}
		return null;
	}

	public static Long PhoneNumberWithoutCountryCodeAsInteger(String rawPhoneNumber) {
		String value = PhoneNumberWithoutCountryCode(rawPhoneNumber);
		if (value == null) return null;
		return Long.parseLong(value);
	}

	public static Integer CountryCode(String rawInput) {
		try {
			return PhoneNumberUtil.getInstance().parse(rawInput, null).getCountryCode();
		} catch (Exception e) {
//			e.printStackTrace();
		}
		return null;
	}

	public static String PhoneNumberCountryCode(String rawInput) {
		Integer code = CountryCode(rawInput);
		if (code == null) return null;
		return code.toString();
	}



	public String getFormatted() {
		Phonenumber.PhoneNumber number = new Phonenumber.PhoneNumber();
		number.setCountryCode(this.getCode());
		number.setNationalNumber(this.getExample());
		return PhoneNumberUtil.getInstance().format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
	}
}
