package com.clubchretien.abonnement.controllers.v2;

import com.clubchretien.abonnement.services.MarchandService;
import com.clubchretien.abonnement.services.PartenaireService;
import com.owomi.commons.exceptions.*;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.PermissionResponseWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("v2.0/marchand")
public class MarchandController {

    private static Logger logger = LogManager.getLogger(MarchandController.class);
    @Autowired
    UserService userService;
    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    PartenaireService partenaireService;
    @Autowired
    MarchandService marchandService;


    @PostMapping("/permission/marchand/response")
	//@ApiVersions({"2.0"})
    public ResponseEntity<CommonResponseWs> responsePermissionMarchand(@RequestBody PermissionResponseWs permissionResponseWs, @RequestParam String pin) throws DataNotFoundException, NotificationException, WesternSmsException, OwomiException, AuthentificationException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        CommonResponseWs commonResponseWs = marchandService.responsePermissionMarchand(permissionResponseWs, pin, partenaire);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }


}
