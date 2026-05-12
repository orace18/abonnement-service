package com.clubchretien.abonnement.core.sliders.services;

import com.clubchretien.abonnement.core.sliders.ws.MobileSliderCreateWs;
import com.clubchretien.abonnement.ws.SlidersWs;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.SliderWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.entities.Pays;
import com.owomi.database.entities.Slider;
import com.owomi.database.repositories.PaysRepository;
import com.owomi.database.repositories.SliderRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class MobileSliderService {

    private static Logger logger = LogManager.getLogger(MobileSliderService.class);

    @Autowired
    SliderRepository sliderRepository;
    @Autowired
    Mapper mapper;
    @Autowired
    PaysRepository paysRepository;


    public CommonResponseWs listSlider(Partenaire partenaire, Integer max) {
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 401, null);
        Pageable pageable =  PageRequest.of(0, max);
        Pays pays = partenaire.getPays();
        Page<Slider> entities = sliderRepository.findAllByPaysAndStatutOrderByIdDesc(pays, "enabled", pageable);
        List<Slider> list = entities.getContent();
        if (list.size() < max) {
            int remains = max - entities.getContent().size();
            pageable =  PageRequest.of(0, remains);
            pays = paysRepository.findOneByCode("INTER");
            entities = sliderRepository.findAllByPaysAndStatutOrderByIdDesc(pays, "enabled", pageable);
            if (!entities.getContent().isEmpty()) list.addAll(entities.getContent());
        }
        pageable = PageRequest.of(0, max);
        List<SliderWs> content = list.stream().map(this::convertEntityToWs).collect(Collectors.toList());
        Page<SliderWs> result = new PageImpl<>(content, pageable, max);
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
    }

    private SlidersWs convertEntityToWs(Slider entity) {
        if (entity == null) return null;
        SlidersWs result = this.mapper.map(entity, SlidersWs.class);
        return result;
    }

    public CommonResponseWs createSlider(MobileSliderCreateWs ws) {
        logger.debug("slider inputs is ====> " + ws);
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        Slider entity = new Slider();
        Pays pays = paysRepository.findOneByIndicatif(ws.getCountryPhoneCode());
        entity.setPays(pays);
        entity.setUrlImage(ws.getImage().getUrl());
        entity.setCreatedDate(new Date());
        entity.setIdAction(null);
        entity.setTypeAction("NO ACTION");
        entity.setStatut("enabled");
        entity = sliderRepository.save(entity);
        SlidersWs result = this.convertEntityToWs(entity);
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
    }

}
