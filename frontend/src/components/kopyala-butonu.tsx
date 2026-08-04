"use client";

import { useState } from "react";
import { Check, Copy } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/** Kopyalama sonrası ikon 1.2 sn ✓'e döner (§6.1). */
export function KopyalaButonu({
  deger,
  etiket = "Kopyala",
  varyant = "outline",
  className,
}: {
  deger: string;
  etiket?: string;
  varyant?: "outline" | "ghost";
  className?: string;
}) {
  const [kopyalandi, setKopyalandi] = useState(false);

  async function kopyala() {
    await navigator.clipboard.writeText(deger);
    setKopyalandi(true);
    setTimeout(() => setKopyalandi(false), 1200);
  }

  return (
    <Button
      variant={varyant}
      size="sm"
      className={cn("gap-1.5", className)}
      onClick={kopyala}
    >
      {kopyalandi ? <Check className="size-3.5" /> : <Copy className="size-3.5" />}
      {kopyalandi ? "Kopyalandı" : etiket}
    </Button>
  );
}
