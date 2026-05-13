package edu.hanouti.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.hanouti.services.ConseilsIAService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Petit serveur HTTP sur le LAN : page HTML par conseil pour scan QR depuis le téléphone (même Wi‑Fi).
 */
public final class ConseilShareServer {

    private static final Object LOCK = new Object();
    private static HttpServer server;
    private static int port;
    private static volatile ConseilsIAService service;

    private ConseilShareServer() {
    }

    /** Démarre une seule fois sur 0.0.0.0:port libre. */
    public static int ensureStarted(ConseilsIAService conseilsService) throws IOException {
        synchronized (LOCK) {
            service = conseilsService;
            if (server != null)
                return port;
            InetSocketAddress addr = new InetSocketAddress("0.0.0.0", 0);
            server = HttpServer.create(addr, 0);
            port = server.getAddress().getPort();
            server.createContext("/c", new ConseilPageHandler());
            ExecutorService ex = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "conseil-share-http");
                t.setDaemon(true);
                return t;
            });
            server.setExecutor(ex);
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> stopQuietly(), "conseil-share-shutdown"));
            return port;
        }
    }

    private static void stopQuietly() {
        synchronized (LOCK) {
            if (server != null) {
                server.stop(0);
                server = null;
            }
        }
    }

    /**
     * Adresses IPv4 candidates triées du plus plausible au moins (Wi‑Fi / Ethernet d’abord).
     * Évite en priorité VMware/VirtualBox/Hyper‑V (ex. 192.168.146.x) qui ne sont pas joignables depuis le téléphone sur le Wi‑Fi maison.
     */
    public static List<String> rankedLanIpv4Addresses() {
        List<IpScore> scored = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback())
                    continue;
                for (java.net.InetAddress a : java.util.Collections.list(nic.getInetAddresses())) {
                    if (!(a instanceof Inet4Address) || a.isLoopbackAddress())
                        continue;
                    Inet4Address ia = (Inet4Address) a;
                    String ip = ia.getHostAddress();
                    int sc = scoreInterfaceForLanShare(nic, ia, ip);
                    scored.add(new IpScore(ip, sc));
                }
            }
        } catch (Exception ignored) {
        }
        scored.sort((a, b) -> Integer.compare(b.score, a.score));
        List<String> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (IpScore s : scored) {
            if (seen.add(s.ip))
                out.add(s.ip);
        }
        if (out.isEmpty())
            out.add("127.0.0.1");
        return out;
    }

    /** Meilleure IP pour le QR (première de {@link #rankedLanIpv4Addresses()}). */
    public static String guessLanIpv4() {
        List<String> r = rankedLanIpv4Addresses();
        return r.isEmpty() ? "127.0.0.1" : r.get(0);
    }

    private static int scoreInterfaceForLanShare(NetworkInterface nic, Inet4Address addr, String ip) {
        String name = nic.getName() != null ? nic.getName().toLowerCase(Locale.ROOT) : "";
        String disp = nic.getDisplayName() != null ? nic.getDisplayName().toLowerCase(Locale.ROOT) : "";
        String blob = name + " " + disp;
        int s = 0;
        if (addr.isSiteLocalAddress())
            s += 50;
        else if (!addr.isAnyLocalAddress())
            s += 15;

        if (disp.matches(".*(wi-?fi|wifi|wlan|wireless|802\\.11|ieee 802\\.11|gigabit ethernet|ethernet).*"))
            s += 150;
        if (name.matches("^(wlan|wlp|wl|enp|eno|eth|ether).*"))
            s += 90;

        if (blob.contains("vmware") || blob.contains("vmnet") || blob.contains("virtualbox") || blob.contains("vbox"))
            s -= 280;
        if (blob.contains("vethernet") || blob.contains("hyper-v") || blob.contains("wsl") || blob.contains("docker")
                || blob.contains("tap-windows") || blob.contains("vpn") || blob.contains("zero tier")
                || blob.contains("nordlynx") || blob.contains("windscribe"))
            s -= 220;
        if (nic.isVirtual())
            s -= 40;

        // Plages très souvent = VM / outil virtuel, pas le LAN Wi‑Fi du routeur
        if (ip.startsWith("192.168.146.") || ip.startsWith("192.168.234.") || ip.startsWith("192.168.183.")
                || ip.startsWith("192.168.189.") || ip.startsWith("192.168.56.") || ip.startsWith("192.168.122."))
            s -= 200;
        // Hotspot Windows / partage fréquent — souvent joignable depuis le téléphone quand même, léger bonus si nom wifi
        if (ip.startsWith("192.168.137."))
            s -= 30;

        return s;
    }

    private static final class IpScore {
        final String ip;
        final int score;

        IpScore(String ip, int score) {
            this.ip = ip;
            this.score = score;
        }
    }

    private static final class ConseilPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            boolean responseStarted = false;
            try {
                if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                    ex.sendResponseHeaders(405, -1);
                    responseStarted = true;
                    ex.close();
                    return;
                }
                String raw = ex.getRequestURI().getRawQuery();
                int id = -1;
                if (raw != null) {
                    for (String part : raw.split("&")) {
                        if (part.startsWith("id=")) {
                            try {
                                id = Integer.parseInt(part.substring(3));
                            } catch (NumberFormatException ignored) {
                            }
                            break;
                        }
                    }
                }
                ConseilsIAService svc = service;
                Map<String, Object> row = (id > 0 && svc != null) ? svc.getConseilById(id) : null;

                byte[] body;
                String contentType = "text/html; charset=UTF-8";
                if (row == null) {
                    String html = pageShell("Conseil introuvable",
                            "<p>ID invalide ou conseil absent. Rechargez la liste sur le PC.</p>");
                    body = html.getBytes(StandardCharsets.UTF_8);
                    ex.getResponseHeaders().set("Content-Type", contentType);
                    ex.sendResponseHeaders(404, body.length);
                } else {
                    int idProd = 0;
                    Object idp = row.get("id_produit");
                    if (idp instanceof Number)
                        idProd = ((Number) idp).intValue();
                    else if (idp != null) {
                        try {
                            idProd = Integer.parseInt(idp.toString());
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    String nom = svc.getNomProduit(idProd);
                    if (nom == null || nom.isBlank())
                        nom = "Produit #" + idProd;
                    String type = String.valueOf(row.getOrDefault("type", ""));
                    String desc = String.valueOf(row.getOrDefault("description", ""));
                    String urg = String.valueOf(row.getOrDefault("urgence", ""));
                    String etat = String.valueOf(row.getOrDefault("etat", ""));
                    Object sc = row.get("score");
                    String score = sc != null ? String.valueOf(sc) : "—";
                    String inner = ""
                            + "<div class=\"card\"><h1>" + esc(nom) + "</h1>"
                            + "<span class=\"tag\">" + esc(type) + "</span>"
                            + "<p class=\"desc\">" + esc(desc) + "</p>"
                            + "<dl>"
                            + "<dt>Urgence</dt><dd>" + esc(urg) + "</dd>"
                            + "<dt>État</dt><dd>" + esc(etat) + "</dd>"
                            + "<dt>Score IA</dt><dd>" + esc(score) + "</dd>"
                            + "<dt>ID conseil</dt><dd>" + id + "</dd>"
                            + "</dl></div>";
                    String html = pageShell("Conseil #" + id, inner);
                    body = html.getBytes(StandardCharsets.UTF_8);
                    ex.getResponseHeaders().set("Content-Type", contentType);
                    ex.sendResponseHeaders(200, body.length);
                }
                responseStarted = true;
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(body);
                }
            } catch (Throwable t) {
                if (!responseStarted) {
                    String html = pageShell("Erreur",
                            "<p>Erreur serveur : "
                                    + esc(t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName())
                                    + "</p>");
                    byte[] body = html.getBytes(StandardCharsets.UTF_8);
                    ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    ex.sendResponseHeaders(500, body.length);
                    try (OutputStream os = ex.getResponseBody()) {
                        os.write(body);
                    }
                }
            }
        }

        private static String esc(String s) {
            if (s == null)
                return "";
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        }

        private static String pageShell(String title, String innerHtml) {
            return "<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\"/>"
                    + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>"
                    + "<title>" + esc(title) + " — 7anouti</title>"
                    + "<style>"
                    + "body{font-family:system-ui,sans-serif;background:#0f172a;color:#e2e8f0;margin:0;padding:16px;}"
                    + ".card{max-width:520px;margin:0 auto;background:#1e293b;border-radius:14px;padding:20px;"
                    + "border:1px solid #38bdf8;}"
                    + "h1{font-size:1.15rem;margin:0 0 12px;}"
                    + ".tag{display:inline-block;font-size:.72rem;font-weight:700;padding:4px 10px;border-radius:99px;"
                    + "background:rgba(56,189,248,.15);color:#38bdf8;border:1px solid #38bdf8;margin-bottom:14px;}"
                    + ".desc{line-height:1.45;color:#94a3b8;font-size:.95rem;}"
                    + "dl{display:grid;grid-template-columns:auto 1fr;gap:6px 14px;font-size:.88rem;margin-top:16px;}"
                    + "dt{color:#64748b;}dd{margin:0;}"
                    + "</style></head><body>" + innerHtml + "</body></html>";
        }
    }
}
