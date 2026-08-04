import { CheckCircle2, CircleSlash, TriangleAlert } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

export function HttpDurumRozeti({ durum }: { durum: number | null }) {
  if (durum === null) {
    return (
      <Badge variant="outline" className="text-muted-foreground">
        yanıt yok
      </Badge>
    );
  }
  const basarili = durum >= 200 && durum < 300;
  const kalici = [400, 401, 403, 404, 410, 422].includes(durum);
  const stil = basarili
    ? "bg-durum-iyi/10 text-durum-iyi"
    : kalici
      ? "bg-durum-kritik/10 text-durum-kritik"
      : "bg-durum-uyari/15 text-durum-uyari";
  const Icon = basarili ? CheckCircle2 : kalici ? CircleSlash : TriangleAlert;
  return (
    <Badge className={cn(stil, "gap-1 border-none")}>
      <Icon className="size-3.5" aria-hidden="true" />
      {durum}
    </Badge>
  );
}
