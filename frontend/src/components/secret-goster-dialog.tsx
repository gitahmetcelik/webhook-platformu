"use client";

import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";

export function SecretGosterDialog({ secret, onOpenChange }: { secret: string | null; onOpenChange: () => void }) {
  return (
    <Dialog open={secret !== null} onOpenChange={onOpenChange}>
      <DialogContent data-tur="endpoint-secret-goster">
        <DialogHeader>
          <DialogTitle>İmza Secret&apos;ı</DialogTitle>
          <DialogDescription>
            Bu değer sadece şimdi gösteriliyor — bir daha buradan okunamaz, güvenli bir yere kaydedin.
          </DialogDescription>
        </DialogHeader>
        <pre className="overflow-x-auto rounded-md border bg-muted/30 p-3 text-sm">{secret}</pre>
        <DialogFooter>
          <Button
            onClick={() => {
              if (secret) {
                navigator.clipboard.writeText(secret);
                toast.success("Secret kopyalandı");
              }
            }}
          >
            Kopyala
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
