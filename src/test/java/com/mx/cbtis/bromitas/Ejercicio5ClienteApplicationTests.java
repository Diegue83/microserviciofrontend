package com.mx.cbtis.bromitas;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import com.mx.cbtis.Ejercicio5ClienteApplication;

@SpringBootTest
class Ejercicio5ClienteApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void mainArrancaLaAplicacion() {
		String[] args = {};
		try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
			Ejercicio5ClienteApplication.main(args);
			spring.verify(() -> SpringApplication.run(Ejercicio5ClienteApplication.class, args));
		}
	}

}
