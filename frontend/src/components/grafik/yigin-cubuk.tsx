"use client";

/**
 * Yatay yığın çubuk — parça-bütün ilişkisi için (durum dağılımı vb, §7.1). Pasta grafik yerine.
 * Segment renkleri durum token'larından gelir (§5.3) — bu bir kategorik seri paleti değildir.
 */
export function YiginCubuk({
  segmentler,
  className,
}: {
  segmentler: { etiket: string; deger: number; renkSinifi: string }[];
  className?: string;
}) {
  const toplam = segmentler.reduce((t, s) => t + s.deger, 0);

  if (toplam === 0) {
    return (
      <div className={className}>
        <div className="h-3 w-full rounded-full bg-muted" />
        <p className="mt-1 text-xs text-muted-foreground">Veri yok</p>
      </div>
    );
  }

  return (
    <div className={className}>
      <div
        className="flex h-3 w-full overflow-hidden rounded-full"
        role="img"
        aria-label={segmentler.map((s) => `${s.etiket}: ${s.deger}`).join(", ")}
      >
        {segmentler
          .filter((s) => s.deger > 0)
          .map((s, i) => (
            <div
              key={s.etiket}
              className={`${s.renkSinifi} h-full ${i > 0 ? "ml-0.5" : ""}`}
              style={{ flexGrow: s.deger, flexBasis: 0 }}
              title={`${s.etiket}: ${s.deger}`}
            />
          ))}
      </div>
      <ul className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
        {segmentler.map((s) => (
          <li key={s.etiket} className="flex items-center gap-1.5">
            <span className={`size-2 rounded-full ${s.renkSinifi}`} aria-hidden="true" />
            {s.etiket}: <span className="font-medium text-foreground">{s.deger}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
