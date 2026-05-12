package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.RecipientService;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.CreateRecipientWs;
import com.owomi.commons.ws.RecipientWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("v1.0/recipient")
public class RecipientController {
    private static Logger logger = LogManager.getLogger(RecipientController.class);

    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    UserService userService;
    @Autowired
    RecipientService recipientService;
    @Autowired
    PartenaireRepository partenaireRepository;

    @PostMapping("/create")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CreateRecipientWs> createRecipient(@RequestBody CreateRecipientWs createRecipientWs) throws AuthentificationException, DataNotFoundException, OwomiException {
        logger.debug("In createRecipient ===> {}", createRecipientWs);
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        //RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CreateRecipientWs response = recipientService.createRecipent(createRecipientWs, partenaire);
        logger.debug("Out createRecipient ===> {}", response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/update")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> updateRecipient(@RequestBody CreateRecipientWs createRecipientWs ) throws AuthentificationException, DataNotFoundException, OwomiException {
        logger.debug("In ==> updateRecipient");
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        //RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        recipientService.updateRecipent(createRecipientWs, partenaire);
        logger.debug("Out ==> updateRecipient");
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        return new ResponseEntity<CommonResponseWs>(commonResponseWs, HttpStatus.OK);
    }
    @GetMapping("/list")
	//@ApiVersions({"1.0"})
    public ResponseEntity<List<RecipientWs>> getRecipientList() throws AuthentificationException, DataNotFoundException, OwomiException {
        logger.debug("In ==> updateRecipient");
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        //RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        List<RecipientWs> recipientWsList = recipientService.getRecipientList(partenaire);
        logger.debug("Out ==> updateRecipient");
        return new ResponseEntity<>(recipientWsList, HttpStatus.OK);
    }
    @GetMapping("/{id}")
    //@ApiVersions({"1.0"})
    public ResponseEntity<RecipientWs> getRecipientList(@PathVariable Integer id) throws AuthentificationException, DataNotFoundException, OwomiException {
        logger.debug("In ==> getRecipient");
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        //RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        RecipientWs recipientWs = recipientService.getRecipient(partenaire, id);
        logger.debug("Out ==> getRecipient");
        return new ResponseEntity<>(recipientWs, HttpStatus.OK);
    }
    @DeleteMapping("/{id}")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> deleteRecipient(@PathVariable Integer id) throws AuthentificationException, DataNotFoundException, OwomiException {
        logger.debug("In ==> getRecipient");
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        //RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = recipientService.deleteRecipient(partenaire, id);
        logger.debug("Out ==> getRecipient");
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }
}
