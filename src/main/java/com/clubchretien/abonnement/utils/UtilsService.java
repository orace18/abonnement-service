package com.clubchretien.abonnement.utils;

import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.EntityUtils;
import com.owomi.commons.ws.*;
import com.owomi.database.entities.*;
import com.owomi.database.repositories.*;
import com.owomi.rest.client.dataservice.DataServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class UtilsService {
    private static Logger logger = LogManager.getLogger(UtilsService.class);

    @Autowired
    ConfigProduitRepository configProduitRepository;
    @Autowired
    FraisRepository fraisRepository;
    @Autowired
    PanierProduitRepository panierProduitRepository;
    @Autowired
    CommandespRepository commandespRepository;
    @Autowired
    ProduitPaysRepository produitPaysRepository;
    @Autowired
    OperationStringRepository operationStringRepository;
    @Autowired
    CorporateRepository corporateRepository;
    @Autowired
    GrandLivreRepository grandLivreRepository;
    @Autowired
    Mapper mapper;

    @Autowired
    DataServiceClient dataServiceClient;
    @Autowired
    ImageRepository imageRepository;
    @Autowired
    ModeCalculRepository modeCalculRepository;

    public Map<String, Double> getFraisAndCv(RequestContextWs requestContext, ProduitPays produitPays, double qte) {
        ConfigProduit configProduit = configProduitRepository.findOneByProduitpays(produitPays);
        double prixTotal = qte * configProduit.getPrix();
        Map<String, Double> resultat = new HashMap<>();
        double frs = 0.0;
        double commission = 0.0;
        List<Frais> frais = fraisRepository.findByConfig(configProduit);
        for (Frais f : frais) {
            double min = f.getMontantmin();
            if (f.getCommissionreseau()) {
                List<ModeCalcul> calculs = this.modeCalculRepository.findByFrais(f);
                for (ModeCalcul mc : calculs) {
                    if (configProduit.getQuantifiable()) {
                        if (mc.getMontantinf() < configProduit.getPrix() && configProduit.getPrix() <= mc.getMontantsup()) {
                            commission += (mc.getFraisfixe() + ((mc.getFraisvar() / 100) * configProduit.getPrix())) * qte;
                        }
                        if (mc.getMontantinf() < configProduit.getPrix() && mc.getMontantsup() == -1 && configProduit.getPrix() < Double.POSITIVE_INFINITY) {
                            commission += (mc.getFraisfixe() + ((mc.getFraisvar() / 100) * configProduit.getPrix())) * qte;
                        }
                    } else {
                        if (mc.getMontantinf() < prixTotal && prixTotal <= mc.getMontantsup()) {
                            commission += (mc.getFraisfixe() + ((mc.getFraisvar() / 100) * prixTotal));
                        }
                        if (mc.getMontantinf() < prixTotal && mc.getMontantsup() == -1 && prixTotal < Double.POSITIVE_INFINITY) {
                            commission += (mc.getFraisfixe() + ((mc.getFraisvar()/100) * prixTotal));
                        }
                    }
                }
            }
            if (!f.getInclus()) {
                List<ModeCalcul> calculs = this.modeCalculRepository.findByFrais(f);
                HashSet<ModeCalcul> mcset = new HashSet<>(calculs);
                for (ModeCalcul mc : mcset) {
                    double temp = 0;
                    if (configProduit.getQuantifiable()) {
                        if (mc.getMontantinf() < configProduit.getPrix() && configProduit.getPrix() <= mc.getMontantsup()) {
                            temp = (mc.getFraisfixe() + ((mc.getFraisvar() / 100) * configProduit.getPrix())) * qte;
                            if (temp < min) {
                                frs += min;
                            } else {
                                frs += temp;
                            }
                        }
                        if (mc.getMontantinf() < configProduit.getPrix() && mc.getMontantsup() == -1 && configProduit.getPrix() < Double.POSITIVE_INFINITY) {
                            temp = (mc.getFraisfixe() + ((mc.getFraisvar()/100) * configProduit.getPrix())) * qte;
                            if (temp < min) {
                                frs += min;
                            } else {
                                frs += temp;
                            }
                        }
                    } else {
                        if (mc.getMontantinf() < prixTotal && prixTotal <= mc.getMontantsup()) {
                            temp = (mc.getFraisfixe() + ((mc.getFraisvar()/100) * prixTotal));
                            if (temp < min) {
                                frs += min;
                            } else {
                                frs += temp;
                            }
                        }
                        if (mc.getMontantinf() < prixTotal && mc.getMontantsup() == -1 && prixTotal < Double.POSITIVE_INFINITY) {
                            temp = (mc.getFraisfixe() + ((mc.getFraisvar()/100) * prixTotal));
                            if (temp < min) {
                                frs += min;
                            } else {
                                frs += temp;
                            }
                        }
                    }
                }
            }
        }
        resultat.put("Frais", frs);
        resultat.put("Cv", commission * 0.5);
        resultat.put("TotalHorsTaxes", prixTotal);
        return resultat;
    }

    public void saveGrandLivre(RequestContextWs requestContext,PlanComptable acteurOut, Double montant, String libelle, OperationString operationString, Operation operation) throws DataNotFoundException {
        Corporate corporateTemporaire = corporateRepository.findOneByMatricule("Transit");
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporateTemporaire, "Matricule", "Transit");
        GrandLivre grandLivreDebit = new GrandLivre("0", Integer.decode(operation.getNumero()), new Date(), operationString.getMontant(), libelle, operation, operationString, acteurOut);
        GrandLivre grandLivreCredit = new GrandLivre("1", Integer.decode(operation.getNumero()), new Date(), operationString.getMontant(), libelle, operation, operationString, corporateTemporaire.getNumeroCompte());
        grandLivreRepository.save(grandLivreDebit);
        grandLivreRepository.save(grandLivreCredit);
    }

    public Produit createProduit(RequestContextWs requestContext,ProduitWs produitWs) throws ParseException {
        Produit produit = new Produit();
        String date = produitWs.getDatecreation();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        produit.setDatecreation(formatter.parse(date));
        produit.setDeleted(produitWs.getDeleted());
        produit.setDeleterequested(produitWs.getDeleterequested());
        produit.setDescription(produitWs.getDescription());
        GenreProduit genreProduit = mapper.map(produitWs.getGenreproduit(), GenreProduit.class);
        produit.setGenreproduit(genreProduit);
        Corporate corporate = mapper.map(produitWs.getCorporateWs(), Corporate.class);
        produit.setCorporate(corporate);
        Categorie categorie = mapper.map(produitWs.getCategorieId(), Categorie.class);
        produit.setCategorieId(categorie);
        produit.setNom(produitWs.getNom());
        return produit;
    }

    //Cette fonction aide pour la modication des produit c'est une partie de la fonction updateProduit qui est dans la class
    // ProduitService elle prend en parametre l'objet "ProduitWs" et retourne un objet de type Produit
    public Produit updateProduit(RequestContextWs requestContext,ProduitWs produitWs) throws ParseException {
        Produit produit = new Produit();
        produit.setId(produitWs.getId());
        String date = produitWs.getDatecreation();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        produit.setDatecreation(formatter.parse(date));
        produit.setDeleted(produitWs.getDeleted());
        produit.setDeleterequested(produitWs.getDeleterequested());
        produit.setDescription(produitWs.getDescription());
        GenreProduit genreProduit = mapper.map(produitWs.getGenreproduit(), GenreProduit.class);
        produit.setGenreproduit(genreProduit);
        Corporate corporate = mapper.map(produitWs.getCorporateWs(), Corporate.class);
        produit.setCorporate(corporate);
        Categorie categorie = mapper.map(produitWs.getCategorieId(), Categorie.class);
        produit.setCategorieId(categorie);
        produit.setNom(produitWs.getNom());
        return produit;
    }


    //Cette fonction permet de convertir un objet de type Produit qu'elle prend en parametre en un objet de type "ProduitWs" et le retourne
    public ProduitWs createProduitWs(RequestContextWs requestContext,Produit produit) {
        ProduitWs produitWs = new ProduitWs();
        List<ImageWs> imageWsList = new ArrayList<>();
        List<Image> imageList = this.imageRepository.findByProduit(produit);
        for (Image image : imageList) {
            ImageWs imageWs = new ImageWs();
            imageWs.setId(image.getId());
            imageWs.setEmplacement(image.getEmplacement());
            imageWs.setUrl(image.getUrl());
            imageWs.setHash(image.getHash());
            imageWsList.add(imageWs);
        }
        produitWs.setId(produit.getId());
        produitWs.setDatecreation(String.valueOf(produit.getDatecreation()));
        produitWs.setDeleted(produit.getDeleted());
        produitWs.setDeleterequested(produit.getDeleterequested());
        produitWs.setEtat(produit.getEtat());
        produitWs.setDescription(produit.getDescription());
        produitWs.setGenre(produit.getGenre());
        produitWs.setNom(produit.getNom());
        produitWs.setStatus(produit.getStatus());
        produitWs.setType(produit.getType());
        GenreProduitWs genreProduitWs = mapper.map(produit.getGenreproduit(), GenreProduitWs.class);
        produitWs.setGenreproduit(genreProduitWs);
        CategorieWs categorieWs = mapper.map(produit.getCategorieId(), CategorieWs.class);
        produitWs.setCategorieId(categorieWs);
        CorporateWs corporateWs = createCorporateWs(requestContext,produit.getCorporate());
        produitWs.setCorporateWs(corporateWs);
        return produitWs;
    }
    //Cette fonction permet de convertir un objet de type Corporate en un objet de type "CorporateWs" et le retourne
    public CorporateWs createCorporateWs(RequestContextWs requestContext,Corporate corporate) {
        CorporateWs corporateWs = new CorporateWs();
        corporateWs.setMatricule(corporate.getMatricule());
        corporateWs.setId(corporate.getId());
        corporateWs.setLibelle(corporate.getLibelle());
        corporateWs.setStatut(corporate.isStatut());
        corporateWs.setType_corporate(corporate.getTypeCorporate().getId());
        corporateWs.setNumero_compte(corporate.getNumeroCompte().getId());
        return corporateWs;
    }


    //Cette fonction permet de recuperer un corporate depuis la base de donnée et le retourne en un objet de type "CorporateWs"
    //elle prend en parametre l'id d'un corporate au cas ou ce corporate n'existe pas un message d'erreur est retourné
    public CorporateWs getCorporateWs(RequestContextWs requestContext,int id) throws OwomiException {

        Corporate corporate = corporateRepository.findOneById(id);
        if (corporate == null) throw new OwomiException("", ErrorCodeEnums.GET_CORPORATE_FROM_DATABASE_ERROR, "");
        CorporateWs corporateWs = new CorporateWs();
        corporateWs.setMatricule(corporate.getMatricule());
        corporateWs.setId(corporate.getId());
        corporateWs.setLibelle(corporate.getLibelle());
        corporateWs.setStatut(corporate.isStatut());
        corporateWs.setType_corporate(corporate.getTypeCorporate().getId());
        corporateWs.setNumero_compte(corporate.getNumeroCompte().getId());

        return corporateWs;
    }

    //Cette fonction permet de convertir un objet de type ProduitPays qu'elle prend en parametre en un objet de type "ProduitPaysWs" et le retourne
    public ProduitPaysWs createProduitPaysWs(RequestContextWs requestContext,ProduitPays produitPays) {

        PaysWs paysWs = mapper.map(produitPays.getPays(), PaysWs.class);
        ProduitWs produitWs = createProduitWs(requestContext,produitPays.getProduit());

        ProduitPaysWs produitPaysWs = mapper.map(produitPays, ProduitPaysWs.class);
        produitPaysWs.setProduit(produitWs);
        produitPaysWs.setPays(paysWs);

        return produitPaysWs;


    }

    //Cette fonction permet de convertir un objet de type ConfigProduit qu'elle prend en parametre en un objet de type "ConfigProduitWs" et le retourne
    public ConfigProduitWs createConfigProduitWs(RequestContextWs requestContext,ConfigProduit configProduit) {

        ConfigProduitWs configProduitWs = mapper.map(configProduit, ConfigProduitWs.class);

        ProduitPaysWs produitPaysWs = createProduitPaysWs(requestContext,configProduit.getProduitpays());
        configProduitWs.setProduitpays(produitPaysWs);

        TypeOperationWs typeOperationWs = mapper.map(configProduit.getTypeoperation(), TypeOperationWs.class);
        configProduitWs.setTypeoperation(typeOperationWs);

        return configProduitWs;

    }


    //Cette fonction aide a la creation des configProduit c'est une partie de la fonction createConfigProduit qui est dans la class
    // ConfigProduitService elle prend en parametre l'objet "ConfigProduitWs" et retourne un objet de type ConfigProduit
    public ConfigProduit createConfigProduit(RequestContextWs requestContext,ConfigProduitWs configProduitWs) throws ParseException {

        ConfigProduit configProduit = new ConfigProduit();
        configProduit.setPrix(configProduitWs.getPrix());
        configProduit.setDisponibilite(configProduitWs.getDisponibilite());
        configProduit.setStockable(configProduitWs.isStockable());
        configProduit.setDelailivraison(configProduitWs.getDelailivraison());
        configProduit.setDelaiprudence(configProduitWs.getDelaiprudence());
        configProduit.setPromo(configProduitWs.isPromo());
        configProduit.setSpecificite(configProduitWs.getSpecificite());
        configProduit.setUrlWebview(configProduitWs.getUrlWebview());
        configProduit.setCodesdk(configProduitWs.getCodesdk());
        configProduit.setMemoindicatif(configProduitWs.getMemoindicatif());
        configProduit.setStatutpuv(configProduitWs.getStatutpuv());
        configProduit.setQuantifiable(configProduitWs.getQuantifiable());
        configProduit.setActif(configProduitWs.getQuantifiable());

        String date = configProduitWs.getDatecreation();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        configProduit.setDatecreation(formatter.parse(date));

        ProduitPays produitPays = mapper.map(configProduitWs.getProduitpays(), ProduitPays.class);
        configProduit.setProduitpays(produitPays);

        TypeOperation typeOperation = mapper.map(configProduitWs.getTypeoperation(), TypeOperation.class);
        configProduit.setTypeoperation(typeOperation);

        return configProduit;
    }


    //Cette fonction aide pour la modification des configProduit c'est une partie de la fonction updateConfigProduit qui est dans la class
    // ConfigProduitService elle prend en parametre l'objet "ConfigProduitWs" et retourne un objet de type ConfigProduit
    public ConfigProduit updateConfigProduit(RequestContextWs requestContext,ConfigProduitWs configProduitWs) throws ParseException {


        ConfigProduit configProduit = new ConfigProduit();
        configProduit.setId(configProduitWs.getId());
        configProduit.setPrix(configProduitWs.getPrix());
        configProduit.setDisponibilite(configProduitWs.getDisponibilite());
        configProduit.setStockable(configProduitWs.isStockable());
        configProduit.setDelailivraison(configProduitWs.getDelailivraison());
        configProduit.setDelaiprudence(configProduitWs.getDelaiprudence());
        configProduit.setPromo(configProduitWs.isPromo());
        configProduit.setSpecificite(configProduitWs.getSpecificite());
        configProduit.setUrlWebview(configProduitWs.getUrlWebview());
        configProduit.setCodesdk(configProduitWs.getCodesdk());
        configProduit.setMemoindicatif(configProduitWs.getMemoindicatif());
        configProduit.setStatutpuv(configProduitWs.getStatutpuv());
        configProduit.setQuantifiable(configProduitWs.getQuantifiable());
        configProduit.setActif(configProduitWs.getQuantifiable());

        String date = configProduitWs.getDatecreation();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        configProduit.setDatecreation(formatter.parse(date));

        ProduitPays produitPays = mapper.map(configProduitWs.getProduitpays(), ProduitPays.class);
        configProduit.setProduitpays(produitPays);

        TypeOperation typeOperation = mapper.map(configProduitWs.getTypeoperation(), TypeOperation.class);
        configProduit.setTypeoperation(typeOperation);

        return configProduit;
    }

    public  String encrypt(String password, String clef){
        RSAPublicKey key = new RSAPublicKey() {
            @Override
            public BigInteger getPublicExponent() {
                return null;
            }

            @Override
            public String getAlgorithm() {
                return null;
            }

            @Override
            public String getFormat() {
                return null;
            }

            @Override
            public byte[] getEncoded() {
                return new byte[0];
            }

            @Override
            public BigInteger getModulus() {
                return null;
            }
        };
        try
        {
            byte[] keyBytes = DatatypeConverter.parseBase64Binary(clef);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            try {
                key = (RSAPublicKey) kf.generatePublic(spec);
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            }
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE,key);
            return new String(Base64.getEncoder().encode(cipher.doFinal(password.getBytes("UTF8"))));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }


    public static String decrypt(String password, String clef){
        logger.info("starting decryption");

        try
        {
            //String passwordC= Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
           // byte[] dec = Base64.getDecoder().decode(password.getBytes());
            byte[] dec = password.getBytes();
            byte[] keyBytes = DatatypeConverter.parseBase64Binary(clef);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            PrivateKey key = kf.generatePrivate(spec);
            Cipher cipher=Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE,key);
            return new String(cipher.doFinal(dec), StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

    }

    public String getPrivateKey(Integer idPartenaire,HttpServletRequest request) throws OwomiException {
        return dataServiceClient.getPrivate(idPartenaire.longValue(),request.getHeader("access-token")).getPrivateKey();
        //return dataServiceClient.getPrivate(idPartenaire).getPrivateKey();
    }

    public String createKey(Integer idPartenaire) throws OwomiException {
        PublicKeyWs key = dataServiceClient.createKey(String.valueOf(idPartenaire));
        return key.getPublicKey();
    }







}
