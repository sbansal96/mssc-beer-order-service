package com.shubham.beer.order.service.services.impl;

import java.util.UUID;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shubham.beer.order.service.domain.BeerOrder;
import com.shubham.beer.order.service.domain.BeerOrderEventEnum;
import com.shubham.beer.order.service.domain.BeerOrderStatusEnum;
import com.shubham.beer.order.service.repositories.BeerOrderRepository;
import com.shubham.beer.order.service.services.BeerOrderManager;
import com.shubham.beer.order.service.sm.BeerOrderStateChangeInterceptor;
import com.shubham.brewery.model.BeerOrderDto;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class BeerOrderManagerImpl implements BeerOrderManager {

	public static final String ORDER_ID_HEADER = "ORDER_ID_HEADER";

	private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
	private final BeerOrderRepository beerOrderRepository;
	private final BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;

	@Transactional
	@Override
	public BeerOrder newBeerOrder(BeerOrder beerOrder) {
		beerOrder.setId(null);
		beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);

		BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder);
		sendBeerOrderEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
		return savedBeerOrder;
	}

	@SuppressWarnings("deprecation")
	private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum eventEnum) {
		StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = build(beerOrder);
		Message<BeerOrderEventEnum> msg = MessageBuilder.withPayload(eventEnum)
				.setHeader(ORDER_ID_HEADER, beerOrder.getId().toString()).build();

		sm.sendEvent(msg);
	}

	@SuppressWarnings("deprecation")
	private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder) {
		StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = stateMachineFactory
				.getStateMachine(beerOrder.getId());

		sm.stop();

		sm.getStateMachineAccessor().doWithAllRegions(sma -> {
			sma.addStateMachineInterceptor(beerOrderStateChangeInterceptor);
			sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null));
		});

		sm.start();

		return sm;
	}

	@Override
	public void processValidationResult(UUID beerOrderId, Boolean isValid) {
		BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderId);

		if (isValid) {
			sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED);
			BeerOrder validatedOrder = beerOrderRepository.findOneById(beerOrderId);
			sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATE_ORDER);

		} else {
			sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_FAILED);
		}
	}

	@Override
	public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto) {
		BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
		sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_SUCCESS);
		updateAllocatedQty(beerOrderDto, beerOrder);

	}

	@Override
	public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto) {
		BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
		sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);

		updateAllocatedQty(beerOrderDto, beerOrder);

	}

	@Override
	public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto) {
		BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
		sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_FAILED);

	}

	private void updateAllocatedQty(BeerOrderDto beerOrderDto, BeerOrder beerOrder) {
		BeerOrder allocatedOrder = beerOrderRepository.getOne(beerOrderDto.getId());

		allocatedOrder.getBeerOrderLines().forEach(beerOrderLine -> {
			beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
				if (beerOrderLine.getId().equals(beerOrderLineDto.getId())) {
					beerOrderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
				}
			});
		});

		beerOrderRepository.saveAndFlush(beerOrder);
	}

}
