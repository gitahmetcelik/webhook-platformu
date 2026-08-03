import { Badge } from "@/components/ui/badge";
import type { TeslimatDurumu } from "@/lib/types";
import { cn } from "@/lib/utils";

const RENKLER: Record<TeslimatDurumu, string> = {
  KUYRUKTA: "bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300",
  BASARILI: "bg-green-100 text-green-800 dark:bg-green-950 dark:text-green-300",
  HATALI: "bg-orange-100 text-orange-800 dark:bg-orange-950 dark:text-orange-300",
  DLQ: "bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300",
  KALICI_HATA: "bg-red-200 text-red-900 dark:bg-red-900 dark:text-red-200",
  BEKLEMEDE: "bg-purple-100 text-purple-800 dark:bg-purple-950 dark:text-purple-300",
};

const ETIKETLER: Record<TeslimatDurumu, string> = {
  KUYRUKTA: "Kuyrukta",
  BASARILI: "Başarılı",
  HATALI: "Hatalı (yeniden deneniyor)",
  DLQ: "Ölü Mektup Kutusu",
  KALICI_HATA: "Kalıcı Hata",
  BEKLEMEDE: "Beklemede (devre açık)",
};

export function TeslimatDurumRozeti({ durum, className }: { durum: TeslimatDurumu; className?: string }) {
  return <Badge className={cn(RENKLER[durum], "border-none", className)}>{ETIKETLER[durum]}</Badge>;
}
