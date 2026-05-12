package com.clubchretien.abonnement.services;


import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.BanqueWs;
import com.owomi.commons.ws.RecipientFieldWs;
import com.owomi.database.entities.Banque;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.BanqueRepository;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BanqueService {

    @Autowired
    BanqueRepository banqueRepository;

    @Autowired
    Mapper mapper;


    //Cette fonction permet de recupérer une banque dans la base de donnée grace a son id qu'elle prend en parametre;
    // au cas ou l'id de cette banque n'existe pas un message d'erreur est envoyé le signalant.
    public BanqueWs getBanque(Integer id) throws OwomiException {
        Banque banque = banqueRepository.findOneById(id);
        if(banque == null) throw new OwomiException("",ErrorCodeEnums.GET_BANQUE_FROM_DATABASE_ERROR,"");
        BanqueWs banqueWs = this.banqueParseToBanqueWs(banque);
        return banqueWs;
    }

    //Cette fonction permet de recupérer la liste de toutes les banque du pays d'un partenaire présente dans la base de donnée.
    public List<BanqueWs> getListBanque(Partenaire partenaire){

        //List<Banque> banqueList = banqueRepository.findByPaysOrInternational(partenaire.getPays(), true);
        List<Banque> banqueList = new ArrayList<>();
        List<BanqueWs> banqueWsList = new ArrayList<>();

        for (Banque banque : banqueList){

            BanqueWs banqueWs= this.banqueParseToBanqueWs(banque);
            banqueWsList.add(banqueWs);
        }
        return banqueWsList;
    }

    private BanqueWs banqueParseToBanqueWs(Banque banque){
        BanqueWs banqueWs = new BanqueWs();
        banqueWs.setId(banque.getId());
        //banqueWs.setAutomatiqued(banque.getAutomatiqued());
        //banqueWs.setEnvoie_autorised(banque.getEnvoie_autorised());
        //banqueWs.setMax_amount_autorised_by_day(banque.getMax_amount_autorised_by_day()!= null ? banque.getMax_amount_autorised_by_day().intValue() : null);
        //banqueWs.setMax_amount_autorised_by_month(banque.getMax_amount_autorised_by_month() != null ? banque.getMax_amount_autorised_by_month().intValue() : null);
        //banqueWs.setMax_amount_autorised_by_operation(banque.getMax_amount_autorised_by_operation()!= null ? banque.getMax_amount_autorised_by_operation().intValue() : null);
        //banqueWs.setMax_amount_to_manual_operation(banque.getMax_amount_to_manual_operation() != null ? banque.getMax_amount_to_manual_operation().intValue() : null);
        banqueWs.setNom(banque.getNom());
        banqueWs.setNumerodecompte(banque.getNumerodecompte() != null ? banque.getNumerodecompte().getNumero() : null);
        banqueWs.setSigle(banque.getSigle());
        //banqueWs.setFichier(banque.getFichier());

        String[][] recipientFieldsPossibilityGroup = {
                { "iban", "numero_du_compte", "intitule_du_compte", "adresse", "ville", "tel", "swift"},
                {"tel", "nom_utlisateur"}
        };
        String[] recipientFieldsPossibilityChosen = recipientFieldsPossibilityGroup[banque.getIban() != null ? 0 : 1];

        List<RecipientFieldWs> recipientFieldWsList = new ArrayList<>();

        for (String recipientFieldString : recipientFieldsPossibilityChosen) {
            RecipientFieldWs recipientFieldWs = new RecipientFieldWs();
            recipientFieldWs.setNom(recipientFieldString);
            recipientFieldWs.setIsRequired(true);
            recipientFieldWsList.add(recipientFieldWs);
        }
        banqueWs.setFields(recipientFieldWsList);
        return banqueWs;
    }


}
