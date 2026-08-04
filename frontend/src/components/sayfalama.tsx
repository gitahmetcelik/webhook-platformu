import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";

export function Sayfalama({
  sayfa,
  toplamSayfa,
  onDegistir,
}: {
  sayfa: number;
  toplamSayfa: number;
  onDegistir: (sayfa: number) => void;
}) {
  if (toplamSayfa <= 1) return null;

  return (
    <div className="flex items-center justify-center gap-3 pt-2">
      <Button variant="outline" size="sm" disabled={sayfa === 0} onClick={() => onDegistir(sayfa - 1)}>
        <ChevronLeft className="size-4" />
        Önceki
      </Button>
      <span className="text-sm text-muted-foreground tabular-nums">
        {sayfa + 1} / {toplamSayfa}
      </span>
      <Button
        variant="outline"
        size="sm"
        disabled={sayfa + 1 >= toplamSayfa}
        onClick={() => onDegistir(sayfa + 1)}
      >
        Sonraki
        <ChevronRight className="size-4" />
      </Button>
    </div>
  );
}
