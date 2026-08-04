"use client";

import { usePathname } from "next/navigation";

/** Rota degistiginde icerigi yeniden mount ederek her sayfa gecisinde hafif bir giris animasyonu tetikler. */
export function SayfaGecisi({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  return (
    <div key={pathname} className="duration-300 animate-in fade-in-0 slide-in-from-bottom-2">
      {children}
    </div>
  );
}
