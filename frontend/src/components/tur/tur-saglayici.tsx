"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { createPortal } from "react-dom";
import { useAuth } from "@/components/auth-provider";
import { useAktifUygulama } from "@/components/uygulama-provider";
import { api } from "@/lib/api";
import { azaltilmisHareketMi } from "@/lib/hareket";
import { turDurumu, turDurumunuKaydet } from "@/lib/tur";
import { TUM_TURLAR, turBul, type TurAdimi, type TurBaglami } from "@/lib/turlar";
import { anchorBekle } from "./anchor-bekle";
import { TurPopover } from "./tur-popover";
import { TurSpotlight, TurTiklamaKatmani, type SpotlightDikdortgen } from "./tur-spotlight";

/**
 * Basit bir telemetri kanalı — backend'de henüz tur olayları ucu yok (VARSAYIM: §10.9'daki
 * `tur.*` olayları burada console.info ile gözlemlenebilir hale getiriliyor; gerçek bir
 * `/v1/telemetri` ucu eklendiğinde bu fonksiyon değiştirilir, çağıran kod aynı kalır).
 */
function telemetriGonder(olay: string, veri?: Record<string, unknown>) {
  if (process.env.NODE_ENV !== "production") {
    console.info(`[tur] ${olay}`, veri ?? {});
  }
}

function dikdortgenOlustur(el: HTMLElement): SpotlightDikdortgen {
  const r = el.getBoundingClientRect();
  return { x: r.left, y: r.top, w: r.width, h: r.height };
}

type MotorDurumu =
  | { asama: "bosta" }
  | { asama: "hazirlaniyor" }
  | { asama: "gosteriliyor"; anchorEl: HTMLElement; rect: SpotlightDikdortgen };

