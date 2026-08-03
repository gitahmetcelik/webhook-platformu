"use client";

import { useQuery } from "@tanstack/react-query";
import { usePathname, useRouter } from "next/navigation";
import { createContext, useContext, useEffect, useState } from "react";
import { apiAnahtariniOku, apiAnahtariniTemizle } from "@/lib/auth";
import { api } from "@/lib/api";
import type { OrganizasyonBen } from "@/lib/types";

const AuthContext = createContext<{ organizasyon: OrganizasyonBen | undefined; cikisYap: () => void }>({
  organizasyon: undefined,
  cikisYap: () => {},
});

/** Bkz Faz 4.1 — API anahtarı yoksa /giris'e yönlendirir, varsa organizasyon bilgisini context'e koyar. */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  // localStorage sunucuda yok - anahtar durumu SADECE mount sonrasi (client-only) okunur,
  // ilk render'da sunucuyla ayni ("kontrol edilmedi") degeri dondurmesi lazim, aksi halde
  // hydration mismatch olusuyor (gercekten calistirilinca tarayicida bulundu - bkz Faz 4.6).
  const [hazir, setHazir] = useState(false);
  const [anahtarVar, setAnahtarVar] = useState(false);

  useEffect(() => {
    const varMi = apiAnahtariniOku() !== null;
    setAnahtarVar(varMi);
    setHazir(true);
    if (!varMi && pathname !== "/giris") {
      router.replace("/giris");
    }
  }, [pathname, router]);

  const { data } = useQuery({
    queryKey: ["organizasyon-ben"],
    queryFn: api.organizasyon.ben,
    enabled: hazir && anahtarVar && pathname !== "/giris",
    retry: false,
  });

  function cikisYap() {
    apiAnahtariniTemizle();
    router.push("/giris");
  }

  if (pathname === "/giris") {
    return <>{children}</>;
  }
  if (!hazir || !anahtarVar) {
    return null;
  }

  return <AuthContext.Provider value={{ organizasyon: data, cikisYap }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
