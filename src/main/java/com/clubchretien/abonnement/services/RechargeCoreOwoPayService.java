package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.constants.RechargeConstant;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.enums.owopay.CoreTransactionSensEnums;
import com.owomi.commons.enums.owopay.CoreTransactionStatusEnums;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.CalculUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.owopay.core.CoreOwoPayCallbackWs;
import com.owomi.commons.ws.owopay.core.CoreOwoPayTransactionCreateWs;
import com.owomi.commons.ws.owopay.core.CoreOwoPayTransactionDistributionListCreateWs;
import com.owomi.database.entities.Banques;
import com.owomi.database.entities.ClientOwoPay;
import com.owomi.database.entities.Devise;
import com.owomi.database.entities.Recharge;
import com.owomi.database.repositories.RechargeRepository;
import com.owomi.rest.client.owopay.core.CoreOwoPayTransactionServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
public class RechargeCoreOwoPayService implements RechargeConstant {

	private static Logger logger = LogManager.getLogger(RechargeCoreOwoPayService.class);


	@Autowired
	CoreOwoPayTransactionServiceClient coreOwoPayTransactionServiceClient;
	@Autowired
	RechargeRepository rechargeRepository;
	@Autowired
	RechargeCoreOwoPayClientCallbackService rechargeCoreOwoPayClientCallbackService;

	@Autowired
	RechargeService rechargeService;

	@Autowired
	WithdrawalAccountService withdrawalAccountService;

	@Autowired
	Mapper mapper;


	private CoreOwoPayTransactionCreateWs modelDistributionRecharge(
			ClientOwoPay client, Recharge recharge
	) {
		CoreOwoPayTransactionCreateWs ws = new CoreOwoPayTransactionCreateWs();
		ws.setClientId(client.getId());
		ws.setClientSecret(client.getSecret());
		ws.setReference(recharge.getReferenceCoreTransaction());
		ws.setOperation(recharge.getId().longValue());

		Banques banques = recharge.getPaymentMeans().getBanque();

		Devise devise = banques.getPays().getDevise();

		String libelle = "Recharge No." + recharge.getId() + " ";

		ws.setSourceMatricule(banques.getCorporateRecharge().getMatricule());
		ws.setDeviseCode(devise.getCode());
		ws.setAmount(recharge.getMontant());
		ws.setLibelle(libelle);

		CoreOwoPayTransactionDistributionListCreateWs bank = new CoreOwoPayTransactionDistributionListCreateWs();
		bank.setDestinationMatricule(banques.getCorporateRecharge().getMatricule());
		bank.setSens(CoreTransactionSensEnums.DEBIT.getSens());
		bank.setAmount(recharge.getMontant());
		bank.setLibelle(recharge.getReference());

		CoreOwoPayTransactionDistributionListCreateWs user = new CoreOwoPayTransactionDistributionListCreateWs();
		user.setDestinationMatricule(recharge.getCorporate().getMatricule());
		user.setSens(CoreTransactionSensEnums.CREDIT.getSens());
		user.setAmount(recharge.getMontant());
		user.setLibelle(libelle + "de " + CalculUtils.Round(recharge.getMontant(), 2) + devise.getCode());

		List<CoreOwoPayTransactionDistributionListCreateWs> distributions = Stream.of(bank, user).filter(v -> v.getAmount() > 0).collect(Collectors.toList());
		ws.setDistributions(distributions);
		return ws;
	}

	public void processCoreTransactionSuccessRecharge(Recharge recharge) {
		if (!RECHARGE_INITIAL_STATE.equals(recharge.getEtat())) return;
		recharge = this.rechargeRepository.findOneById(recharge.getId());
		boolean processCore = recharge.getIdCoreTransaction() == null && recharge.getStatusCoreTransaction() == null;
		boolean runCore = recharge.getIdCoreTransaction() != null && recharge.getStatusCoreTransaction() == null;
		ClientOwoPay client = this.rechargeCoreOwoPayClientCallbackService.clientRechargeCore();
		CoreOwoPayTransactionCreateWs model = this.modelDistributionRecharge(client, recharge);
		if (processCore) this.processCoreTransaction(recharge, model);
		else if (runCore) this.runCoreTransactionSuccessSuccessRecharge(recharge);
	}

	private CoreOwoPayTransactionCreateWs modelRunCoreTransactionSuccessRecharge(Recharge recharge) {
		CoreOwoPayTransactionCreateWs ws = new CoreOwoPayTransactionCreateWs();
		ws.setId(recharge.getIdCoreTransaction());
		ClientOwoPay client = this.rechargeCoreOwoPayClientCallbackService.clientRechargeCore();
		ws.setClientId(client.getId());
		ws.setClientSecret(client.getSecret());
		return ws;
	}

	public void runCoreTransactionSuccessSuccessRecharge(Recharge recharge) {
		if (recharge.getIdCoreTransaction() == null) return;
		CoreOwoPayTransactionCreateWs model = this.modelRunCoreTransactionSuccessRecharge(recharge);
		try {
			this.runCoreTransaction(model, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public void processCoreTransaction(Recharge recharge, CoreOwoPayTransactionCreateWs model) {
		if (!RECHARGE_INITIAL_STATE.equals(recharge.getEtat())) return;
		try {
			// create a distribution
			CommonResponseWs commonResponseWs = this.createCoreTransaction(model, false);
			if (!commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) return;
			model.setId(commonResponseWs.getId().longValue());
			recharge.setIdCoreTransaction(model.getId());
			recharge.setProcessing(false);
			recharge = this.rechargeRepository.save(recharge);
			// run a distribution
			this.runCoreTransaction(model, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void callbackClient(Recharge recharge, CoreOwoPayCallbackWs ws) {
		Boolean compensated = recharge.getCompensed() != null && recharge.getCompensed();
		logger.debug("callback transaction {} compensated {}", recharge.getId(), compensated);
		if (compensated) return;
		List<String> statuses = Stream.of(
				CoreTransactionStatusEnums.SUCCESS, CoreTransactionStatusEnums.FAIL
		).map(CoreTransactionStatusEnums::getValue).collect(Collectors.toList());
		String status = recharge.getStatusCoreTransaction();
		if (!statuses.contains(status)) return;
		Boolean state = CoreTransactionStatusEnums.SUCCESS.getValue().equalsIgnoreCase(recharge.getStatusCoreTransaction());
		recharge.setEtat(RECHARGE_ECHEC_STATE);
		if (state) recharge.setEtat(RECHARGE_VALIDE_STATE);
		recharge.setCompensed(true);
		recharge.setProcessing(false);
		this.rechargeRepository.save(recharge);
		this.rechargeService.notification(recharge, state);
		this.withdrawalAccountService.listenRechargeSilence(recharge, state);
	}

	private void notification() {

	}


	public CommonResponseWs createCoreTransaction(CoreOwoPayTransactionCreateWs ws, Boolean async) throws OwomiException {
		if (async) return this.coreOwoPayTransactionServiceClient.createAsync(ws);
		return this.coreOwoPayTransactionServiceClient.create(ws);
	}

	public CommonResponseWs runCoreTransaction(CoreOwoPayTransactionCreateWs ws, Boolean async) throws OwomiException {
		if (async) return this.coreOwoPayTransactionServiceClient.runAsync(ws);
		return this.coreOwoPayTransactionServiceClient.run(ws);
	}

}
