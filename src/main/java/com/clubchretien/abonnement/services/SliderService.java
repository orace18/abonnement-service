package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.i18n.I18nCommonsFactory;
import com.clubchretien.abonnement.i18n.I18nFactory;
import com.clubchretien.abonnement.utils.ActivityLogUtils;
import com.clubchretien.abonnement.ws.SlidersWs;
import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.EntityUtils;
import com.owomi.commons.ws.*;
import com.owomi.database.entities.*;
import com.owomi.database.repositories.*;
import com.owomi.file.manager.services.FileService;
import com.owomi.rest.client.shopizerService.ShopizerServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;


@Component
public class SliderService {

    private static Logger logger = LogManager.getLogger(SliderService.class);

    @Autowired
    SliderRepository sliderRepository;
    @Autowired
    WorldCitiesRepository worldCitiesRepository;
    @Autowired
    ProduitRepository produitRepository;
    @Autowired
    Mapper mapper;
    @Autowired
    CategorieRepository categorieRepository;
    @Autowired
    ActivityLogRepository activityLogRepository;
    @Autowired
    PaysRepository paysRepository;
    @Autowired
    PartenaireRepository partenaireRepository;
    @Autowired
    FileService fileService;
    @Autowired
    ActivityLogService activityLogService;
    @Autowired
    ActivityLogUtils activityLogUtils;
    @Autowired
    I18nFactory i18n;
    @Autowired
    I18nCommonsFactory i18nCommons;
    @Autowired
    ShopizerServiceClient shopizerServiceClient;
    @Autowired
    MapPromoService mapPromoService;


    @Transactional
    public CommonResponseWs getPartenaireSliders(RequestContextWs requestContextWs, Partenaire partenaire) throws DataNotFoundException {
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        Pageable pageable =  PageRequest.of(0, 10);
        Pays pays = partenaire.getPays();
        Pays paysInter = paysRepository.findOneByCode("INTER");

        List<Slider> sliderPaysList = sliderRepository.findByPaysAndStatutOrderByIdDesc(pays, "enabled", pageable);
        List<Slider> sliderInterList = sliderRepository.findByPaysAndStatutOrderByIdDesc(paysInter, "enabled", pageable);

        List<Slider> sliderToReturnList = new ArrayList<>();
        int paysSize = sliderPaysList.size();
        logger.debug(paysSize);
        int interSize = sliderInterList.size();
        logger.debug(interSize);

        for (int i = 0; i < 5; i++) {
            if (sliderToReturnList.size() == 5) break;
            if (paysSize != 0) {
                Slider slider = sliderPaysList.get(0);
                paysSize--;
                sliderPaysList.remove(0);
                sliderToReturnList.add(slider);
            }
            if (sliderToReturnList.size() == 5) break;
            if (interSize != 0) {
                Slider slider = sliderInterList.get(0);
                interSize--;
                sliderInterList.remove(0);
                sliderToReturnList.add(slider);
            }

        }
        List<SlidersWs> sliderToreturnWsList = new ArrayList<>();
        for (Slider slider : sliderToReturnList){
            SlidersWs slidersWs = new SlidersWs();
            slidersWs.setId(slider.getId());
            slidersWs.setStatut(slider.getStatut());
            slidersWs.setCreatedDate(slider.getCreatedDate());
            slidersWs.setIdAction(slider.getIdAction());
            slidersWs.setTypeAction(slider.getTypeAction());
            slidersWs.setUrlImage(slider.getUrlImage());
            slidersWs.setIdPays(slider.getPays().getId());
            sliderToreturnWsList.add(slidersWs);
        }

        commonResponseWs.setMessage(i18n.internationalize("partenaire.sliders.message"));
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        commonResponseWs.setCode(200);
        commonResponseWs.setData(sliderToreturnWsList);
        return commonResponseWs;
    }

