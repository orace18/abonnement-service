
package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.services.v4.BillingAddressService;
import com.google.gson.Gson;
import com.owomi.commons.constants.MobileConstant;
import com.owomi.commons.enums.*;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.*;
import com.owomi.commons.ws.billing.response.RiderStatusWs;
import com.owomi.database.entities.*;
import com.owomi.database.repositories.*;
import com.owomi.rest.client.accountService.AccountServiceClient;
import com.owomi.rest.client.billingv4.BillingRiderStatusClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@Component
public class RiderService implements MobileConstant {
	private static Logger logger = LogManager.getLogger(RiderService.class);
	@Autowired
	RiderRepository riderRepository;
	@Autowired
	CorporateRepository corporateRepository;
	@Autowired
	PartenaireCorporateRepository partenaireCorporateRepository;
	@Autowired
	NotificationService notificationService;
	@Autowired
	BillingAddressService billingAddressService;
	@Autowired
	BillingRiderStatusClient billingRiderStatusClient;

	@Autowired
	Mapper mapper;

	@Autowired
	AccountServiceClient accountServiceClient;
	@Autowired
	AchatsCDPRepository achatsCDPRepository;

	@Autowired
	RechargeService rechargeService;

	@Autowired
	RiderCommandeRepository riderCommandeRepository;

	@Autowired
	RedirectLinkService redirectLinkService;

	@Autowired
	CorporateService corporateService;

	public CommonResponseWs listAffiliation(
			RequestContextWs requestContextWs, String type, Integer page, Integer size,
			Partenaire partenaire
	) {
		CommonResponseWs commonResponseWs = new CommonResponseWs();
		Pageable pageable = PageRequest.of(page, size);
		Page<Rider> entities = new PageImpl<>(new ArrayList<>(0), pageable, 0);
		if (type == null) {
			entities = riderRepository.findAllByAffiliationAndStatusNotInAndDeletedOrderByIdDesc(
					partenaire.getTelephone(), RiderStatusEnums.strict_unvalidated(), false, pageable
			);
		}
		if (type != null && !BoutiqueMarchandGradeEnums.PRO.getValue().equalsIgnoreCase(type)) {
			entities = riderRepository.findAllByAffiliationAndAffiliationTypeAndStatusNotInAndDeletedOrderByIdDesc(
					partenaire.getTelephone(), type, RiderStatusEnums.strict_unvalidated(), false, pageable
			);
		}
		if (type != null && BoutiqueMarchandGradeEnums.PRO.getValue().equalsIgnoreCase(type)) {
			entities = riderRepository.findAllByAffiliationAndGradeInAndDeletedOrderByIdDesc(
					partenaire.getTelephone(), Collections.singletonList(type), false, pageable
			);
		}
		List<RiderWs> content = entities.getContent().stream().map(this::convertRiderToRiderWs).collect(Collectors.toList());
		Page<RiderWs> result = new PageImpl<>(content, pageable, entities.getTotalElements());
		return new CommonResponseWs(WsOperationResponseStatus.success, "List Affiliation", 200, result);
	}

