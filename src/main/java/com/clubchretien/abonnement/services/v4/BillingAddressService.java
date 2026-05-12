package com.clubchretien.abonnement.services.v4;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.billing.response.AdressePartenaireWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.AdresseRepository;
import com.owomi.rest.client.billingv4.BillingAdressePartenaireClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Component
public class BillingAddressService {
	private static Logger logger = LogManager.getLogger(BillingAddressService.class);
	@Autowired
	Mapper mapper;

	@Autowired
	BillingAdressePartenaireClient billingAdressePartenaireClient;
	@Autowired
	AdresseRepository adresseRepository;

	public CommonResponseWs create(AdressePartenaireWs ws, Partenaire partenaire) throws DataNotFoundException, OwomiException, AuthentificationException {
		ws.setPartenaire(partenaire.getId().toString());
		return billingAdressePartenaireClient.create(ws);
	}


	public CommonResponseWs update(AdressePartenaireWs ws, Partenaire partenaire) throws DataNotFoundException, OwomiException, AuthentificationException {
		ws.setPartenaire(partenaire.getId().toString());
		return billingAdressePartenaireClient.update(ws);
	}

	public CommonResponseWs get(String id, Partenaire partenaire) throws DataNotFoundException, OwomiException, AuthentificationException {
		return billingAdressePartenaireClient.find(id);
	}

	public AdressePartenaireWs adressePartenaire(String locationId, Partenaire partenaire) {
		try {
			return this.address(locationId, partenaire);
		} catch (Exception ignored) {
		}
		return null;
	}

	public AdressePartenaireWs address(String id, Partenaire partenaire) throws DataNotFoundException, AuthentificationException, OwomiException {
		CommonResponseWs responseWs = this.get(id, partenaire);
		return this.address(responseWs);
	}

	public AdressePartenaireWs address(CommonResponseWs responseWs) throws DataNotFoundException, AuthentificationException, OwomiException {
		if (!responseWs.getStatus().equals(WsOperationResponseStatus.success)) {
			return null;
		}
		Gson gson = new Gson();
		return gson.fromJson(gson.toJson(responseWs.getData()), AdressePartenaireWs.class);
	}

	public List<AdressePartenaireWs> addresses(CommonResponseWs responseWs) throws DataNotFoundException, AuthentificationException, OwomiException {
		if (!responseWs.getStatus().equals(WsOperationResponseStatus.success)) {
			return new ArrayList<>();
		}
		Type listType = new TypeToken<ArrayList<AdressePartenaireWs>>() {
		}.getType();
		Gson gson = new Gson();
		List<AdressePartenaireWs> list = gson.fromJson(gson.toJson(responseWs.getData()), listType);
		return list;
	}


	public List<AdressePartenaireWs> addresses(Partenaire partenaire) throws DataNotFoundException, AuthentificationException, OwomiException {
		CommonResponseWs responseWs = this.get(partenaire);
		return this.addresses(responseWs);
	}

	public CommonResponseWs get(Partenaire partenaire) throws DataNotFoundException, OwomiException, AuthentificationException {
		return billingAdressePartenaireClient.listByPartenaire(partenaire.getId().toString());
	}

	public CommonResponseWs delete(String id, Partenaire partenaire) throws DataNotFoundException, OwomiException, AuthentificationException {
		return billingAdressePartenaireClient.delete(id);
	}

}
