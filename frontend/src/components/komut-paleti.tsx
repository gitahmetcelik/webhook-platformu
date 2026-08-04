"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useTheme } from "next-themes";
import {
  Activity,
  BarChart3,
  ClipboardList,
  Compass,
  Monitor,
  Moon,
  Radio,
  Send,
  Sun,
} from "lucide-react";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

type Komut = {
  kimlik: string;
  etiket: string;
  grup: "Sayfaya git" | "Tema" | "Tur";
  icon: typeof Compass;
  calistir: () => void;
};

/** ⌘K komut paleti: sayfaya git, tur başlat, tema değiştir (§8.3). */
export function KomutPaleti() {
  const [acik, setAcik] = useState(false);
  const [sorgu, setSorgu] = useState("");
  const router = useRouter();
  const { setTheme } = useTheme();

  useEffect(() => {
    function tusDinleyici(e: KeyboardEvent) {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        setAcik((v) => !v);
      }
    }
    window.addEventListener("keydown", tusDinleyici);
    return () => window.removeEventListener("keydown", tusDinleyici);
  }, []);

  const komutlar: Komut[] = useMemo(
    () => [
      { kimlik: "git-olaylar", etiket: "Olaylar", grup: "Sayfaya git", icon: Activity, calistir: () => router.push("/olaylar") },
      { kimlik: "git-endpointler", etiket: "Endpoint'ler", grup: "Sayfaya git", icon: Radio, calistir: () => router.push("/endpointler") },
      { kimlik: "git-test", etiket: "Test Aracı", grup: "Sayfaya git", icon: Send, calistir: () => router.push("/test") },
      { kimlik: "git-kullanim", etiket: "Kullanım", grup: "Sayfaya git", icon: BarChart3, calistir: () => router.push("/kullanim") },
      { kimlik: "git-audit", etiket: "Audit", grup: "Sayfaya git", icon: ClipboardList, calistir: () => router.push("/audit") },
      { kimlik: "tema-acik", etiket: "Açık tema", grup: "Tema", icon: Sun, calistir: () => setTheme("light") },
      { kimlik: "tema-koyu", etiket: "Koyu tema", grup: "Tema", icon: Moon, calistir: () => setTheme("dark") },
      { kimlik: "tema-sistem", etiket: "Sistem teması", grup: "Tema", icon: Monitor, calistir: () => setTheme("system") },
      {
        kimlik: "tur-baslat",
        etiket: "Tanıtım turunu başlat",
        grup: "Tur",
        icon: Compass,
        calistir: () => window.dispatchEvent(new Event("webhook-platformu:tur-baslat")),
      },
    ],
    [router, setTheme],
  );

  const filtrelenmis = komutlar.filter((k) =>
    k.etiket.toLocaleLowerCase("tr").includes(sorgu.toLocaleLowerCase("tr")),
  );

  function sec(komut: Komut) {
    komut.calistir();
    setAcik(false);
    setSorgu("");
  }

  return (
    <Dialog
      open={acik}
      onOpenChange={(deger) => {
        setAcik(deger);
        if (!deger) setSorgu("");
      }}
    >
      <DialogContent className="top-[20%] max-w-md translate-y-0 p-0" showCloseButton={false}>
        <DialogTitle className="sr-only">Komut paleti</DialogTitle>
        <Input
          autoFocus
          value={sorgu}
          onChange={(e) => setSorgu(e.target.value)}
          placeholder="Sayfaya git, tur başlat, tema değiştir…"
          className="rounded-b-none border-x-0 border-t-0 focus-visible:ring-0"
        />
        <ul role="listbox" aria-label="Komutlar" className="max-h-72 overflow-y-auto p-1">
          {filtrelenmis.length === 0 && (
            <li className="px-3 py-6 text-center text-sm text-muted-foreground">Sonuç yok</li>
          )}
          {filtrelenmis.map((komut) => (
            <li key={komut.kimlik}>
              <button
                type="button"
                role="option"
                aria-selected={false}
                onClick={() => sec(komut)}
                className={cn(
                  "flex w-full items-center gap-2 rounded-md px-3 py-2 text-left text-sm",
                  "hover:bg-muted focus-visible:bg-muted focus-visible:outline-none",
                )}
              >
                <komut.icon className="size-4 text-muted-foreground" aria-hidden="true" />
                {komut.etiket}
                <span className="ml-auto text-xs text-muted-foreground">{komut.grup}</span>
              </button>
            </li>
          ))}
        </ul>
      </DialogContent>
    </Dialog>
  );
}
