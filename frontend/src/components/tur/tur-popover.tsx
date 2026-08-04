"use client";

import { useEffect, useId, useRef } from "react";
import { autoUpdate, flip, offset, shift, useFloating, type Placement } from "@floating-ui/react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const YERLESIM_ESLEME: Record<string, Placement> = {
  top: "top",
  bottom: "bottom",
  left: "left",
  right: "right",
};

function odaklanabilirElemanlar(kok: HTMLElement): HTMLElement[] {
  return Array.from(
    kok.querySelectorAll<HTMLElement>(
      'a[href], button:not([disabled]), input:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
    ),
  );
}

export function TurPopover({
  anchorEl,
  yerlesim = "bottom",
  baslik,
  metin,
  adimNo,
  toplamAdim,
  ilkAdim,
  sonAdim,
  modalMi,
  onIleri,
  onGeri,
  onGec,
  onKapat,
}: {
  anchorEl: HTMLElement;
  yerlesim?: string;
  baslik: string;
  metin: string;
  adimNo: number;
  toplamAdim: number;
  ilkAdim: boolean;
  sonAdim: boolean;
  modalMi: boolean;
  onIleri: () => void;
  onGeri: () => void;
  onGec: () => void;
  onKapat: () => void;
}) {
  const baslikId = useId();
  const metinId = useId();
  const kutuRef = useRef<HTMLDivElement>(null);

  const { refs, floatingStyles } = useFloating({
    elements: { reference: anchorEl },
    placement: YERLESIM_ESLEME[yerlesim] ?? "bottom",
    middleware: [offset(12), flip(), shift({ padding: 8 })],
    whileElementsMounted: autoUpdate,
  });

  // Adım değişince odağı popover'a taşı; §10.3 adım 10.
  useEffect(() => {
    kutuRef.current?.focus();
  }, [baslik]);

  function tusYonet(e: React.KeyboardEvent) {
    const hedefEtiketAdi = (e.target as HTMLElement).tagName;
    const metinAlaninda = hedefEtiketAdi === "INPUT" || hedefEtiketAdi === "TEXTAREA";

    if (e.key === "Escape") {
      e.preventDefault();
      onKapat();
      return;
    }
    if (!metinAlaninda && e.key === "ArrowRight") {
      e.preventDefault();
      onIleri();
      return;
    }
    if (!metinAlaninda && e.key === "ArrowLeft" && !ilkAdim) {
      e.preventDefault();
      onGeri();
      return;
    }
    if (modalMi && e.key === "Tab" && kutuRef.current) {
      const elemanlar = odaklanabilirElemanlar(kutuRef.current);
      if (elemanlar.length === 0) return;
      const ilk = elemanlar[0];
      const son = elemanlar[elemanlar.length - 1];
      if (e.shiftKey && document.activeElement === ilk) {
        e.preventDefault();
        son.focus();
      } else if (!e.shiftKey && document.activeElement === son) {
        e.preventDefault();
        ilk.focus();
      }
    }
  }

  return (
    <div
      ref={(el) => {
        refs.setFloating(el);
        kutuRef.current = el;
      }}
      style={floatingStyles}
      role="dialog"
      aria-modal={modalMi}
      aria-labelledby={baslikId}
      aria-describedby={metinId}
      tabIndex={-1}
      onKeyDown={tusYonet}
      className={cn(
        "z-[70] w-80 max-w-[calc(100vw-24px)] rounded-xl bg-popover p-4 text-popover-foreground shadow-xl ring-1 ring-foreground/10",
        "duration-[var(--sure-temel)] animate-in fade-in-0 zoom-in-95",
      )}
    >
      <div aria-live="polite" className="sr-only">
        Adım {adimNo} / {toplamAdim}: {baslik}
      </div>

      <div className="flex items-start justify-between gap-2">
        <h2 id={baslikId} className="text-sm font-semibold tracking-tight">
          {baslik}
        </h2>
        <Button variant="ghost" size="icon-xs" onClick={onKapat} aria-label="Turu kapat">
          <X className="size-3.5" />
        </Button>
      </div>
      <p id={metinId} className="mt-1 text-sm text-muted-foreground">
        {metin}
      </p>

      <div className="mt-3 flex items-center justify-center gap-1.5">
        {Array.from({ length: toplamAdim }).map((_, i) => (
          <span
            key={i}
            className={cn(
              "h-1.5 rounded-full",
              i === adimNo - 1 ? "w-5 bg-primary" : "w-1.5 bg-muted",
            )}
          />
        ))}
      </div>

      <div className="mt-3 flex items-center justify-between gap-2">
        <Button variant="ghost" size="sm" onClick={onGec}>
          Geç
        </Button>
        <div className="flex gap-2">
          {!ilkAdim && (
            <Button variant="outline" size="sm" onClick={onGeri}>
              Geri
            </Button>
          )}
          <Button size="sm" onClick={onIleri}>
            {sonAdim ? "Bitir" : "İleri"}
          </Button>
        </div>
      </div>
    </div>
  );
}
