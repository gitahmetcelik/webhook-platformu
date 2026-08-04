"use client";

import { useEffect, useRef, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import {
  Activity,
  BarChart3,
  ClipboardList,
  Radio,
  Send,
  type LucideIcon,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { turTamamlandiMi, turuTamamla } from "@/lib/tur";

type Adim = {
  route: string;
  anchor: string;
  icon: LucideIcon;
  baslik: string;
  aciklama: string;
};

const ADIMLAR: Adim[] = [
  {
    route: "/",
    anchor: "anasayfa-istatistikler",
    icon: Activity,
    baslik: "Ana Sayfa",
    aciklama:
      "Buradaki kartlar kota, endpoint sayısı, açık devre ve sağlık uyarılarını tek bakışta özetler — her sabah ilk bakacağınız yer.",
  },
  {
    route: "/olaylar",
    anchor: "olaylar-filtre",
    icon: Activity,
    baslik: "Olaylar",
    aciklama:
      "Uygulamanıza gelen her event burada listelenir. Bu kutuya bir olay tipi yazarak (örn. siparis.olusturuldu) listeyi daraltabilirsiniz.",
  },
  {
    route: "/endpointler",
    anchor: "endpoint-yeni-buton",
    icon: Radio,
    baslik: "Endpoint'ler",
    aciklama:
      "Abone endpoint'lerinizi buradan eklersiniz. Her satırda retry profili, son 24 saat başarı oranı, sağlık skoru ve devre kesici durumu görünür — devre açılırsa yine buradan tek tıkla sıfırlarsınız.",
  },
  {
    route: "/test",
    anchor: "test-gonder-buton",
    icon: Send,
    baslik: "Test Aracı",
    aciklama:
      "Terminale hiç dokunmadan bu düğmeyle gerçek bir event gönderip teslimatı tetikleyebilirsiniz — sonuç ekranına otomatik yönlendirilirsiniz.",
  },
  {
    route: "/kullanim",
    anchor: "kullanim-anahtar-buton",
    icon: BarChart3,
    baslik: "Kullanım",
    aciklama:
      "Aylık kotanızı ve günlük başarı/başarısızlık dökümünü bu sayfada izlersiniz. Yeni bir API anahtarı da buradaki düğmeyle üretilir.",
  },
  {
    route: "/audit",
    anchor: "audit-baslik",
    icon: ClipboardList,
    baslik: "Audit Log",
    aciklama:
      "Devre sıfırlama, secret rotasyonu, API anahtarı üretimi gibi hassas her aksiyon burada kalıcı olarak izlenir. Turu burada tamamlıyoruz — iyi çalışmalar!",
  },
];

const KUTU_GENISLIK = 320;
const BOSLUK = 12;

export function OnboardingTur() {
  const pathname = usePathname();
  const router = useRouter();
  const [acik, setAcik] = useState(false);
  const [adim, setAdim] = useState(0);
  const [kutu, setKutu] = useState<{ top: number; left: number; anchorRect: DOMRect } | null>(null);
  const zamanlayici = useRef<number | null>(null);

  const guncelAdim = ADIMLAR[adim];

  // Baslangic: giris sonrasi ilk kez gorulen kullanicida otomatik ac. /giris'te ASLA acilmaz -
  // AuthProvider orada kimlik dogrulamasi yapmadan children'i render ediyor, tur burada
  // acilirsa giris formunun tamamini kapatiyordu (gercekten calistirilinca bulundu).
  useEffect(() => {
    if (pathname !== "/giris" && !turTamamlandiMi()) {
      setAcik(true);
    }
    function dinle() {
      setAdim(0);
      setAcik(true);
    }
    window.addEventListener("webhook-platformu:tur-baslat", dinle);
    return () => window.removeEventListener("webhook-platformu:tur-baslat", dinle);
  }, [pathname]);

  // Adim degistiginde o adimin sayfasina git.
  useEffect(() => {
    if (!acik) return;
    if (pathname !== guncelAdim.route) {
      router.push(guncelAdim.route);
    }
  }, [acik, adim, guncelAdim.route, pathname, router]);

  // Hedef eleman DOM'da belirene kadar (rota degisimi/veri yuklemesi surebilir) polling ile
  // ara, bulununca konumunu olc. Eski konumu ANINDA silmiyoruz - rota degisimi sirasinda kisa
  // sureli titremeyi onlemek icin yeni konum bulunana kadar oncekini gosterir.
  useEffect(() => {
    if (!acik || pathname !== guncelAdim.route) return;
    let iptal = false;
    let deneme = 0;

    function konumHesapla(el: Element) {
      const r = el.getBoundingClientRect();
      const genislik = typeof window !== "undefined" ? window.innerWidth : 1024;
      const left = Math.min(Math.max(BOSLUK, r.left), genislik - KUTU_GENISLIK - BOSLUK);
      const altBosluk = typeof window !== "undefined" ? window.innerHeight - r.bottom : 999;
      const top = altBosluk > 220 ? r.bottom + BOSLUK : r.top - BOSLUK;
      setKutu({ top, left, anchorRect: r });
    }

    function dene() {
      if (iptal) return;
      const el = document.querySelector(`[data-tur="${guncelAdim.anchor}"]`);
      if (el) {
        konumHesapla(el);
        return;
      }
      if (deneme++ < 60) {
        zamanlayici.current = window.requestAnimationFrame(dene);
      }
    }
    dene();

    function yenidenHesapla() {
      const el = document.querySelector(`[data-tur="${guncelAdim.anchor}"]`);
      if (el) konumHesapla(el);
    }
    window.addEventListener("resize", yenidenHesapla);
    window.addEventListener("scroll", yenidenHesapla, true);

    return () => {
      iptal = true;
      if (zamanlayici.current) cancelAnimationFrame(zamanlayici.current);
      window.removeEventListener("resize", yenidenHesapla);
      window.removeEventListener("scroll", yenidenHesapla, true);
    };
  }, [acik, adim, pathname, guncelAdim.route, guncelAdim.anchor]);

  if (!acik || pathname === "/giris") return null;

  function kapat() {
    turuTamamla();
    setAcik(false);
    setKutu(null);
  }

  const sonAdim = adim === ADIMLAR.length - 1;
  const Icon = guncelAdim.icon;
  const yukarida = kutu ? kutu.top < (kutu.anchorRect?.top ?? 0) : false;

  return (
    <>
      {/* Sayfayla etkilesimi ENGELLEMEZ (pointer-events-none) - kullanici turu acikken bile
          arayuzu kullanabilsin diye, sadece hafif bir odak vurgusu saglar. */}
      <div className="pointer-events-none fixed inset-0 z-40 bg-background/40 duration-200 animate-in fade-in-0" />

      {kutu && (
        <div
          className="pointer-events-none fixed z-40 rounded-lg ring-2 ring-primary ring-offset-2 ring-offset-background transition-all duration-300"
          style={{
            top: kutu.anchorRect.top - 4,
            left: kutu.anchorRect.left - 4,
            width: kutu.anchorRect.width + 8,
            height: kutu.anchorRect.height + 8,
          }}
        />
      )}

      {kutu && (
        <div
          className={cn(
            "fixed z-50 w-80 rounded-xl bg-popover p-4 text-popover-foreground shadow-xl ring-1 ring-foreground/10 duration-200 animate-in fade-in-0",
            yukarida ? "slide-in-from-bottom-1" : "slide-in-from-top-1",
          )}
          style={{ top: kutu.top, left: kutu.left, maxWidth: `calc(100vw - ${BOSLUK * 2}px)` }}
        >
          <div className="flex items-start gap-3">
            <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <Icon className="size-4" />
            </span>
            <div className="flex flex-col gap-1">
              <h2 className="text-sm font-semibold tracking-tight">{guncelAdim.baslik}</h2>
              <p className="text-sm text-muted-foreground">{guncelAdim.aciklama}</p>
            </div>
          </div>

          <div className="mt-4 flex items-center justify-center gap-1.5">
            {ADIMLAR.map((_, i) => (
              <span
                key={i}
                className={cn(
                  "h-1.5 rounded-full transition-all",
                  i === adim ? "w-5 bg-primary" : "w-1.5 bg-muted",
                )}
              />
            ))}
          </div>

          <div className="mt-4 flex items-center justify-between gap-2">
            <Button variant="ghost" size="sm" onClick={kapat}>
              Geç
            </Button>
            <div className="flex gap-2">
              {adim > 0 && (
                <Button variant="outline" size="sm" onClick={() => setAdim((a) => a - 1)}>
                  Geri
                </Button>
              )}
              <Button size="sm" onClick={() => (sonAdim ? kapat() : setAdim((a) => a + 1))}>
                {sonAdim ? "Bitir" : "İleri"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
