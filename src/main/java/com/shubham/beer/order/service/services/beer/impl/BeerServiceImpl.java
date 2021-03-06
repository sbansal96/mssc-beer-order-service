package com.shubham.beer.order.service.services.beer.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.shubham.beer.order.service.services.beer.BeerService;
import com.shubham.brewery.model.BeerDto;

import java.util.Optional;
import java.util.UUID;

@ConfigurationProperties(prefix = "brewery", ignoreUnknownFields = false)
@Service
public class BeerServiceImpl implements BeerService {

	public final String BEER_PATH_V1 = "/api/v1/beer/";
	public final String BEER_UPC_PATH_V1 = "/api/v1/beer/beerUpc/";
	private final RestTemplate restTemplate;

	private String beerServiceHost;

	public BeerServiceImpl(RestTemplateBuilder restTemplateBuilder) {
		this.restTemplate = restTemplateBuilder.build();
	}

	@Override
	public Optional<BeerDto> getBeerById(UUID uuid) {
		return Optional.of(restTemplate.getForObject(beerServiceHost + BEER_PATH_V1 + uuid.toString(), BeerDto.class));
	}

	@Override
	public Optional<BeerDto> getBeerByUpc(String upc) {
		System.out.println(beerServiceHost + BEER_UPC_PATH_V1 + upc);
		return Optional.of(restTemplate.getForObject(beerServiceHost + BEER_UPC_PATH_V1 + upc, BeerDto.class));
	}

	public void setBeerServiceHost(String beerServiceHost) {
		this.beerServiceHost = beerServiceHost;
	}

}
