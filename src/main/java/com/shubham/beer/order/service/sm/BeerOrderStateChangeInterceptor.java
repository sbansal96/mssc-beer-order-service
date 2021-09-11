package com.shubham.beer.order.service.sm;

import java.util.Optional;
import java.util.UUID;

import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

import com.shubham.beer.order.service.domain.BeerOrder;
import com.shubham.beer.order.service.domain.BeerOrderEventEnum;
import com.shubham.beer.order.service.domain.BeerOrderStatusEnum;
import com.shubham.beer.order.service.repositories.BeerOrderRepository;
import com.shubham.beer.order.service.services.impl.BeerOrderManagerImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BeerOrderStateChangeInterceptor
		extends StateMachineInterceptorAdapter<BeerOrderStatusEnum, BeerOrderEventEnum> {

	private final BeerOrderRepository beerOrderRepository;

	@Override
	public void preStateChange(State<BeerOrderStatusEnum, BeerOrderEventEnum> state,
			Message<BeerOrderEventEnum> message, Transition<BeerOrderStatusEnum, BeerOrderEventEnum> transition,
			StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine,
			StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> rootStateMachine) {
		Optional.ofNullable(message)
				.flatMap(msg -> Optional
						.ofNullable((String) msg.getHeaders().getOrDefault(BeerOrderManagerImpl.ORDER_ID_HEADER, " ")))
				.ifPresent(orderId -> {
					log.debug("Saving state for order id: " + orderId + " Status: " + state.getId());

					BeerOrder beerOrder = beerOrderRepository.getOne(UUID.fromString(orderId));
					beerOrder.setOrderStatus(state.getId());
					beerOrderRepository.saveAndFlush(beerOrder);
				});
	}

}