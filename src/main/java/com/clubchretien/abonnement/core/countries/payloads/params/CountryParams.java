package com.clubchretien.abonnement.core.countries.payloads.params;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode
@ToString(callSuper = true)
public class CountryParams {

	private String code;
	private String search;

	public boolean hasData() {
		return this.getCode() != null || this.getSearch() != null;
	}
}
