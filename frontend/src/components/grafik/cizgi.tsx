"use client";

import { useId, useState } from "react";
import { ChevronDown, ChevronUp } from "lucide-react";
import { Button } from "@/components/ui/button";
import { SERI_CSS_DEGISKENI, type Seri } from "./tipler";

/**
 * Tek veya çift serili çizgi grafik. Tek seri → başlık seriyi adlandırır, efsane yok.
 * Çift seri → doğrudan etiket (§7.1). Her grafiğin "Tabloyu göster" alternatifi vardır (§7.3).
 */
export function CizgiGrafik({
  seriler,
  genislik = 480,
  yukseklik = 160,
  className,
}: {
  seriler: [Seri] | [Seri, Seri];
  genislik?: number;
  yukseklik?: number;
  className?: string;
}) {
  const gradientId = useId();
  const [tabloAcik, setTabloAcik] = useState(false);
  const [imlecIndeksi, setImlecIndeksi] = useState<number | null>(null);

  const uzunluk = seriler[0].veri.length;
  const tumDegerler = seriler.flatMap((s) => s.veri.map((v) => v.deger));
  const min = Math.min(0, ...tumDegerler);
  const max = Math.max(...tumDegerler, 1);
  const aralik = max - min || 1;
  const kenarBosluk = { sol: 4, sag: 4, ust: 8, alt: 20 };
  const cizimGenislik = genislik - kenarBosluk.sol - kenarBosluk.sag;
  const cizimYukseklik = yukseklik - kenarBosluk.ust - kenarBosluk.alt;
  const adim = uzunluk > 1 ? cizimGenislik / (uzunluk - 1) : 0;

  function nokta(deger: number, i: number) {
    const x = kenarBosluk.sol + i * adim;
    const y = kenarBosluk.ust + cizimYukseklik - ((deger - min) / aralik) * cizimYukseklik;
    return { x, y };
  }

  return (
    <div className={className}>
      <div className="flex items-baseline justify-between">
        <h4 className="text-sm font-medium text-foreground">
          {seriler.length === 1
            ? seriler[0].ad
            : seriler.map((s) => s.ad).join(" / ")}
        </h4>
        {seriler.length === 2 && (
          <div className="flex items-center gap-3 text-xs text-muted-foreground">
            {seriler.map((s) => (
              <span key={s.ad} className="flex items-center gap-1">
                <span
                  className="size-2 rounded-full"
                  style={{ background: SERI_CSS_DEGISKENI[s.renk] }}
                  aria-hidden="true"
                />
                {s.ad}
              </span>
            ))}
          </div>
        )}
      </div>

      {!tabloAcik ? (
        <svg
          width="100%"
          height={yukseklik}
          viewBox={`0 0 ${genislik} ${yukseklik}`}
          role="img"
          aria-label={`${seriler.map((s) => s.ad).join(" ve ")} zaman içinde`}
          onMouseLeave={() => setImlecIndeksi(null)}
          onMouseMove={(e) => {
            const rect = e.currentTarget.getBoundingClientRect();
            const oranX = (e.clientX - rect.left) / rect.width;
            const indeks = Math.round(oranX * (uzunluk - 1));
            setImlecIndeksi(Math.min(Math.max(indeks, 0), uzunluk - 1));
          }}
        >
          <defs>
            <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--color-border)" stopOpacity={0.6} />
              <stop offset="100%" stopColor="var(--color-border)" stopOpacity={0.6} />
            </linearGradient>
          </defs>
          <line
            x1={kenarBosluk.sol}
            y1={kenarBosluk.ust + cizimYukseklik}
            x2={genislik - kenarBosluk.sag}
            y2={kenarBosluk.ust + cizimYukseklik}
            stroke="var(--color-border)"
            strokeWidth={1}
          />
          {seriler.map((seri) => (
            <path
              key={seri.ad}
              d={`M${seri.veri.map((v, i) => { const p = nokta(v.deger, i); return `${p.x},${p.y}`; }).join(" L")}`}
              fill="none"
              stroke={SERI_CSS_DEGISKENI[seri.renk]}
              strokeWidth={2}
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          ))}
          {imlecIndeksi !== null && (
            <>
              <line
                x1={kenarBosluk.sol + imlecIndeksi * adim}
                y1={kenarBosluk.ust}
                x2={kenarBosluk.sol + imlecIndeksi * adim}
                y2={kenarBosluk.ust + cizimYukseklik}
                stroke="var(--color-border)"
                strokeWidth={1}
                strokeDasharray="3 3"
              />
              {seriler.map((seri) => {
                const v = seri.veri[imlecIndeksi];
                const p = nokta(v.deger, imlecIndeksi);
                return (
                  <circle
                    key={seri.ad}
                    cx={p.x}
                    cy={p.y}
                    r={4}
                    fill={SERI_CSS_DEGISKENI[seri.renk]}
                    stroke="var(--color-background)"
                    strokeWidth={2}
                  />
                );
              })}
            </>
          )}
        </svg>
      ) : (
        <div className="overflow-x-auto rounded-md border">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b bg-muted/50 text-left">
                <th className="px-2 py-1 font-medium">Etiket</th>
                {seriler.map((s) => (
                  <th key={s.ad} className="px-2 py-1 font-medium">{s.ad}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {seriler[0].veri.map((v, i) => (
                <tr key={v.etiket} className="border-b last:border-0">
                  <td className="px-2 py-1 text-muted-foreground">{v.etiket}</td>
                  {seriler.map((s) => (
                    <td key={s.ad} className="px-2 py-1">{s.veri[i]?.deger}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {imlecIndeksi !== null && !tabloAcik && (
        <p className="mt-1 text-xs text-muted-foreground" aria-live="polite">
          {seriler[0].veri[imlecIndeksi].etiket}:{" "}
          {seriler.map((s) => `${s.ad} ${s.veri[imlecIndeksi]?.deger}`).join(" · ")}
        </p>
      )}

      <Button
        variant="ghost"
        size="sm"
        className="mt-1 h-auto gap-1 px-1 py-0.5 text-xs text-muted-foreground"
        onClick={() => setTabloAcik((v) => !v)}
      >
        {tabloAcik ? <ChevronUp className="size-3" /> : <ChevronDown className="size-3" />}
        {tabloAcik ? "Grafiği göster" : "Tabloyu göster"}
      </Button>
    </div>
  );
}
