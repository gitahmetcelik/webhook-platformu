"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";
import { Webhook } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
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
      router.push("/");
    } catch {
      toast.error("Bağlantı hatası — backend ayakta mı?");
    } finally {
      setDogrulaniyor(false);
    }
  }

  return (
    <div className="flex min-h-[calc(100vh-1px)] items-center justify-center bg-muted/30 px-4">
      <Card className="w-full max-w-sm">
        <CardContent className="flex flex-col gap-4">
          <div className="flex flex-col items-center gap-2 pb-2 text-center">
            <span className="flex size-11 items-center justify-center rounded-xl bg-primary text-primary-foreground">
              <Webhook className="size-5" />
            </span>
            <h1 className="text-lg font-semibold tracking-tight">Webhook Platformu</h1>
            <p className="text-sm text-muted-foreground">
              Organizasyonunuzun API anahtarını girin (Kullanım &gt; API Anahtarları altında üretilir).
            </p>
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="anahtar">API Anahtarı</Label>
            <Input
              id="anahtar"
              value={anahtar}
              onChange={(e) => setAnahtar(e.target.value)}
              placeholder="whsk_live_..."
              className="font-mono text-sm"
              onKeyDown={(e) => e.key === "Enter" && girisYap()}
              autoFocus
            />
          </div>
          <Button onClick={girisYap} disabled={dogrulaniyor || !anahtar}>
            {dogrulaniyor ? "Doğrulanıyor…" : "Giriş Yap"}
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
