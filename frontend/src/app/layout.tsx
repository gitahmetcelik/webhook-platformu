import type { Metadata } from "next";
import { Suspense } from "react";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { TemaSaglayici } from "@/components/tema-saglayici";
import { QueryProvider } from "@/components/query-provider";
import { AuthProvider } from "@/components/auth-provider";
import { UygulamaProvider } from "@/components/uygulama-provider";
import { UstMenu } from "@/components/ust-menu";
import { TurSaglayici } from "@/components/tur/tur-saglayici";
import { HosGeldinKarti } from "@/components/tur/hos-geldin-karti";
import { KontrolListesi } from "@/components/tur/kontrol-listesi";
import { SayfaGecisi } from "@/components/sayfa-gecisi";
import { KomutPaleti } from "@/components/komut-paleti";
import { Toaster } from "@/components/ui/sonner";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Webhook Platformu",
  description: "Webhook teslimat platformu — dashboard",
};

// Nonce'li CSP (bkz src/proxy.ts) her istekte taze bir nonce üretir; statik sayfalar build
// zamanında üretildiği için nonce alamaz. Kök layout'ta zorlanan dinamik render tüm rotalara
// kademeleniyor (bkz Next content-security-policy.md "Forcing dynamic rendering").
export const dynamic = "force-dynamic";

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="tr"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
      suppressHydrationWarning
    >
      <body className="flex min-h-full flex-col bg-muted/20">
        <TemaSaglayici>
          <QueryProvider>
            <AuthProvider>
              <UygulamaProvider>
                <UstMenu />
                <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6">
                  <SayfaGecisi>{children}</SayfaGecisi>
                </main>
                <Suspense fallback={null}>
                  <TurSaglayici />
                </Suspense>
                <HosGeldinKarti />
                <KontrolListesi />
                <KomutPaleti />
                <Toaster />
              </UygulamaProvider>
            </AuthProvider>
          </QueryProvider>
        </TemaSaglayici>
      </body>
    </html>
  );
}
