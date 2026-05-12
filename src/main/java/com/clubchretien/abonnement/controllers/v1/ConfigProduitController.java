package com.clubchretien.abonnement.controllers.v1;


import com.clubchretien.abonnement.services.ConfigProduitService;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.ConfigProduitResponseWs;
import com.owomi.commons.ws.ConfigProduitWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping("v1.0/configProduit")
public class ConfigProduitController {

    private static Logger logger = LogManager.getLogger(ConfigProduitController.class);

    @Autowired
    ConfigProduitService configProduitService;
    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    UserService userService;

    //Ce controller lorsqu'il est contacté appelle la fonction createConfigProduit qui se situe dans la class ConfigProduitService il prend
    //en parametre un Json dont les attributs sont les mêmes que l'objet "ConfigProduitWs" et retourne l'object
    //"ConfigProduitResponseWs" sous format Json
    @RequestMapping(method = RequestMethod.POST, value = "/create")
	//@ApiVersions({"1.0"})
    public ResponseEntity<ConfigProduitResponseWs> createConfigFrais(@RequestBody ConfigProduitWs configProduitWs) throws AuthentificationException, DataNotFoundException, ParseException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        ConfigProduitResponseWs configProduitResponseWs = configProduitService.createConfigProduit(requestContext,configProduitWs);
        return new ResponseEntity<ConfigProduitResponseWs>(configProduitResponseWs, HttpStatus.OK);
    }


    //Ce controller lorsqu'il est contacté appelle la fonction getConfigProduit qui se situe dans la class ConfigProduitService il prend
    //en parametre l'id du configProduit recherché dans l'URL et retourne cette derniere dans l'object "ConfigProduitWs" sous format Json
    @RequestMapping(method = RequestMethod.GET, value = "/{id}")
	//@ApiVersions({"1.0"})
    public ResponseEntity<ConfigProduitWs> getConfigFrais(@PathVariable Integer id ) throws AuthentificationException, OwomiException, DataNotFoundException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        ConfigProduitWs configProduitWs = configProduitService.getConfigProduit(requestContext,id);
        return new ResponseEntity<ConfigProduitWs>(configProduitWs, HttpStatus.OK);
    }


    //Ce controller lorsqu'il est contacté appelle la fonction getConfigProduitList qui se situe dans la class ConfigProduitService et
    //retourne une liste d'Object "ConfigProduitWs" sous format Json
    @RequestMapping(method = RequestMethod.GET, value = "/list")
	//@ApiVersions({"1.0"})
    public ResponseEntity<List<ConfigProduitWs>> getConfigFraisList() throws AuthentificationException, DataNotFoundException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        List<ConfigProduitWs> configProduitWsListt = configProduitService.getConfigProduitList(requestContext);
        return new ResponseEntity<List<ConfigProduitWs>>(configProduitWsListt, HttpStatus.OK);
    }


    //Ce controller lorsqu'il est contacté appelle la methode updateConfigProduit qui se situe dans la class ConfigProduitService il prend
    //en parametre un Json dont les attributs sont les même que l'objet "ConfigProduitWs" et retourne l'object
    //"CommonResponseWs" sous format Json
    @RequestMapping(method = RequestMethod.POST, value = "/update")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> updateConfigFrais(@RequestBody ConfigProduitWs configProduitWs) throws AuthentificationException, DataNotFoundException, ParseException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        configProduitService.updateConfigProduit(requestContext,configProduitWs);
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        return new ResponseEntity<CommonResponseWs>(commonResponseWs, HttpStatus.OK);
    }


    //Ce controller lorsqu'il est contacté appelle la methode deleteConfigProduit qui se situe dans la class ConfigProduitService il prend
    //en parametre l'id du configProduit à supprimer dans l'URL et retourne l'object "CommonResponseWs" sous format Json
    @RequestMapping(method = RequestMethod.DELETE, value = "/delete/{id}")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> deleteConfigFrais(@PathVariable Integer id ) throws AuthentificationException, DataNotFoundException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        configProduitService.deleteConfigProduit(requestContext,id);
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        return new ResponseEntity<CommonResponseWs>(commonResponseWs, HttpStatus.OK);
    }



}