export function TurSaglayici() {
  const pathname = usePathname();
  const router = useRouter();
  const searchParams = useSearchParams();
  const { organizasyon } = useAuth();
  const { uygulama } = useAktifUygulama();

  const [aktifTurKimligi, setAktifTurKimligi] = useState<string | null>(null);
  const [adimIndeksi, setAdimIndeksi] = useState(0);
  const [motor, setMotor] = useState<MotorDurumu>({ asama: "bosta" });
  const calismaKimligi = useRef(0);
  const derinBaglantiIslendi = useRef(false);

  const { data: endpointler } = useQuery({
    queryKey: ["endpointler", uygulama?.id],
    queryFn: () => api.endpointler.listele(uygulama!.id),
    enabled: !!uygulama,
  });
  const { data: olaylarOzet } = useQuery({
    queryKey: ["olaylar-ozet", uygulama?.id],
    queryFn: () => api.olaylar.listele(uygulama!.id, { boyut: 1 }),
    enabled: !!uygulama,
  });

  const baglam: TurBaglami = {
    endpointSayisi: endpointler?.length ?? 0,
    olaySayisi: olaylarOzet?.totalElements ?? 0,
  };

  const aktifTur = aktifTurKimligi ? turBul(aktifTurKimligi) : undefined;
  const aktifAdim: TurAdimi | undefined = aktifTur?.adimlar[adimIndeksi];

  const turuBaslat = useCallback((turKimligi: string, baslangicIndeksi = 0) => {
    setAktifTurKimligi(turKimligi);
    setAdimIndeksi(baslangicIndeksi);
    telemetriGonder("tur.baslatildi", { tur: turKimligi });
  }, []);

  const turuKapat = useCallback(
    (sonucDurumu: "tamamlandi" | "birakildi") => {
      if (aktifTurKimligi && organizasyon) {
        turDurumunuKaydet(organizasyon.id, aktifTurKimligi, { durum: sonucDurumu, adimIndeksi });
        telemetriGonder(sonucDurumu === "tamamlandi" ? "tur.tamamlandi" : "tur.birakildi", {
          tur: aktifTurKimligi,
          adim: adimIndeksi,
        });
      }
      setAktifTurKimligi(null);
      setMotor({ asama: "bosta" });
    },
    [aktifTurKimligi, adimIndeksi, organizasyon],
  );

  // §10.3 — adım geçişinin tam sırası. `calismaId` eskimiş async çalışmaları iptal etmek için.
  useEffect(() => {
    if (!aktifTur || !aktifAdim) return;
    const buCalisma = ++calismaKimligi.current;
    setMotor({ asama: "hazirlaniyor" });

    async function calistir() {
      let indeks = adimIndeksi;
      let adim = aktifTur!.adimlar[indeks];

      while (adim) {
        if (calismaKimligi.current !== buCalisma) return;

        if (adim.onkosul && !adim.onkosul(baglam)) {
          indeks++;
          adim = aktifTur!.adimlar[indeks];
          continue;
        }

        if (adim.rota !== null && pathname !== adim.rota) {
          router.push(adim.rota);
          // Rota değişimi bu effect'i pathname bağımlılığı üzerinden yeniden tetikleyecek.
          return;
        }

        const el = await anchorBekle(adim.anchor, { zamanAsimi: 4000 });
        if (calismaKimligi.current !== buCalisma) return;

        if (!el) {
          telemetriGonder("tur.anchor-bulunamadi", { tur: aktifTur!.kimlik, anchor: adim.anchor });
          indeks++;
          adim = aktifTur!.adimlar[indeks];
          continue;
        }

        el.scrollIntoView({ block: "center", behavior: azaltilmisHareketMi() ? "auto" : "smooth" });
        if (indeks !== adimIndeksi) setAdimIndeksi(indeks);
        setMotor({ asama: "gosteriliyor", anchorEl: el, rect: dikdortgenOlustur(el) });
        telemetriGonder("tur.adim-goruldu", { tur: aktifTur!.kimlik, adim: adim.kimlik, indeks });
        return;
      }

      // Tüm kalan adımlar atlandı — tur biter.
      turuKapat("tamamlandi");
    }

    calistir();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [aktifTurKimligi, adimIndeksi, pathname]);

  // Gösterilen anchor'ın konumunu resize/scroll'da güncel tut.
  useEffect(() => {
    if (motor.asama !== "gosteriliyor") return;
    function guncelle() {
      if (motor.asama === "gosteriliyor") {
        setMotor({ ...motor, rect: dikdortgenOlustur(motor.anchorEl) });
      }
    }
    window.addEventListener("resize", guncelle);
    window.addEventListener("scroll", guncelle, true);
    return () => {
      window.removeEventListener("resize", guncelle);
      window.removeEventListener("scroll", guncelle, true);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [motor.asama === "gosteriliyor" ? motor.anchorEl : null]);

  // anchor-tikla ilerlemesi: kullanıcı gerçek anchor'a tıklarsa tur ilerler (uygulamanın kendi
  // onClick'i de normal şekilde çalışmaya devam eder, preventDefault YAPILMAZ).
  useEffect(() => {
    if (motor.asama !== "gosteriliyor" || !aktifAdim) return;
    if (typeof aktifAdim.ilerleme !== "object" || aktifAdim.ilerleme.tip !== "anchor-tikla") return;

    function tiklamaDinle(e: MouseEvent) {
      if (motor.asama === "gosteriliyor" && motor.anchorEl.contains(e.target as Node)) {
        telemetriGonder("tur.adim-ilerledi", { yol: "anchor-tikla" });
        ileri();
      }
    }
    document.addEventListener("click", tiklamaDinle, true);
    return () => document.removeEventListener("click", tiklamaDinle, true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [motor, aktifAdim]);

  function ileri() {
    if (!aktifTur) return;
    if (adimIndeksi >= aktifTur.adimlar.length - 1) {
      turuKapat("tamamlandi");
      return;
    }
    telemetriGonder("tur.adim-ilerledi", { yol: "ileri" });
    setAdimIndeksi((i) => i + 1);
  }

  function geri() {
    setAdimIndeksi((i) => Math.max(0, i - 1));
  }

  // Tanıtım turunu tetikleyen genel olay + üst menüdeki "?" (mevcut olay adı korunur).
  useEffect(() => {
    function dinle(e: Event) {
      const turKimligi = e instanceof CustomEvent && typeof e.detail === "string" ? e.detail : "tanitim";
      turuBaslat(turKimligi);
    }
    window.addEventListener("webhook-platformu:tur-baslat", dinle);
    return () => window.removeEventListener("webhook-platformu:tur-baslat", dinle);
  }, [turuBaslat]);

  // İlk giriş: organizasyon yüklendiğinde tanıtım turu hiç görülmediyse hoş geldin olayını
  // fırlatır — kendisi otomatik BAŞLAMAZ, karar hos-geldin-karti.tsx'te (§10.6, kullanıcı onayı).
  useEffect(() => {
    if (!organizasyon || pathname === "/giris") return;
    if (turDurumu(organizasyon.id, "tanitim") === undefined) {
      window.dispatchEvent(new Event("webhook-platformu:hos-geldin-goster"));
    }
  }, [organizasyon, pathname]);

  // Derin bağlantı: ?tur=...&adim=... — yalnız kayıtlı tur kimliklerinden (allowlist), §13.F.
  useEffect(() => {
    if (derinBaglantiIslendi.current) return;
    const turParam = searchParams.get("tur");
    if (!turParam) return;
    const bulunanTur = TUM_TURLAR.find((t) => t.kimlik === turParam);
    if (!bulunanTur) return;
    derinBaglantiIslendi.current = true;
    const adimParam = Number(searchParams.get("adim"));
    const baslangic = Number.isInteger(adimParam) && adimParam >= 0 && adimParam < bulunanTur.adimlar.length ? adimParam : 0;
    turuBaslat(bulunanTur.kimlik, baslangic);
  }, [searchParams, turuBaslat]);

  if (!aktifTur || !aktifAdim || pathname === "/giris") return null;
  if (motor.asama !== "gosteriliyor") return null;

  const spotlightGoster = aktifAdim.spotlight !== "yok";
  const etkilesim = aktifAdim.etkilesim ?? "engelli";

  return createPortal(
    <>
      {spotlightGoster && <TurSpotlight rect={motor.rect} />}
      <TurTiklamaKatmani rect={motor.rect} mod={etkilesim} />
      <TurPopover
        key={aktifAdim.kimlik}
        anchorEl={motor.anchorEl}
        yerlesim={aktifAdim.yerlesim}
        baslik={aktifAdim.baslik}
        metin={aktifAdim.metin}
        adimNo={adimIndeksi + 1}
        toplamAdim={aktifTur.adimlar.length}
        ilkAdim={adimIndeksi === 0}
        sonAdim={adimIndeksi === aktifTur.adimlar.length - 1}
        modalMi={etkilesim === "engelli"}
        onIleri={ileri}
        onGeri={geri}
        onGec={() => turuKapat("birakildi")}
        onKapat={() => turuKapat("birakildi")}
      />
    </>,
    document.body,
  );
}
