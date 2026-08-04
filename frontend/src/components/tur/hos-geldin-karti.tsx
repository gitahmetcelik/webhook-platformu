"use client";

import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import { Compass } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/components/auth-provider";
import { turDurumunuKaydet } from "@/lib/tur";

/**
 * İlk girişte turu ZORLA başlatmaz — önce onay kartı (§10.6, kullanıcı kararı). Yalnız
 * `webhook-platformu:hos-geldin-goster` olayına (bkz tur-saglayici.tsx) tepki verir.
 */
export function HosGeldinKarti() {
  const pathname = usePathname();
  const { organizasyon } = useAuth();
  const [acik, setAcik] = useState(false);

  useEffect(() => {
    function dinle() {
      setAcik(true);
    }
    window.addEventListener("webhook-platformu:hos-geldin-goster", dinle);
    return () => window.removeEventListener("webhook-platformu:hos-geldin-goster", dinle);
  }, []);

  if (!acik || pathname === "/giris" || !organizasyon) return null;

  function kapat(kaydet: boolean) {
    if (kaydet && organizasyon) {
      turDurumunuKaydet(organizasyon.id, "tanitim", { durum: "birakildi", adimIndeksi: 0 });
    }
    setAcik(false);
  }

  return (
    <div
      role="dialog"
      aria-labelledby="hos-geldin-baslik"
      className="fixed inset-x-0 bottom-4 z-[65] mx-auto flex w-full max-w-sm flex-col gap-3 rounded-xl border bg-popover p-4 text-popover-foreground shadow-xl duration-[var(--sure-temel)] animate-in fade-in-0 slide-in-from-bottom-2 sm:right-4 sm:left-auto"
    >
      <div className="flex items-center gap-2">
        <span className="flex size-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
          <Compass className="size-4" />
        </span>
        <h2 id="hos-geldin-baslik" className="text-sm font-semibold">
          2 dakikada gezelim mi?
        </h2>
      </div>
      <p className="text-sm text-muted-foreground">
        Panelin işleyişini kısa bir turla gösterebiliriz — istediğiniz an atlayabilirsiniz.
      </p>
      <div className="flex justify-end gap-2">
        <Button variant="ghost" size="sm" onClick={() => kapat(true)}>
          Kendim keşfederim
        </Button>
        <Button
          size="sm"
          onClick={() => {
            setAcik(false);
            window.dispatchEvent(new Event("webhook-platformu:tur-baslat"));
          }}
        >
          Evet, gezelim
        </Button>
      </div>
    </div>
  );
}
