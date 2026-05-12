package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.MapPromoService;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1.0/map_promo")
public class MapPromoController {

    private static Logger logger = LogManager.getLogger(MapPromoController.class);

    @Autowired
    MapPromoService mapPromoService;

    @RequestMapping(method = RequestMethod.GET, value = "/produit/list")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> getListProductByCodePaysAndTypePromo(@RequestParam Integer codePays, @RequestParam String typePromo) throws OwomiException, DataNotFoundException {
        CommonResponseWs commonResponseWs = mapPromoService.getProductListByCodePaysAndTypePromo(codePays, typePromo);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/produit/listCity")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> getListProductByCodePaysAndTypePromoAndCity(@RequestParam Integer codePays, @RequestParam String typePromo, @RequestParam Integer idCity) throws OwomiException, DataNotFoundException {
        CommonResponseWs commonResponseWs = mapPromoService.getProductListByCodePaysAndTypePromoAndCity(codePays, typePromo, idCity);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }
}
