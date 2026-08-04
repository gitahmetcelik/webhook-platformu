"use client";

import { CircleHelp } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { TUM_TURLAR } from "@/lib/turlar";

/** Üst menüdeki "?" — tüm turların listesi, herhangi biri yeniden başlatılabilir (§10.6). */
export function YardimMenusu() {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        render={
          <Button variant="ghost" size="icon-sm" data-tur="menu-yardim" title="Turlar" />
        }
      >
        <CircleHelp className="size-4" />
      </DropdownMenuTrigger>
      <DropdownMenuContent>
        {TUM_TURLAR.map((tur) => (
          <DropdownMenuItem
            key={tur.kimlik}
            onClick={() =>
              window.dispatchEvent(new CustomEvent("webhook-platformu:tur-baslat", { detail: tur.kimlik }))
            }
          >
            <div className="flex flex-col">
              <span className="font-medium">{tur.baslik}</span>
              <span className="text-xs text-muted-foreground">{tur.aciklama}</span>
            </div>
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
