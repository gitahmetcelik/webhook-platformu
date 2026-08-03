package com.webhookplatformu.servis;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Kullanim sayaci (bkz Faz 4.2) - basit bir gunluk upsert sayaci oldugu icin tam bir JPA
 * entity/repository yerine dogrudan JdbcTemplate kullanildi (composite-key entity'nin getirdigi
 * ekstra karmasiklik bu is icin gereksiz).
 */
@Component
public class KullanimSayaciServisi {

    private final JdbcTemplate jdbcTemplate;

    public KullanimSayaciServisi(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void artir(UUID organizasyonId, boolean basarili) {
        int basariliArtis = basarili ? 1 : 0;
        int basarisizArtis = basarili ? 0 : 1;
        jdbcTemplate.update("""
                INSERT INTO webhook.kullanim_sayaci (organizasyon_id, gun, teslimat_sayisi, basarili, basarisiz)
                VALUES (?, ?, 1, ?, ?)
                ON CONFLICT (organizasyon_id, gun) DO UPDATE SET
                    teslimat_sayisi = webhook.kullanim_sayaci.teslimat_sayisi + 1,
                    basarili = webhook.kullanim_sayaci.basarili + EXCLUDED.basarili,
                    basarisiz = webhook.kullanim_sayaci.basarisiz + EXCLUDED.basarisiz
                """, organizasyonId, LocalDate.now(ZoneOffset.UTC), basariliArtis, basarisizArtis);
    }

    public int buAyToplam(UUID organizasyonId) {
        LocalDate ayBaslangici = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        Integer toplam = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(teslimat_sayisi), 0) FROM webhook.kullanim_sayaci
                WHERE organizasyon_id = ? AND gun >= ?
                """, Integer.class, organizasyonId, ayBaslangici);
        return toplam == null ? 0 : toplam;
    }

    public record GunlukKullanim(LocalDate gun, int teslimatSayisi, int basarili, int basarisiz) {
    }

    public List<GunlukKullanim> buAyGunluk(UUID organizasyonId) {
        LocalDate ayBaslangici = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        return jdbcTemplate.query("""
                SELECT gun, teslimat_sayisi, basarili, basarisiz FROM webhook.kullanim_sayaci
                WHERE organizasyon_id = ? AND gun >= ? ORDER BY gun
                """, (rs, i) -> new GunlukKullanim(rs.getObject("gun", LocalDate.class), rs.getInt("teslimat_sayisi"),
                rs.getInt("basarili"), rs.getInt("basarisiz")), organizasyonId, ayBaslangici);
    }
}
