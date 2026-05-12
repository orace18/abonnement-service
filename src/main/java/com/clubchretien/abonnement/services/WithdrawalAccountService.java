package com.clubchretien.abonnement.services;

import com.google.gson.Gson;
import com.owomi.commons.constants.MobileConstant;
import com.owomi.commons.enums.FirebaseAppEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.*;
import com.owomi.commons.ws.owopay.PaymentRequestWs;
import com.owomi.database.entities.*;
import com.owomi.database.repositories.PartenaireCorporateRepository;
import com.owomi.database.repositories.RechargeRepository;
import com.owomi.database.repositories.WithdrawalAccountRepository;
import com.owomi.rest.client.dataservice.WithdrawalAccountServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Date;

@Component
@Slf4j
public class WithdrawalAccountService implements MobileConstant {

	public static String TYPE_RECHARGE = "VALIDATION MOYEN RETRAIT";
	@Autowired
	WithdrawalAccountServiceClient withdrawalAccountServiceClient;
	@Autowired
	PaymentGatewayService paymentGatewayService;
	@Autowired
	WithdrawalAccountRepository withdrawalAccountRepository;
	@Autowired
	PartenaireCorporateRepository partenaireCorporateRepository;
	@Autowired
	RechargeService rechargeService;
	@Autowired
	RechargeRepository rechargeRepository;
	@Autowired
	NotificationService notificationService;
	@Autowired
	Mapper mapper;

	public CommonResponseWs create(Partenaire partenaire, WithdrawalAccountCreateWs ws) throws OwomiException, DataNotFoundException, AuthentificationException {
		ws.setPartenaireId(partenaire.getId());
		CommonResponseWs commonResponseWs = this.withdrawalAccountServiceClient.create(ws);
		if (!commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) return commonResponseWs;
		Gson gson = new Gson();
		WithdrawalAccountWs accountWs = gson.fromJson(gson.toJson(commonResponseWs.getData()), WithdrawalAccountWs.class);
		WithdrawalAccount account = this.withdrawalAccountRepository.findOneById(accountWs.getId());
		CommonResponseWs initiate = this.initiateRecharge(account, account.getValidationAmount());
		commonResponseWs.setInfo(gson.toJson(initiate));
		commonResponseWs.setId(initiate.getId());
		return commonResponseWs;
	}

	public CommonResponseWs createRecharge(WithdrawalAccount entity, Corporate corporate, CreateRechargeInitWs ws, Partenaire partenaire) throws OwomiException {
		ClientOwoPay clientRecharge = this.rechargeService.clientRecharge();
		if (clientRecharge == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "CLIENT RECHARGE NOT FOUND", 404, null);
		if (corporate == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "CORPORATE NOT FOUND", 404, null);
		ws.setMatricule(corporate.getMatricule());
		CreateRechargeWs createRechargeWs = mapper.map(ws, CreateRechargeWs.class);
		createRechargeWs.setIdCorporate(corporate.getId());
		createRechargeWs.setValide(false);
		createRechargeWs.setEtat(RechargeService.RECHARGE_INITIAL_STATE);
		String ref = createRechargeWs.getType() + Math.random() * 100000 + new Timestamp(System.currentTimeMillis()).getTime();
		createRechargeWs.setReference(ref);
		createRechargeWs.setDate(new Date());
		createRechargeWs.setDateExterne(new Date());
		Recharge recharge = this.rechargeService.mapRechargeWsToRecharge(createRechargeWs, partenaire);
		recharge.setMarchandTransactionRef(entity.getId().toString());
		CommonResponseWs commonResponseWs = this.rechargeService.initPayment(partenaire, ref, createRechargeWs, clientRecharge);
		Double idPaiement = (Double) commonResponseWs.getData();
		if (idPaiement != null) recharge.setReferenceExterne(Long.toString(idPaiement.longValue()));
		this.rechargeRepository.save(recharge);
		return commonResponseWs;
	}

