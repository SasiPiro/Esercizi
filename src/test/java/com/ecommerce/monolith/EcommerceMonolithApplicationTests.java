package com.ecommerce.monolith;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
class EcommerceMonolithApplicationTests {

	@Test
	void contextLoads() {
	}

}
