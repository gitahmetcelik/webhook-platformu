/**
 * FE-B6 (§10.8) — kayıtlı tur anchor'ları ↔ koddaki gerçek `data-tur`/`dataTur` kullanımı
 * denetimi. Metin taraması yapar (TS derleyicisi çalıştırmaz), path alias çözümlemesine
 * ihtiyaç duymaz. `node --experimental-strip-types scripts/tur-anchor-denetimi.ts` ile çalışır.
 *
 * 1. lib/turlar/**\/*.ts içindeki tüm `anchor:` değerlerini toplar (kayıtlı anchor'lar).
 * 2. src/**\/*.tsx|ts içindeki tüm `data-tur=`/`dataTur=` kullanımlarını tarar — şablon
 *    literalleri (`nav-${...}`) bilinçli olarak beyaz listeye alınır (VARSAYIM: tek dinamik
 *    desen bu, ust-menu.tsx'teki `nav-${baglanti.href}`).
 * 3. Kayıtlı ama kodda olmayan anchor → hata (tur kırık).
 * 4. Kodda olan ama hiçbir turda kullanılmayan → uyarı (ölü anchor).
 * 5. Her `rota` değerinin src/app altında gerçek bir App Router rotasına karşılık geldiğini
 *    doğrular (`rota: null` atlanır — dinamik/app-güdümlü navigasyon, bkz lib/turlar/tipler.ts).
 */
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join, relative } from "node:path";

const KOK = join(import.meta.dirname, "..");
const TURLAR_DIZINI = join(KOK, "src/lib/turlar");
const SRC_DIZINI = join(KOK, "src");
const APP_DIZINI = join(KOK, "src/app");

const NAV_JOKER = "__NAV_JOKER__";

function dosyalariBul(dizin: string, uzantilar: string[]): string[] {
  const sonuc: string[] = [];
  for (const girdi of readdirSync(dizin)) {
    const tamYol = join(dizin, girdi);
    const bilgi = statSync(tamYol);
    if (bilgi.isDirectory()) {
      sonuc.push(...dosyalariBul(tamYol, uzantilar));
    } else if (uzantilar.some((u) => girdi.endsWith(u))) {
      sonuc.push(tamYol);
    }
  }
  return sonuc;
}

function kayitliAnchorlariTopla(): { anchor: string; tur: string }[] {
  const sonuc: { anchor: string; tur: string }[] = [];
  for (const dosya of dosyalariBul(TURLAR_DIZINI, [".ts"])) {
    if (dosya.endsWith("tipler.ts") || dosya.endsWith("index.ts")) continue;
    const icerik = readFileSync(dosya, "utf8");
    const turKimligiEslesme = icerik.match(/kimlik:\s*"([^"]+)"/);
    const turKimligi = turKimligiEslesme?.[1] ?? relative(TURLAR_DIZINI, dosya);
    for (const m of icerik.matchAll(/\banchor:\s*"([^"]+)"/g)) {
      sonuc.push({ anchor: m[1], tur: turKimligi });
    }
  }
  return sonuc;
}

function koddakiAnchorlariTopla(): Set<string> {
  const sonuc = new Set<string>();
  for (const dosya of dosyalariBul(SRC_DIZINI, [".tsx", ".ts"])) {
    if (dosya.includes(`${join("src", "lib", "turlar")}`) || dosya.includes(join("src", "lib", "tur-anchorlari.ts"))) {
      continue;
    }
    const icerik = readFileSync(dosya, "utf8");
    for (const m of icerik.matchAll(/data-tur=(\{[^}]*\}|"[^"]*")/g)) {
      if (m[1].includes("nav-${")) {
        sonuc.add(NAV_JOKER);
        continue;
      }
      for (const dm of m[1].matchAll(/"([a-zA-Z0-9/_-]+)"/g)) {
        sonuc.add(dm[1]);
      }
    }
    for (const m of icerik.matchAll(/\bdataTur=(\{[^}]*\}|"[^"]*")/g)) {
      for (const dm of m[1].matchAll(/"([a-zA-Z0-9/_-]+)"/g)) {
        sonuc.add(dm[1]);
      }
    }
  }
  return sonuc;
}

function gecerliRotalariTopla(): Set<string> {
  const sonuc = new Set<string>();
  function gez(dizin: string, ustYol: string) {
    let girdiler: string[];
    try {
      girdiler = readdirSync(dizin);
    } catch {
      return;
    }
    if (girdiler.includes("page.tsx")) sonuc.add(ustYol || "/");
    for (const girdi of girdiler) {
      const tamYol = join(dizin, girdi);
      if (statSync(tamYol).isDirectory()) {
        const segment = girdi.startsWith("[") ? "*" : girdi;
        gez(tamYol, `${ustYol}/${segment}`);
      }
    }
  }
  gez(APP_DIZINI, "");
  return sonuc;
}

function rotalariDenetle(gecerliRotalar: Set<string>): string[] {
  const hatalar: string[] = [];
  for (const dosya of dosyalariBul(TURLAR_DIZINI, [".ts"])) {
    if (dosya.endsWith("tipler.ts") || dosya.endsWith("index.ts")) continue;
    const icerik = readFileSync(dosya, "utf8");
    for (const m of icerik.matchAll(/\brota:\s*"([^"]+)"/g)) {
      const rota = m[1];
      const eslesiyor = [...gecerliRotalar].some((r) => {
        const desen = "^" + r.replace(/\*/g, "[^/]+") + "$";
        return new RegExp(desen).test(rota);
      });
      if (!eslesiyor) {
        hatalar.push(`  ${relative(KOK, dosya)}: rota "${rota}" src/app altında bir sayfaya karşılık gelmiyor`);
      }
    }
  }
  return hatalar;
}

const kayitliAnchorlar = kayitliAnchorlariTopla();
const koddakiAnchorlar = koddakiAnchorlariTopla();
const gecerliRotalar = gecerliRotalariTopla();

let hataVar = false;

const kirikAnchorlar = kayitliAnchorlar.filter(({ anchor }) => {
  if (anchor.startsWith("nav-")) return !koddakiAnchorlar.has(NAV_JOKER) && !koddakiAnchorlar.has(anchor);
  return !koddakiAnchorlar.has(anchor);
});

if (kirikAnchorlar.length > 0) {
  hataVar = true;
  console.error("HATA — kayıtlı ama kodda karşılığı olmayan anchor'lar (tur kırık):");
  for (const { anchor, tur } of kirikAnchorlar) {
    console.error(`  [${tur}] "${anchor}"`);
  }
}

const kayitliSet = new Set(kayitliAnchorlar.map((a) => a.anchor));
const olubAnchorlar = [...koddakiAnchorlar].filter((a) => a !== NAV_JOKER && !kayitliSet.has(a));
if (olubAnchorlar.length > 0) {
  console.warn("UYARI — kodda olan ama hiçbir turda kullanılmayan anchor'lar:");
  for (const a of olubAnchorlar) console.warn(`  "${a}"`);
}

const rotaHatalari = rotalariDenetle(gecerliRotalar);
if (rotaHatalari.length > 0) {
  hataVar = true;
  console.error("HATA — geçersiz rota'lar:");
  for (const h of rotaHatalari) console.error(h);
}

if (hataVar) {
  process.exit(1);
} else {
  console.log(`Tur anchor denetimi geçti — ${kayitliAnchorlar.length} kayıtlı anchor, ${koddakiAnchorlar.size} koddaki anchor.`);
}
