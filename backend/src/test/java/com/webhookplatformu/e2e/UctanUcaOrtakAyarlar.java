package com.webhookplatformu.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.webhookplatformu.depo.ApiAnahtariRepository;
import com.webhookplatformu.depo.EndpointRepository;
import com.webhookplatformu.depo.OrganizasyonRepository;
import com.webhookplatformu.depo.UygulamaRepository;
import com.webhookplatformu.guvenlik.ApiAnahtariServisi;
import com.webhookplatformu.guvenlik.SifrelemeServisi;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.Organizasyon;
import com.webhookplatformu.varlik.RetryProfili;
import com.webhookplatformu.varlik.Uygulama;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Faz 5.1 uctan uca test altyapisi: gercek Postgres + RabbitMQ (delayed-message plugin'li,
 * gelistirme docker-compose.yml'deki AYNI imaj) + test-alici (kendi Dockerfile'indan build
 * edilir - herhangi bir registry'ye publish edilmis degil, bkz test-alici/README yorumlari).
 *
 * <p>Org/uygulama/endpoint/API anahtari icin REST API yok (bkz {@code VeriTohumlayici}) - bu
 * sinif ayni deseni (repository/servis'e dogrudan erisim) testler icin tekrarliyor. Endpoint
 * OLUSTURMA gercek HTTP uzerinden yapiliyor (UygulamaEndpointController'i da test etsin diye).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class UctanUcaOrtakAyarlar {

    protected static final Duration BEKLEME_ZAMAN_ASIMI = Duration.ofSeconds(45);
    protected static final Duration BEKLEME_ARALIGI = Duration.ofMillis(300);

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("webhook_platformu")
            .withUsername("webhook")
            .withPassword("webhook_sifre");

    @Container
    static final RabbitMQContainer rabbitmq = new RabbitMQContainer(
            DockerImageName.parse("heidiks/rabbitmq-delayed-message-exchange:3.13.0-management")
                    .asCompatibleSubstituteFor("rabbitmq"));

    /**
     * test-alici'nin imza dogrulamasi tek bir WEBHOOK_SECRET env degiskenine karsi calisiyor
     * (bkz server.js) - container test siniflari arasinda paylasilan tek bir instance oldugu
     * icin bu sabit deger sadece "imza dogrulama" senaryosunun kullandigi endpoint'e verilir
     * (bkz {@code SABIT_IMZA_SECRET} kullanimlari) - diger testlerin rastgele secret'lari bu
     * degerle eslesmez, ama o testler zaten imza sonucunu kontrol etmiyor.
     */
    protected static final String SABIT_IMZA_SECRET = "e2e-sabit-imza-secreti-01234567";

    @Container
    static final org.testcontainers.containers.GenericContainer<?> testAlici =
            new org.testcontainers.containers.GenericContainer<>(
                    new ImageFromDockerfile().withFileFromPath(".", Paths.get("../test-alici")))
                    .withExposedPorts(4000)
                    .withEnv("WEBHOOK_SECRET", SABIT_IMZA_SECRET)
                    .waitingFor(Wait.forHttp("/saglik").forStatusCode(200));

    @DynamicPropertySource
    static void ozellikler(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", () -> rabbitmq.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @Autowired
    protected TestRestTemplate restTemplate;
    @Autowired
    protected OrganizasyonRepository organizasyonRepository;
    @Autowired
    protected UygulamaRepository uygulamaRepository;
    @Autowired
    protected EndpointRepository endpointRepository;
    @Autowired
    protected ApiAnahtariRepository apiAnahtariRepository;
    @Autowired
    protected ApiAnahtariServisi apiAnahtariServisi;
    @Autowired
    protected SifrelemeServisi sifrelemeServisi;
    @Autowired
    protected ObjectMapper objectMapper;

    private final org.springframework.web.client.RestTemplate testAliciIstemcisi =
            new org.springframework.boot.web.client.RestTemplateBuilder().build();

    /** Yeni bir organizasyon + uygulama + (HTTP uzerinden) bir endpoint kurar. Her test kendi izole kiracisini kullanir. */
    protected record Kiraci(UUID organizasyonId, UUID uygulamaId, UUID endpointId, String apiAnahtari) {
    }

    protected Kiraci yeniKiraciOlustur(RetryProfili retryProfili, Integer hizSiniriSn, int aylikKota) {
        Organizasyon organizasyon = new Organizasyon("Test Org " + UUID.randomUUID(), aylikKota);
        organizasyonRepository.save(organizasyon);

        Uygulama uygulama = new Uygulama(organizasyon.getId(), "Test Uygulama", "test");
        uygulamaRepository.save(uygulama);

        var uretilen = apiAnahtariServisi.uret(organizasyon.getId());

        var istek = new com.webhookplatformu.api.dto.EndpointOlusturmaIstegi(testAliciWebhookUrl(), null,
                retryProfili, hizSiniriSn);
        ResponseEntity<com.webhookplatformu.api.dto.EndpointOlusturmaYaniti> yanit = restTemplate.exchange(
                "/v1/uygulamalar/" + uygulama.getId() + "/endpointler", HttpMethod.POST,
                new HttpEntity<>(istek, yetkiliBasliklar(uretilen.duzAnahtar())),
                com.webhookplatformu.api.dto.EndpointOlusturmaYaniti.class);
        if (yanit.getStatusCode() != HttpStatus.CREATED || yanit.getBody() == null) {
            throw new IllegalStateException("Endpoint olusturulamadi: " + yanit);
        }

        return new Kiraci(organizasyon.getId(), uygulama.getId(), yanit.getBody().id(), uretilen.duzAnahtar());
    }

    protected Kiraci yeniKiraciOlustur(RetryProfili retryProfili) {
        return yeniKiraciOlustur(retryProfili, null, 10_000);
    }

    protected HttpHeaders yetkiliBasliklar(String apiAnahtari) {
        HttpHeaders basliklar = new HttpHeaders();
        basliklar.set("Authorization", "Bearer " + apiAnahtari);
        return basliklar;
    }

    /**
     * String donuyor (typed DTO degil) bilincli - kota/idempotency testleri basarili (2xx) VE
     * hata (4xx) yanitlarini ayni cagriyla bekliyor, hata govdesi OlayOlusturmaYaniti sekline
     * uymadigi icin tipli deserializasyon bu durumda patlardi.
     */
    protected ResponseEntity<String> olayGonder(
            UUID uygulamaId, String apiAnahtari, String idempotencyKey, String olayTipi, Map<String, Object> payload) {
        HttpHeaders basliklar = yetkiliBasliklar(apiAnahtari);
        basliklar.set("Idempotency-Key", idempotencyKey);
        ObjectNode govde = objectMapper.createObjectNode();
        govde.put("tip", olayTipi);
        govde.set("payload", objectMapper.valueToTree(payload));
        return restTemplate.exchange("/v1/uygulamalar/" + uygulamaId + "/olaylar", HttpMethod.POST,
                new HttpEntity<>(govde, basliklar), String.class);
    }

    protected com.webhookplatformu.api.dto.OlayOlusturmaYaniti govdeyiOlayYanitiOlarakOku(ResponseEntity<String> yanit) {
        try {
            return objectMapper.readValue(yanit.getBody(), com.webhookplatformu.api.dto.OlayOlusturmaYaniti.class);
        } catch (Exception e) {
            throw new IllegalStateException("Yanit govdesi okunamadi: " + yanit.getBody(), e);
        }
    }

    /** {@code /v1/teslimatlar/{id}} (detay) DTO'sundan sadece durum ozetini cikarir - GET /{id} her zaman TeslimatDetayYaniti doner. */
    protected com.webhookplatformu.api.dto.TeslimatOzetiYaniti teslimatOzetiGetir(String apiAnahtari, UUID teslimatId) {
        return teslimatDetayGetir(apiAnahtari, teslimatId).teslimat();
    }

    protected com.webhookplatformu.api.dto.TeslimatDetayYaniti teslimatDetayGetir(String apiAnahtari, UUID teslimatId) {
        ResponseEntity<com.webhookplatformu.api.dto.TeslimatDetayYaniti> yanit = restTemplate.exchange(
                "/v1/teslimatlar/" + teslimatId, HttpMethod.GET, new HttpEntity<>(yetkiliBasliklar(apiAnahtari)),
                com.webhookplatformu.api.dto.TeslimatDetayYaniti.class);
        return yanit.getBody();
    }

    protected com.webhookplatformu.api.dto.EndpointYaniti endpointDetayGetir(String apiAnahtari, UUID endpointId) {
        ResponseEntity<com.webhookplatformu.api.dto.EndpointYaniti> yanit = restTemplate.exchange(
                "/v1/endpointler/" + endpointId, HttpMethod.GET, new HttpEntity<>(yetkiliBasliklar(apiAnahtari)),
                com.webhookplatformu.api.dto.EndpointYaniti.class);
        return yanit.getBody();
    }

    /** Tip donusumu denemeden sadece HTTP durum kodunu kontrol etmek icin (orn. 404 bekleyen negatif senaryolar). */
    protected ResponseEntity<String> hamIstek(HttpMethod metod, String yol, String apiAnahtari) {
        return restTemplate.exchange(yol, metod, new HttpEntity<>(yetkiliBasliklar(apiAnahtari)), String.class);
    }

    // --- test-alici kontrol uc noktalari (bkz test-alici/server.js) ---

    protected String testAliciTabanUrl() {
        return "http://" + testAlici.getHost() + ":" + testAlici.getMappedPort(4000);
    }

    protected String testAliciWebhookUrl() {
        return testAliciTabanUrl() + "/webhook";
    }

    protected void testAliciModAyarla(String mod) {
        testAliciModAyarla(mod, null, null, null);
    }

    protected void testAliciModAyarla(String mod, Integer esik, String dizi, Integer ms) {
        Map<String, Object> govde = new java.util.HashMap<>();
        govde.put("mod", mod);
        if (esik != null) govde.put("esik", esik);
        if (dizi != null) govde.put("dizi", dizi);
        if (ms != null) govde.put("ms", ms);
        testAliciIstemcisi.postForEntity(testAliciTabanUrl() + "/varsayilan-mod", govde, String.class);
    }

    protected void testAliciSifirla() {
        testAliciIstemcisi.postForEntity(testAliciTabanUrl() + "/sifirla", null, Void.class);
        testAliciModAyarla("ok");
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> testAliciAlinanlar() {
        ResponseEntity<List> yanit = testAliciIstemcisi.getForEntity(testAliciTabanUrl() + "/alinanlar", List.class);
        return (List<Map<String, Object>>) (List<?>) yanit.getBody();
    }
}
