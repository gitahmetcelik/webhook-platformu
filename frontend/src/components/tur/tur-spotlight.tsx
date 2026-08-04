"use client";

import { useId } from "react";

export type SpotlightDikdortgen = { x: number; y: number; w: number; h: number };

/**
 * Tek SVG maskesi — delik yumuşak morph olsun diye (§10.4). Karartma her zaman
 * `pointer-events: none` + `aria-hidden` — tıklama yönetimi ayrı katmanda (`TurTiklamaKatmani`).
 */
export function TurSpotlight({ rect }: { rect: SpotlightDikdortgen | null }) {
  const maskeId = useId();

  if (!rect) return null;

  return (
    <svg
      className="pointer-events-none fixed inset-0 z-[60] h-full w-full"
      aria-hidden="true"
    >
      <defs>
        <mask id={maskeId}>
          <rect width="100%" height="100%" fill="white" />
          <rect
            className="tur-maske-delik"
            x={rect.x - 6}
            y={rect.y - 6}
            width={rect.w + 12}
            height={rect.h + 12}
            rx={8}
            fill="black"
          />
        </mask>
      </defs>
      <rect
        width="100%"
        height="100%"
        fill="rgb(0 0 0 / 0.55)"
        mask={`url(#${maskeId})`}
      />
    </svg>
  );
}

/**
 * Tıklama yönetimi spotlight'tan AYRI, denetlenen bir katmanda (§10.4 / §13.F — spotlight'ın
 * kendisi asla tıklama yakalayıcısı olmaz).
 */
export function TurTiklamaKatmani({
  rect,
  mod,
  onDisariTikla,
}: {
  rect: SpotlightDikdortgen | null;
  mod: "engelli" | "sadece-anchor" | "serbest";
  onDisariTikla?: () => void;
}) {
  if (mod === "serbest" || !rect) return null;

  if (mod === "engelli") {
    return <div className="fixed inset-0 z-[59]" onClick={(e) => e.preventDefault()} />;
  }

  // sadece-anchor: dört parçaya bölünmüş overlay, yalnız anchor dikdörtgeninin DIŞINI yutar.
  return (
    <>
      <div
        className="fixed inset-x-0 top-0 z-[59]"
        style={{ height: Math.max(rect.y, 0) }}
        onClick={onDisariTikla}
      />
      <div
        className="fixed inset-x-0 bottom-0 z-[59]"
        style={{ top: rect.y + rect.h }}
        onClick={onDisariTikla}
      />
      <div
        className="fixed z-[59]"
        style={{ top: rect.y, height: rect.h, left: 0, width: Math.max(rect.x, 0) }}
        onClick={onDisariTikla}
      />
      <div
        className="fixed z-[59]"
        style={{ top: rect.y, height: rect.h, left: rect.x + rect.w, right: 0 }}
        onClick={onDisariTikla}
      />
    </>
  );
}
