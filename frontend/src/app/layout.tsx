import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { QueryProvider } from "@/components/query-provider";
import { AuthProvider } from "@/components/auth-provider";
import { UygulamaProvider } from "@/components/uygulama-provider";
import { UstMenu } from "@/components/ust-menu";
import { OnboardingTur } from "@/components/onboarding-tur";
import { SayfaGecisi } from "@/components/sayfa-gecisi";
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

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="tr"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="flex min-h-full flex-col bg-muted/20">
        <QueryProvider>
          <AuthProvider>
            <UygulamaProvider>
              <UstMenu />
              <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6">
                <SayfaGecisi>{children}</SayfaGecisi>
              </main>
              <OnboardingTur />
              <Toaster />
            </UygulamaProvider>
          </AuthProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
