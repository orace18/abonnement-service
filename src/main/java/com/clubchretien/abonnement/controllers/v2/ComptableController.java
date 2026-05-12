package com.clubchretien.abonnement.controllers.v2;

import com.clubchretien.abonnement.services.ComptableService;
import com.owomi.commons.ws.TransersComptableWs;
import com.owomi.commons.ws.VenteComptableWs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("v2.0/comptables")
public class ComptableController {

    @Autowired
    private ComptableService comptableService;

    @GetMapping("/transferts-valider")
	//@ApiVersions({"2.0"})
    public ResponseEntity<List<TransersComptableWs>> getTransferValider(@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") Date dateDebut, @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") Date dateFin){
        return new ResponseEntity<>(comptableService.getTransferValider(dateDebut, dateFin), HttpStatus.OK);
    }

    @GetMapping("/ventes")
	//@ApiVersions({"2.0"})
    public ResponseEntity<List<VenteComptableWs>> getVentes(@RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") Date dateDebut, @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") Date dateFin){
        return new ResponseEntity<>(comptableService.getVentes(dateDebut, dateFin), HttpStatus.OK);
    }
}
