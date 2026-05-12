package com.clubchretien.abonnement.services;

import com.owomi.commons.enums.ProduitStatusEnums;
import com.owomi.commons.enums.TypePromoEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.EntityUtils;
import com.owomi.commons.ws.*;
import com.owomi.database.entities.BoutiqueMarchand;
import com.owomi.database.entities.BoutiqueShopizer;
import com.owomi.database.entities.Corporate;
import com.owomi.database.entities.MapPromo;
import com.owomi.database.repositories.*;
import com.owomi.rest.client.shopizerService.ShopizerServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MapPromoService {

    private static Logger logger = LogManager.getLogger(MapPromoService.class);

    @Autowired
    MapPromoRepository mapPromoRepository;

    @Autowired
    BoutiqueMarchandRepository boutiqueMarchandRepository;

    @Autowired
    BoutiqueShopizerRepository boutiqueShopizerRepository;

    @Autowired
    CorporateRepository corporateRepository;

    @Autowired
    Mapper mapper;

    @Autowired
    ShopizerServiceClient shopizerServiceClient;

    @Autowired
    PromoRepository promoRepository;

//    BoutiqueMarchand boutiqueMarchand = boutiqueMarchandRepository.findOneById(idMarchand);
//
//    BoutiqueMarchandWs boutiqueMarchandWs = new BoutiqueMarchandWs();
//        boutiqueMarchandWs.setId(idMarchand);
//        boutiqueMarchandWs.setCaracteristique(boutiqueMarchand.getCaracteristique());
//        boutiqueMarchandWs.setIdVille(boutiqueMarchand.getIdVille());
//        boutiqueMarchandWs.setNom(boutiqueMarchand.getNom());
//        boutiqueMarchandWs.setPhoto(boutiqueMarchand.getPhoto());
//        boutiqueMarchandWs.setPrix(boutiqueMarchand.getPrix());
//    Corporate corporate = corporateRepository.findOneById(boutiqueMarchand.getCorporate().getId());
//    CorporateWs corporateWs = mapper.map(corporate, CorporateWs.class);
//        boutiqueMarchandWs.setCorporate(corporateWs);
//    BoutiqueShopizer boutiqueShopizer = boutiqueShopizerRepository.findOneById(boutiqueMarchand.getBoutiqueShopizer().getId());
//    BoutiqueShopizerWs boutiqueShopizerWs = mapper.map(boutiqueShopizer, BoutiqueShopizerWs.class);
//        boutiqueMarchandWs.setBoutiqueShopizer(boutiqueShopizerWs);
//        return boutiqueMarchandWs;


    public CommonResponseWs getProductListByCodePaysAndTypePromo(Integer codePays, String typePromo) throws OwomiException, DataNotFoundException {
        CommonResponseWs response = new CommonResponseWs();
        MapPromo mapPromo = mapPromoRepository.findByCodePaysAndTypePromo(codePays, typePromo);
        EntityUtils.checkIfEntityIsNull(MapPromo.class,mapPromo,"codePays and typePromo",typePromo);
        ProduitShopizerListWs produitShopizerListWs = shopizerServiceClient.productListByCategory(mapPromo.getStore(),mapPromo.getIdCategorie());
        this.filterProductsLite(produitShopizerListWs);
        response.setData(produitShopizerListWs);
        response.setStatus(WsOperationResponseStatus.success);
        response.setCode(HttpStatus.OK.value());
        return response;
    }

    public CommonResponseWs getProductListByCodePaysAndTypePromoAndCity(Integer codePays, String typePromo, Integer idCity) throws OwomiException, DataNotFoundException {
        CommonResponseWs response = new CommonResponseWs();
        MapPromo mapPromo = mapPromoRepository.findByCodePaysAndTypePromoAndWorldCities_Id(codePays, typePromo, idCity);
        EntityUtils.checkIfEntityIsNull(MapPromo.class,mapPromo,"codePays and typePromo",typePromo);
        ProduitShopizerListWs produitShopizerListWs = shopizerServiceClient.productListByCategory(mapPromo.getStore(),mapPromo.getIdCategorie());
        this.filterProductsLite(produitShopizerListWs);
        response.setData(produitShopizerListWs);
        response.setStatus(WsOperationResponseStatus.success);
        response.setCode(HttpStatus.OK.value());
        return response;
    }

    public void filterProductsLite (ProduitShopizerListWs produitShopizerListWs) throws OwomiException {
        List<Integer> marchandIds = produitShopizerListWs.getProducts().stream().map(ProduitShopizer::getMarchand).distinct().filter(Objects::nonNull).collect(Collectors.toList());
        logger.debug("liste des marchands " + marchandIds.toString());
        logger.debug("liste des marchands " + marchandIds);
        Map<Integer, BoutiqueMarchand> marchandMap = null;
        if(!marchandIds.isEmpty()){
            marchandMap = boutiqueMarchandRepository.findAllByIdIn(marchandIds).stream().collect(Collectors.toMap(BoutiqueMarchand::getId, Function.identity()));
        }

        List<ProduitShopizer> produitShopizerList = new ArrayList<>();
        for(ProduitShopizer produitShopizer : produitShopizerListWs.getProducts()){
            if(produitShopizer.getDescription().getHighlights()!=null
                    && produitShopizer.getProductWeight() != null
                    && produitShopizer.getProductWeight().doubleValue() != 0d
                    && produitShopizer.getMarchand() != null
                    && produitShopizer.getMarchand().intValue() != 0
                    && produitShopizer.getCommission() != null
                    && produitShopizer.getCommission().doubleValue() != 0d
                    && (produitShopizer.getDescription().getHighlights().equals(ProduitStatusEnums.A_CHERCHER.getValue())
                    || produitShopizer.getDescription().getHighlights().equals(ProduitStatusEnums.A_DEPOSER.getValue())
                    || produitShopizer.getDescription().getHighlights().equals(ProduitStatusEnums.SAUF_PRESENTIEL.getValue())
                    || produitShopizer.getDescription().getHighlights().equals(ProduitStatusEnums.APPROVISIONNEMENT_DIRECT.getValue())
                    || produitShopizer.getDescription().getHighlights().equals(ProduitStatusEnums.NON_DISPONIBLE.getValue())
                    || produitShopizer.getDescription().getHighlights().equals(ProduitStatusEnums.DISPONIBLE.getValue())
            )) {
                BoutiqueMarchand boutiqueMarchand = marchandMap.get(produitShopizer.getMarchand());
                BoutiqueMarchandWs marchandWs = (boutiqueMarchand != null ? mapper.map(boutiqueMarchand, BoutiqueMarchandWs.class) : null);
                if (produitShopizer.getDescription().getHighlights().equals(ProduitStatusEnums.SAUF_PRESENTIEL.getValue())) {
                    produitShopizer.setBoutiqueMarchand(marchandWs);
                    produitShopizer.setInfo(ProduitStatusEnums.SAUF_PRESENTIEL.getValue());
                    produitShopizerList.add(produitShopizer);
                } else if ((produitShopizer.getDescription().getHighlights().equals(ProduitStatusEnums.A_CHERCHER.getValue()) || produitShopizer.getDescription().getHighlights().equals(ProduitStatusEnums.A_DEPOSER.getValue())) && produitShopizer.getQuantity() != 0) {
                    produitShopizer.setBoutiqueMarchand(marchandWs);
                    produitShopizer.setInfo(produitShopizer.getQuantity().toString());
                    produitShopizerList.add(produitShopizer);
                } else if ((produitShopizer.getDescription().getHighlights().equals(ProduitStatusEnums.A_CHERCHER.getValue()) || produitShopizer.getDescription().getHighlights().equals(ProduitStatusEnums.A_DEPOSER.getValue())) && produitShopizer.getQuantity() == 0) {
                    produitShopizer.setBoutiqueMarchand(marchandWs);
                    produitShopizer.setInfo("A confirmer");
                    produitShopizerList.add(produitShopizer);
                } else if (produitShopizer.getDescription().getHighlights().equals(ProduitStatusEnums.DISPONIBLE.getValue())) {
                    produitShopizer.setBoutiqueMarchand(marchandWs);
                    produitShopizer.setInfo(produitShopizer.getQuantity().toString());
                    produitShopizerList.add(produitShopizer);
                } else if (produitShopizer.getQuantity() != 0 && !produitShopizer.getDescription().getHighlights().equals(ProduitStatusEnums.NON_DISPONIBLE.getValue())) {
                    produitShopizer.setBoutiqueMarchand(marchandWs);
                    produitShopizer.setInfo(produitShopizer.getQuantity().toString());
                    produitShopizerList.add(produitShopizer);
                }
            }
        }

        produitShopizerListWs.setProducts(produitShopizerList);
        if(!marchandIds.isEmpty()) this.applyPromoIfIsAvailable(produitShopizerListWs);
    }

    public void applyPromoIfIsAvailable(ProduitShopizerListWs produitShopizerListWs) throws OwomiException {
        Set<String> stores = produitShopizerListWs.getProducts().stream()
                .map(p -> p.getManufacturer().getCode())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<PrixAchatWs> promos = new ArrayList<>();
        if (!stores.isEmpty()) {
            promoRepository.findAllByDateFinPromoAfterAndTypePromoAndPrixAchat_StoreIn(new Date(), TypePromoEnums.PROMO_PRIX_ACHAT.toString(), stores).stream()
                    .map(p -> {
                        PrixAchatWs prixAchatWs = mapper.map(p.getPrixAchat(), PrixAchatWs.class);
                        prixAchatWs.setProductList(Arrays.stream(p.getPrixAchat().getProducts().split(",")).filter(s -> !s.isEmpty()).map(Integer::valueOf).collect(Collectors.toList()));
                        return prixAchatWs;
                    })
                    .collect(Collectors.toList());
        }
        List<ProduitShopizer> products = produitShopizerListWs.getProducts().stream()
                .map(produit -> {
                    Optional<Double> reduction = promos.stream()
                            .filter(promo -> promo.getProductList().contains(produit.getId()))
                            .findFirst()
                            .map(PrixAchatWs::getReduction);
                    produit.setPromoIsOn(reduction.isPresent());
                    produit.setPromotionalPrice(produit.getPrice() - (produit.getPrice() * reduction.orElse(0.0d)));
                    return produit;
                })
                .collect(Collectors.toList());

        produitShopizerListWs.setProducts(products);
    }


    private BoutiqueMarchandWs getBoutiqueMarchand(Integer idMarchand) {
        logger.debug("id marchand ============ " + idMarchand);
        BoutiqueMarchand boutiqueMarchand = boutiqueMarchandRepository.findOneById(idMarchand);

        BoutiqueMarchandWs boutiqueMarchandWs = new BoutiqueMarchandWs();
        boutiqueMarchandWs.setId(idMarchand);
        boutiqueMarchandWs.setCaracteristique(boutiqueMarchand.getCaracteristique());
        boutiqueMarchandWs.setIdVille(boutiqueMarchand.getIdVille());
        boutiqueMarchandWs.setNom(boutiqueMarchand.getNom());
        boutiqueMarchandWs.setPhoto(boutiqueMarchand.getPhoto());
        boutiqueMarchandWs.setPrix(boutiqueMarchand.getPrix());
        Corporate corporate = corporateRepository.findOneById(boutiqueMarchand.getCorporate().getId());
        CorporateWs corporateWs = mapper.map(corporate, CorporateWs.class);
        boutiqueMarchandWs.setCorporate(corporateWs);
        BoutiqueShopizer boutiqueShopizer = boutiqueShopizerRepository.findOneById(boutiqueMarchand.getBoutiqueShopizer().getId());
        BoutiqueShopizerWs boutiqueShopizerWs = mapper.map(boutiqueShopizer, BoutiqueShopizerWs.class);
        boutiqueMarchandWs.setBoutiqueShopizer(boutiqueShopizerWs);
        return boutiqueMarchandWs;
    }

}
