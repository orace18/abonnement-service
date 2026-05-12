package com.clubchretien.abonnement.services;

import com.owomi.commons.utils.CollectionUtils;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.commons.ws.TypeCorporateWs;
import com.owomi.database.entities.TypeCorporate;
import com.owomi.database.repositories.TypeCorporateRepository;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Component
public class TypeCorporateService {
    @Autowired
    TypeCorporateRepository typeCorporateRepository;
    @Autowired
    Mapper mapper;
    @Transactional
    public List<TypeCorporateWs> list(RequestContextWs requestContext){
        List<TypeCorporate> typeCorporateList = typeCorporateRepository.findAll();
        List<TypeCorporateWs> typeCorporateWsList = new ArrayList<>();
        if(CollectionUtils.checkIfCollectionIsNullOrEmpty(typeCorporateList))
            return typeCorporateWsList;
        for (TypeCorporate typeCorporate:typeCorporateList){
            TypeCorporateWs typeCorporateWs = mapper.map(typeCorporate, TypeCorporateWs.class);
            typeCorporateWsList.add(typeCorporateWs);
        }
        return typeCorporateWsList;
    }
}
