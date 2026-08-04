"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";
import { BarChart3, Key, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { PageHeader } from "@/components/page-header";
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

  const kotaOrani = organizasyon
    ? Math.min(100, Math.round((organizasyon.buAyKullanim / organizasyon.aylikKota) * 100))
    : 0;

  return (
    <div className="flex flex-col gap-6">
      <PageHeader icon={BarChart3} title="Kullanım" description="Aylık kota ve API anahtarı yönetimi" />

      <Card>
        <CardContent>
          {organizasyon && (
            <div className="flex flex-col gap-2" data-tur="kullanim-kota-cubugu">
              <div className="flex items-center justify-between text-sm">
                <span>
                  Bu ay: <strong className="tabular-nums">{organizasyon.buAyKullanim}</strong> /{" "}
                  {organizasyon.aylikKota} teslimat
                </span>
                <span className="font-medium text-muted-foreground tabular-nums">%{kotaOrani}</span>
              </div>
              <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
                <div
                  className={`h-full transition-all ${kotaOrani >= 100 ? "bg-destructive" : kotaOrani >= 80 ? "bg-amber-500" : "bg-primary"}`}
                  style={{ width: `${kotaOrani}%` }}
                />
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <Card size="sm" data-tur="kullanim-gunluk-dokum">
        <CardHeader>
          <CardTitle>Günlük dağılım (bu ay)</CardTitle>
        </CardHeader>
        <CardContent className="px-0">
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
                  <TableCell className="tabular-nums">{g.teslimatSayisi}</TableCell>
                  <TableCell className="tabular-nums text-emerald-600 dark:text-emerald-400">
                    {g.basarili}
                  </TableCell>
                  <TableCell className="tabular-nums text-destructive">{g.basarisiz}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Card size="sm">
        <CardHeader className="flex-row items-center justify-between">
          <CardTitle>API Anahtarları</CardTitle>
          <Button
            data-tur="kullanim-anahtar-buton"
            size="sm"
            onClick={() => uretMutasyonu.mutate()}
            disabled={uretMutasyonu.isPending}
          >
            <Key className="size-4" />
            Yeni Anahtar Üret
          </Button>
        </CardHeader>
        <CardContent className="flex flex-col gap-3 px-0">
          {sonUretilen && (
            <div className="mx-(--card-spacing) rounded-md border bg-muted p-3 font-mono text-sm break-all">
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
                  <TableCell>
                    {a.iptalEdilme ? (
                      <span className="text-muted-foreground">İptal edildi</span>
                    ) : (
                      <span className="text-emerald-600 dark:text-emerald-400">Aktif</span>
                    )}
                  </TableCell>
                  <TableCell>
                    {!a.iptalEdilme && (
                      <Button
                        variant="outline"
                        size="sm"
                        data-tur="kullanim-anahtar-iptal"
                        onClick={() => iptalMutasyonu.mutate(a.id)}
                      >
                        <Trash2 className="size-4" />
                        İptal Et
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}
