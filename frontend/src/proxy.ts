import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

// SS13.D MUST — panel abone sunucularinin yanit govdelerini/basliklarini ekrana basiyor, yani
// ucuncu taraf icerigi render ediyor. Nonce'li, strict-dynamic CSP burada. `unsafe-inline`/
// `unsafe-eval` HICBIR KOSULDA eklenmez (gelistirmede React'in eval kullanimi icin istisna var).
//
// VARSAYIM: nonce kullanimi TUM sayfalari dinamik render'a zorluyor (bkz Next dokumantasyonu
// content-security-policy.md "Static vs Dynamic Rendering with CSP") - bu revizyondan once 8
// rotanin cogu statikti (build ciktisinda "○"), bu degisiklikten sonra "ƒ" (sunucu-uretimli)
// olacaklar. Kabul edilen odun: statik CDN onbelleklemesi kaybedilir, karsiliginda script
// enjeksiyonuna karsi somut bir savunma katmani kazanilir.
//
// VARSAYIM: `require-trusted-types-for 'script'` (SPEC'in istedigi) BILINCLI OLARAK EKLENMEDI -
// gercekten denendi, Next/Turbopack'in kendi chunk yukleyicisi kayitli bir Trusted Types
// policy'si olmadan `script.src` atadigi icin TAM UYGULAMAYI KIRIYOR (tarayicida dogrulandi:
// "Failed to set the 'src' property on 'HTMLScriptElement': This document requires
// 'TrustedScriptURL' assignment"). Once Next'in kendi runtime'ini kapsayan bir varsayilan
// policy (`trustedTypes.createPolicy('default', ...)`) kayit edilmeden bu direktif eklenemez -
// SS18'e acik karar olarak eklenecek.
export function proxy(request: NextRequest) {
  const nonce = Buffer.from(crypto.randomUUID()).toString("base64");
  const isDev = process.env.NODE_ENV === "development";
  const apiTabani = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

  const cspHeader = `
    default-src 'self';
    script-src 'self' 'nonce-${nonce}' 'strict-dynamic'${isDev ? " 'unsafe-eval'" : ""};
    style-src 'self' 'nonce-${nonce}'${isDev ? " 'unsafe-inline'" : ""};
    img-src 'self' data:;
    font-src 'self';
    connect-src 'self' ${apiTabani};
    frame-src 'none';
    object-src 'none';
    base-uri 'none';
    form-action 'self';
    frame-ancestors 'none';
    upgrade-insecure-requests;
  `
    .replace(/\s{2,}/g, " ")
    .trim();

  const requestHeaders = new Headers(request.headers);
  requestHeaders.set("x-nonce", nonce);
  requestHeaders.set("Content-Security-Policy", cspHeader);

  const response = NextResponse.next({ request: { headers: requestHeaders } });

  response.headers.set("Content-Security-Policy", cspHeader);
  response.headers.set("X-Content-Type-Options", "nosniff");
  response.headers.set("X-Frame-Options", "DENY");
  response.headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
  response.headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
  response.headers.set("Cross-Origin-Opener-Policy", "same-origin");
  if (!isDev) {
    response.headers.set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload");
  }

  return response;
}

export const config = {
  matcher: [
    {
      source: "/((?!_next/static|_next/image|favicon.ico).*)",
      missing: [
        { type: "header", key: "next-router-prefetch" },
        { type: "header", key: "purpose", value: "prefetch" },
      ],
    },
  ],
};