    @Transactional
    public Resource getSliderImage(RequestContextWs requestContextWs,Integer idSlider) throws DataNotFoundException, OwomiException {
        Slider slider = sliderRepository.getById(idSlider);
        EntityUtils.checkIfEntityIsNull(Slider.class, slider, slider.getId());
        if (slider.getUrlImage() == null) throw new OwomiException(i18n.internationalize("slider.image.not.found.ex.message"), ErrorCodeEnums.IMAGE_NOT_FOUND," ");
        String url = slider.getUrlImage();
        Resource resource = fileService.loadFileAsResource(url);
        return resource;
    }

   /* @Transactional
    public CommonResponseWs getPartenaireSliders(RequestContextWs requestContextWs, String telephone, int page, int size) throws DataNotFoundException {
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        Pageable pageable = new PageRequest(page, size);
        Partenaire partenaire = partenaireRepository.findOneByTelephone(telephone);
        EntityUtils.checkIfEntityIsNull(Partenaire.class, partenaire, partenaire.getId());
        Pays pays = partenaire.getPays();
        Pays paysInter = paysRepository.findOneByCode("INTER");
        EntityUtils.checkIfEntityIsNull(Pays.class, pays, pays.getId());

        List<Slider> sliderPaysList = sliderRepository.findByPaysOrderByIdDesc(pays, pageable);
        Integer[] sliderPaysId = new Integer[0];
        Integer[] sliderInterId = new Integer[0];
        Integer i = 0;
        for (Slider slider : sliderPaysList) {
            if (slider.getStatut().equalsIgnoreCase("enabled")) {
                Integer id = slider.getId();
                sliderPaysId[i] = id;
                i++;
            }
        }
        List<Slider> sliderInterList = sliderRepository.findByPaysOrderByIdDesc(paysInter, pageable);
        for (Slider sliderInter : sliderInterList) {
            if (sliderInter.getStatut().equalsIgnoreCase("enabled")) {
                Integer id = sliderInter.getId();
                sliderInterId[i] = id;
                i++;

            }
        }
        List<Integer> list = sliderWsList(sliderPaysId, sliderInterId);
        List<SliderWs> sliderList = new ArrayList<>();
        for (Integer id : list){
            Slider slider = sliderRepository.findOneById(id);
            SliderWs sliderWs = mapper.map(slider, SliderWs.class);
            sliderList.add(sliderWs);
        }
        commonResponseWs.setMessage("liste des sliders du partenaire récupérer avec succès");
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        commonResponseWs.setCode(200);
        commonResponseWs.setData(sliderList);
        return commonResponseWs;
    }

    public static List<Integer> sliderWsList (Integer [] sliderPaysId, Integer [] sliderInterId) {
        int i = 0;
        List<Integer> toreturn = new LinkedList<>();
        for (i = 0; i < 5; i++){
            toreturn.add(sliderPaysId[i]);
            toreturn.add(sliderInterId[i]);
        if (toreturn.size() < 5) {
            i++;
        }
    }
        return toreturn;
    }
*/


