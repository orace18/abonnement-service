package com.clubchretien.abonnement.services;


import com.owomi.database.entities.Partenaire;
import com.owomi.database.entities.RedirectLink;
import com.owomi.database.entities.RedirectLinkUtilization;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.database.repositories.RedirectLinkRepository;
import com.owomi.database.repositories.RedirectLinkUtilizationRepository;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Component
public class RedirectLinkService {

	private static Logger logger = LogManager.getLogger(RedirectLinkService.class);

	@Autowired
	Mapper mapper;

	@Autowired
	RedirectLinkRepository redirectLinkRepository;
	@Autowired
	RedirectLinkUtilizationRepository redirectLinkUtilizationRepository;

	@Autowired
	PartenaireRepository partenaireRepository;

	private String uniqueRedirectHashLink(int characters, int attempt) {
		for (int i = 0; i < attempt; i++) {
			String hash = RandomStringUtils.randomAlphanumeric(characters);
			if (redirectLinkRepository.findByHashAndDeletedTimeStampIsNull(hash) == null) {
				return hash;
			}
		}
		return null;
	}

	private String hashGenerator() {
		String hash = this.uniqueRedirectHashLink(8, 3);
		if (hash != null) {
			return hash;
		}
		return UUID.randomUUID().toString() + "-" + new Date().getTime();
	}

	public RedirectLink redirectLink(Partenaire partenaire, Date expiration) {
		RedirectLink link = this.redirectLinkRepository.findByAuthorAndDeletedTimeStampIsNull(
				partenaire
		).stream().findFirst().orElse(null);
		if (link != null) return link;
		link = new RedirectLink();
		link.setAuthor(partenaire);
		link.setExterne(false);
		link.setHash(this.hashGenerator());
		link.setUrlFbd(null);
		link.setType("RIDER INTEGRATION");
		link.setExpiredAt(expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
		link = redirectLinkRepository.save(link);
		return link;
	}

	public RedirectLinkUtilization useLink(RedirectLink link, Partenaire partenaire) {
		RedirectLinkUtilization linkUtilization = new RedirectLinkUtilization();
		linkUtilization.setLink(link);
		linkUtilization.setPhone(partenaire.getTelephone());
		linkUtilization.setEmail(partenaire.getEmail());
		linkUtilization.setInstalledAt(LocalDateTime.now());
		linkUtilization.setUsed(true);
		linkUtilization = redirectLinkUtilizationRepository.save(linkUtilization);
		return linkUtilization;
	}


}
