# syntax=docker/dockerfile:1.7
#
# motor-spring-starter GitHub Packages'tan cekiliyor (bkz Faz 5.0) - build sirasinda kimlik
# dogrulamasi gerekiyor. Token BuildKit secret'i olarak baglaniyor: build-arg kullanilsaydi
# token image katmanlarinda ve `docker history` ciktisinda kalirdi.
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml .
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml,required=true \
    mvn -B -q dependency:go-offline

COPY src src
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml,required=true \
    mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# Root olmayan kullanici - uygulamanin dosya sistemine yazma ihtiyaci yok.
RUN useradd --system --uid 1001 webhook
USER webhook

COPY --from=builder /build/target/backend-*.jar app.jar

# Worker'da spring.lifecycle.timeout-per-shutdown-phase=60s ayarli - devam eden teslimatlar
# konteyner durdurulurken tamamlanabilsin diye (compose'da stop_grace_period ile eslesmeli).
STOPSIGNAL SIGTERM

ENTRYPOINT ["java", "-jar", "app.jar"]
