package projet.hanouti.GestionCommandes.services;

import com.itextpdf.text.pdf.draw.LineSeparator;
import projet.hanouti.GestionCommandes.entities.Commande;
import projet.hanouti.GestionCommandes.entities.LigneCommande;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Service de génération de factures PDF et de QR Codes.
 *
 * Génère automatiquement :
 *  1. Une facture PDF professionnelle (iText 5)
 *  2. Un QR Code contenant le chemin/référence de la facture (ZXing)
 *
 * Le QR Code est ensuite transmis au module livraison avec la facture.
 *
 * Dépendances Maven :
 *  - com.itextpdf:itextpdf:5.5.13.3
 *  - com.google.zxing:core:3.5.3
 *  - com.google.zxing:javase:3.5.3
 *
 * Dossier de sortie : src/main/resources/factures/
 */
public class FactureService {

    private static final String DOSSIER_FACTURES = "src/main/resources/factures/";
    private static final String DOSSIER_QR       = "src/main/resources/factures/qrcodes/";
    private static final int    QR_SIZE          = 300; // pixels

    // =========================================================
    // GENERATION PDF
    // =========================================================

    /**
     * Génère une facture PDF pour une commande donnée.
     *
     * Contenu de la facture :
     *  - En-tête : logo 7ANOUTI-E, numéro de commande, date
     *  - Tableau des produit : nom, quantité, prix unitaire, sous-total
     *  - Total TTC
     *  - Adresse de livraison + mode de paiement
     *
     * @param commande la commande facturée
     * @param lignes   les lignes de commande
     * @return chemin absolu du fichier PDF généré
     */
    public String genererPDF(Commande commande, List<LigneCommande> lignes) {
        // Création du dossier si nécessaire
        new File(DOSSIER_FACTURES).mkdirs();

        String nomFichier = "FACTURE_" + commande.getNumeroCommande() + ".pdf";
        String cheminPdf  = DOSSIER_FACTURES + nomFichier;

        try {
            Document document = new Document(PageSize.A4, 50, 50, 60, 60);
            PdfWriter.getInstance(document, new FileOutputStream(cheminPdf));
            document.open();

            // ---- Fonts ----
            Font fontTitre    = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD,
                    new BaseColor(41, 128, 185));
            Font fontSousTitre = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL,
                    new BaseColor(100, 100, 100));
            Font fontNormal   = new Font(Font.FontFamily.HELVETICA, 10);
            Font fontBold     = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            Font fontTotal    = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,
                    new BaseColor(41, 128, 185));
            Font fontHeader   = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,
                    BaseColor.WHITE);

            // ---- En-tête ----
            Paragraph titre = new Paragraph("7ANOUTI-E", fontTitre);
            titre.setAlignment(Element.ALIGN_CENTER);
            document.add(titre);

            Paragraph sousTitre = new Paragraph("Plateforme e-commerce intelligente", fontSousTitre);
            sousTitre.setAlignment(Element.ALIGN_CENTER);
            sousTitre.setSpacingAfter(20f);
            document.add(sousTitre);

            // Séparateur
            LineSeparator separator = new LineSeparator();
            separator.setLineColor(new BaseColor(41, 128, 185));
            document.add(new Chunk(separator));
            document.add(Chunk.NEWLINE);

            // ---- Infos commande ----
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String dateStr = commande.getDateCreation() != null
                    ? commande.getDateCreation().format(dtf)
                    : "—";

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(10f);
            infoTable.setSpacingAfter(20f);
            infoTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            ajouterCelluleInfo(infoTable, "Numéro de commande :", commande.getNumeroCommande(),
                    fontBold, fontNormal);
            ajouterCelluleInfo(infoTable, "Date :", dateStr, fontBold, fontNormal);
            ajouterCelluleInfo(infoTable, "Mode de paiement :",
                    commande.getModePaiement().name(), fontBold, fontNormal);
            ajouterCelluleInfo(infoTable, "Adresse de livraison :",
                    commande.getAdresseLivraison(), fontBold, fontNormal);

            if (commande.getDateLivraisonPreferee() != null) {
                ajouterCelluleInfo(infoTable, "Date de livraison préférée :",
                        commande.getDateLivraisonPreferee().toString(), fontBold, fontNormal);
            }
            document.add(infoTable);

            // ---- Tableau des produit ----
            PdfPTable prodTable = new PdfPTable(new float[]{3f, 1f, 1.5f, 1.5f});
            prodTable.setWidthPercentage(100);
            prodTable.setSpacingBefore(5f);
            prodTable.setSpacingAfter(15f);

            // En-têtes colonnes
            BaseColor headerColor = new BaseColor(41, 128, 185);
            ajouterCelluleHeader(prodTable, "Produit",      fontHeader, headerColor);
            ajouterCelluleHeader(prodTable, "Qté",          fontHeader, headerColor);
            ajouterCelluleHeader(prodTable, "Prix unitaire",fontHeader, headerColor);
            ajouterCelluleHeader(prodTable, "Sous-total",   fontHeader, headerColor);

            // Lignes produit
            boolean ligneAlternee = false;
            for (LigneCommande ligne : lignes) {
                BaseColor bg = ligneAlternee ? new BaseColor(240, 248, 255) : BaseColor.WHITE;
                ajouterCelluleProduit(prodTable, ligne.getNomProduit(), fontNormal, bg);
                ajouterCelluleProduit(prodTable, String.valueOf(ligne.getQuantite()), fontNormal, bg);
                ajouterCelluleProduit(prodTable,
                        String.format("%.2f TND", ligne.getPrixUnitaire()), fontNormal, bg);
                ajouterCelluleProduit(prodTable,
                        String.format("%.2f TND", ligne.getSousTotal()), fontNormal, bg);
                ligneAlternee = !ligneAlternee;
            }
            document.add(prodTable);

            // ---- Total ----
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(40);
            totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            PdfPCell labelTotal = new PdfPCell(new Phrase("TOTAL TTC", fontTotal));
            labelTotal.setBorder(Rectangle.TOP);
            labelTotal.setBorderColorTop(new BaseColor(41, 128, 185));
            labelTotal.setBorderWidthTop(2f);
            labelTotal.setPadding(8f);
            totalTable.addCell(labelTotal);

            PdfPCell valeurTotal = new PdfPCell(
                    new Phrase(String.format("%.2f TND", commande.getTotal()), fontTotal));
            valeurTotal.setBorder(Rectangle.TOP);
            valeurTotal.setBorderColorTop(new BaseColor(41, 128, 185));
            valeurTotal.setBorderWidthTop(2f);
            valeurTotal.setPadding(8f);
            valeurTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalTable.addCell(valeurTotal);

            document.add(totalTable);

            // ---- Pied de page ----
            document.add(Chunk.NEWLINE);
            document.add(new Chunk(separator));
            Paragraph footer = new Paragraph(
                    "Merci pour votre commande sur 7ANOUTI-E. Ce document vaut facture.",
                    fontSousTitre
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(10f);
            document.add(footer);

            document.close();
            System.out.println("[FactureService] PDF généré : " + cheminPdf);

        } catch (Exception e) {
            System.err.println("[FactureService.genererPDF] Erreur : " + e.getMessage());
            return null;
        }

        return cheminPdf;
    }

    // =========================================================
    // GENERATION QR CODE
    // =========================================================

    /**
     * Génère un QR Code PNG à partir du chemin du fichier PDF.
     * Le QR Code encode l'identifiant unique de la facture.
     *
     * @param cheminPdf chemin du fichier PDF de la facture
     * @return chemin absolu du fichier QR Code PNG généré
     */
    public String genererQRCode(String cheminPdf) {
        if (cheminPdf == null || cheminPdf.isBlank()) return null;

        new File(DOSSIER_QR).mkdirs();

        // Nom du QR Code basé sur le nom du PDF
        String nomPdf  = new File(cheminPdf).getName().replace(".pdf", "");
        String nomQR   = "QR_" + nomPdf + ".png";
        String cheminQR = DOSSIER_QR + nomQR;

        try {
            QRCodeWriter writer = new QRCodeWriter();

            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 2);

            // Le QR Code encode le chemin/référence de la facture
            BitMatrix bitMatrix = writer.encode(cheminPdf, BarcodeFormat.QR_CODE,
                    QR_SIZE, QR_SIZE, hints);

            // Conversion BitMatrix → BufferedImage
            BufferedImage image = new BufferedImage(QR_SIZE, QR_SIZE, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < QR_SIZE; x++) {
                for (int y = 0; y < QR_SIZE; y++) {
                    image.setRGB(x, y, bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }

            ImageIO.write(image, "PNG", new File(cheminQR));
            System.out.println("[FactureService] QR Code généré : " + cheminQR);

        } catch (Exception e) {
            System.err.println("[FactureService.genererQRCode] Erreur : " + e.getMessage());
            return null;
        }

        return cheminQR;
    }

    // =========================================================
    // GENERATION COMPLETE (PDF + QR)
    // =========================================================

    /**
     * Génère le PDF ET le QR Code en une seule opération.
     * Met à jour automatiquement les champs facture_pdf et facture_qr de la commande.
     *
     * @param commande la commande à facturer
     * @param lignes   les lignes de la commande
     * @return la commande avec ses champs facture_pdf et facture_qr remplis
     */
    public Commande genererFactureComplete(Commande commande, List<LigneCommande> lignes) {
        String cheminPdf = genererPDF(commande, lignes);
        if (cheminPdf != null) {
            commande.setFacturePdf(cheminPdf);
            String cheminQR = genererQRCode(cheminPdf);
            if (cheminQR != null) {
                commande.setFactureQr(cheminQR);
            }
        }
        return commande;
    }

    // =========================================================
    // METHODES PRIVEES UTILITAIRES PDF
    // =========================================================

    private void ajouterCelluleHeader(PdfPTable table, String texte, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(8f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void ajouterCelluleProduit(PdfPTable table, String texte, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(6f);
        cell.setBorderColor(new BaseColor(220, 220, 220));
        table.addCell(cell);
    }

    private void ajouterCelluleInfo(PdfPTable table, String label, String valeur,
                                    Font fontLabel, Font fontValeur) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, fontLabel));
        cellLabel.setBorder(Rectangle.NO_BORDER);
        cellLabel.setPadding(3f);
        table.addCell(cellLabel);

        PdfPCell cellValeur = new PdfPCell(new Phrase(valeur != null ? valeur : "—", fontValeur));
        cellValeur.setBorder(Rectangle.NO_BORDER);
        cellValeur.setPadding(3f);
        table.addCell(cellValeur);
    }
}