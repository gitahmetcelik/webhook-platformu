/** Seri rengi her zaman token'lardan biri — kategorik seride serbest renk yok (bkz §7.2). */
export type SeriRengi = "seri-1" | "seri-2" | "seri-3";

export type Seri = {
  ad: string;
  renk: SeriRengi;
  /** [x etiketi, y değeri] çiftleri, x sırasına göre. */
  veri: { etiket: string; deger: number }[];
};

/** En fazla üç seri — dördüncüsü tip seviyesinde eklenemez (bkz §7.2). */
export type SeriListesi = [Seri] | [Seri, Seri] | [Seri, Seri, Seri];

export const SERI_CSS_DEGISKENI: Record<SeriRengi, string> = {
  "seri-1": "var(--seri-1)",
  "seri-2": "var(--seri-2)",
  "seri-3": "var(--seri-3)",
};
