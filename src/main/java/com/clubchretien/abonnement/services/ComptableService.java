package com.clubchretien.abonnement.services;

import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.*;
import com.owomi.database.entities.BoutiqueMarchand;
import com.owomi.database.entities.Commande;
import com.owomi.database.repositories.BoutiqueMarchandRepository;
import com.owomi.database.repositories.BoutiqueShopizerRepository;
import com.owomi.database.repositories.CommandeRepository;
import com.owomi.database.repositories.TransfertsRepository;
import com.owomi.rest.client.shopizerService.ShopizerServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ComptableService {

    private static Logger logger = LogManager.getLogger(ComptableService.class);

    @Autowired
    private TransfertsRepository transfertsRepository;

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private BoutiqueShopizerRepository boutiqueShopizerRepository;

    @Autowired
    private BoutiqueMarchandRepository boutiqueMarchandRepository;
    @Autowired
    private ShopizerServiceClient shopizerServiceClient;

    public List<TransersComptableWs> getTransferValider(Date dateDebut, Date dateFin){
        List<TransersComptableWs> transfertsList = transfertsRepository.findByValiderTrueAndDateExpeditionBetween(dateDebut, dateFin)
                .stream()
                .map(t -> new TransersComptableWs(t.getMontant().toString(),
                        t.getExpediteur().getLibelle(),
                        t.getDestinataire().getLibelle(),
                        t.getDateExpedition().toString()))
                .collect(Collectors.toList());


        return transfertsList;
    }

    public List<VenteComptableWs> getVentes(Date dateDebut, Date dateFin){
        CommonResponseWs commonResponseWs = new CommonResponseWs();

        List<VenteComptableWs> venteComptableWsList = new ArrayList<>();

        //commande finaliser avec etat=DELIVERED
        List<Commande> commandes = commandeRepository.findAllByFinaliseTrueAndDateCommandeBetween(dateDebut, dateFin);

        commandes.stream().filter(commande -> commande.getEtat().equalsIgnoreCase("delivered")).forEach(commande -> {
            //1-Recuperation de tous les produits avec commande_id
            List<ProduitComptable> produits = new ArrayList<>();
            try {
                CommandeShopizerWs commandeShopizerWs = shopizerServiceClient.getOrder(commande.getCommandeId());
                List<ProduitShopizerWs> produitShopizers = commandeShopizerWs.getProducts();
                for(ProduitShopizerWs produit:produitShopizers){

                    if(produit == null){
                        continue;
                    }

                    Integer marchand = produit.getProduct().getMarchand();
                    String owner = null;
                    if(marchand!=null){
                        //productOwner
                        BoutiqueMarchand boutiqueMarchand = boutiqueMarchandRepository.findOneById(marchand);

                        if(boutiqueMarchand!=null){
                            owner = boutiqueMarchand.getNom();
                        }
                    }
                    produits.add(
                        new ProduitComptable(
                            produit.getProduct().getDescription().getName(),
                            produit.getPrice(),
                            produit.getProduct().getProductWeight().toString(),
                            produit.getOrderedQuantity().toString(),
                            owner,
                            produit.getProduct().getCommission().toString()
                    ));
                }

            } catch (OwomiException e) {
                e.printStackTrace();
            }


            //2-Recuperation du montantFac, date commande et du market place
            String totalAmount = commande.getPrixFacture().toString();
            String date = commande.getDateCommande().toString();
            String story = boutiqueShopizerRepository.findOneByCodeBoutique(commande.getStore()).getLibelle();

            venteComptableWsList.add(new VenteComptableWs(totalAmount, date,  story, produits));
        });



        return venteComptableWsList;
    }

}
