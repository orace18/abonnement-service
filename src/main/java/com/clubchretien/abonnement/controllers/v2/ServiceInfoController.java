package com.clubchretien.abonnement.controllers.v2;

import com.clubchretien.abonnement.services.ServiceInfoService;
import com.owomi.commons.ws.ResponseWs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("serviceInfo")
@RequestMapping("v2.0/serviceInfo")
public class ServiceInfoController {

    @Autowired
    ServiceInfoService service;
    @GetMapping("/check")
	//@ApiVersions({"2.0"})
    public ResponseEntity<ResponseWs> userInfo(@RequestParam("email") String email){
        return ResponseEntity.ok(service.checkUserInfo(email));
    }

    @GetMapping("/update")
    //@ApiVersions({"2.0"})
    public ResponseEntity<ResponseWs> updateUser(@RequestParam("email") String email){
        return ResponseEntity.ok(service.updateUser(email));
    }

}
