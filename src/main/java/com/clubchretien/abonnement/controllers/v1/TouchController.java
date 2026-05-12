package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.TouchService;
import com.owomi.commons.ws.CallBackTouchWs;
import com.owomi.database.repositories.CallbackTouchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1.0/touch")
public class TouchController {

    @Autowired
    TouchService touchService;
    @Autowired
    CallbackTouchRepository callbackTouchRepository;


    @PostMapping("/callback")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CallBackTouchWs> callback (@RequestBody CallBackTouchWs callBackTouchWs){
        CallBackTouchWs response = touchService.callback(callBackTouchWs);
        return new ResponseEntity<CallBackTouchWs>(response, HttpStatus.OK);
    }
}
