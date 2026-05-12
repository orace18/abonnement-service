package com.clubchretien.abonnement.services;

import com.owomi.commons.constants.MobileConstant;
import com.owomi.commons.constants.ResponseConstant;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.CreateRechargeInitWs;
import com.owomi.commons.ws.ecofam.EcoFamCreateRechargeInitWs;
import com.owomi.commons.ws.owopay.ClientOwoPayWs;
import com.owomi.database.entities.ClientOwoPay;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.ClientOwoPayRepository;
import com.owomi.rest.client.payerfactureService.PayerFactureEcoFamAbonneGarantServiceClient;
import com.owomi.rest.client.payerfactureService.PayerFactureEcoFamServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EcoFamAccountService implements MobileConstant, ResponseConstant {
    private static Logger logger = LogManager.getLogger(EcoFamAccountService.class);

	@Autowired
	PayerFactureEcoFamServiceClient payerFactureEcoFamServiceClient;
	@Autowired
	PayerFactureEcoFamAbonneGarantServiceClient payerFactureEcoFamAbonneGarantServiceClient;
	@Autowired
	ClientOwoPayRepository clientOwoPayRepository;

	@Autowired
	Mapper mapper;

	public CommonResponseWs listAskedGuarantees(Partenaire partenaire, Integer page, Integer size) throws OwomiException {
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 404, null);
		return payerFactureEcoFamAbonneGarantServiceClient.listAskedGuarantees(partenaire.getId(), page, size);
	}

	public CommonResponseWs listGivenGuarantees(Partenaire partenaire, Integer page, Integer size) throws OwomiException {
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 404, null);
		return payerFactureEcoFamAbonneGarantServiceClient.listGivenGuarantees(partenaire.getId(), page, size);
	}

	public CommonResponseWs listReceivedGuarantees(Partenaire partenaire, Integer page, Integer size) throws OwomiException {
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 404, null);
		return payerFactureEcoFamAbonneGarantServiceClient.listReceivedGuarantees(partenaire.getId(), page, size);
	}

	public CommonResponseWs profilFinancier(Partenaire partenaire) throws OwomiException {
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 404, null);
		return payerFactureEcoFamServiceClient.profilFinancier(partenaire.getId());
	}

	public CommonResponseWs accountEcoFamSolde(Partenaire partenaire) throws OwomiException {
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 404, null);
		return payerFactureEcoFamServiceClient.getAccountSolde(partenaire.getId());
	}

	private ClientOwoPay clientEcoFamRecharge() throws OwomiException {
		String name = "RECHARGE ECO_FAM";
		return this.clientOwoPayRepository.findOneByName(name);
	}

	public CommonResponseWs initializeAccountEcoFamRecharge(Partenaire partenaire, CreateRechargeInitWs ws) throws OwomiException {
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 404, null);
		logger.debug("initializeAccountEcoFamRecharge ws {} ", ws);
		EcoFamCreateRechargeInitWs request = mapper.map(ws, EcoFamCreateRechargeInitWs.class);
		request.setIdPartenaire(partenaire.getId());
        ClientOwoPay clientOwoPay = this.clientEcoFamRecharge();
		request.setClientOwoPay(mapper.map(clientOwoPay, ClientOwoPayWs.class));
		request.setWeb(ws.getWeb() != null && ws.getWeb());
        return payerFactureEcoFamServiceClient.initializeRecharge(request);
    }

    public CommonResponseWs listAccountEcoFamRecharge(Partenaire partenaire, Integer page, Integer size) throws OwomiException {
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 404, null);
        return payerFactureEcoFamServiceClient.listRecharge(partenaire.getId(), page, size);
    }

    public CommonResponseWs listAccountTransactions(Partenaire partenaire, Integer page, Integer size) throws OwomiException {
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 404, null);
        return payerFactureEcoFamServiceClient.transactionAccount(partenaire.getId(), page, size);
    }
}
