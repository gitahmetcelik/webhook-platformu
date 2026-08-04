/** Tek tırnak içinde güvenli kabuk alıntılama: `'` -> `'\''` (§13.F — komut enjeksiyonu). */
function kabukAlinti(deger: string): string {
  return `'${deger.replace(/'/g, `'\\''`)}'`;
}

export function curlOlustur({
  url,
  yontem = "POST",
  basliklar,
  govde,
}: {
  url: string;
  yontem?: string;
  basliklar: Record<string, string>;
  govde: string;
}): string {
  const parcalar = [`curl -X ${kabukAlinti(yontem)}`, kabukAlinti(url)];
  for (const [ad, deger] of Object.entries(basliklar)) {
    parcalar.push(`-H ${kabukAlinti(`${ad}: ${deger}`)}`);
  }
  if (govde) {
    parcalar.push(`-d ${kabukAlinti(govde)}`);
  }
  return parcalar.join(" \\\n  ");
}
