"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Activity, BarChart3, CircleHelp, ClipboardList, LogOut, Radio, Send, Webhook } from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuth } from "@/components/auth-provider";
import { Button } from "@/components/ui/button";

const baglantilar = [
  { href: "/olaylar", etiket: "Olaylar", icon: Activity },
  { href: "/endpointler", etiket: "Endpoint'ler", icon: Radio },
  { href: "/test", etiket: "Test Aracı", icon: Send },
  { href: "/kullanim", etiket: "Kullanım", icon: BarChart3 },
  { href: "/audit", etiket: "Audit", icon: ClipboardList },
];

export function UstMenu() {
  const yol = usePathname();
  const { organizasyon, cikisYap } = useAuth();

  if (yol === "/giris") {
    return null;
  }

  return (
    <header className="sticky top-0 z-40 border-b bg-background/80 backdrop-blur-sm">
      <div className="mx-auto flex max-w-6xl items-center gap-1 px-4 py-3">
        <Link href="/" className="mr-4 flex items-center gap-2 font-semibold tracking-tight">
          <span className="flex size-7 items-center justify-center rounded-md bg-primary text-primary-foreground">
            <Webhook className="size-4" />
          </span>
          <span className="hidden sm:inline">Webhook Platformu</span>
        </Link>
        <nav className="flex items-center gap-1">
          {baglantilar.map((baglanti) => {
            const aktif = yol?.startsWith(baglanti.href);
            const Icon = baglanti.icon;
            return (
              <Link
                key={baglanti.href}
                href={baglanti.href}
                className={cn(
                  "flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm transition-colors",
                  aktif
                    ? "bg-primary/10 font-medium text-primary"
                    : "text-muted-foreground hover:bg-muted hover:text-foreground",
                )}
              >
                <Icon className="size-4" />
                <span className="hidden md:inline">{baglanti.etiket}</span>
              </Link>
            );
          })}
        </nav>
        <div className="ml-auto flex items-center gap-3">
          {organizasyon && (
            <span className="hidden text-sm text-muted-foreground sm:inline">{organizasyon.ad}</span>
          )}
          <Button
            variant="ghost"
            size="icon-sm"
            title="Tanıtım turunu başlat"
            onClick={() => window.dispatchEvent(new Event("webhook-platformu:tur-baslat"))}
          >
            <CircleHelp className="size-4" />
          </Button>
          <Button variant="outline" size="sm" onClick={cikisYap}>
            <LogOut className="size-4" />
            Çıkış
          </Button>
        </div>
      </div>
    </header>
  );
}
