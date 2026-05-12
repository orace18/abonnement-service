package com.clubchretien.abonnement.services;

import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.database.entities.WhitelistPhone;
import com.owomi.database.repositories.WhitelistPhoneRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class WhitelistPhoneService {

    private static Logger logger = LogManager.getLogger(WhitelistPhoneService.class);

    @Autowired
    private WhitelistPhoneRepository whitelistPhoneRepository;

    public CommonResponseWs checkTelephone(String codePays, String telephone) {
        CommonResponseWs response = new CommonResponseWs();
        telephone = telephone.replaceAll(" ", "");
        codePays = codePays.replaceAll(" ", "");
        Boolean verification = this.checkPrefix(codePays, telephone);
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, verification);
    }

    private boolean checkPrefix(String codePays, String telephone) {
        List<WhitelistPhone> prefixes = whitelistPhoneRepository.findAllByCodePays(codePays)
                .stream().sorted(Comparator.comparing(v -> v.getOperatorPrefix().length()))
                .collect(Collectors.toList());
        if (prefixes.isEmpty()) return true;
        Collections.reverse(prefixes);
        String fullPhone = codePays + "" + telephone;
        for (WhitelistPhone prefix : prefixes) {
            if (
                    telephone.length() == prefix.getNumberLength()
                            && telephone.startsWith(prefix.getOperatorPrefix())
            ) return true;
        }
        if (Objects.equals(codePays, "243")) return true;
        return false;
    }
}
