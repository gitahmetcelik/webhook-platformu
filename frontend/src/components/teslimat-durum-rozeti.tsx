import { CheckCircle2, CircleSlash, Clock, TriangleAlert, XCircle } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import type { TeslimatDurumu } from "@/lib/types";
import { cn } from "@/lib/utils";

const STILLER: Record<TeslimatDurumu, string> = {
  KUYRUKTA: "bg-seri-1/10 text-seri-1",
  BASARILI: "bg-durum-iyi/10 text-durum-iyi",
  HATALI: "bg-durum-uyari/15 text-durum-uyari",
  DLQ: "bg-durum-kritik/10 text-durum-kritik",
  KALICI_HATA: "bg-durum-kritik/10 text-durum-kritik",
  BEKLEMEDE: "bg-seri-1/10 text-seri-1",
};

const IKONLAR: Record<TeslimatDurumu, typeof CheckCircle2> = {
  KUYRUKTA: Clock,
  BASARILI: CheckCircle2,
  HATALI: TriangleAlert,
  DLQ: XCircle,
  KALICI_HATA: CircleSlash,
  BEKLEMEDE: Clock,
};

const ETIKETLER: Record<TeslimatDurumu, string> = {
  KUYRUKTA: "Kuyrukta",
  BASARILI: "Başarılı",
  HATALI: "Hatalı (yeniden deneniyor)",
  DLQ: "Ölü Mektup Kutusu",
  KALICI_HATA: "Kalıcı Hata (denenmedi)",
  BEKLEMEDE: "Beklemede (devre açık)",
};

export function TeslimatDurumRozeti({ durum, className }: { durum: TeslimatDurumu; className?: string }) {
  const Icon = IKONLAR[durum];
  return (
    <Badge className={cn(STILLER[durum], "gap-1 border-none", className)}>
      <Icon className="size-3.5" aria-hidden="true" />
      {ETIKETLER[durum]}
    </Badge>
  );
}
