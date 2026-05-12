package com.clubchretien.abonnement.services;

import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.PartenaireBonusAccountSoldeWs;
import com.owomi.commons.ws.PartenaireBonusAccountWs;
import com.owomi.commons.ws.billing.response.PageResponse;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.CvToDeviseRepository;
import com.owomi.database.repositories.PartenaireBonusAccountRepository;
import com.owomi.rest.client.compensationService.PartenaireBonusAccountServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class PartenaireBonusAccountService {
	private static Logger logger = LogManager.getLogger(PartenaireBonusAccountService.class);
	@Autowired
	PartenaireBonusAccountServiceClient partenaireBonusAccountServiceClient;
	@Autowired
	PartenaireBonusAccountRepository partenaireBonusAccountRepository;

	@Autowired
	CvToDeviseRepository cvToDeviseRepository;

	@Autowired
	NotificationService notificationService;


	@Autowired
	Mapper mapper;

	public PartenaireBonusAccountSoldeWs solde(Partenaire partenaire) {
		return this.solde(partenaire.getId());
	}

	public CommonResponseWs soldeBonusPartenaire(Partenaire partenaire) throws OwomiException {
		return this.partenaireBonusAccountServiceClient.soldeBonusResponse(partenaire.getId());
	}

	public PartenaireBonusAccountSoldeWs solde(Integer partenaire) {
		return this.partenaireBonusAccountServiceClient.soldeBonus(partenaire);
	}

	public PageResponse<PartenaireBonusAccountWs> statementAccountBonus(
			Integer partenaire, Integer page, Integer size
	) {
		return this.partenaireBonusAccountServiceClient.statementAccountBonus(partenaire, page, size);
	}


}
