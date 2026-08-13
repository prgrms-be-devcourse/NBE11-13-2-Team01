package com.example.delivery_project;

import com.example.delivery_project.domain.repository.DeliveryPlanRepository;
import com.example.delivery_project.domain.repository.DeliveryStopRepository;
import com.example.delivery_project.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "weather.api.key=test-key")
class DeliveryProjectApplicationTests {

    @MockitoBean
    private DeliveryPlanRepository deliveryPlanRepository;

    @MockitoBean
    private DeliveryStopRepository deliveryStopRepository;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void contextLoads() {
    }

}
