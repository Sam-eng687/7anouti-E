import argparse, json, math, datetime, sys, os
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import A4
from reportlab.lib import colors

W, H = A4

# ══════════════════════════════════════════
# PALETTE
# ══════════════════════════════════════════
BG     = colors.HexColor("#0a0d1a")
CARD   = colors.HexColor("#0d1526")
CARD2  = colors.HexColor("#111a2e")
ROYAL  = colors.HexColor("#2563EB")
NEON   = colors.HexColor("#38BDF8")
NEON2  = colors.HexColor("#7DD3FC")
GREEN  = colors.HexColor("#10B981")
ORANGE = colors.HexColor("#F97316")
VIOLET = colors.HexColor("#8B5CF6")
ROSE   = colors.HexColor("#F472B6")
GOLD   = colors.HexColor("#FBBF24")
T1     = colors.HexColor("#F1F5F9")
T2     = colors.HexColor("#94A3B8")
T3     = colors.HexColor("#475569")
T4     = colors.HexColor("#1E293B")
BORDER = colors.HexColor("#1e2a40")
LINE   = colors.HexColor("#0f1a2e")

# Palette cards créative
CARD_COLORS = [NEON, GREEN, VIOLET, ORANGE, GOLD, ROSE, colors.HexColor("#06b6d4")]

MX   = 24   # marge gauche/droite
GAP  = 12   # espace entre éléments
HEADER_H = 68
FOOTER_H = 30

NOW   = datetime.datetime.now().strftime("%d/%m/%Y  %H:%M")
# Date en français
mois_fr = ["", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin", 
           "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"]
now_dt = datetime.datetime.now()
TODAY = f"{now_dt.day:02d} {mois_fr[now_dt.month]} {now_dt.year}"

# ══════════════════════════════════════════
# HELPERS
# ══════════════════════════════════════════
def rr(c, x, y, w, h, r, fill, stroke=None, lw=0.5):
    c.setFillColor(fill)
    if stroke:
        c.setStrokeColor(stroke); c.setLineWidth(lw)
        c.roundRect(x, y, w, h, r, fill=1, stroke=1)
    else:
        c.roundRect(x, y, w, h, r, fill=1, stroke=0)

def badge(c, x, y, w, h, text, bg, fg, r=5, fs=7):
    rr(c, x, y, w, h, r, bg, fg, 0.5)
    c.setFillColor(fg); c.setFont("Helvetica-Bold", fs)
    c.drawCentredString(x+w/2, y+h/2-2.5, text)

def hline(c, x1, x2, y, col=BORDER, lw=0.4):
    c.setStrokeColor(col); c.setLineWidth(lw); c.line(x1, y, x2, y)

def gradient_line(c, x1, x2, y):
    """Ligne dégradée bleue"""
    steps = 40
    sw = (x2-x1)/steps
    for i in range(steps):
        t = i/steps
        alpha = math.sin(t*math.pi)
        col = colors.HexColor("#38BDF8")
        c.setStrokeColorRGB(0.22, 0.74, 0.97, alpha)
        c.setLineWidth(1.5)
        c.line(x1+i*sw, y, x1+(i+1)*sw, y)

def header_bar(c, subtitle, accent_col, page_num, total_pages):
    # Fond header
    rr(c, 0, H-HEADER_H, W, HEADER_H, 0, CARD)
    # Barre accent gauche (couleur du module)
    c.setFillColor(accent_col); c.rect(0, 0, 4, H, fill=1, stroke=0)
    # Ligne dégradée bas header — toujours bleue néon
    gradient_line(c, 4, W, H-HEADER_H)

    # Logo "7anouti-E" — SANS ESPACE, toujours en NEON bleu
    c.setFillColor(NEON); c.setFont("Helvetica-Bold", 20)
    c.drawString(MX+2, H-30, "7anouti")
    c.setFillColor(NEON); c.setFont("Helvetica-Bold", 20)
    c.drawString(MX+60, H-30, "-E")

    # Sous-titre — toujours en T3
    c.setFillColor(T3); c.setFont("Helvetica", 8)
    c.drawString(MX+2, H-46, subtitle)

    # Date + badge page
    c.setFillColor(T3); c.setFont("Helvetica", 8)
    c.drawRightString(W-MX, H-28, TODAY)
    badge(c, W-MX-90, H-50, 86, 14, "RAPPORT PREMIUM", colors.HexColor("#1a0a40"), ROYAL, 5, 7)

