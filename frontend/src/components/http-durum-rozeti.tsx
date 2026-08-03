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
  const renk = basarili
    ? "bg-green-100 text-green-800 dark:bg-green-950 dark:text-green-300"
    : kalici
      ? "bg-red-200 text-red-900 dark:bg-red-900 dark:text-red-200"
      : "bg-orange-100 text-orange-800 dark:bg-orange-950 dark:text-orange-300";
  return <Badge className={cn(renk, "border-none")}>{durum}</Badge>;
}
