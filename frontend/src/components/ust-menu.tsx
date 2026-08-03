"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

const baglantilar = [
  { href: "/olaylar", etiket: "Olaylar" },
  { href: "/endpointler", etiket: "Endpoint'ler" },
  { href: "/test", etiket: "Test Aracı" },
];

export function UstMenu() {
  const yol = usePathname();

  return (
    <header className="border-b bg-background">
      <div className="mx-auto flex max-w-6xl items-center gap-6 px-4 py-3">
        <span className="font-semibold">Webhook Platformu</span>
        <nav className="flex gap-4">
          {baglantilar.map((baglanti) => (
            <Link
              key={baglanti.href}
              href={baglanti.href}
              className={cn(
                "text-sm text-muted-foreground hover:text-foreground",
                yol?.startsWith(baglanti.href) && "font-medium text-foreground",
              )}
            >
              {baglanti.etiket}
            </Link>
          ))}
        </nav>
      </div>
    </header>
  );
}