	public CommonResponseWs statisticsAffiliation(Partenaire partenaire) {
		if (partenaire == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire null", 400, null);
		List<RiderWs> riders = this.riderRepository.findAllByAffiliationAndStatusNotInAndDeletedOrderByIdDesc(
				partenaire.getTelephone(), RiderStatusEnums.strict_unvalidated(), false
		).stream().map(v -> this.convertRiderToNetworkRider(v, partenaire, true)).collect(Collectors.toList());
		RiderNetworkStatisticsWs result = new RiderNetworkStatisticsWs();
		result.setRiders((long) riders.size());
		Long operational = riders.stream().filter(RiderWs::getOperational).count();
		result.setOperational(operational);
		result.setUnOperational(result.getRiders() - result.getOperational());
		Long cashProblem = riders.stream().filter(v -> v.getStatePaidCash() < 2).count();
		result.setCashProblem(cashProblem);
		return new CommonResponseWs(WsOperationResponseStatus.success, "success", 200, result);
	}

	private RiderWs convertRiderToNetworkRider(Rider rider, Partenaire partenaire, boolean stats) {
		RiderWs result = new RiderWs();
		if (!stats) {
			result = this.convertRiderToRiderWs(rider);
			Date date = Optional.ofNullable(rider.getIdAchatsCDP())
					.map(v -> this.achatsCDPRepository.findOneById(v))
					.map(AchatsCDP::getAcquisitionDate).orElse(null);
			result.setDateAchatsCDP(date);
			result.setDateLastCommande(this.lastCommandeDate(rider));
		}
		result.setStatePaidCash(this.statePaidCash(rider));
		result.setOperational(this.operational(rider));
		result.setPromotionInProgress(false);
		return result;
	}

	private Double solde(Rider rider) {
		Double solde = 0.0;
		Gson gson = new Gson();
		try {
			Corporate corporate = this.corporateRepository.findOneById(rider.getIdCorporate());
			CommonResponseWs commonResponseWs = this.accountServiceClient.getSolde(corporate.getNumeroCompte().getId());
			solde = gson.fromJson(gson.toJson(commonResponseWs.getData()), Double.class);
			solde = solde == null ? 0.0 : solde;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return solde;
	}

	private Date lastCommandeDate(Rider rider) {
		return new Date();
	}

	private Integer statePaidCash(Rider rider) {
		Double solde = this.solde(rider);
		int state = 0;
		if (solde > 0) state = 1;
		if (solde > 1000) state = 2;
		return state;
	}

	private Boolean operational(Rider rider) {
		return true;
	}

	public CommonResponseWs listValidationAffiliation(
			RequestContextWs requestContextWs, Boolean validated,
			Integer page, Integer size, Partenaire partenaire
	) {
		Pageable pageable = PageRequest.of(page, size);
		Page<Rider> entities = new PageImpl<>(new ArrayList<>(0), pageable, 0);
		if (validated) {
			entities = riderRepository.findAllByAffiliationAndStatusInAndDeletedOrderByIdDesc(
					partenaire.getTelephone(), RiderStatusEnums.validated(), false, pageable
			);
		}
		if (!validated) {
			entities = riderRepository.findAllByAffiliationAndStatusInAndDeletedOrderByIdDesc(
					partenaire.getTelephone(), RiderStatusEnums.unvalidated(), false, pageable
			);
		}
		List<RiderWs> content = entities.getContent().stream().map(this::convertRiderToRiderWs).collect(Collectors.toList());
		Page<RiderWs> result = new PageImpl<>(content, pageable, entities.getTotalElements());
		return new CommonResponseWs(WsOperationResponseStatus.success, "List Affiliation", 200, result);
	}

	public CommonResponseWs validationAffiliation(
			RequestContextWs requestContextWs, Boolean validated,
			Integer idRider, Partenaire partenaire
	) {
		Rider rider = riderRepository.findOneByIdAndDeleted(idRider, false);
		if (rider == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "Il n'existe pas de rider avec cet id", 404, null);
		boolean isValidated = validated != null && validated;
		String status = RiderStatusEnums.AFFILIATION_VALIDATED.getValue();
		if (!isValidated || !validateAffiliationWithRider(rider)) status = RiderStatusEnums.AFFILIATION_REJECTED.getValue();
		rider.setStatus(status);
		rider = this.riderRepository.save(rider);
		if (isValidated) this.networkIntegration(rider);
		if (rider.getIdCautionReservation() == null) this.operationalizationRider(rider, isValidated);
		this.notificationValidationRider(rider);
		CommonResponseWs commonResponseWs = new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
		commonResponseWs.setId(rider.getId());
		return commonResponseWs;
	}

	private Boolean validateAffiliationWithRider(Rider rider) {
		Corporate corporate = this.corporateRepository.findOneById(rider.getIdCorporate());
		Corporate affiliate = this.corporateRepository.findOneByMatricule(rider.getAffiliation());
		PartenaireCorporate pcRider = this.partenaireCorporateRepository.findOneByPk_Corporate(corporate);
		PartenaireCorporate pcAffiliate = this.partenaireCorporateRepository.findOneByPk_Corporate(affiliate);
		if(pcRider == null || pcRider.getPartenaire() == null)return  false;
		if (pcAffiliate == null || pcAffiliate.getPartenaire() == null) return true;
		if (!pcRider.getPartenaire().equals(pcAffiliate.getPartenaire().getId())) return true;
		return false;
	}

	private void notificationValidationRider(Rider rider) {
		try {
			String title = "Validation Affiliation";
			String message = "Votre demande d'affiliation a été traité. Status : " + rider.getStatus();
			Corporate riderCorporate = this.corporateRepository.findOneById(rider.getIdCorporate());
			PartenaireCorporate pc = this.partenaireCorporateRepository.findOneByPk_Corporate(riderCorporate);
			this.notificationService.notificationAndAl(FirebaseAppEnums.RYDER, pc.getPartenaire(), title, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public RiderWs convertRiderToRiderWs(Rider rider) {
		RiderWs result = mapper.map(rider, RiderWs.class);
		result.setLocation_id(rider.getLocation_id());
		if (rider.getLocation_id() != null) {
			try {
				result.setAdresse(this.billingAddressService.address(rider.getLocation_id(), null));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			CommonResponseWs commonResponseWs = this.billingRiderStatusClient.findByRider(rider.getId());
			logger.debug("status commons " + commonResponseWs);
			Gson gson = new Gson();
			RiderStatusWs status = gson.fromJson(gson.toJson(commonResponseWs.getData()), RiderStatusWs.class);
			logger.debug("status result " + status);
			if (status != null) {
				//result.setLocalization(status.getAdresse());
				result.setOnline(status.getOnline() != null && status.getOnline());
				result.setOnlineDuration(status.getOnlineDuration());
			}
			result.setLocation(status);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (result.getOnline() == null || !result.getOnline()) result.setOnlineDuration(null);
		result.setOccupied(this.occupied(rider));
		return result;
	}

	private Boolean occupied(Rider rider) {
		List<RiderCommande> pending = this.riderCommandeRepository.findAllByRiderIdAndStatusIn(
				rider.getId(), RiderCommandeStatusEnum.pending()
		);
		return !pending.isEmpty();
	}

	private void networkIntegration(Rider rider) {
		try {
			if (rider == null) return;
			Corporate corporate = this.corporateRepository.findOneById(rider.getIdCorporate());
			PartenaireCorporate pc = this.partenaireCorporateRepository.findOneByPk_Corporate(corporate);
			Partenaire partenaire = pc.getPartenaire();
			if (partenaire != null && partenaire.getParent() != null) return;
			corporate = this.corporateRepository.findOneByMatricule(rider.getAffiliation());
			pc = this.partenaireCorporateRepository.findOneByPk_Corporate(corporate);
			Partenaire author = pc.getPartenaire();
			RedirectLink link = this.redirectLinkService.redirectLink(author, Date.from(Instant.now()));
			if (link != null) this.redirectLinkService.useLink(link, partenaire);
			this.corporateService.getCorporate(new RequestContextWs(), partenaire, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public CommonResponseWs operationalizationRider(Rider rider, Boolean validated) {
		try {
			if (rider == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 404, null);
			if (!validated) return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
			if (BoutiqueMarchandGradeEnums.BASIC.getValue().equalsIgnoreCase(rider.getGrade()))
				return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
			AchatsCDP achatsCDP = this.achatsCDPRepository.findOneById(rider.getIdAchatsCDP());
			if (achatsCDP == null) return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
			Double amount = 10000.0;
			Corporate corporate = this.corporateRepository.findOneById(rider.getIdCorporate());
			if (corporate == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 404, null);
			if (rider.getIdCautionReservation() == null) {
				this.cautionRiderReservation(rider, amount, true);
				this.rechargeService.saveGrandLivre(corporate.getNumeroCompte().getId(), amount, "CAUTION LIVREUR");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
	}

	public CommonResponseWs cautionRiderReservation(Rider rider, Double amount, Boolean permanent) throws OwomiException {
		if (rider == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 404, null);
		Corporate corporate = this.corporateRepository.findOneById(rider.getIdCorporate());
		if (corporate == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 404, null);
		ReservationWs ws = new ReservationWs();
		ws.setId(rider.getIdCautionReservation());
		ws.setLeve(false);
		ws.setPermanent(permanent);
		ws.setMontant(amount);
		ws.setClient("OWOMI CAUTION LIVREUR ");
		ws.setMemo("Reservation caution livreur");
		ws.setIdOperation(rider.getId().longValue());
		ws.setNumeroCompte(corporate.getNumeroCompte().getId());
		CommonResponseWs commonResponseWs;
		if (ws.getId() == null) commonResponseWs = accountServiceClient.createReservation(ws);
		else {
			commonResponseWs = accountServiceClient.getReservation(rider.getIdCautionReservation());
			logger.debug("reservation response " + commonResponseWs);
			if (commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) {
				Gson gson = new Gson();
				ReservationWs reservation = gson.fromJson(gson.toJson(commonResponseWs.getData()), ReservationWs.class);
				ws.setLeve(reservation.getLeve());
				ws.setPermanent(permanent);
				if (!reservation.getLeve()) commonResponseWs = accountServiceClient.updateReservation(ws);
			}
		}
		logger.debug("reservation response " + commonResponseWs);
		if (!commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) return commonResponseWs;
		Gson gson = new Gson();
		ws = gson.fromJson(gson.toJson(commonResponseWs.getData()), ReservationWs.class);
		rider.setIdCautionReservation(ws.getId());
		riderRepository.save(rider);
		return commonResponseWs;
	}


}

   