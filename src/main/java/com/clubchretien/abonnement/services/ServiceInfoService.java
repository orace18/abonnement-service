package com.clubchretien.abonnement.services;

import com.owomi.commons.enums.StatusSpecPartenaire;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.ResponseWs;
import com.owomi.database.entities.SpecialPartenaire;
import com.owomi.database.repositories.SpecialPartenaireRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ServiceInfoService {
    @Autowired
    private SpecialPartenaireRepository repository;

    /**
     * check user
     * TODO
     *  1 search user by email in special_partenaire
     *      if user not exist
     *          return :
     *              code: 400
     *              status: not_found
     *              message: not special partenaire
     *      else:
     *          return:
     *              code: 301
     *              status: PIN_MANQUANT
     *              message: special user found \n
     *                      he/her must reset the pin
     *
     * @param email
     * @return
     */
    public ResponseWs checkUserInfo(String email){
        ResponseWs responseWs = new ResponseWs();
        SpecialPartenaire partenaire = repository.findSpecialPartenaireByEmail(email);
        if(partenaire ==null ){
            responseWs.setCode(400);
            responseWs.setMsg("not special partenaire");
            responseWs.setStatus(StatusSpecPartenaire.ras.toString());
            responseWs.setData(email);
            return responseWs;
        }else if (partenaire.getStatus().equalsIgnoreCase(StatusSpecPartenaire.finaliser.toString())){
            responseWs.setStatus(StatusSpecPartenaire.ras.toString());
            responseWs.setCode(301);
            responseWs.setData(partenaire);
            return responseWs;
        }
        else {
            responseWs.setStatus(partenaire.getStatus());
            responseWs.setCode(301);
            responseWs.setData(partenaire);
            return responseWs;
        }
    }

    public ResponseWs updateUser(String email){
        ResponseWs responseWs = new ResponseWs();
       SpecialPartenaire specialPartenaire = repository.findSpecialPartenaireByEmail(email);
        if(specialPartenaire == null){
            responseWs.setCode(701);
            responseWs.setMsg("not special partenaire");
            responseWs.setStatus("not_found");
            responseWs.setData(email);
            return responseWs;
        }else {
            if(specialPartenaire.getStatus().equalsIgnoreCase(StatusSpecPartenaire.finaliser.toString())) {
                responseWs.setCode(702);
                responseWs.setStatus(StatusSpecPartenaire.finaliser.toString());
                responseWs.setData(email);
                return responseWs;
            }else {
                if (specialPartenaire.getSolde() != 0.0) {
                    CommonResponseWs res = ajusterSolde(specialPartenaire.getTelephone(), specialPartenaire.getSolde());
                    if (res.getStatus() ==  WsOperationResponseStatus.success) {
                        specialPartenaire.setStatus(StatusSpecPartenaire.finaliser.toString());
                        repository.save(specialPartenaire);
                        responseWs.setData(specialPartenaire);
                        responseWs.setStatus(specialPartenaire.getStatus());
                        responseWs.setCode(703);
                        responseWs.setMsg("partenaire updated");
                        return responseWs;
                    }
                    responseWs.setCode(res.getCode());
                    responseWs.setStatus("");
                    return responseWs;
                }else {
                    specialPartenaire.setStatus(StatusSpecPartenaire.finaliser.toString());
                    repository.save(specialPartenaire);
                    responseWs.setData(specialPartenaire);
                    responseWs.setStatus(specialPartenaire.getStatus());
                    responseWs.setCode(703);
                    responseWs.setMsg("partenaire updated");
                    return responseWs;
                }
            }
        }
    }

    private CommonResponseWs ajusterSolde(String telephone, Double solde) {
        final String uri = "http://10.3.100.4:6001/data/service/v1.0/operation/reajust?matricule="+telephone+"&newSolde="+solde;
        //matricule=22961879613&newSolde=7540
        RestTemplate restTemplate = new RestTemplate();
        CommonResponseWs result = restTemplate.getForObject(uri, CommonResponseWs.class);
        return result;
    }
}
