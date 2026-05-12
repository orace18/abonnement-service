package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.ProduitService;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.*;
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
@RequestMapping("v1.0/produit")
public class ProduitController {
    private static Logger logger = LogManager.getLogger(ProduitController.class);
    @Autowired
    ProduitService produitService;
    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    UserService userService;

    //Ce controller lorsqu'il est contacté appelle la fonction createProduit qui se situe dans la class ProduitService il prend
    //en parametre un Json dont les attributs sont les mêmes que l'objet "ProduitWs" et retourne l'object
    //"ProduitResponseWs" sous format Json
    @RequestMapping(method = RequestMethod.POST, value = "/create")
	//@ApiVersions({"1.0"})
    public ResponseEntity<ProduitResponseWs> createProduit(@RequestBody ProduitWs produitWs) throws AuthentificationException, DataNotFoundException, ParseException, OwomiException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        ProduitResponseWs produitResponseWs = produitService.createProduit(requestContext,produitWs);
        return new ResponseEntity<ProduitResponseWs>(produitResponseWs, HttpStatus.OK);
    }


    //Ce controller lorsqu'il est contacté appelle la fonction getProduit qui se situe dans la class ProduitService il prend
    //en parametre l'id du produit recherché dans l'URL et retourne cette derniere dans l'object "ProduitWs" sous format Json
    @RequestMapping(method = RequestMethod.GET, value = "/{id}/{idCorporate}")
    //@ApiVersions({"1.0"})
    public ResponseEntity<ProduitWs> getProduit(@PathVariable Integer id,@PathVariable("idCorporate") Integer idCorporate ) throws AuthentificationException, OwomiException, DataNotFoundException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        ProduitWs produitWs = produitService.getProduit(requestContext,id,idCorporate);
        return new ResponseEntity<ProduitWs>(produitWs, HttpStatus.OK);
    }


    //Ce controller lorsqu'il est contacté appelle la fonction getProduitList qui se situe dans la class ProduitService et
    //retourne une liste d'Object "ProduitWs" sous format Json
    @RequestMapping(method = RequestMethod.GET, value = "/list")
    //@ApiVersions({"1.0"})
    public ResponseEntity<List<ProduitWs>> getProduitList(Integer idCorporate) throws AuthentificationException, DataNotFoundException, OwomiException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        List<ProduitWs> fraisWsList = produitService.getProduitList(requestContext,idCorporate);
        return new ResponseEntity<List<ProduitWs>>(fraisWsList, HttpStatus.OK);
    }


    @RequestMapping(method = RequestMethod.GET, value = "/list/categorie")
    //@ApiVersions({"1.0"})
    public ResponseEntity<List<CategorieWs>> getCategorieList(Integer idCorporate) throws AuthentificationException, DataNotFoundException, OwomiException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        List<CategorieWs> categorieWsList = produitService.getCategorieList(requestContext,idCorporate);
        return new ResponseEntity<List<CategorieWs>>(categorieWsList, HttpStatus.OK);
    }


    //Ce controller lorsqu'il est contacté appelle la methode updateProduit qui se situe dans la class ProduitService il prend
    //en parametre un Json dont les attributs sont les même que l'objet "ProduitWs" et retourne l'object
    //"CommonResponseWs" sous format Json
    @RequestMapping(method = RequestMethod.POST, value = "/update")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> updateProduit(@RequestBody ProduitWs produitWs,Integer idCorporate) throws AuthentificationException, DataNotFoundException, ParseException, OwomiException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        produitService.updateProduit(requestContext,produitWs,idCorporate);
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }


    //Ce controller lorsqu'il est contacté appelle la methode deleteProduit qui se situe dans la class ProduitService il prend
    //en parametre l'id du produit à supprimer dans l'URL et retourne l'object "CommonResponseWs" sous format Json
    @RequestMapping(method = RequestMethod.DELETE, value = "/delete/{id}")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> deleteProduit(@PathVariable Integer id,Integer idCorporate ) throws AuthentificationException, DataNotFoundException, OwomiException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        produitService.deleteProduit(requestContext,id,idCorporate);
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }
}
