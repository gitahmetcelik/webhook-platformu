package com.webhookplatformu.servis;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Aktif HTTP istegine ait trace id'yi verir. Motorun kendi {@code TraceBaglamServisi}'si ayni
 * isi yapiyor ama PACKAGE-PRIVATE oldugu icin (bkz gorev-motoru motor-spring-starter) urun
 * tarafindan cagrilamiyor - bu sinif ayni mantigi urun icin tekrarliyor.
 *
 * <p>Ayni HTTP istegi icinde her cagrida AYNI degeri dondurmesi kritik: bir olaydan dogan N
 * teslimatin hepsine ayni trace id yazilabilsin diye (bkz {@code OlayController}). Brave
 * bridge'i classpath'te oldugu icin ({@code micrometer-tracing-bridge-brave}) Spring Boot her
 * HTTP istegi icin bir span aciyor, {@code tracer.currentSpan()} o span'i donduruyor.</p>
 */
@Component
public class TraceKimligiSaglayici {

    private final Tracer tracer;

    public TraceKimligiSaglayici(ObjectProvider<Tracer> tracerSaglayici) {
        this.tracer = tracerSaglayici.getIfAvailable();
    }

    public String mevcutTraceId() {
        if (tracer != null) {
            Span mevcut = tracer.currentSpan();
            if (mevcut != null) {
                return mevcut.context().traceId();
            }
        }
        // Tracer/aktif span yoksa (orn. zamanlanmis bir gorev icinden) yine de bos birakmiyoruz -
        // izlenebilirlik icin uretilmis bir id, hic id olmamasindan iyi.
        return UUID.randomUUID().toString().replace("-", "");
    }
}
