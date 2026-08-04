"use client";

import { useEffect, useState } from "react";

function goreceliMetin(tarih: Date, simdi: Date): string {
  const farkSn = Math.round((simdi.getTime() - tarih.getTime()) / 1000);
  if (farkSn < 5) return "az önce";
  if (farkSn < 60) return `${farkSn} sn önce`;
  const farkDk = Math.round(farkSn / 60);
  if (farkDk < 60) return `${farkDk} dk önce`;
  const farkSaat = Math.round(farkDk / 60);
  if (farkSaat < 24) return `${farkSaat} sa önce`;
  const farkGun = Math.round(farkSaat / 24);
  return `${farkGun} gün önce`;
}

/** "3 dk önce" + hover'da tam ISO. Sunucu/istemci hidrasyon uyuşmazlığını önlemek için
 * göreceli metin yalnız mount sonrası hesaplanır; sunucu her zaman ISO tarihi basar. */
export function GoreceliZaman({ tarih, className }: { tarih: string; className?: string }) {
  const [monteEdildi, setMonteEdildi] = useState(false);

  useEffect(() => {
    setMonteEdildi(true);
  }, []);

  const d = new Date(tarih);
  const metin = monteEdildi ? goreceliMetin(d, new Date()) : d.toISOString();

  return (
    <time dateTime={d.toISOString()} title={d.toLocaleString("tr-TR")} className={className}>
      {metin}
    </time>
  );
}