def page_footer(c, page, total, label):
    rr(c, 0, 0, W, FOOTER_H, 0, CARD)
    hline(c, 0, W, FOOTER_H, BORDER, 0.4)
    c.setFillColor(T3); c.setFont("Helvetica", 7)
    c.drawString(MX, 11, f"7anouti-E  ·  {label}  ·  {NOW}")
    c.setFillColor(NEON); c.setFont("Helvetica-Bold", 7)
    c.drawRightString(W-MX, 11, f"Page {page} sur {total}")

def section_title(c, text, y, col=NEON):
    c.setFillColor(col); c.setFont("Helvetica-Bold", 9)
    c.drawString(MX, y, text)
    hline(c, MX, W-MX, y-5, BORDER)
    return y - 18

def kpi_row(c, kpis, y, h=70):
    n  = len(kpis)
    kw = (W - 2*MX - (n-1)*GAP) / n
    for i, (lbl, val, sub, col, bg) in enumerate(kpis):
        kx = MX + i*(kw+GAP)
        # Card fond + bordure couleur
        rr(c, kx, y, kw, h, 9, bg, col, 0.8)
        # Barre accent top
        c.setFillColor(col); c.roundRect(kx, y+h-4, kw, 4, 3, fill=1, stroke=0)
        # Label
        c.setFillColor(T3); c.setFont("Helvetica-Bold", 7)
        c.drawString(kx+12, y+h-18, lbl.upper())
        # Valeur
        fs = 14 if len(str(val)) > 11 else 17
        c.setFillColor(col); c.setFont("Helvetica-Bold", fs)
        c.drawString(kx+12, y+h-36, str(val))
        # Sous-texte
        c.setFillColor(T2); c.setFont("Helvetica", 8)
        c.drawString(kx+12, y+10, sub)
    return y - GAP


