/**
 * Hedef `data-tur` elemanı DOM'a girene kadar MutationObserver ile bekler; rAF polling
 * yerine tek seferlik değişim bildirimi kullanır (§10.3 adım 5). Zaman aşımında (varsayılan
 * 4000ms) `null` döner — tur bu durumda adımı sessizce atlar, kullanıcıya hata göstermez.
 */
export function anchorBekle(
  anchor: string,
  { zamanAsimi = 4000 }: { zamanAsimi?: number } = {},
): Promise<HTMLElement | null> {
  return new Promise((resolve) => {
    const mevcut = document.querySelector<HTMLElement>(`[data-tur="${anchor}"]`);
    if (mevcut) {
      resolve(mevcut);
      return;
    }

    const gozlemci = new MutationObserver(() => {
      const el = document.querySelector<HTMLElement>(`[data-tur="${anchor}"]`);
      if (el) {
        temizle();
        resolve(el);
      }
    });

    const zamanlayici = window.setTimeout(() => {
      temizle();
      resolve(null);
    }, zamanAsimi);

    function temizle() {
      gozlemci.disconnect();
      window.clearTimeout(zamanlayici);
    }

    gozlemci.observe(document.body, { childList: true, subtree: true, attributes: true });
  });
}
