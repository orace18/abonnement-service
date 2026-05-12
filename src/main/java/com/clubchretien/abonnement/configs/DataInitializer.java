package com.clubchretien.abonnement.configs;

import com.owomi.database.entities.FormuleAbonnement;
import com.owomi.database.entities.enums.Duree;
import com.owomi.database.entities.enums.TypeAbonnement;
import com.owomi.database.repositories.repositories.FormuleAbonnementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private FormuleAbonnementRepository formuleAbonnementRepository;

    @Autowired
    private AbonnementProperties abonnementProperties;

    @Override
    public void run(String... args) throws Exception {
        // Vérifier si des données existent déjà
        if (formuleAbonnementRepository.count() == 0) {
            System.out.println("Initialisation des formules d'abonnement pour " + abonnementProperties.getPays() + "...");

            creerFormules();

            System.out.println("Données créées avec succès pour " + abonnementProperties.getPays() + " !");
        } else {
            System.out.println("Données déjà présentes, pas d'initialisation nécessaire.");
        }
    }

    private void creerFormules() {
        String pays = abonnementProperties.getPays();

        // CLASSIC
        creerFormule(pays, TypeAbonnement.CLASSIC, Duree.UN_MOIS,
                abonnementProperties.getClassic().getUnmois());
        creerFormule(pays, TypeAbonnement.CLASSIC, Duree.TROIS_MOIS,
                abonnementProperties.getClassic().getTroismois());
        creerFormule(pays, TypeAbonnement.CLASSIC, Duree.SIX_MOIS,
                abonnementProperties.getClassic().getSixmois());
        creerFormule(pays, TypeAbonnement.CLASSIC, Duree.DOUZE_MOIS,
                abonnementProperties.getClassic().getDouzemois());

        // GOLD
        creerFormule(pays, TypeAbonnement.GOLD, Duree.UN_MOIS,
                abonnementProperties.getGold().getUnmois());
        creerFormule(pays, TypeAbonnement.GOLD, Duree.TROIS_MOIS,
                abonnementProperties.getGold().getTroismois());
        creerFormule(pays, TypeAbonnement.GOLD, Duree.SIX_MOIS,
                abonnementProperties.getGold().getSixmois());
        creerFormule(pays, TypeAbonnement.GOLD, Duree.DOUZE_MOIS,
                abonnementProperties.getGold().getDouzemois());

        // DIAMOND
        creerFormule(pays, TypeAbonnement.DIAMOND, Duree.UN_MOIS,
                abonnementProperties.getDiamond().getUnmois());
        creerFormule(pays, TypeAbonnement.DIAMOND, Duree.TROIS_MOIS,
                abonnementProperties.getDiamond().getTroismois());
        creerFormule(pays, TypeAbonnement.DIAMOND, Duree.SIX_MOIS,
                abonnementProperties.getDiamond().getSixmois());
        creerFormule(pays, TypeAbonnement.DIAMOND, Duree.DOUZE_MOIS,
                abonnementProperties.getDiamond().getDouzemois());
    }

    private void creerFormule(String pays, TypeAbonnement type, Duree duree,
                              AbonnementProperties.DureeConfig config) {
        FormuleAbonnement formule = new FormuleAbonnement(
                pays,
                type,
                duree,
                config.getPrix(),
                config.getMaxLeveeFond()
        );
        formuleAbonnementRepository.save(formule);
        System.out.println("Créé: " + pays + " - " + type + " - " + duree +
                " - " + config.getPrix() + " " + abonnementProperties.getDevise());
    }
}