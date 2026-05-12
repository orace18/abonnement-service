package com.clubchretien.abonnement.services;

import com.owomi.commons.constants.MobileConstant;
import com.owomi.commons.constants.ResponseConstant;
import com.owomi.commons.enums.FirebaseAppEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.NotificationException;
import com.owomi.commons.ws.*;
import com.owomi.database.entities.*;
import com.owomi.database.repositories.CorporateDelegationRepository;
import com.owomi.database.repositories.CorporateRepository;
import com.owomi.database.repositories.PartenaireCorporateRepository;
import com.owomi.database.repositories.PartenaireRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CorporateDelegationService implements MobileConstant, ResponseConstant {
	private static Logger logger = LogManager.getLogger(CorporateDelegationService.class);

	@Autowired
	CorporateRepository corporateRepository;
	@Autowired
	PartenaireRepository partenaireRepository;
	@Autowired
	PartenaireCorporateRepository partenaireCorporateRepository;
	@Autowired
	CorporateDelegationRepository corporateDelegationRepository;
	@Autowired
	NotificationService notificationService;
	@Autowired
	Mapper mapper;

	public List<CorporateDelegation> create(CorporateDelegationCreateWs ws, Partenaire owner) {
		logger.debug("create " + ws);
		logger.debug("create owner " + owner);
		Corporate corporate = this.corporateRepository.findOneByMatricule(ws.getMatricule());
		if (corporate == null) return new ArrayList<>();
		logger.debug("create corporate " + corporate);
		PartenaireCorporate ownerPC = this.partenaireCorporateRepository.findOneByPk_CorporateAndPk_Partenaire(corporate, owner);
		if (ownerPC == null) return new ArrayList<>();
		logger.debug("create ownerPC " + ownerPC);
		Partenaire manager = this.partenaireRepository.findOneByTelephone(ws.getTelephone());
		if (manager == null) return new ArrayList<>();
		logger.debug("create manager " + manager);
		logger.debug("create privileges " + ws.getPrivileges());
		for (String privilege : ws.getPrivileges()) {
			CorporateDelegation delegation = this.corporateDelegationRepository.findOneByOwnerAndAccountAndManagerAndPrivilege(
					ownerPC.getPartenaire(), ownerPC.getCorporate(),
					manager, privilege
			);
			if (delegation == null) delegation = new CorporateDelegation();
			delegation.setAccount(ownerPC.getCorporate());
			delegation.setAccount(ownerPC.getCorporate());
			delegation.setOwner(ownerPC.getPartenaire());
			delegation.setManager(manager);
			delegation.setExpiration(ws.getExpiration());
			delegation.setPrivilege(privilege);
			delegation.setRevoked(false);
			delegation = this.corporateDelegationRepository.save(delegation);
			logger.debug("create save delegation " + delegation);
		}
		return this.list(ownerPC.getPartenaire(), ownerPC.getCorporate(), manager);
	}

	public void revoke(CorporateDelegationCreateWs ws, Partenaire owner) {
		Corporate corporate = this.corporateRepository.findOneByMatricule(ws.getMatricule());
		Partenaire manager = this.partenaireRepository.findOneByTelephone(ws.getTelephone());
		for (String privilege : ws.getPrivileges()) {
			CorporateDelegation delegation = this.corporateDelegationRepository.findOneByOwnerAndAccountAndManagerAndPrivilege(
					owner, corporate, manager, privilege
			);
			if (delegation == null) continue;
			delegation.setExpiration(new Date());
			delegation.setRevoked(true);
			this.corporateDelegationRepository.save(delegation);
		}
	}

	public List<CorporateDelegation> list(Partenaire owner, Corporate corporate, Partenaire manager) {
		if (owner == null || corporate == null || manager == null) return new ArrayList<>();
		logger.debug("list owner  " + owner.getId());
		logger.debug("list corporate  " + corporate.getId());
		logger.debug("list manager  " + manager.getId());
		return this.corporateDelegationRepository.findByOwnerAndAccountAndManagerAndRevokedAndExpirationGreaterThan(
				owner, corporate, manager, false, new Date()
		);
	}

	public CorporateDelegationSimpleWs convertForOwner(List<CorporateDelegation> delegations) {
		if (delegations.isEmpty()) return null;
		Partenaire partenaire = delegations.stream().map(CorporateDelegation::getManager).findFirst().orElse(null);
		Corporate corporate = delegations.stream().map(CorporateDelegation::getAccount).findFirst().orElse(null);
		CorporateDelegationSimpleWs ws = new CorporateDelegationSimpleWs();
		ws.setPartenaire(mapper.map(partenaire, PartenaireWs.class));
		ws.setAccount(mapper.map(corporate, CorporateWs.class));
		List<CorporateDelegationSimpleWs.CorporateDelegationPrivilegeWs> privileges = delegations.stream()
				.map(v -> mapper.map(v, CorporateDelegationSimpleWs.CorporateDelegationPrivilegeWs.class))
				.collect(Collectors.toList());
		ws.setPrivileges(privileges);
		return ws;
	}

	public CorporateDelegationSimpleWs convertForManager(List<CorporateDelegation> delegations) {
		if (delegations.isEmpty()) return null;
		Partenaire partenaire = delegations.stream().map(CorporateDelegation::getOwner).findFirst().orElse(null);
		Corporate corporate = delegations.stream().map(CorporateDelegation::getAccount).findFirst().orElse(null);
		CorporateDelegationSimpleWs ws = new CorporateDelegationSimpleWs();
		ws.setPartenaire(mapper.map(partenaire, PartenaireWs.class));
		ws.setAccount(mapper.map(corporate, CorporateWs.class));
		List<CorporateDelegationSimpleWs.CorporateDelegationPrivilegeWs> privileges = delegations.stream()
				.map(v -> mapper.map(v, CorporateDelegationSimpleWs.CorporateDelegationPrivilegeWs.class))
				.collect(Collectors.toList());
		ws.setPrivileges(privileges);
		return ws;
	}

	public CommonResponseWs createDelegation(CorporateDelegationCreateWs ws, Partenaire owner) {
		Corporate corporate = this.corporateRepository.findOneByMatricule(ws.getMatricule());
		if (corporate == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "corporate null", 200, null);
		PartenaireCorporate ownerPC = this.partenaireCorporateRepository.findOneByPk_CorporateAndPk_Partenaire(corporate, owner);
		if (ownerPC == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "owner null", 200, null);
		Partenaire manager = this.partenaireRepository.findOneByTelephone(ws.getTelephone());
		if (manager == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "manager null", 200, null);
		List<CorporateDelegation> list = this.create(ws, owner);
		CorporateDelegationSimpleWs result = this.convertForOwner(list);
		try {
			this.notificationCreateDelegation(result, owner);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new CommonResponseWs(WsOperationResponseStatus.success, "success", 200, result);
	}

	private void notificationCreateDelegation(CorporateDelegationSimpleWs ws, Partenaire owner) throws NotificationException {
		Partenaire partenaire = this.partenaireRepository.findOneById(ws.getPartenaire().getId());
		String title = "Delegation de compte";
		String message = "Vous venez de deleguer votre compte " +
				ws.getAccount().getMatricule() + " a " +
				ws.getPartenaire().getNom() + " " + ws.getPartenaire().getPrenoms();
		this.notificationService.notificationAndAl(FirebaseAppEnums.MOBILE, owner, title, message);
		message = "Vous venez d'etre designe comme gestionnaire du compte " +
				ws.getAccount().getMatricule() + " par " +
				owner.getNom() + " " + owner.getPrenoms()
		;
		this.notificationService.notificationAndAl(FirebaseAppEnums.MOBILE, partenaire, title, message);
	}

	public CommonResponseWs revokeDelegation(CorporateDelegationCreateWs ws, Partenaire owner) {
		if (owner == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "owner null", 200, null);
		Corporate corporate = this.corporateRepository.findOneByMatricule(ws.getMatricule());
		if (corporate == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "corporate null", 200, null);
		Partenaire manager = this.partenaireRepository.findOneByTelephone(ws.getTelephone());
		if (manager == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "manager null", 200, null);
		this.revoke(ws, owner);
		return new CommonResponseWs(WsOperationResponseStatus.success, "success", 200, null);
	}

	public CommonResponseWs listDelegationOwner(Partenaire owner, Corporate corporate) {
		List<CorporateDelegation> list = this.corporateDelegationRepository.findByOwnerAndAccountAndRevokedAndExpirationGreaterThan(
				owner, corporate, false, new Date()
		);
		List<Partenaire> partenaires = list.stream().map(CorporateDelegation::getManager)
				.map(Partenaire::getId).distinct().map(v -> this.partenaireRepository.findOneById(v))
				.collect(Collectors.toList());
		List<CorporateDelegationSimpleWs> result = new ArrayList<>();
		for (Partenaire partenaire : partenaires) {
			List<CorporateDelegation> l = this.list(owner, corporate, partenaire);
			CorporateDelegationSimpleWs ws = this.convertForOwner(l);
			result.add(ws);
		}
		return new CommonResponseWs(WsOperationResponseStatus.success, "success", 200, result);
	}

	public CommonResponseWs listDelegationOwner(Partenaire owner) {
		List<CorporateDelegation> list = this.corporateDelegationRepository.findByOwnerAndRevokedAndExpirationGreaterThan(
				owner, false, new Date()
		);
		List<Corporate> corporates = list.stream().map(CorporateDelegation::getAccount)
				.map(Corporate::getId).distinct().map(v -> this.corporateRepository.findOneById(v))
				.collect(Collectors.toList());
		List<CorporateDelegationSimpleWs> result = new ArrayList<>();
		for (Corporate corporate : corporates) {
			CommonResponseWs commonResponseWs = this.listDelegationOwner(owner, corporate);
			if (!commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) continue;
			List<CorporateDelegationSimpleWs> r = (List<CorporateDelegationSimpleWs>) commonResponseWs.getData();
			result.addAll(r);
		}
		return new CommonResponseWs(WsOperationResponseStatus.success, "success", 200, result);
	}

	public CommonResponseWs listDelegationManager(Partenaire manager) {
		logger.debug("listDelegationManager manager " + manager.getId());
		List<CorporateDelegation> list = this.corporateDelegationRepository.findByManagerAndRevokedAndExpirationGreaterThan(
				manager, false, new Date()
		);
		logger.debug("listDelegationManager list " + list.size());
		List<PartenaireCorporateId> pcs = list.stream()
				.map(v -> {
					PartenaireCorporateId pc = new PartenaireCorporateId();
					pc.setCorporate(v.getAccount());
					pc.setPartenaire(v.getOwner());
					return pc;
				}).collect(Collectors.toList());
		logger.debug("listDelegationManager pcs " + pcs.size());
		List<CorporateDelegationSimpleWs> result = new ArrayList<>();
		for (PartenaireCorporateId pc : pcs) {
			logger.debug("listDelegationManager pc c " + pc.getCorporate().getMatricule());
			logger.debug("listDelegationManager pc p " + pc.getPartenaire().getId());
			List<CorporateDelegation> l = this.list(pc.getPartenaire(), pc.getCorporate(), manager);
			CorporateDelegationSimpleWs ws = this.convertForManager(l);
			if (ws != null) result.add(ws);
		}
		return new CommonResponseWs(WsOperationResponseStatus.success, "success", 200, result);
	}


}
