"use client";

import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import {
  Activity,
  BarChart3,
  ClipboardList,
  Radio,
  Send,
  Webhook,
  type LucideIcon,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { turTamamlandiMi, turuTamamla } from "@/lib/tur";

type Adim = {
  icon: LucideIcon;
  baslik: string;
  aciklama: string;
};

const ADIMLAR: Adim[] = [
  {
    icon: Webhook,
    baslik: "Webhook Platformu'na hoş geldiniz",
    aciklama:
      "Bu birkaç ekranlık kısa bir tur — her sayfanın ne işe yaradığını gösterir. İstediğiniz an Geç'e basıp atlayabilirsiniz.",
  },
  {
    icon: Activity,
    baslik: "Ana Sayfa ve Olaylar",
    aciklama:
      "Ana sayfa; kota, endpoint sayısı, açık devre ve sağlık uyarılarını tek bakışta özetler. Olaylar sayfasında ise uygulamanıza gelen her event'i ve tetiklediği teslimatları görürsünüz.",
  },
  {
    icon: Radio,
    baslik: "Endpoint'ler",
    aciklama:
      "Abone endpoint'lerinizi buradan yönetirsiniz: retry profili, olay filtresi, son 24 saatlik başarı oranı, sağlık skoru ve devre kesici durumu. Devre açılırsa tek tıkla sıfırlayabilirsiniz.",
  },
  {
    icon: Send,
    baslik: "Test Aracı",
    aciklama:
      "Terminale hiç dokunmadan, hazır senaryolardan bir event seçip gönderin — gerçek bir teslimatı tetikler ve doğrudan sonuç sayfasına yönlendirir.",
  },
  {
    icon: BarChart3,
    baslik: "Kullanım",
    aciklama: "Aylık kotanızı, günlük başarı/başarısızlık dökümünü ve API anahtarlarınızı buradan yönetirsiniz.",
  },
  {
    icon: ClipboardList,
    baslik: "Audit Log",
    aciklama:
      "Devre sıfırlama, secret rotasyonu, API anahtarı üretimi gibi hassas her aksiyon burada kalıcı olarak izlenir.",
  },
];

export function OnboardingTur() {
  const pathname = usePathname();
  const [acik, setAcik] = useState(false);
  const [adim, setAdim] = useState(0);

  useEffect(() => {
    // /giris'te henuz kimlik dogrulanmadi - tur burada gosterilirse giris formunu kapatir
    // (gercekten calistirilinca bulundu). Sadece giris sonrasi sayfalarda tetiklenir.
    if (pathname !== "/giris" && !turTamamlandiMi()) {
      setAcik(true);
    }
    // Tur her yerden yeniden başlatılabilsin diye (bkz ust-menu.tsx "Tur" butonu).
    function dinle() {
      setAdim(0);
      setAcik(true);
    }
    window.addEventListener("webhook-platformu:tur-baslat", dinle);
    return () => window.removeEventListener("webhook-platformu:tur-baslat", dinle);
  }, [pathname]);

  if (!acik || pathname === "/giris") return null;

  function kapat() {
    turuTamamla();
    setAcik(false);
  }

  const { icon: Icon, baslik, aciklama } = ADIMLAR[adim];
  const sonAdim = adim === ADIMLAR.length - 1;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 duration-200 animate-in fade-in-0">
      <div className="w-full max-w-sm rounded-xl bg-popover p-5 text-popover-foreground shadow-xl ring-1 ring-foreground/10 duration-200 animate-in zoom-in-95 fade-in-0">
        <div className="flex flex-col items-center gap-3 text-center">
          <span className="flex size-12 items-center justify-center rounded-xl bg-primary/10 text-primary">
            <Icon className="size-6" />
          </span>
          <h2 className="text-base font-semibold tracking-tight">{baslik}</h2>
          <p className="text-sm text-muted-foreground">{aciklama}</p>
        </div>

        <div className="mt-5 flex items-center justify-center gap-1.5">
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

        <div className="mt-5 flex items-center justify-between gap-2">
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
              {sonAdim ? "Başlayalım" : "İleri"}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
