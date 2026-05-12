package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.constants.RechargeConstant;
import com.google.gson.Gson;
import com.owomi.commons.constants.MobileConstant;
import com.owomi.commons.constants.ResponseConstant;
import com.owomi.commons.enums.CommandPaymentStatusEnums;
import com.owomi.commons.enums.FirebaseAppEnums;
import com.owomi.commons.enums.PaymentTypeEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.NotificationException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.StringUtils;
import com.owomi.commons.ws.*;
import com.owomi.database.entities.*;
import com.owomi.database.repositories.*;
import com.owomi.rest.client.accountService.AccountServiceClient;
import com.owomi.rest.client.owopay.OwoPayClient;
import com.owomi.velocity.clients.VelocityClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class RechargeService implements MobileConstant, ResponseConstant, RechargeConstant {
	private static Logger logger = LogManager.getLogger(RechargeService.class);

	@Autowired
	RechargeCoreOwoPayService rechargeCoreOwoPayService;
	@Autowired
	RechargeRepository rechargeRepository;
	@Autowired
	CorporateRepository corporateRepository;
	@Autowired
	PaymentMeansRepository paymentMeansRepository;
	@Autowired
	PartenaireRepository partenaireRepository;
	@Autowired
	AccountServiceClient accountServiceClient;
	@Autowired
	OwoPayClient owoPayClient;
	@Autowired
	ClientOwoPayRepository clientOwoPayRepository;
	@Autowired
	PaymentGatewayService paymentGatewayService;

	@Autowired
	NotificationService notificationService;

	@Autowired
	VelocityClient velocityClient;

	@Autowired
	PaymentRepository paymentRepository;

	@Autowired
	WithdrawalAccountService withdrawalAccountService;

	@Autowired
	Mapper mapper;


	public ClientOwoPay clientRecharge() {
		String name = "RECHARGE"; // 13L
		return this.clientOwoPayRepository.findOneByName(name);
	}

	public CommonResponseWs listRecharge(
			RequestContextWs requestContextWs, Partenaire partenaire,
			Integer page, Integer size
	) {
		Pageable pageable = PageRequest.of(page, size);
		if (partenaire == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire NOT FOUND", 404, null);
		Page<Recharge> entities = this.rechargeRepository.findByPartenaireOrderByDateDesc(partenaire, pageable);
		List<RechargeWs> content = entities.getContent().stream()
				.map(this::convertRechargeToWs)
				.collect(Collectors.toList());
		Page<RechargeWs> result = new PageImpl<>(content, pageable, entities.getTotalElements());
		return new CommonResponseWs(WsOperationResponseStatus.success, "recharges", 200, result);
	}

	public CommonResponseWs listRecharge(
			RequestContextWs requestContextWs, Partenaire partenaire, String matricule,
			Integer page, Integer size
	) {
		Pageable pageable = PageRequest.of(page, size);
		Corporate corporate = this.corporateRepository.findOneByMatricule(matricule);
		if (corporate == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "corporate NOT FOUND", 404, null);
		Page<Recharge> entities = this.rechargeRepository.findByPartenaireAndCorporateOrderByDateDesc(partenaire, corporate, pageable);
		List<RechargeWs> content = entities.getContent().stream()
				.map(this::convertRechargeToWs).collect(Collectors.toList());
		Page<RechargeWs> result = new PageImpl<>(content, pageable, entities.getTotalElements());
		return new CommonResponseWs(WsOperationResponseStatus.success, "recharges", 200, result);
	}

	public Double moyenneRecharge(Partenaire partenaire, String matricule, Integer mois) {
		if (partenaire == null) return 0.0;
		if (matricule == null) matricule = partenaire.getTelephone();
		Corporate corporate = this.corporateRepository.findOneByMatricule(matricule);
		if (corporate == null) return 0.0;
		Instant end = Instant.now();
		Instant start = end.minus(mois * 30, ChronoUnit.DAYS);
		List<Recharge> recharges = this.rechargeRepository.findByPartenaireAndCorporateAndTypeAndEtatAndDateBetween(
				partenaire, corporate, PaymentTypeEnums.RECHARGE.getValue(), RECHARGE_VALIDE_STATE, Date.from(start), Date.from(end)
		);
		double sum = recharges.stream().map(Recharge::getMontant).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();
		return sum / mois;
	}

	public CommonResponseWs updateRecharges(Integer idPartenaire) {
		Partenaire partenaire = this.partenaireRepository.findOneById(idPartenaire);
		this.updateRecharges(partenaire);
		return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
	}


	public void updateRecharges(Partenaire partenaire) {
		logger.debug("updateRecharges partenaire " + partenaire.getId());
		List<Recharge> all = this.rechargeRepository.findByPartenaireAndEtatOrderByDateDesc(
				partenaire, RECHARGE_INITIAL_STATE
		);
		logger.debug("all partenaire " + all.size());
		Instant now = Instant.now();
		List<Recharge> wait = all.stream().filter(v -> v.getDateExterne() != null)
				.filter(v -> v.getDateExterne().before(Date.from(now.minus(5, ChronoUnit.MINUTES))))
				.collect(Collectors.toList());
		List<Recharge> waitDays = all.stream()
				.filter(v -> v.getDateExterne() == null).filter(v -> v.getDate() != null)
				.filter(v -> v.getDate().before(Date.from(now.minus(1, ChronoUnit.DAYS))))
				.collect(Collectors.toList());
		wait.addAll(waitDays);
		logger.debug("wait partenaire " + wait.size());
		for (Recharge recharge : wait) {
			if (recharge.getReferenceExterne() == null) {
				this.failed(recharge);
				continue;
			}
			try {
				Long id = Long.parseLong(recharge.getReferenceExterne());
				this.certifiedRechargePaiement(id);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private Recharge cancelRecharge(Recharge recharge) {
		recharge.setEtat(RECHARGE_ECHEC_STATE);
		recharge.setValide(true);
		recharge.setCompensed(true);
		recharge = this.rechargeRepository.save(recharge);
		return recharge;
	}

	private Recharge failed(Long paymentId) {
		Recharge recharge = rechargeRepository.findOneByReferenceExterne(paymentId.toString());
		return cancelRecharge(recharge);
	}

	private Recharge failed(Recharge recharge) {
		if (recharge == null) return null;
		recharge.setEtat(RECHARGE_ECHEC_STATE);
		recharge.setValide(true);
		recharge.setCompensed(true);
		recharge = this.rechargeRepository.save(recharge);
		return recharge;
	}


	private RechargeWs convertRechargeToWs(Recharge recharge) {
		RechargeWs ws = mapper.map(recharge, RechargeWs.class);
		if (recharge.getCorporate() != null) ws.getCorporate().setName(recharge.getCorporate().getLibelle());
		if (recharge.getPartenaire() != null) {
			Devise devise = recharge.getPartenaire().getPays().getDevise();
			ws.getCorporate().setDevise(mapper.map(devise, DeviseWs.class));
		}
		return ws;
	}


	public CommonResponseWs createRecharge(RequestContextWs requestContextWs, CreateRechargeInitWs ws, Partenaire partenaire) throws OwomiException {
		ClientOwoPay clientRecharge = this.clientRecharge();
		if (clientRecharge == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "CLIENT RECHARGE NOT FOUND", 404, null);
		Corporate corporate = corporateRepository.findOneByMatricule(ws.getMatricule());
		if (corporate == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "CORPORATE NOT FOUND", 404, null);
		boolean provisoire = corporate.getProvisoire() != null && corporate.getProvisoire();
		if (provisoire)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "corporate provisoire", 496, null);
		CreateRechargeWs createRechargeWs = mapper.map(ws, CreateRechargeWs.class);
		createRechargeWs.setIdCorporate(corporate.getId());
		createRechargeWs.setValide(false);
		createRechargeWs.setEtat(RECHARGE_INITIAL_STATE);
		String ref = createRechargeWs.getType() + Math.random() * 100000 + new Timestamp(System.currentTimeMillis()).getTime();
		createRechargeWs.setReference(ref);
		createRechargeWs.setDate(new Date());
		createRechargeWs.setDateExterne(new Date());
		Recharge recharge = mapRechargeWsToRecharge(createRechargeWs, partenaire);
		CommonResponseWs commonResponseWs = initPayment(partenaire, ref, createRechargeWs, clientRecharge);
		Double idPaiement = (Double) commonResponseWs.getData();
		if (idPaiement != null) recharge.setReferenceExterne(Long.toString(idPaiement.longValue()));
		rechargeRepository.save(recharge);
		return commonResponseWs;
	}

	public CommonResponseWs initPayment(Partenaire partenaire, String ref, CreateRechargeWs createRechargeWs, ClientOwoPay clientRecharge) throws OwomiException {
		DemandePaiementWs demandePaiementWs = new DemandePaiementWs();
		demandePaiementWs.setType(PaymentTypeEnums.RECHARGE.getValue());
		demandePaiementWs.setMessage(createRechargeWs.getType());
		demandePaiementWs.setIdPartenaire(partenaire.getId());
		demandePaiementWs.setRefClient(ref);
		demandePaiementWs.setMontant(createRechargeWs.getMontant());
		demandePaiementWs.setDevise(createRechargeWs.getDevise());
		demandePaiementWs.setClientId(clientRecharge.getId());
		demandePaiementWs.setClientSecret(clientRecharge.getSecret());
		//demandePaiementWs.setCallbackUrl(clientRecharge.getCallbackUrl());
		DemandePaiementMetaDataWs metaData = new DemandePaiementMetaDataWs();
		String metaDataStr = new Gson().toJson(metaData);
		demandePaiementWs.setMetadata(metaDataStr);
		// return owoPayClient.initPaiement(demandePaiementWs);
		return this.paymentGatewayService.initializePaiement(demandePaiementWs);
	}

	private CommonResponseWs saveGrandLivre(int numeroCompte, Double montant) throws OwomiException {
		return saveGrandLivre(numeroCompte, montant, "RECHARGE");
	}

	public CommonResponseWs saveGrandLivre(int numeroCompte, Double montant, String libelle) throws OwomiException {
		ResumeGrandLivreWs resumeGrandLivreWs = new ResumeGrandLivreWs();
		resumeGrandLivreWs.setEcriture(Integer.parseInt(StringUtils.generateCodeVerification()));
		resumeGrandLivreWs.setLibelle(libelle);
		resumeGrandLivreWs.setMontant(montant);
		resumeGrandLivreWs.setNumeroCompte(numeroCompte);
		resumeGrandLivreWs.setSens("1");
		return accountServiceClient.saveGrandLivre(resumeGrandLivreWs);
	}

	public Recharge mapRechargeWsToRecharge(CreateRechargeWs createRechargeWs, Partenaire partenaire) {
		Recharge recharge = new Recharge();
		recharge.setReferenceCoreTransaction(StringUtils.randomUUID(true));
		Corporate corporate = corporateRepository.findOneById(createRechargeWs.getIdCorporate());
		recharge.setCorporate(corporate);
		recharge.setDate(createRechargeWs.getDate());
		recharge.setTelephone(createRechargeWs.getNumMobileMoney());
		recharge.setFichier(createRechargeWs.getFichier());
		recharge.setIdBanque(createRechargeWs.getIdBanque());
		recharge.setMontant(createRechargeWs.getMontant());
		recharge.setPartenaire(partenaire);
		recharge.setReference(createRechargeWs.getReference());
		recharge.setType(createRechargeWs.getType());
		recharge.setValide(createRechargeWs.getValide());
		recharge.setEtat(createRechargeWs.getEtat());
		recharge.setEmetteur(partenaire.getNom() + "" + partenaire.getPrenoms());
		recharge.setBanqueEmettrice(createRechargeWs.getBanqueEmettrice());
		recharge.setReferenceExterne(createRechargeWs.getReferenceExterne());
		recharge.setDateExterne(createRechargeWs.getDateExterne());
		recharge.setCompensed(false);
		recharge.setProcessing(false);
		return recharge;
	}

	public void listenPaiement(OwoPayPaymentWs paymentWs) throws OwomiException, NotificationException {
		logger.debug("listenPaiement " + paymentWs);
		Boolean state = CommandPaymentStatusEnums.SUCCESS.getValue().equals(paymentWs.getStatut());
		logger.debug("state " + state);
		Recharge recharge = rechargeRepository.findOneByReferenceExterne(paymentWs.getId().toString());
		if (recharge == null) return;
		if (recharge.getProcessing() != null && recharge.getProcessing()) return;
		recharge.setProcessing(true);
		recharge = rechargeRepository.save(recharge);
		if (recharge.isValide()) return;
		if (!RECHARGE_INITIAL_STATE.equals(recharge.getEtat())) return;
		logger.debug("recharge " + recharge);
		PaymentMeans paymentMeans = null;
		if (paymentWs.getPaymentMeans() != null && paymentWs.getPaymentMeans().getId() != null)
			paymentMeans = paymentMeansRepository.findOneById(paymentWs.getPaymentMeans().getId());
		logger.debug("paymentMeans " + paymentMeans);
		this.listenRechargePaiement(recharge, state, paymentMeans);
	}

	private void listenRechargePaiement(Boolean state, Recharge recharge, PaymentMeans paymentMeans) throws OwomiException, NotificationException {
		if (recharge == null) return;
		recharge = rechargeRepository.findOneById(recharge.getId());
		if (recharge.isValide()) return;
		if (!RECHARGE_INITIAL_STATE.equals(recharge.getEtat())) return;
		recharge.setPaymentMeans(paymentMeans);
		recharge.setValide(true);
		recharge.setEtat(RECHARGE_ECHEC_STATE);
		recharge = rechargeRepository.save(recharge);
		boolean compensated = recharge.getCompensed() != null && recharge.getCompensed();
		boolean processing = recharge.getProcessing() != null && recharge.getProcessing();
		if (state && !compensated && processing) {
			saveGrandLivre(recharge.getCorporate().getNumeroCompte().getId(), recharge.getMontant());
			recharge.setCompensed(true);
			recharge.setProcessing(false);
			recharge = rechargeRepository.save(recharge);
		}
		if (state) recharge.setEtat(RECHARGE_VALIDE_STATE);
		recharge = rechargeRepository.save(recharge);
		String compte = recharge.getCorporate().getLibelle() + " " + recharge.getCorporate().getMatricule();
		String devise = recharge.getPartenaire().getPays().getDevise().getCode();
		String bank = "";
		if (recharge.getPaymentMeans() != null) bank = recharge.getPaymentMeans().getNom();
		String title = "Recharge de compte OWOMI";
		String message = "Votre recharge de " + recharge.getMontant() + " " + devise + " sur le compte " +
				compte + " a été rejetée. Veuillez vérifier votre compte chez votre banque " + bank + " avant de réssayer.";
		if (state) {
			message = "Votre compte " + compte + " a été crédité de " + recharge.getMontant() + " " + devise + " avec succès";
		}
		this.notificationService.notificationAndAl(FirebaseAppEnums.MOBILE, recharge.getPartenaire(), title, message);
		try {
			this.withdrawalAccountService.listenRecharge(recharge, state);
		} catch (Exception ignored) {
		}
	}

	private void listenRechargePaiement(Recharge recharge, Boolean state, PaymentMeans paymentMeans) throws OwomiException, NotificationException {
		if (recharge == null) return;
		recharge = rechargeRepository.findOneById(recharge.getId());
		if (recharge.isValide()) return;
		if (!RECHARGE_INITIAL_STATE.equals(recharge.getEtat())) return;
		recharge.setPaymentMeans(paymentMeans);
		recharge.setValide(true);
		recharge = rechargeRepository.save(recharge);
		boolean compensated = recharge.getCompensed() != null && recharge.getCompensed();
		boolean processing = recharge.getProcessing() != null && recharge.getProcessing();
		boolean valid = state && !compensated && processing;
		if (!valid) return;
		recharge = rechargeRepository.save(recharge);
		this.rechargeCoreOwoPayService.processCoreTransactionSuccessRecharge(recharge);
	}

	public void notification(Recharge recharge, Boolean state) {
		if (!RECHARGE_VALIDE_STATE.equals(recharge.getEtat())) return;
		try {
			String compte = recharge.getCorporate().getLibelle() + " " + recharge.getCorporate().getMatricule();
			String devise = recharge.getPartenaire().getPays().getDevise().getCode();
			String bank = "";
			if (recharge.getPaymentMeans() != null) bank = recharge.getPaymentMeans().getNom();
			String title = "Recharge de compte OWOMI";
			String message = "Votre recharge de " + recharge.getMontant() + " " + devise + " sur le compte " +
					compte + " a été rejetée. Veuillez vérifier votre compte chez votre banque " + bank + " avant de réssayer.";
			if (state) {
				message = "Votre compte " + compte + " a été crédité de " + recharge.getMontant() + " " + devise + " avec succès";
			}
			this.notificationService.notificationAndAl(FirebaseAppEnums.MOBILE, recharge.getPartenaire(), title, message);
			FirebaseAppEnums app = this.notificationService.app(recharge.getCorporate());
			if (!FirebaseAppEnums.MOBILE.equals(app))
				this.notificationService.notificationAndAl(recharge.getCorporate(), title, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public CommonResponseWs certifiedRechargePaiement(Long idPaiement) throws OwomiException, NotificationException {
		logger.debug("certifiedRechargePaiement Commande id = " + idPaiement);
		CommonResponseWs commonResponseWs = new CommonResponseWs();
		if (idPaiement == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "Paiement not initialised", 404, null);
		logger.debug("certifiedRechargePaiement check status paiement = " + idPaiement);
		Payment payment = this.paymentRepository.findOneById(idPaiement);
		if (payment == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "payment not found", 404, null);
		commonResponseWs = paymentGatewayService.statusPaiement(new RequestContextWs(), idPaiement);
		if (!WsOperationResponseStatus.success.equals(commonResponseWs.getStatus())) return commonResponseWs;
		logger.debug("certifiedCommandePaiement check status paiement = " + commonResponseWs);
		String status = (String) commonResponseWs.getData();
		if (CommandPaymentStatusEnums.PENDING.getValue().equals(status)) return commonResponseWs;
		boolean callbacked = CommandPaymentStatusEnums.SUCCESS.getValue().equals(status)
				&& !payment.getStatut().equalsIgnoreCase(status);
		payment = this.paymentRepository.findOneById(idPaiement);
		OwoPayPaymentWs ws = mapper.map(payment, OwoPayPaymentWs.class);
		boolean success = CommandPaymentStatusEnums.SUCCESS.getValue().equals(ws.getStatut());
		boolean fail = CommandPaymentStatusEnums.FAIL.getValue().equals(ws.getStatut());
		if (!callbacked && success) this.listenPaiement(ws);
		if (fail) this.listenPaiement(ws);
		if ("INITIAL".equalsIgnoreCase(status)) {
			this.failed(ws.getId());
			commonResponseWs.setData(CommandPaymentStatusEnums.FAIL.getValue());
		}
		return commonResponseWs;
	}


}
