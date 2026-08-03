"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { api } from "@/lib/api";

export default function KullanimSayfasi() {
  const queryClient = useQueryClient();
  const [sonUretilen, setSonUretilen] = useState<string | null>(null);

  const { data: organizasyon } = useQuery({ queryKey: ["organizasyon-ben"], queryFn: api.organizasyon.ben });
  const { data: gunlukKullanim } = useQuery({ queryKey: ["kullanim"], queryFn: api.kullanim.listele });
  const { data: apiAnahtarlari } = useQuery({
    queryKey: ["api-anahtarlari"],
    queryFn: api.organizasyon.apiAnahtarlariListele,
  });

  const uretMutasyonu = useMutation({
    mutationFn: api.organizasyon.apiAnahtariUret,
    onSuccess: (yanit) => {
      setSonUretilen(yanit.anahtar);
      queryClient.invalidateQueries({ queryKey: ["api-anahtarlari"] });
      toast.success("Yeni API anahtarı üretildi — bir daha gösterilmeyecek, şimdi kopyalayın.");
    },
    onError: (hata: Error) => toast.error(hata.message),
  });

  const iptalMutasyonu = useMutation({
    mutationFn: api.organizasyon.apiAnahtariIptalEt,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["api-anahtarlari"] });
      toast.success("API anahtarı iptal edildi");
    },
    onError: (hata: Error) => toast.error(hata.message),
  });

  const kotaOrani = organizasyon ? Math.min(100, Math.round((organizasyon.buAyKullanim / organizasyon.aylikKota) * 100)) : 0;

  return (
    <div className="flex flex-col gap-8">
      <div>
        <h1 className="text-xl font-semibold">Kullanım</h1>
        {organizasyon && (
          <div className="mt-3 flex flex-col gap-2">
            <div className="flex items-center justify-between text-sm">
              <span>
                Bu ay: <strong>{organizasyon.buAyKullanim}</strong> / {organizasyon.aylikKota} teslimat
              </span>
              <span className="text-muted-foreground">%{kotaOrani}</span>
            </div>
            <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
              <div
                className={`h-full ${kotaOrani >= 100 ? "bg-destructive" : "bg-primary"}`}
                style={{ width: `${kotaOrani}%` }}
              />
            </div>
          </div>
        )}
      </div>

      <div>
        <h2 className="mb-2 text-sm font-medium text-muted-foreground">Günlük dağılım (bu ay)</h2>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Gün</TableHead>
              <TableHead>Toplam</TableHead>
              <TableHead>Başarılı</TableHead>
              <TableHead>Başarısız</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {(!gunlukKullanim || gunlukKullanim.length === 0) && (
              <TableRow>
                <TableCell colSpan={4} className="text-center text-muted-foreground">
                  Bu ay henüz kullanım yok.
                </TableCell>
              </TableRow>
            )}
            {gunlukKullanim?.map((g) => (
              <TableRow key={g.gun}>
                <TableCell className="text-sm text-muted-foreground">{g.gun}</TableCell>
                <TableCell>{g.teslimatSayisi}</TableCell>
                <TableCell>{g.basarili}</TableCell>
                <TableCell>{g.basarisiz}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <div>
        <div className="mb-2 flex items-center justify-between">
          <h2 className="text-sm font-medium text-muted-foreground">API Anahtarları</h2>
          <Button size="sm" onClick={() => uretMutasyonu.mutate()} disabled={uretMutasyonu.isPending}>
            Yeni Anahtar Üret
          </Button>
        </div>
        {sonUretilen && (
          <div className="mb-2 rounded-md border bg-muted p-3 font-mono text-sm break-all">
            {sonUretilen}
            <p className="mt-1 font-sans text-xs text-muted-foreground">
              Bu değer bir daha gösterilmeyecek — güvenli bir yere kaydedin.
            </p>
          </div>
        )}
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Anahtar</TableHead>
              <TableHead>Oluşturulma</TableHead>
              <TableHead>Durum</TableHead>
              <TableHead />
            </TableRow>
          </TableHeader>
          <TableBody>
            {apiAnahtarlari?.map((a) => (
              <TableRow key={a.id}>
                <TableCell className="font-mono text-sm">{a.anahtarOnek}</TableCell>
                <TableCell className="text-sm text-muted-foreground">
                  {new Date(a.olusturulma).toLocaleString("tr-TR")}
                </TableCell>
                <TableCell>{a.iptalEdilme ? "İptal edildi" : "Aktif"}</TableCell>
                <TableCell>
                  {!a.iptalEdilme && (
                    <Button variant="outline" size="sm" onClick={() => iptalMutasyonu.mutate(a.id)}>
                      İptal Et
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
