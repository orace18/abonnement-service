package com.clubchretien.abonnement.core.corporate.services;

import com.clubchretien.abonnement.core.corporate.ws.MobileCorporateWs;
import com.clubchretien.abonnement.services.NotificationService;
import com.owomi.commons.constants.MobileConstant;
import com.owomi.commons.constants.ResponseConstant;
import com.owomi.commons.enums.*;
import com.owomi.commons.enums.ecofam.AbonneGroupeTypeEnums;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.EntityUtils;
import com.owomi.commons.utils.MobileUtils;
import com.owomi.commons.ws.*;
import com.owomi.commons.ws.acm.AcmAccountSoldeWs;
import com.owomi.commons.ws.acm.AcmSoldeWs;
import com.owomi.commons.ws.billing.response.PageResponse;
import com.owomi.database.entities.*;
import com.owomi.database.repositories.*;
import com.owomi.rest.client.accountService.AccountMouvementSoldeServiceClient;
import com.owomi.rest.client.accountService.AccountServiceClient;
import com.owomi.rest.client.dataservice.CorporateMatriculeServiceClient;
import com.owomi.rest.client.owopay.core.CoreOwoPayTransactionServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Component
public class MobileCorporateService implements MobileConstant, ResponseConstant {
    private static Logger logger = LogManager.getLogger(MobileCorporateService.class);

    @Autowired
    Mapper mapper;
    @Autowired
    CorporateRepository corporateRepository;
    @Autowired
    PartenaireRepository partenaireRepository;
    @Autowired
    PartenaireCorporateRepository  partenaireCorporateRepository;
    @Autowired
    PlanComptableBaseRepository planComptableBaseRepository;
    @Autowired
    PlanComptableRepository planComptableRepository;
    @Autowired
    FormeJuridiqueRepository formeJuridiqueRepository;
    @Autowired
    TypeCorporateRepository typeCorporateRepository;
    @Autowired
    HabileteRepository habileteRepository;
    @Autowired
    NatureCorporateRepository natureCorporateRepository;
    @Autowired
    AccountMouvementSoldeServiceClient accountMouvementSoldeServiceClient;
    @Autowired
    AccountServiceClient accountServiceClient;
    @Autowired
    NotificationService notificationService;

    @Autowired
    CorporateMatriculeServiceClient corporateMatriculeServiceClient;
    @Autowired
    CoreOwoPayTransactionServiceClient coreOwoPayTransactionServiceClient;

    private PlanComptable createPlanComptable(String libelle){
        logger.info("RECUPERATION PLAN COMPTABLE ====> ");
        PlanComptableBase planComptableBase = planComptableBaseRepository.findByLibelle("Clients")
                .stream().findFirst().orElse(null);
        logger.info("PLAN COMPTABLE ====> " + planComptableBase.toString() + " === " + planComptableBase.getNumero());
        String number = MobileUtils.generateCompteNumberEightDigits(4111);
        PlanComptable  planComptable = new PlanComptable();
        planComptable.setLibelle(libelle);
        planComptable.setNumero(planComptableBase.getNumero()+number);
        planComptable.setParentbase(planComptableBase);
        planComptable.setDatecreation(new Date());
        planComptable = planComptableRepository.save(planComptable);
        return planComptable;
    }

