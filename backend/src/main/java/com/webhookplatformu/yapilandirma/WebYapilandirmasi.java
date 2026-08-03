package com.webhookplatformu.yapilandirma;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Frontend (Next.js, farklı port) ile backend farklı origin'ler olduğu için tarayıcı
 * CORS preflight (OPTIONS) gönderiyor — bu olmadan tüm /v1/** çağrıları tarayıcıda
 * engelleniyordu (Faz 3.6 kapı testinde gerçekten çalıştırılınca bulundu).
 */
@Configuration
public class WebYapilandirmasi implements WebMvcConfigurer {

    @Value("${webhook.izinli-originler:http://localhost:3000}")
    private String[] izinliOriginler;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/v1/**")
                .allowedOrigins(izinliOriginler)
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
