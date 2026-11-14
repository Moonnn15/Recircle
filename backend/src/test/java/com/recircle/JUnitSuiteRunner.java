package com.recircle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class JUnitSuiteRunner {

    public static void main(String[] args) throws Exception {
        System.setProperty("spring.datasource.url", "jdbc:h2:mem:recircle_test_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        System.setProperty("spring.datasource.driver-class-name", "org.h2.Driver");
        System.setProperty("spring.datasource.username", "sa");
        System.setProperty("spring.datasource.password", "");
        System.setProperty("spring.jpa.hibernate.ddl-auto", "update");
        System.setProperty("recircle.security.jwt-secret", "ReCircleCampusTrustSharingSecretKey2026SRMInstituteOfScienceAndTech!");
        System.setProperty("recircle.campus.allowed-domains", "srmist.edu.in,campus.edu,edu.in");

        try (AnnotationConfigWebApplicationContext wac = new AnnotationConfigWebApplicationContext()) {
            wac.setServletContext(new MockServletContext());
            wac.register(ReCircleApplication.class);
            wac.refresh();

            // Run DataInitializer manually to seed the 10 campus categories and demo accounts
            com.recircle.config.DataInitializer initializer = wac.getBean(com.recircle.config.DataInitializer.class);
            initializer.run();

            com.recircle.security.JwtAuthenticationFilter jwtFilter = wac.getBean(com.recircle.security.JwtAuthenticationFilter.class);
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .addFilters(jwtFilter)
                .build();

            ReCircleBackendIntegrationTest testInstance = new ReCircleBackendIntegrationTest();

            Field mockMvcField = ReCircleBackendIntegrationTest.class.getDeclaredField("mockMvc");
            mockMvcField.setAccessible(true);
            mockMvcField.set(testInstance, mockMvc);

            Field objectMapperField = ReCircleBackendIntegrationTest.class.getDeclaredField("objectMapper");
            objectMapperField.setAccessible(true);
            objectMapperField.set(testInstance, wac.getBean(com.fasterxml.jackson.databind.ObjectMapper.class));

            List<Method> testMethods = Arrays.stream(ReCircleBackendIntegrationTest.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Test.class))
                .sorted(Comparator.comparingInt(m -> {
                    Order o = m.getAnnotation(Order.class);
                    return o != null ? o.value() : 999;
                }))
                .toList();

            int passed = 0;
            for (Method m : testMethods) {
                m.setAccessible(true);
                DisplayName dn = m.getAnnotation(DisplayName.class);
                String label = dn != null ? dn.value() : m.getName();
                m.invoke(testInstance);
                passed++;
                System.out.println("[PASS] " + label);
            }
            System.out.println("=================================================================");
            System.out.println("ALL JUNIT 5 INTEGRATION TESTS PASSED: " + passed + "/" + testMethods.size());
            System.out.println("=================================================================");
        }
    }
}
