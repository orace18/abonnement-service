package com.clubchretien.abonnement.controllers.v1;


import com.clubchretien.abonnement.services.BanqueService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.BanqueWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("v1.0/banque")
public class BanqueController {

    private static Logger logger = LogManager.getLogger(BanqueController.class);


    @Autowired
    BanqueService banqueService;
    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    UserService userService;
    @Autowired
    PartenaireRepository partenaireRepository;



    //Ce controller lorsqu'il est contacté appelle la fonction getBanque qui se situe dans la class BanqueService il prend
    //en parametre l'id de la banque recherché dans l'URL et retourne cette derniere dans l'object "BanqueWs" sous format Json
    @RequestMapping(method = RequestMethod.GET, value = "/{id}")
	//@ApiVersions({"1.0"})
    public ResponseEntity<BanqueWs> getBanque(@PathVariable Integer id ) throws AuthentificationException, OwomiException {
        logger.debug("In ==> getBanque" + id);
        BanqueWs banqueWs = banqueService.getBanque(id);
        logger.debug("Out ==> getBanque" + id);
        return new ResponseEntity<BanqueWs>(banqueWs, HttpStatus.OK);
    }


    //Ce controller lorsqu'il est contacté appelle la fonction getBanqueList qui se situe dans la class BanqueService et
    //retourne une liste d'Object "BanqueWs" sous format Json
    @RequestMapping(method = RequestMethod.GET, value = "/list")
	//@ApiVersions({"1.0"})
    public ResponseEntity<List<BanqueWs>> getBanqueList() throws AuthentificationException, DataNotFoundException {
        logger.debug("In ==> getBanqueList");
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        List<BanqueWs> banqueWsList = banqueService.getListBanque(partenaire);
        logger.debug("Out ==> getBanqueList");
        return new ResponseEntity<List<BanqueWs>>(banqueWsList, HttpStatus.OK);
    }


}