	private CommonResponseWs initiateRecharge(WithdrawalAccount entity, Double amount) throws OwomiException, DataNotFoundException, AuthentificationException {
		boolean validated = entity.getValidated() != null && entity.getValidated();
		if (validated) return new CommonResponseWs(WsOperationResponseStatus.failed, "COMPTE DEJA VALIDER", 202, null);
		if (amount == null) amount = entity.getValidationAmount();
		CreateRechargeInitWs recharge = new CreateRechargeInitWs();
		recharge.setMontant(amount);
		recharge.setDevise(entity.getPartenaire().getPays().getDevise().getCode());
		recharge.setType(TYPE_RECHARGE);
		PartenaireCorporate pc = this.partenaireCorporateRepository.findOneByCurrentAndPk_Partenaire(
				true, entity.getPartenaire()
		);
		if (pc == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "no partenaire corporate", 400, null);
		Long idPaiement = null;
		Gson gson = new Gson();
		try {
			CommonResponseWs commonResponseWs = this.createRecharge(
					entity, pc.getCorporate(), recharge, entity.getPartenaire()
			);
			if (commonResponseWs.getData() == null) return commonResponseWs;
			idPaiement = gson.fromJson(gson.toJson(commonResponseWs.getData()), Long.class);
		} catch (Exception e) {
			e.printStackTrace();
			log.debug("error response " + e.getMessage());
		}
		if (idPaiement == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "No paiement", 400, null);
		CommonResponseWs commonResponseWs = new CommonResponseWs(WsOperationResponseStatus.failed, "error in process paiement", 400, null);
		try {
			PaymentRequestWs paymentRequestWs = new PaymentRequestWs();
			paymentRequestWs.setIdPaiement(idPaiement);
			paymentRequestWs.setIdPaymentMean(entity.getWithdrawalMeans().getInverse().getId());
			paymentRequestWs.setInfoPayment(entity.getRetraitInfo());
			commonResponseWs = this.paymentGatewayService.paiement(new RequestContextWs(), paymentRequestWs);
			log.debug("paiement response " + commonResponseWs);
		} catch (Exception e) {
			e.printStackTrace();
			log.debug("error response " + e.getMessage());
		}
		commonResponseWs.setId(idPaiement.intValue());
		return commonResponseWs;
	}

	public void listenRecharge(Recharge recharge, Boolean state) {
		if (!recharge.getType().contains(TYPE_RECHARGE)) return;
		if (recharge.getMarchandTransactionRef() == null) return;
		Long accountId = Long.parseLong(recharge.getMarchandTransactionRef());
		WithdrawalAccount account = this.withdrawalAccountRepository.findOneById(accountId);
		boolean validated = account.getValidated() != null && account.getValidated();
		if (validated) return;
		account.setValidated(state);
		this.withdrawalAccountRepository.save(account);
		if (!state) return;
		try {
			String title = "Validation de moyen de retrait OWOMI";
			String message = "Votre moyen de retrait " + account.getHolderName()
					+ " a été validé. Vous êtes maintenant capable de faire des " +
					"retraits avec ce moyen de retrait. Soyez OWOMI !";
			this.notificationService.notificationAndAl(FirebaseAppEnums.MOBILE, recharge.getPartenaire(), title, message);
			FirebaseAppEnums app = this.notificationService.app(recharge.getCorporate());
			if (!FirebaseAppEnums.MOBILE.equals(app))
				this.notificationService.notificationAndAl(recharge.getCorporate(), title, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void listenRechargeSilence(Recharge recharge, Boolean state) {
		try {
			this.listenRecharge(recharge, state);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public CommonResponseWs update(Partenaire partenaire, WithdrawalAccountCreateWs ws) throws OwomiException, DataNotFoundException, AuthentificationException {
		ws.setPartenaireId(partenaire.getId());
		CommonResponseWs commonResponseWs = this.withdrawalAccountServiceClient.update(ws);
		if (!commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) return commonResponseWs;
		Gson gson = new Gson();
		WithdrawalAccountWs accountWs = gson.fromJson(gson.toJson(commonResponseWs.getData()), WithdrawalAccountWs.class);
		WithdrawalAccount account = this.withdrawalAccountRepository.findOneById(accountWs.getId());
		CommonResponseWs initiate = this.initiateRecharge(account, account.getValidationAmount());
		commonResponseWs.setInfo(gson.toJson(initiate));
		commonResponseWs.setId(initiate.getId());
		return commonResponseWs;
	}

	public CommonResponseWs delete(Long id) throws OwomiException {
		return this.withdrawalAccountServiceClient.delete(id);
	}

	public CommonResponseWs list(Partenaire partenaire, Integer page, Integer size) throws OwomiException {
		return this.withdrawalAccountServiceClient.list(partenaire.getId(), page, size);
	}


}
