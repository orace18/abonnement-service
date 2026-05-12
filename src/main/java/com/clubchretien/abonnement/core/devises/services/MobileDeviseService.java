package com.clubchretien.abonnement.core.devises.services;


import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.DeviseWs;
import com.owomi.database.entities.Devise;
import com.owomi.database.repositories.DeviseRepository;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MobileDeviseService {

	@Autowired
	Mapper mapper;

	@Autowired
	DeviseRepository deviseRepository;

	private DeviseWs convertEntityToWs(Devise entity) {
		if (entity == null) return null;
		DeviseWs result = this.mapper.map(entity, DeviseWs.class);
		return result;
	}

	public CommonResponseWs listDevises()  {
		List<Devise> list = deviseRepository.findAll();
		Pageable pageable = PageRequest.of(0, Math.max(1, list.size()));
		List<DeviseWs> content = list.stream().map(this::convertEntityToWs).collect(Collectors.toList());
		Page<DeviseWs> result = new PageImpl<>(content, pageable, list.size());
		return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
	}
}
