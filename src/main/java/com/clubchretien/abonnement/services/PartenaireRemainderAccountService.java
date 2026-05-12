package com.clubchretien.abonnement.services;

import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRemainderAccountRepository;
import com.owomi.rest.client.compensationService.PartenaireRemainderAccountServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class PartenaireRemainderAccountService {
	private static Logger logger = LogManager.getLogger(PartenaireRemainderAccountService.class);
	@Autowired
	PartenaireRemainderAccountServiceClient partenaireRemainderAccountServiceClient;
	@Autowired
	PartenaireRemainderAccountRepository partenaireRemainderAccountRepository;

	@Autowired
	Mapper mapper;

	public CommonResponseWs listRemainder(
			Partenaire partenaire, String search, Boolean used, Integer page, Integer size
	) throws OwomiException {
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 401, null);
		return this.partenaireRemainderAccountServiceClient.listMerchantRemainderStatementAccount(partenaire.getId(), search, used, page, size);
	}

	public CommonResponseWs statementAccountRemainder(
			Partenaire partenaire, Integer idBoutique, Integer page, Integer size
	) throws OwomiException {
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 401, null);
		return this.partenaireRemainderAccountServiceClient.statementAccountRemainder(idBoutique, partenaire.getId(), page, size);
	}

	public CommonResponseWs soldeRemainder(Partenaire partenaire, Integer idBoutique) throws OwomiException {
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 401, null);
		return this.partenaireRemainderAccountServiceClient.soldeRemainderResponse(idBoutique, partenaire.getId());
	}

}
