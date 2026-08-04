"use client";

import { useEffect, useState } from "react";
import { Monitor, Moon, Sun } from "lucide-react";
import { useTheme } from "next-themes";
import { Button } from "@/components/ui/button";

const SIRA = ["system", "light", "dark"] as const;
const IKON = { system: Monitor, light: Sun, dark: Moon } as const;
const ETIKET = { system: "Sistem", light: "Açık", dark: "Koyu" } as const;

export function TemaAnahtari() {
  const { theme, setTheme } = useTheme();
  const [monteEdildi, setMonteEdildi] = useState(false);

  useEffect(() => setMonteEdildi(true), []);

  const gecerli = (monteEdildi ? theme : "system") as (typeof SIRA)[number];
  const Icon = IKON[gecerli] ?? Monitor;

  return (
    <Button
      variant="ghost"
      size="icon-sm"
      data-tur="menu-tema-anahtari"
      title={`Tema: ${ETIKET[gecerli] ?? "Sistem"} — değiştirmek için tıklayın`}
      onClick={() => {
        const indeks = SIRA.indexOf(gecerli);
        setTheme(SIRA[(indeks + 1) % SIRA.length]);
      }}
    >
      <Icon className="size-4" />
    </Button>
  );
}
