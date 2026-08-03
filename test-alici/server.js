// Kontrol edilebilir webhook alicisi - webhook-platformu'nun kapi testleri icin.
// Davranis query/basliklarla kontrol edilir:
//   ?mod=ok              -> 200
//   ?mod=hata             -> 500
//   ?mod=yavas&ms=30000    -> yaniti ms kadar geciktirir
//   ?mod=akis&esik=20&dizi=x -> ayni 'dizi' icin ilk 'esik' istekte 500, sonrasi 200
//
// WEBHOOK_SECRET ortam degiskeni ayarlanmissa gelen X-Webhook-Signature dogrulanir ve
// sonucu loglanir (imza=gecerli / imza=gecersiz / imza=yok).

const express = require('express');
const crypto = require('crypto');

const app = express();
const PORT = process.env.PORT || 4000;
const WEBHOOK_SECRET = process.env.WEBHOOK_SECRET || '';

app.use(express.raw({ type: '*/*', limit: '5mb' }));

const alinanlar = [];
const akisSayaclari = new Map();

function imzaDogrula(headers, rawBody) {
    const webhookId = headers['x-webhook-id'];
    const timestamp = headers['x-webhook-timestamp'];
    const signatureHeader = headers['x-webhook-signature'];

    if (!webhookId || !timestamp || !signatureHeader) {
        return 'yok';
    }
    if (!WEBHOOK_SECRET) {
        return 'dogrulanamadi (WEBHOOK_SECRET tanimli degil)';
    }

    const imzalananIcerik = `${webhookId}.${timestamp}.${rawBody.toString('utf8')}`;
    const beklenen = crypto.createHmac('sha256', WEBHOOK_SECRET).update(imzalananIcerik).digest('base64');
    const gelenListe = signatureHeader.split(' ');

    const gecerliMi = gelenListe.some((parca) => {
        const [versiyon, deger] = parca.split(',');
        return versiyon === 'v1' && deger === beklenen;
    });

    return gecerliMi ? 'gecerli' : 'gecersiz';
}

app.all('/webhook', async (req, res) => {
    const mod = req.query.mod || 'ok';
    const rawBody = Buffer.isBuffer(req.body) ? req.body : Buffer.from('');
    const imzaSonucu = imzaDogrula(req.headers, rawBody);

    let govdeJson = null;
    try {
        govdeJson = JSON.parse(rawBody.toString('utf8'));
    } catch (e) {
        // gövde JSON değilse ham metin olarak saklanır
    }

    const kayit = {
        zaman: new Date().toISOString(),
        mod,
        headers: req.headers,
        govde: govdeJson !== null ? govdeJson : rawBody.toString('utf8'),
        imza: imzaSonucu,
    };
    alinanlar.push(kayit);

    console.log(`[test-alici] istek alindi mod=${mod} imza=${imzaSonucu}`);

    if (mod === 'hata') {
        return res.status(500).send('kasitli hata (mod=hata)');
    }

    if (mod === 'yavas') {
        const ms = parseInt(req.query.ms, 10) || 30000;
        await new Promise((resolve) => setTimeout(resolve, ms));
        return res.status(200).send('gec ama basarili');
    }

    if (mod === 'akis') {
        const dizi = req.query.dizi || 'varsayilan';
        const esik = parseInt(req.query.esik, 10) || 20;
        const mevcut = (akisSayaclari.get(dizi) || 0) + 1;
        akisSayaclari.set(dizi, mevcut);
        if (mevcut <= esik) {
            return res.status(500).send(`akis modu: deneme ${mevcut}/${esik} basarisiz`);
        }
        return res.status(200).send(`akis modu: esik asildi, basarili`);
    }

    // mod=ok (varsayilan)
    return res.status(200).send('tamam');
});

app.get('/alinanlar', (req, res) => {
    res.json(alinanlar);
});

app.post('/sifirla', (req, res) => {
    alinanlar.length = 0;
    akisSayaclari.clear();
    res.status(204).send();
});

app.get('/saglik', (req, res) => {
    res.status(200).send('ayakta');
});

app.listen(PORT, () => {
    console.log(`test-alici ${PORT} portunda dinliyor`);
});
