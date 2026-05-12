package com.clubchretien.abonnement.core.sliders.controllers;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.core.sliders.services.MobileSliderService;
import com.clubchretien.abonnement.core.sliders.ws.MobileSliderCreateWs;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.utils.RequestContextUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static com.owomi.commons.constants.TokenConstant.ACCESS_TOKEN;

@RestController
@RequestMapping("/mobile/v1.0/slider")
@SecurityRequirements({@SecurityRequirement(name = ACCESS_TOKEN)})
public class MobileSliderController {

    @Autowired
    MobileSliderService service;
    @Autowired
    SecurityUtils securityUtils;
    @Autowired
    HttpServletRequest httpServletRequest;

    public Logger logger = LogManager.getLogger(MobileSliderController.class);

    @GetMapping("/read")
    public CommonResponseWs readSlider(@RequestParam(required = false, defaultValue = "5") Integer size) {
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        Partenaire partenaire = this.securityUtils.getPartenaireFromHeader(httpServletRequest);
        return service.listSlider(partenaire, size);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/create")
    public CommonResponseWs create(@RequestBody MobileSliderCreateWs ws) throws IOException, AuthentificationException, DataNotFoundException {
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        return service.createSlider(ws);
    }
}
