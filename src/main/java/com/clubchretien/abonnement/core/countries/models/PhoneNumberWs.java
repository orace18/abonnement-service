package com.clubchretien.abonnement.core.countries.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class PhoneNumberWs {

	@ToString.Include
	@EqualsAndHashCode.Include
	private String code;


	@ToString.Include
	@EqualsAndHashCode.Include
	private String number;

	@ToString.Include
	@EqualsAndHashCode.Include
	private String fullNumber;

	public String getFullNumber() {
		if (fullNumber == null || fullNumber.trim().isEmpty())
			fullNumber = Stream.of(this.getCode(), this.getNumber()).filter(Objects::nonNull)
					.map(String::trim).filter(Predicate.not(String::isEmpty))
					.collect(Collectors.joining());
		return fullNumber;
	}
}