# ══════════════════════════════════════════
# PAGE 1 — STATISTIQUES
# ══════════════════════════════════════════
def page_stats(c, d, page=1, total=1):
    c.setFillColor(BG); c.rect(0, 0, W, H, fill=1, stroke=0)
    header_bar(c, "Statistiques Ventes  ·  Performances Produits", NEON, page, total)

    CONTENT_TOP = H - HEADER_H - GAP
    CONTENT_BOT = FOOTER_H + GAP

    # ── KPI ──
    KPI_Y = CONTENT_TOP - 74
    kpi_row(c, [
        ("Revenu Total",    f"{float(d.get('revenu',0)):,.0f} TND", "+8.4% ce mois",  NEON,   colors.HexColor("#0c2040")),
        ("Qté Vendue",      str(d.get('qte',0)),                    "+12% vs mois",   GREEN,  colors.HexColor("#062818")),
        ("Taux Retour",     f"{float(d.get('retour',0)):.1f}%",     "amélioration",   ROSE,   colors.HexColor("#280618")),
        ("Produits Actifs", str(d.get('produits',0)),               "tous actifs",    ROYAL,  colors.HexColor("#180840")),
    ], KPI_Y, h=70)

    # ── Tableau ──
    TABLE_Y = KPI_Y - GAP*2 - 14
    cy = section_title(c, "DÉTAIL DES PERFORMANCES PAR PRODUIT", TABLE_Y)

    # En-têtes
    HDR_H = 20
    rr(c, MX, cy-HDR_H+4, W-2*MX, HDR_H, 5, colors.HexColor("#0d1a2e"))
    cols_x = [MX+6, MX+92, MX+162, MX+222, MX+290, MX+360, MX+425]
    hdrs   = ["RÉFÉRENCE", "PÉRIODE", "SEMAINE", "VENTES", "REVENU TND", "RETOUR %", "STATUT"]
    hcols  = [NEON, T3, T3, GREEN, GREEN, NEON, GOLD]
    for h2, cx2, hc in zip(hdrs, cols_x, hcols):
        c.setFillColor(hc); c.setFont("Helvetica-Bold", 7)
        c.drawString(cx2, cy-HDR_H+8, h2)

    # Lignes
    ROW_H = 22
    produits = d.get('produits_list', [])
    row_y = cy - HDR_H - 4
    for i, p in enumerate(produits):
        ry = row_y - i*ROW_H
        if ry < CONTENT_BOT + 40: break
        bg_row = colors.HexColor("#0d1526") if i % 2 == 0 else CARD
        rr(c, MX, ry-ROW_H+5, W-2*MX, ROW_H, 4, bg_row)
        ret  = float(p.get('taux_retour', 0))
        sc   = ROSE if ret > 4 else (GOLD if ret > 2.5 else GREEN)
        stat = p.get('classement', '') or ''
        
        # Badge "Non classé" au lieu de "—"
        if not stat or stat == '—':
            stat_display = "Non classé"
            scol = T3
        else:
            stat_display = stat
            scol = GOLD if 'Top 10' in stat else (NEON if 'Top 50' in stat else ROSE)
        
        vals = [
            (p.get('reference','')[:16],  T1,   True),
            (str(p.get('periode',''))[:10],T3,   False),
            (str(p.get('semaine','')),     T3,   False),
            (str(p.get('quantite_vendue',0)), GREEN, True),
            (f"{p.get('revenu',0):,.0f}", GREEN, False),
            (f"{ret:.1f}%",               sc,   True),
            (stat_display,                 scol, True),
        ]
        for (txt, col, bold), cx2 in zip(vals, cols_x):
            c.setFillColor(col)
            c.setFont("Helvetica-Bold" if bold else "Helvetica", 8)
            c.drawString(cx2, ry-ROW_H+9, txt)

    # ── Bar chart avec légende ──
    chart_top = row_y - len(produits)*ROW_H - GAP*2
    if chart_top > CONTENT_BOT + 140:  # Plus d'espace pour la légende
        cy2 = section_title(c, "REVENUS PAR PRODUIT (TND)", chart_top)
        max_r  = max((p.get('revenu', 1) for p in produits), default=1)
        BH_MAX = 60
        BW     = 36
        by0    = cy2 - BH_MAX - 24
        bx0    = MX + 12

        # Dessiner les barres
        for i, p in enumerate(produits):
            rev = p.get('revenu', 0)
            bh  = max(4, (rev/max_r)*BH_MAX)
            bx  = bx0 + i*(BW+20)
            col = CARD_COLORS[i % len(CARD_COLORS)]
            rr(c, bx, by0, BW, bh, 4, col)
            c.setFillColor(col); c.setFont("Helvetica-Bold", 6.5)
            c.drawCentredString(bx+BW/2, by0+bh+5, f"{rev:,.0f}")
            ref = p.get('reference','').replace('REF-','')[:7]
            c.setFillColor(T3); c.setFont("Helvetica", 6.5)
            c.drawCentredString(bx+BW/2, by0-11, ref)
        
        # Légende des couleurs en bas
        legend_y = by0 - 32
        legend_x = MX + 12
        c.setFillColor(T3); c.setFont("Helvetica-Bold", 6.5)
        c.drawString(legend_x, legend_y, "Légende :")
        
        legend_x += 50
        for i, p in enumerate(produits[:6]):  # Max 6 pour la légende
            col = CARD_COLORS[i % len(CARD_COLORS)]
            ref = p.get('reference','').replace('REF-','')[:8]
            
            # Carré de couleur
            c.setFillColor(col)
            c.rect(legend_x, legend_y-2, 8, 8, fill=1, stroke=0)
            
            # Nom du produit
            c.setFillColor(T3); c.setFont("Helvetica", 6.5)
            c.drawString(legend_x + 12, legend_y, ref)
            
            legend_x += 80

    # ── Bande analyse supprimée ──

    page_footer(c, page, total, "Statistiques Ventes")