    //Méthode pour la gestion des action des sliders
    @Transactional
    public CommonResponseWs readSliderAction(RequestContextWs requestContextWs, Partenaire partenaire, String typeAction, Integer idAction) throws DataNotFoundException, OwomiException {
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        Pays pays = partenaire.getPays();
        Pays payInter = paysRepository.findOneByCode("INTER");
        EntityUtils.checkIfEntityIsNull(Pays.class, pays, pays.getId());
        List<Slider> sliderList = sliderRepository.findByPaysOrPays(pays, payInter);
        for (Slider slider : sliderList){
                if (slider.getTypeAction().equals(typeAction) && slider.getIdAction().equals(idAction) && typeAction.equalsIgnoreCase("Détails produit")) {
                    ProduitShopizer produitShopizer =  shopizerServiceClient.getProductById(idAction,slider.getStore());
                    commonResponseWs.setStatus(WsOperationResponseStatus.success);
                    commonResponseWs.setCode(200);
                    logger.debug(produitShopizer);
                    commonResponseWs.setData(produitShopizer);
                } else if (slider.getTypeAction().equals(typeAction) && slider.getIdAction().equals(idAction) && typeAction.equalsIgnoreCase("Détails catégorie produit")) {
                    ProduitShopizerListWs produitShopizerListWs = shopizerServiceClient.productListByCategoryPagined(slider.getStore(),idAction,0,30);
                    mapPromoService.filterProductsLite(produitShopizerListWs);
                    commonResponseWs.setStatus(WsOperationResponseStatus.success);
                    commonResponseWs.setCode(200);
                    commonResponseWs.setData(produitShopizerListWs);
                } else if (slider.getTypeAction().equals(typeAction) && slider.getIdAction().equals(idAction) && typeAction.equalsIgnoreCase("Activité logs")) {
                    ActivityLog activityLog = activityLogRepository.getById(idAction);
                    EntityUtils.checkIfEntityIsNull(ActivityLog.class, activityLog, activityLog.getId());
                    ActivityLogWs activityLogWs = mapper.map(activityLog, ActivityLogWs.class);
                    activityLogUtils.readableActivityLog(activityLogWs);
                    logger.debug(activityLogWs);
                    commonResponseWs.setStatus(WsOperationResponseStatus.success);
                    commonResponseWs.setCode(200);
                    commonResponseWs.setData(activityLogWs);
                } else {
                    commonResponseWs.setStatus(WsOperationResponseStatus.success);
                    commonResponseWs.setCode(200);
                    commonResponseWs.setData(null);
                }
        }
        return commonResponseWs;
    }

    public CommonResponseWs readActivityLogsForEveryone(RequestContextWs requestContextWs){
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        Partenaire theActivityPartenaire = partenaireRepository.getById(4);
        List<ActivityLog> activityLogList = activityLogRepository.findByActeurAndDestinateur(theActivityPartenaire, theActivityPartenaire);
        List<ActivityLogWs> activityLogWsList = new ArrayList<>();
        for (ActivityLog activityLog : activityLogList){
            activityLogUtils.readableActivityLog(activityLog);
            ActivityLogWs activityLogWs = mapper.map(activityLog, ActivityLogWs.class);
            activityLogWsList.add(activityLogWs);
        }
        commonResponseWs.setMessage(i18n.internationalize("read.activity.log.message"));
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        commonResponseWs.setCode(200);
        commonResponseWs.setData(activityLogWsList);
        return commonResponseWs;
    }

    public CommonResponseWs getTwoSliderByCategorie (Integer cityId,Integer page, Integer pageSize)
    {
        Pays payInter = paysRepository.findOneByCode("INTER");
        WorldCities city = worldCitiesRepository.getById(cityId);
        Pageable pageable =  PageRequest.of(page,pageSize);
        SliderListWs sliderListWs = new SliderListWs();
        List<Slider> citySlider = sliderRepository.findAllByWorldCities_IdOrderByCreatedDateDesc(cityId,pageable);
        if (!citySlider.isEmpty())
        {
            sliderListWs.setCityOneSlider(citySlider.get(0).getId());
            sliderListWs.setCityTwoSlider(citySlider.get(1).getId());
        }
        List<Slider> paysSlider = sliderRepository.findAllByWorldCities_PaysOrderByCreatedDateDesc(city.getPays(),pageable);
        if(!paysSlider.isEmpty())
        {
            sliderListWs.setCountryOneSlider(paysSlider.get(0).getId());
            sliderListWs.setCountryTwoSlider(paysSlider.get(1).getId());
        }
        List<Slider> interSlider = sliderRepository.findAllByWorldCities_PaysOrderByCreatedDateDesc(payInter.getId(),pageable);
        if(!paysSlider.isEmpty())
        {
            sliderListWs.setInternationalOneSlider(interSlider.get(0).getId());
            sliderListWs.setInternationalTwoSlider(interSlider.get(1).getId());
        }
        return new CommonResponseWs(WsOperationResponseStatus.success,sliderListWs,null,null,null,null,null);
    }

}
