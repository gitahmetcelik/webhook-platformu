"use client";

import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import { Check, ListChecks, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/components/auth-provider";
import { turDurumu } from "@/lib/tur";
import { cn } from "@/lib/utils";

const MADDELER = [
  { turKimligi: "tanitim", etiket: "Platformu tanıyın" },
  { turKimligi: "endpoint-yonetimi", etiket: "Bir endpoint ekleyin ve yönetin" },
  { turKimligi: "test-araci", etiket: "Test aracıyla bir event gönderin" },
  { turKimligi: "kullanim-ve-kota", etiket: "Kota ve API anahtarınızı inceleyin" },
  { turKimligi: "audit-izi", etiket: "Audit izini keşfedin" },
] as const;

/** Sağ alttaki kurulum kontrol listesi (§8.3, §10.6) — tüm maddeler tamamlanınca kapanır. */
export function KontrolListesi() {
  const pathname = usePathname();
  const { organizasyon } = useAuth();
  const [kapatildi, setKapatildi] = useState(false);
  const [, zorlaGuncelle] = useState(0);

  // turDurumu localStorage'dan okunuyor; tur tamamlandıkça yeniden çizilmesi için basit bir
  // dinleyici (tur-saglayici aynı sekme içinde çalıştığından "storage" olayı tetiklenmez).
  useEffect(() => {
    function guncelle() {
      zorlaGuncelle((n) => n + 1);
    }
    window.addEventListener("webhook-platformu:tur-baslat", guncelle);
    const araligi = window.setInterval(guncelle, 2000);
    return () => {
      window.removeEventListener("webhook-platformu:tur-baslat", guncelle);
      window.clearInterval(araligi);
    };
  }, []);

  if (!organizasyon || pathname === "/giris" || kapatildi) return null;

  const durumlar = MADDELER.map((m) => ({
    ...m,
    tamamlandi: turDurumu(organizasyon.id, m.turKimligi)?.durum === "tamamlandi",
  }));

  const hepsiTamam = durumlar.every((d) => d.tamamlandi);
  if (hepsiTamam) return null;

  return (
    <div
      data-tur="kontrol-listesi"
      className="fixed right-4 bottom-4 z-30 w-72 rounded-xl border bg-popover p-4 text-popover-foreground shadow-lg"
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <ListChecks className="size-4 text-primary" />
          <h2 className="text-sm font-semibold">Kurulum kontrol listesi</h2>
        </div>
        <Button variant="ghost" size="icon-xs" aria-label="Kontrol listesini kapat" onClick={() => setKapatildi(true)}>
          <X className="size-3.5" />
        </Button>
      </div>
      <ul className="mt-3 flex flex-col gap-2">
        {durumlar.map((m) => (
          <li key={m.turKimligi}>
            <button
              type="button"
              onClick={() => window.dispatchEvent(new CustomEvent("webhook-platformu:tur-baslat", { detail: m.turKimligi }))}
              className="flex w-full items-center gap-2 rounded-md px-1.5 py-1 text-left text-sm hover:bg-muted"
            >
              <span
                className={cn(
                  "flex size-4 shrink-0 items-center justify-center rounded-full border",
                  m.tamamlandi ? "border-durum-iyi bg-durum-iyi/10 text-durum-iyi" : "border-muted-foreground/40",
                )}
              >
                {m.tamamlandi && <Check className="size-3" />}
              </span>
              <span className={cn(m.tamamlandi && "text-muted-foreground line-through")}>{m.etiket}</span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
