package com.smartfarm.ledger.config;

import com.smartfarm.ledger.domain.House;
import com.smartfarm.ledger.repo.HouseRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HouseSeeder {

	@Bean
	ApplicationRunner seedHouses(HouseRepository houses) {
		return args -> {
			upsert(houses, "H1", "1동", "cymbidium", "심비디움", "20–24°C", "10–15°C", "밝게");
			upsert(houses, "H2", "2동", "phalaenopsis", "호접란", "26–29°C", "18–20°C", "그늘");
			upsert(houses, "H3", "3동", "denphale", "덴파레", "25–30°C", "18–20°C", "밝게");
			upsert(houses, "H4", "4동", "oncidium", "온시디움", "23–27°C", "15–18°C", "밝게, 통풍");
		};
	}

	private static void upsert(
			HouseRepository houses,
			String code,
			String name,
			String cropCode,
			String cropName,
			String dayTemp,
			String nightTemp,
			String lightNote) {
		House house = houses.findByCode(code).orElseGet(() -> new House(code, name, cropCode, cropName, dayTemp, nightTemp, lightNote));
		house.rename(name, cropName, dayTemp, nightTemp, lightNote);
		houses.save(house);
	}
}
