package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.TypeCorporateService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.commons.ws.TypeCorporateWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("v1.0/typeCorporate")
public class TypeCorporateController {
    private static Logger logger = LogManager.getLogger(TypeCorporateController.class);
    @Autowired
    TypeCorporateService typeCorporateService;
    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    UserService userService;

    @GetMapping("/list") // type corporate list
	//@ApiVersions({"1.0"})
    public ResponseEntity<List<TypeCorporateWs>> list() throws AuthentificationException, DataNotFoundException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        List<TypeCorporateWs> typeCorporateWsList  = typeCorporateService.list(requestContext);
        HttpStatus httpStatus = HttpStatus.OK;
        return new ResponseEntity<>(typeCorporateWsList, httpStatus);
    }
}
