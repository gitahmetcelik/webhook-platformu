"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { apiAnahtariniKaydet } from "@/lib/auth";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export default function GirisSayfasi() {
  const router = useRouter();
  const [anahtar, setAnahtar] = useState("");
  const [dogrulaniyor, setDogrulaniyor] = useState(false);

  async function girisYap() {
    setDogrulaniyor(true);
    try {
      const yanit = await fetch(`${API_URL}/v1/organizasyon/ben`, {
        headers: { Authorization: `Bearer ${anahtar}` },
      });
      if (!yanit.ok) {
        toast.error("Geçersiz API anahtarı");
        return;
      }
      const organizasyon = await yanit.json();
      apiAnahtariniKaydet(anahtar);
      toast.success(`Giriş yapıldı: ${organizasyon.ad}`);
      router.push("/olaylar");
    } catch {
      toast.error("Bağlantı hatası — backend ayakta mı?");
    } finally {
      setDogrulaniyor(false);
    }
  }

  return (
    <div className="mx-auto flex max-w-sm flex-col gap-4 pt-16">
      <h1 className="text-xl font-semibold">Webhook Platformu</h1>
      <p className="text-sm text-muted-foreground">
        Organizasyonunuzun API anahtarını girin (Ayarlar &gt; API Anahtarları altında üretilir).
      </p>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="anahtar">API Anahtarı</Label>
        <Input
          id="anahtar"
          value={anahtar}
          onChange={(e) => setAnahtar(e.target.value)}
          placeholder="whsk_live_..."
          className="font-mono text-sm"
          onKeyDown={(e) => e.key === "Enter" && girisYap()}
        />
      </div>
      <Button onClick={girisYap} disabled={dogrulaniyor || !anahtar}>
        {dogrulaniyor ? "Doğrulanıyor…" : "Giriş Yap"}
      </Button>
    </div>
  );
}
