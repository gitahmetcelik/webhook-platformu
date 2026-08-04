"use client";

import { useId } from "react";
import { SERI_CSS_DEGISKENI, type SeriRengi } from "./tipler";

/**
 * Tek seri, tek güncel değer + trend için. Çizgi uzunluğu ilk render'da bir kez
 * 0'dan 1'e animasyonlanır (§7.3); reduced-motion'da animasyonsuz çizilir.
 */
export function Sparkline({
  veri,
  renk = "seri-1",
  genislik = 96,
  yukseklik = 28,
  className,
}: {
  veri: number[];
  renk?: SeriRengi;
  genislik?: number;
  yukseklik?: number;
  className?: string;
}) {
  const gradientId = useId();

  if (veri.length < 2) {
    return (
      <svg width={genislik} height={yukseklik} className={className} aria-hidden="true">
        <line
          x1={0}
          y1={yukseklik / 2}
          x2={genislik}
          y2={yukseklik / 2}
          stroke="var(--color-border)"
          strokeWidth={2}
        />
      </svg>
    );
  }

  const min = Math.min(...veri);
  const max = Math.max(...veri);
  const aralik = max - min || 1;
  const adim = genislik / (veri.length - 1);

  const noktalar = veri.map((deger, i) => {
    const x = i * adim;
    const y = yukseklik - ((deger - min) / aralik) * (yukseklik - 4) - 2;
    return `${x},${y}`;
  });

  const yol = `M${noktalar.join(" L")}`;
  const dolguYolu = `${yol} L${genislik},${yukseklik} L0,${yukseklik} Z`;

  return (
    <svg
      width={genislik}
      height={yukseklik}
      viewBox={`0 0 ${genislik} ${yukseklik}`}
      className={className}
      role="img"
      aria-label={`Trend: ${veri.length} veri noktası, son değer ${veri[veri.length - 1]}`}
    >
      <defs>
        <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={SERI_CSS_DEGISKENI[renk]} stopOpacity={0.25} />
          <stop offset="100%" stopColor={SERI_CSS_DEGISKENI[renk]} stopOpacity={0} />
        </linearGradient>
      </defs>
      <path d={dolguYolu} fill={`url(#${gradientId})`} stroke="none" />
      <path
        d={yol}
        fill="none"
        stroke={SERI_CSS_DEGISKENI[renk]}
        strokeWidth={2}
        strokeLinecap="round"
        strokeLinejoin="round"
        className="motion-safe:animate-[sparkline-cizim_600ms_var(--egri-standart)_forwards]"
        pathLength={1}
        style={{ strokeDasharray: 1, strokeDashoffset: 1 }}
      />
    </svg>
  );
}
