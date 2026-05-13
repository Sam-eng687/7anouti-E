const express = require('express');
const { default: makeWASocket, useMultiFileAuthState, DisconnectReason, fetchLatestBaileysVersion } = require('@whiskeysockets/baileys');
const qrcode = require('qrcode-terminal');
const pino = require('pino');
const fs = require('fs');

const app = express();
app.use(express.json());

const PORT = 3000;
let sock = null;
let isConnected = false;

function clearAuth() {
    if (fs.existsSync('./auth_info')) {
        fs.rmSync('./auth_info', { recursive: true, force: true });
        console.log('Auth précédente supprimée.');
    }
}

async function connectWhatsApp() {
    const { state, saveCreds } = await useMultiFileAuthState('./auth_info');
    const { version } = await fetchLatestBaileysVersion();

    sock = makeWASocket({
        version,
        auth: state,
        logger: pino({ level: 'silent' }),
        browser: ['7anouti-E', 'Chrome', '1.0.0'],
        generateHighQualityLinkPreview: false,
        syncFullHistory: false
    });

    sock.ev.on('connection.update', (update) => {
        const { connection, lastDisconnect, qr } = update;

        if (qr) {
            console.clear();
            console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            console.log('  📱  Scannez ce QR code avec WhatsApp');
            console.log('  WhatsApp → Paramètres → Appareils connectés');
            console.log('  → Connecter un appareil → Scanner');
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
            qrcode.generate(qr, { small: true });
            console.log('\n(En attente du scan...)\n');
        }

        if (connection === 'open') {
            isConnected = true;
            console.log('\n✅  WhatsApp connecté !');
            console.log('✅  Service prêt sur http://localhost:' + PORT + '\n');
        }

        if (connection === 'close') {
            isConnected = false;
            const code = lastDisconnect?.error?.output?.statusCode;
            const loggedOut = code === DisconnectReason.loggedOut;
            if (loggedOut) {
                clearAuth();
                setTimeout(connectWhatsApp, 2000);
            } else {
                setTimeout(connectWhatsApp, 3000);
            }
        }
    });

    sock.ev.on('creds.update', saveCreds);
}

// POST /send — called by Java SmsService.java
app.post('/send', async (req, res) => {
    const { phone, message } = req.body;
    if (!phone || !message)
        return res.status(400).json({ success: false, error: 'phone and message required' });
    if (!isConnected || !sock)
        return res.status(503).json({ success: false, error: 'WhatsApp not connected' });
    try {
        const jid = phone.replace('+', '') + '@s.whatsapp.net';
        await sock.sendMessage(jid, { text: message });
        console.log('✅  Message envoyé à', phone);
        res.json({ success: true });
    } catch (err) {
        console.error('❌  Erreur :', err.message);
        res.status(500).json({ success: false, error: err.message });
    }
});

app.get('/status', (req, res) => {
    res.json({ connected: isConnected });
});

app.listen(PORT, () => {
    console.log('7anouti WhatsApp Service démarré...');
    clearAuth();
    connectWhatsApp();
});
