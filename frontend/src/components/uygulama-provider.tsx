"use client";

import { useQuery } from "@tanstack/react-query";
import { createContext, useContext } from "react";
import { api } from "@/lib/api";
import type { Uygulama } from "@/lib/types";

/**
 * Bu fazda gerçek çok-kiracılık/auth yok (bkz Faz 4) — dashboard tek organizasyon
 * varsayımıyla ilk uygulamayı context olarak kullanıyor.
 */
const UygulamaContext = createContext<{ uygulama: Uygulama | undefined; yukleniyor: boolean }>({
  uygulama: undefined,
  yukleniyor: true,
});

export function UygulamaProvider({ children }: { children: React.ReactNode }) {
  const { data, isLoading } = useQuery({
    queryKey: ["uygulamalar"],
    queryFn: api.uygulamalar.listele,
  });

  return (
    <UygulamaContext.Provider value={{ uygulama: data?.[0], yukleniyor: isLoading }}>
      {children}
    </UygulamaContext.Provider>
  );
}

export function useAktifUygulama() {
  return useContext(UygulamaContext);
}
