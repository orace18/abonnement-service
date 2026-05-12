package com.clubchretien.abonnement.services;

import com.owomi.commons.constants.MobileConstant;
import org.springframework.stereotype.Component;

@Component
public class VelocityService implements MobileConstant {
/*
    private NumberFormat formatter = new DecimalFormat("#0");
    @Value("${velocity.depot:/templates/depot.vm}")
    private String velocityDepot;
    @Value("${velocity.virement:/templates/virement.vm}")
    private String velocityVirement;
    @Value("${velocity.mtnmobilemoney:/templates/mtnmobilemoney.vm}")
    private String velocityMtnMobileMoney;
    @Value("${velocity.moovmobilemoney:/templates/moovmobilemoney.vm}")
    private String velocityMoovMobileMoney;

    public String getActivityLogContent(RequestContextWs requestContext, String ... objets){
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
        velocityEngine.setProperty("class.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocityEngine.init();
        Template template = new Template();
        VelocityContext velocityContext = new VelocityContext();
        switch (objets[0]){
            case VELOCITY_DEPOT:
                template = velocityEngine.getTemplate(velocityDepot);
                break;
            case VELOCITY_VIREMENT:
                template = velocityEngine.getTemplate(velocityVirement);
                break;
            case VELOCITY_MTN_MOBILE_MONEY:
                template = velocityEngine.getTemplate(velocityMtnMobileMoney);
                break;
            case VELOCITY_MOOV_MOBILE_MONEY:
                template = velocityEngine.getTemplate(velocityMoovMobileMoney);
                break;
        }
        velocityContext.put(MONTANT_KEY, formatter.format(Double.parseDouble(objets[1])));
        velocityContext.put(MONTANT_FRAIS_KEY, objets[2]);
        StringWriter stringWriter = new StringWriter();
        template.merge(velocityContext, stringWriter);
        return stringWriter.toString();
    }
*/
}