    private Corporate creationDefaultCorporate(Partenaire partenaire) {
        Corporate corporate = this.createDefaultCorporate(partenaire);
        CompletableFuture.runAsync(() -> {
            try {
                this.notificationService.welcomeApp(partenaire);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return corporate;
    }

    public Corporate createDefaultCorporate(Partenaire partenaire) {
        TypeCorporate typeCorporate = typeCorporateRepository.findOneByLibelle(TypeCorporateEnums.ALTERNATIF.getLibelle());
        Habilete habilete = !habileteRepository.findByLibelle(HabileteEnums.PROPRIETAIRE.getLibelle()).isEmpty() ? habileteRepository.findByLibelle(HabileteEnums.PROPRIETAIRE.getLibelle()).get(0) : null;
        FormeJuridique formeJuridique = formeJuridiqueRepository.findByLibelle(FormeJuridiqueEnums.PersonnePhysique.getForm());
        Corporate corporate = this.corporateRepository.findOneByMatricule(partenaire.getTelephone());
        if (corporate != null) return corporate;
        corporate = new Corporate();
        corporate.setLibelle(partenaire.getNom() + " " + partenaire.getPrenoms());
        corporate.setTypeCorporate(typeCorporate);
        corporate.setNumeroCompte(createPlanComptable(partenaire.getNom() + " " + partenaire.getPrenoms()));
        corporate.setStatut(Boolean.TRUE);
        corporate.setMatricule(partenaire.getTelephone());
        corporate.setFormeJuridique(formeJuridique);
        corporate.setAutorise(Boolean.FALSE);
        corporate.setActif(Boolean.TRUE);
        corporate.setCdp(Boolean.FALSE);
        corporate.setAgentAllowed(Boolean.FALSE);
        corporate.setMasterAllowed(Boolean.FALSE);
        corporate.setProvisoire(Boolean.FALSE);
        corporate.setAllowedCashOut(Boolean.TRUE);
        corporate.setNatureCorporate(natureCorporateRepository.findOneByLibelle(NatureCorporateEnums.UTILISATEUR.getNature()));
        corporate.setDevise(partenaire.getPays().getDevise());
        corporate = corporateRepository.save(corporate);
        PartenaireCorporate partenaireCorporate = new PartenaireCorporate();
        partenaireCorporate.setPartenaire(partenaire);
        partenaireCorporate.setCorporate(corporate);
        partenaireCorporate.setHabilete(habilete);
        partenaireCorporate.setCurrent(Boolean.TRUE);
        partenaireCorporate.setActif(Boolean.TRUE);
        partenaireCorporate.setOwner(Boolean.TRUE);
        partenaireCorporateRepository.save(partenaireCorporate);
        return corporate;
    }

    private Boolean stateCreationCorporate(Partenaire partenaire) {
        if (partenaire == null) return false;
        boolean phoneVerified = partenaire.getTelephone() != null && partenaire.getPhoneNumberVerified() != null && partenaire.getPhoneNumberVerified();
        PartenaireCorporate pc = this.partenaireCorporateRepository.findOneByCurrentAndPk_Partenaire(true, partenaire);
        return phoneVerified && pc == null;
    }

    private Corporate principalCorporate(Partenaire partenaire) {
        if (partenaire == null) return null;
        PartenaireCorporate pc = this.partenaireCorporateRepository.findOneByCurrentAndPk_Partenaire(true, partenaire);
        Corporate corporate = Optional.ofNullable(pc).map(PartenaireCorporate::getCorporate).orElse(null);
        boolean state = this.stateCreationCorporate(partenaire);
        if (corporate == null && state) corporate = this.creationDefaultCorporate(partenaire);
        return corporate;
    }

    private Boolean certifiedCorporate(Partenaire partenaire) {
        if (partenaire == null) return false;
        return partenaire.getTelephone() != null && partenaire.getPhoneNumberVerified() != null && partenaire.getPhoneNumberVerified();
    }


    private List<Corporate> corporates(Partenaire partenaire) {
        if (!this.certifiedCorporate(partenaire)) return new ArrayList<>();
        if (this.stateCreationCorporate(partenaire)) this.principalCorporate(partenaire);
        List<PartenaireCorporate> pcs = this.partenaireCorporateRepository.findAllByPk_Partenaire(partenaire);
        List<String> types = Arrays.asList(AbonneGroupeTypeEnums.PROFESSIONAL_GROUP.getValue(), AbonneGroupeTypeEnums.PROFESSIONAL_GROUP.getValue());
        pcs = pcs.stream().filter(v -> !types.contains(v.getCorporate().getTypeCorporate().getLibelle()))
                .filter(v -> !v.getCorporate().getLibelle().startsWith("GROUPE SOCIALE SOLIDAIRE"))
                .filter(v -> !v.getCorporate().getLibelle().startsWith("GROUPE PROFESSIONNEL"))
                .collect(Collectors.toList());
        return pcs.stream().map(PartenaireCorporate::getCorporate).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private MobileCorporateWs convertCorporateToWs(Corporate entity) {
        if(entity == null) return null;
        MobileCorporateWs result = this.mapper.map(entity, MobileCorporateWs.class);
        if (entity.getDevise() != null) result.setDevise(this.mapper.map(entity.getDevise(), DeviseWs.class));
        if (entity.getNumeroCompte() != null) result.setNumeroCompteId(entity.getNumeroCompte().getId());
        if (entity.getNatureCorporate() != null) result.setNatureCorporateLibelle(entity.getNatureCorporate().getLibelle());
        if (entity.getTypeCorporate() != null) result.setTypeCorporateLibelle(entity.getTypeCorporate().getLibelle());
        result.setSolde(this.acmSolde(entity));
        return result;
    }

    private AcmAccountSoldeWs acmAccountSolde(Corporate entity) {
        if(entity == null) return null;
        PlanComptable account = entity.getNumeroCompte();
        if(account == null) return null;
        return this.accountMouvementSoldeServiceClient.getAcmAccountSolde(account.getId());
    }

    public AcmSoldeWs acmSolde(Corporate entity) {
        AcmAccountSoldeWs solde = this.acmAccountSolde(entity);
        return Optional.ofNullable(solde).map(AcmAccountSoldeWs::getSolde).orElse(null);
    }


    public CommonResponseWs listCorporates(Partenaire partenaire) {
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 401, null);
        List<Corporate> corporates = this.corporates(partenaire);
        List<MobileCorporateWs> result = corporates.stream()
                .map(this::convertCorporateToWs).filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
    }

    private Boolean isOwner(Corporate corporate, Partenaire partenaire)  {
        if (partenaire == null || corporate == null) return false;
        PartenaireCorporate pc = this.partenaireCorporateRepository.findOneByPk_CorporateAndPk_Partenaire(corporate, partenaire);
        return Optional.ofNullable(pc).map(PartenaireCorporate::isOwner).orElse(false);
    }

    public CommonResponseWs soldeCorporate(Partenaire partenaire, String matricule) {
        Corporate corporate = this.corporateRepository.findOneByMatricule(matricule);
        if (corporate == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "corporate", 404, null);
        if (!this.isOwner(corporate, partenaire)) return new CommonResponseWs(WsOperationResponseStatus.failed, "owner", 431, null);
        AcmSoldeWs result = this.acmSolde(corporate);
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
    }

    public CommonResponseWs getCorporateByMatricule(Partenaire partenaire, String matricule) {
        Corporate corporate = this.corporateRepository.findOneByMatricule(matricule);
        if (corporate == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "corporate", 404, null);
        MobileCorporateWs result = this.convertCorporateToWs(corporate);
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
    }

    public CorporateWs getCorporateByMatricule(RequestContextWs requestContext, String matricule) throws DataNotFoundException {
        Corporate corporate = corporateRepository.findOneByMatricule(matricule);
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, "matricule", matricule);
        return mapper.map(corporate, CorporateWs.class);
    }

    @Transactional
    public CommonResponseWs getHistoriqueCompteByCorporate(Partenaire partenaire, Integer idCorporate, Integer page, Integer size) {
        Corporate corporate = this.corporateRepository.findOneById(idCorporate);
        if (corporate == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "corporate", 404, null);
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 404, null);
        PageResponse<GrandLivreWs> pages = accountServiceClient.getHistoriqueCompteV2(corporate.getNumeroCompte().getId(), page, size);
        String devise = Optional.ofNullable(corporate.getDevise()).map(Devise::getCode).orElse(null);
        if (devise == null)
            devise = Optional.ofNullable(partenaire.getPays()).map(Pays::getDevise).map(Devise::getCode).orElse(null);
        String finalDevise = devise;
        List<GrandLivreWs> content = pages.getContent().stream().peek(p -> p.setCodeDevise(finalDevise)).collect(Collectors.toList());
        Pageable pageable = PageRequest.of(page, size);
        Page<GrandLivreWs> result = new PageImpl<>(content, pageable, pages.getTotalElements());
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
    }

    public CommonResponseWs listDistributionTransactionByCorporateMatricule(Partenaire partenaire, String matricule, Integer page, Integer size) throws OwomiException {
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 404, null);
        Corporate corporate = this.corporateRepository.findOneByMatricule(matricule);
        if (corporate == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "corporate", 404, null);
        return this.coreOwoPayTransactionServiceClient.listDistributionTransactionByCorporateMatricule(matricule, page, size);
    }

}
