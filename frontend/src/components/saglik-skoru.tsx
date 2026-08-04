/**
 * Endpoint sağlık skoru göstergesi (bkz Faz 5.3). Skor 0-100: son 24 saatteki başarı oranının
 * %70'i + ortalama gecikmenin %30'u. Son 24 saatte hiç trafik yoksa skor `null` — bu "kötü"
 * değil "bilinmiyor" demek, o yüzden nötr gösteriliyor.
 */
export function SaglikSkoru({
  skor,
  uyariAktif,
  ortalamaGecikmeMs,
}: {
  skor: number | null;
  uyariAktif: boolean;
  ortalamaGecikmeMs: number | null;
}) {
  if (skor === null) {
    return <span className="text-muted-foreground">—</span>;
  }

  const renk = uyariAktif ? "text-red-600" : skor >= 90 ? "text-green-600" : "text-amber-600";
  const gecikme = ortalamaGecikmeMs === null ? null : `${Math.round(ortalamaGecikmeMs)}ms`;

  return (
    <span
      className={`font-medium ${renk}`}
      title={gecikme ? `Ortalama gecikme: ${gecikme}` : undefined}
    >
      {skor}
      {uyariAktif && " ⚠"}
    </span>
  );
}
