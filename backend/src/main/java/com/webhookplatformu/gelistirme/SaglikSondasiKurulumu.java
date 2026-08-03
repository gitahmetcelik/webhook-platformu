package com.webhookplatformu.gelistirme;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gorevplatformu.motorspringstarter.ZamanlanmisGorevRepository;
import com.gorevplatformu.motorspringstarter.ZamanlanmisGorevServisi;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * {@code webhook.saglik-sondasi} görev tipini 5 dakikada bir çalışacak şekilde motora bir
 * kere kaydeder (bkz Faz 2.4). {@code ZamanlanmisGorevServisi.olustur()} mükerrer kayda karşı
 * korumasız olduğu için burada açıkça "zaten var mı" kontrolü yapılıyor. Motorun repository
 * bean'leri Faz 0'ın JPA-tarama düzeltmesi sayesinde doğrudan enjekte edilebiliyor.
 *
 * <p><b>ApplicationReadyEvent, ApplicationStartedEvent DEĞİL — kendi kod tabanımızda bulunan
 * gerçek bir hata:</b> motorun {@code GorevTipiKayitDefteri.kataloguSenkronla()}'sı da
 * {@code ApplicationStartedEvent} dinliyor (bkz gorev-motoru Faz 0 düzeltmesi). Aynı event'i
 * dinleyen birden fazla bean arasında Spring'in sıralama garantisi yok — bu sınıf da
 * {@code ApplicationStartedEvent}'i dinlediğinde motorun katalog senkronundan ÖNCE
 * çalışabiliyordu, bu da {@code zamanlanmis_gorevler.tip} FK ihlaline yol açtı (taze bir DB'de
 * gerçekten çalıştırılınca bulundu). {@code ApplicationReadyEvent}, tüm
 * {@code ApplicationStartedEvent} dinleyicileri (katalog senkronu dahil) kesinlikle bittikten
 * sonra ateşleniyor, bu yarışı ortadan kaldırıyor.</p>
 */
@Component
public class SaglikSondasiKurulumu {

    private static final String GOREV_TIPI = "webhook.saglik-sondasi";
    // Spring 6 alanli cron formati: saniye dakika saat gun ay haftaGunu.
    private static final String CRON_5_DAKIKADA_BIR = "0 */5 * * * *";

    private final ZamanlanmisGorevRepository zamanlanmisGorevRepository;
    private final ZamanlanmisGorevServisi zamanlanmisGorevServisi;
    private final ObjectMapper objectMapper;

    public SaglikSondasiKurulumu(ZamanlanmisGorevRepository zamanlanmisGorevRepository,
                                  ZamanlanmisGorevServisi zamanlanmisGorevServisi, ObjectMapper objectMapper) {
        this.zamanlanmisGorevRepository = zamanlanmisGorevRepository;
        this.zamanlanmisGorevServisi = zamanlanmisGorevServisi;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void kaydet() {
        boolean zatenVar = zamanlanmisGorevRepository.findAll().stream()
                .anyMatch(z -> GOREV_TIPI.equals(z.getTip()));
        if (zatenVar) {
            return;
        }
        JsonNode bosPayload = objectMapper.createObjectNode();
        zamanlanmisGorevServisi.olustur(GOREV_TIPI, CRON_5_DAKIKADA_BIR, bosPayload, true);
    }
}
