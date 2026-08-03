import { Badge } from "@/components/ui/badge";
import type { DevreDurumu } from "@/lib/types";
import { cn } from "@/lib/utils";

const RENKLER: Record<DevreDurumu, string> = {
  KAPALI: "bg-green-100 text-green-800 dark:bg-green-950 dark:text-green-300",
  YARI_ACIK: "bg-orange-100 text-orange-800 dark:bg-orange-950 dark:text-orange-300",
  ACIK: "bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300",
};

const ETIKETLER: Record<DevreDurumu, string> = {
  KAPALI: "Sağlıklı",
  YARI_ACIK: "Yoklanıyor",
  ACIK: "Devre Açık",
};

export function DevreDurumRozeti({ durum, className }: { durum: DevreDurumu; className?: string }) {
  return <Badge className={cn(RENKLER[durum], "border-none", className)}>{ETIKETLER[durum]}</Badge>;
}
