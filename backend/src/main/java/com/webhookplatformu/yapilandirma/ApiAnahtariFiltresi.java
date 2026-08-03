package com.webhookplatformu.yapilandirma;

import com.webhookplatformu.guvenlik.ApiAnahtariServisi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Tum {@code /v1/**} istekleri {@code Authorization: Bearer whsk_live_...} ile kimliklenmeli
 * (bkz Faz 4.1). Cozulen organizasyon istek-kapsamli {@link OrganizasyonBaglami}'na yazilir,
 * controller'lar bunu kiracı izolasyonu icin kullanir.
 */
@Component
public class ApiAnahtariFiltresi extends OncePerRequestFilter {

    private final ApiAnahtariServisi apiAnahtariServisi;
    private final OrganizasyonBaglami organizasyonBaglami;

    public ApiAnahtariFiltresi(ApiAnahtariServisi apiAnahtariServisi, OrganizasyonBaglami organizasyonBaglami) {
        this.apiAnahtariServisi = apiAnahtariServisi;
        this.organizasyonBaglami = organizasyonBaglami;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // CORS preflight (OPTIONS) tarayicidan Authorization basligi OLMADAN gelir - servlet
        // filtreleri DispatcherServlet'in CORS islemesinden (WebYapilandirmasi) ONCE calistigi
        // icin burada engellenirse preflight kirilir (bkz Faz 3.6'da bulunan CORS hatasiyla
        // AYNI KATEGORIDE bir tuzak - bu sefer ONCEDEN fark edilip onlendi).
        if (!request.getRequestURI().startsWith("/v1/") || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String yetkiBasligi = request.getHeader("Authorization");
        if (yetkiBasligi == null || !yetkiBasligi.startsWith("Bearer ")) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authorization: Bearer <api-anahtari> gerekli");
            return;
        }

        String duzAnahtar = yetkiBasligi.substring("Bearer ".length());
        Optional<UUID> organizasyonId = apiAnahtariServisi.dogrula(duzAnahtar);
        if (organizasyonId.isEmpty()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Gecersiz veya iptal edilmis API anahtari");
            return;
        }

        organizasyonBaglami.setOrganizasyonId(organizasyonId.get());
        chain.doFilter(request, response);
    }
}