# ══════════════════════════════════════════
# PAGE 2 — CONSEILS
# ══════════════════════════════════════════
def page_conseils(c, d, page=1, total=1):
    c.setFillColor(BG); c.rect(0, 0, W, H, fill=1, stroke=0)
    header_bar(c, "Conseils Stratégiques  ·  Centre de Décision", ROYAL, page, total)

    CONTENT_TOP = H - HEADER_H - GAP
    CONTENT_BOT = FOOTER_H + GAP

    # ── KPI ──
    KPI_Y = CONTENT_TOP - 74
    kpi_row(c, [
        ("Total Conseils", str(d.get('total', 0)),                 "générés ce mois", NEON,   colors.HexColor("#0c2040")),
        ("Appliqués",      str(d.get('applique', 0)),              "actions prises",  GREEN,  colors.HexColor("#062818")),
        ("Nouveaux",       str(d.get('nouveau', 0)),               "en attente",      ORANGE, colors.HexColor("#2a1006")),
        ("Confiance",      f"{float(d.get('confiance',0)):.0f}%", "bonne précision", ROYAL,  colors.HexColor("#180840")),
    ], KPI_Y, h=70)

    # ── Conseils ──
    SEC_Y = KPI_Y - GAP*2 - 14
    cy = section_title(c, "RECOMMANDATIONS  —  ACTIONS PRIORITAIRES", SEC_Y, ROYAL)

    conseils = d.get('list', [])
    urg_map = {
        "Urgent": (ROSE,   colors.HexColor("#280618")),
        "Eleve":  (ROSE,   colors.HexColor("#280618")),
        "Moyen":  (GOLD,   colors.HexColor("#1a1208")),
        "moyen":  (GOLD,   colors.HexColor("#1a1208")),
        "MOYEN":  (GOLD,   colors.HexColor("#1a1208")),
    }
    type_col = {
        "Promotion":    NEON,   "PROMOTION":    NEON,
        "Destockage":   ROSE,   "DESTOCKAGE":   ROSE,
        "Bundle":       ROYAL,  "BUNDLE":       ROYAL,
        "Stock":        ORANGE, "STOCK":        ORANGE,
        "Mise en avant":GREEN,
    }

    CARD_H  = 54
    CARD_GAP = 10

    for i, con in enumerate(conseils[:7]):
        ry = cy - i*(CARD_H + CARD_GAP) - CARD_H
        if ry < CONTENT_BOT: break

        col    = type_col.get(con.get('type',''), NEON)
        urg    = con.get('urgence', 'Moyen')
        ug_fg, ug_bg = urg_map.get(urg, (GOLD, colors.HexColor("#1a1208")))

        # Card
        rr(c, MX, ry, W-2*MX, CARD_H, 8, CARD2, col, 0.7)
        c.setFillColor(col); c.roundRect(MX, ry, 5, CARD_H, 3, fill=1, stroke=0)

        # Badge urgence
        badge(c, MX+12, ry+CARD_H-20, 54, 13, urg, ug_bg, ug_fg, 5, 7)
        # Badge score
        badge(c, W-MX-70, ry+CARD_H-20, 64, 13,
              f"SCORE  {con.get('score',0):.0f}%", colors.HexColor("#1a1208"), GOLD, 5, 7)

        # Titre
        prod = con.get('produit','') or con.get('produit_nom','')
        typ  = con.get('type','')
        c.setFillColor(T1); c.setFont("Helvetica-Bold", 9.5)
        c.drawString(MX+12, ry+CARD_H-36, f"{prod}  —  {typ}")

        # Description
        desc = (con.get('description','') or '')[:96]
        c.setFillColor(T2); c.setFont("Helvetica", 7.5)
        c.drawString(MX+12, ry+9, desc)

    page_footer(c, page, total, "Conseils Stratégiques")


