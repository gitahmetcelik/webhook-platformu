"use client";

import { useQuery } from "@tanstack/react-query";
import { usePathname } from "next/navigation";
import { createContext, useContext, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { apiAnahtariniOku } from "@/lib/auth";
import type { Uygulama } from "@/lib/types";

const UygulamaContext = createContext<{ uygulama: Uygulama | undefined; yukleniyor: boolean }>({
  uygulama: undefined,
  yukleniyor: true,
});

export function UygulamaProvider({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  // Bkz auth-provider.tsx - ayni sebep: localStorage sunucuda yok, mount sonrasina erteleniyor.
  const [anahtarVar, setAnahtarVar] = useState(false);
  useEffect(() => setAnahtarVar(apiAnahtariniOku() !== null), []);

  const { data, isLoading } = useQuery({
    queryKey: ["uygulamalar"],
    queryFn: api.uygulamalar.listele,
    enabled: anahtarVar && pathname !== "/giris",
    retry: false,
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
