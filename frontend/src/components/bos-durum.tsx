import type { LucideIcon } from "lucide-react";
import { Compass } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export function BosDurum({
  ikon: Icon,
  baslik,
  aciklama,
  eylemEtiketi,
  eylem,
  turBaslat,
  className,
}: {
  ikon?: LucideIcon;
  baslik: string;
  aciklama: string;
  eylemEtiketi?: string;
  eylem?: () => void;
  /** Verilirse "Bu paneli gezdir" bağlantısı gösterilir ve tıklanınca ilgili mikro tur açılır. */
  turBaslat?: () => void;
  className?: string;
}) {
  const GosterilenIkon = Icon ?? Compass;

  return (
    <div
      className={cn(
        "flex flex-col items-center gap-3 rounded-lg border border-dashed py-12 text-center",
        className,
      )}
    >
      <div className="flex size-10 items-center justify-center rounded-full bg-muted text-muted-foreground">
        <GosterilenIkon className="size-5" aria-hidden="true" />
      </div>
      <div className="space-y-1">
        <p className="font-medium text-foreground">{baslik}</p>
        <p className="max-w-sm text-sm text-muted-foreground">{aciklama}</p>
      </div>
      <div className="flex items-center gap-3">
        {eylem && eylemEtiketi && (
          <Button size="sm" onClick={eylem}>
            {eylemEtiketi}
          </Button>
        )}
        {turBaslat && (
          <Button variant="link" size="sm" onClick={turBaslat}>
            Bu paneli gezdir
          </Button>
        )}
      </div>
    </div>
  );
}