# ══════════════════════════════════════════
# PAGE 3 — CAMPAGNES
# ══════════════════════════════════════════
def page_campagnes(c, d, page=1, total=1):
    c.setFillColor(BG); c.rect(0, 0, W, H, fill=1, stroke=0)
    header_bar(c, "Campagnes Marketing  ·  Actions & Résultats", ORANGE, page, total)
    CONTENT_TOP = H - HEADER_H - GAP
    CONTENT_BOT = FOOTER_H + GAP

    budget  = float(d.get('budget', 0))
    depense = float(d.get('depense', 0))
    pct     = (depense/budget*100) if budget > 0 else 0

    # ── KPI ──
    KPI_Y = CONTENT_TOP - 74
    kpi_row(c, [
        ("Budget Total", f"{budget:,.0f} TND",   f"{d.get('total',0)} campagnes", NEON,   colors.HexColor("#0c2040")),
        ("Actives",      str(d.get('actives',0)), "en cours",                      GREEN,  colors.HexColor("#062818")),
        ("Total",        str(d.get('total',0)),   "toutes campagnes",              ORANGE, colors.HexColor("#2a1006")),
        ("Dépense",      f"{depense:,.0f} TND",   f"{pct:.0f}% du budget",        VIOLET, colors.HexColor("#180840")),
    ], KPI_Y, h=70)

    # ── Campagnes ──
    SEC_Y = KPI_Y - GAP*2 - 14
    cy = section_title(c, "VOS CAMPAGNES MARKETING", SEC_Y, ORANGE)

    camps = d.get('list', [])
    CARD_H  = 76
    CARD_GAP = 10

    for i, camp in enumerate(camps[:6]):
        ry = cy - i*(CARD_H + CARD_GAP) - CARD_H
        if ry < CONTENT_BOT: break

        # Couleur créative par index
        fg = CARD_COLORS[i % len(CARD_COLORS)]

        # Card
        rr(c, MX, ry, W-2*MX, CARD_H, 9, CARD2, fg, 0.7)
        c.setFillColor(fg); c.roundRect(MX, ry, 5, CARD_H, 3, fill=1, stroke=0)

        # ── Ligne 1 : Nom + badges ──
        c.setFillColor(T1); c.setFont("Helvetica-Bold", 10.5)
        c.drawString(MX+14, ry+CARD_H-18, camp.get('nom','')[:34])

        stat = camp.get('statut','')
        stat_colors = {"ACTIVE": GREEN, "TERMINEE": ROSE, "TERMINÉE": ROSE, "BROUILLON": VIOLET}
        stat_col = stat_colors.get(stat, NEON)
        badge(c, W-MX-118, ry+CARD_H-21, 56, 14, stat, colors.HexColor("#0d1526"), stat_col, 5, 7)
        sc = camp.get('ia_score', 0)
        if sc > 0:
            badge(c, W-MX-58, ry+CARD_H-21, 50, 14,
                  f"★ {sc:.1f}/10", colors.HexColor("#1a1208"), GOLD, 5, 7)

        # ── Ligne 2 : Meta ──
        c.setFillColor(T3); c.setFont("Helvetica", 7.5)
        meta = (f"Objectif: {camp.get('objectif','')}   ·   "
                f"Canal: {camp.get('canal','')}   ·   "
                f"{camp.get('date_debut','')} → {camp.get('date_fin','')}")
        c.drawString(MX+14, ry+CARD_H-34, meta[:74])

        # ── Ligne 3 : Note ──
        note = (camp.get('ia_conseil','') or '')[:84]
        if note:
            rr(c, MX+10, ry+CARD_H-50, W-2*MX-20, 14, 3, colors.HexColor("#150840"))
            c.setFillColor(ROYAL); c.setFont("Helvetica-Bold", 7)
            c.drawString(MX+16, ry+CARD_H-46, "NOTE :")
            c.setFillColor(T2); c.setFont("Helvetica", 7)
            c.drawString(MX+52, ry+CARD_H-46, note)

        # ── Barre budget ──
        alloue = float(camp.get('budget_alloue', 1) or 1)
        dep    = float(camp.get('budget_depense', 0))
        pct_b  = min(dep/alloue, 1)
        BAR_Y  = ry + 14
        BAR_W  = W - 2*MX - 18
        rr(c, MX+9, BAR_Y, BAR_W, 6, 3, colors.HexColor("#0d1020"))
        if pct_b > 0:
            rr(c, MX+9, BAR_Y, BAR_W*pct_b, 6, 3, fg)
        c.setFillColor(T2); c.setFont("Helvetica", 7)
        c.drawString(MX+9, ry+5, f"{dep:,.0f} / {alloue:,.0f} TND")
        c.setFillColor(fg); c.setFont("Helvetica-Bold", 7)
        c.drawRightString(W-MX-9, ry+5, f"{pct_b*100:.0f}%")

    page_footer(c, page, total, "Campagnes Marketing")


# ══════════════════════════════════════════
# RAPPORT COMPLET
# ══════════════════════════════════════════
def rapport_complet(c, d):
    page_stats(c, d['stats'], page=1, total=3)
    c.showPage()
    page_conseils(c, d['conseils'], page=2, total=3)
    c.showPage()
    page_campagnes(c, d['campagnes'], page=3, total=3)


# ══════════════════════════════════════════
# MAIN
# ══════════════════════════════════════════
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode",   required=True, choices=["stats","conseils","campagnes","complet"])
    parser.add_argument("--data",   required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    with open(args.data, encoding="utf-8") as f:
        data = json.load(f)

    os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)
    c = canvas.Canvas(args.output, pagesize=A4)
    c.setTitle(f"7anouti-E — {args.mode.capitalize()}")
    c.setAuthor("HeptaCode · Marketing Intelligence")

    if args.mode == "stats":
        page_stats(c, data)
    elif args.mode == "conseils":
        page_conseils(c, data)
    elif args.mode == "campagnes":
        page_campagnes(c, data)
    elif args.mode == "complet":
        rapport_complet(c, data)

    c.save()
    print(f"PDF généré : {args.output}")

if __name__ == "__main__":
    main()
