package com.clubchretien.abonnement.services;

import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.owopay.core.CoreOwoPayCallbackWs;
import com.owomi.database.entities.ClientOwoPay;
import com.owomi.database.entities.Recharge;
import com.owomi.database.repositories.ClientOwoPayRepository;
import com.owomi.database.repositories.RechargeRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RechargeCoreOwoPayClientCallbackService {

	private static Logger logger = LogManager.getLogger(RechargeCoreOwoPayClientCallbackService.class);


	@Autowired
	RechargeCoreOwoPayService rechargeCoreOwoPayService;
	@Autowired
	RechargeRepository rechargeRepository;

	@Autowired
	ClientOwoPayRepository clientOwoPayRepository;

	@Autowired
	Mapper mapper;

	public ClientOwoPay clientRechargeCore() {
		return this.clientOwoPayRepository.findOneById(30L);
	}

	public CommonResponseWs callbackClient(CoreOwoPayCallbackWs ws) {
		logger.debug("callback  {}", ws);
		CompletableFuture.runAsync(() -> {
			try {
				this.callbackClientProcess(ws);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
	}

	private CommonResponseWs validateCallback(CoreOwoPayCallbackWs ws) {
		ClientOwoPay client = this.clientOwoPayRepository.findOneById(ws.getClientId());
		if (client == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 400, null);
		List<Long> allowed = Stream.of(this.clientRechargeCore()).map(ClientOwoPay::getId).collect(Collectors.toList());
		if (!allowed.contains(client.getId()))
			return new CommonResponseWs(WsOperationResponseStatus.failed, "wrong client callback", 408, null);
		if (ws.getOperation() == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "wrong operation callback", 407, null);
		Recharge recharge = rechargeRepository.findOneByIdAndReferenceCoreTransaction(ws.getOperation().intValue(), ws.getReference());
		if (recharge == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "payment not found", 404, null);
		if (recharge.getIdCoreTransaction() != null && !Objects.equals(recharge.getIdCoreTransaction(), ws.getId()))
			return new CommonResponseWs(WsOperationResponseStatus.failed, "core transaction not same", 404, null);
		return new CommonResponseWs(WsOperationResponseStatus.success, "success", 200, null);
	}

	private CommonResponseWs callbackClientProcess(CoreOwoPayCallbackWs ws) {
		logger.debug("callback  {}", ws);
		CommonResponseWs commonResponseWs = this.validateCallback(ws);
		logger.debug("callback validation response {}", commonResponseWs);
		if (!commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) return commonResponseWs;
		logger.debug("success validation callback");
		Recharge recharge = rechargeRepository.findOneByIdAndReferenceCoreTransaction(ws.getOperation().intValue(), ws.getReference());
		recharge.setStatusCoreTransaction(ws.getStatus());
		recharge.setStatusCoreTransactionDate(new Date());
		recharge = this.rechargeRepository.save(recharge);
		this.rechargeCoreOwoPayService.callbackClient(recharge, ws);
		return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
	}


}
