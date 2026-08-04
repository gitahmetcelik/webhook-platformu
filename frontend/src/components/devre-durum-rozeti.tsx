import { CheckCircle2, RotateCw, XCircle } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import type { DevreDurumu } from "@/lib/types";
import { cn } from "@/lib/utils";

const STILLER: Record<DevreDurumu, string> = {
  KAPALI: "bg-durum-iyi/10 text-durum-iyi",
  YARI_ACIK: "bg-durum-uyari/15 text-durum-uyari",
  ACIK: "bg-durum-kritik/10 text-durum-kritik",
};

const IKONLAR: Record<DevreDurumu, typeof CheckCircle2> = {
  KAPALI: CheckCircle2,
  YARI_ACIK: RotateCw,
  ACIK: XCircle,
};

const ETIKETLER: Record<DevreDurumu, string> = {
  KAPALI: "Sağlıklı",
  YARI_ACIK: "Yoklanıyor",
  ACIK: "Devre Açık",
};

export function DevreDurumRozeti({ durum, className }: { durum: DevreDurumu; className?: string }) {
  const Icon = IKONLAR[durum];
  return (
    <Badge className={cn(STILLER[durum], "gap-1 border-none", className)}>
      <Icon className="size-3.5" aria-hidden="true" />
      {ETIKETLER[durum]}
    </Badge>
  );
}
