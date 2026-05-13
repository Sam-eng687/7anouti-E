package projet.hanouti.produit_fournisseur.utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import projet.hanouti.produit_fournisseur.entities.Fournisseur;
import projet.hanouti.produit_fournisseur.entities.Produit;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfExportUtil {

    private static final BaseColor NAVY       = new BaseColor(5,  10,  56);
    private static final BaseColor PRIMARY    = new BaseColor(25, 43, 204);
    private static final BaseColor TERTIARY   = new BaseColor(79, 97, 255);
    private static final BaseColor LIGHT_BG   = new BaseColor(240,242,255);
    private static final BaseColor ROW_ALT    = new BaseColor(245,247,255);
    private static final BaseColor LOW_STOCK  = new BaseColor(220,226,255);
    private static final BaseColor CRIT_STOCK = new BaseColor(192,202,255);
    private static final BaseColor WHITE      = BaseColor.WHITE;
    private static final BaseColor TEXT_DARK  = new BaseColor(5,  10,  56);
    private static final BaseColor TEXT_MUTED = new BaseColor(100,110,160);
    private static final BaseColor GREEN      = new BaseColor(34, 136, 34);
    private static final BaseColor RED        = new BaseColor(204, 34,  34);

    // 
    // EXPORT PRODUITS
    // 
    public static File export(List<Produit> produits, String vendeurName)
            throws Exception {

        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 40, 40);
        File tempFile = File.createTempFile("7anouti_produits_", ".pdf");
        PdfWriter.getInstance(doc, new FileOutputStream(tempFile));
        doc.open();

        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        //  Header banner 
        doc.add(buildHeader("Rapport des Produits", vendeurName,
                produits.size() + " produit(s)", date));
        doc.add(Chunk.NEWLINE);

        //  Summary cards 
        long critCount = produits.stream()
                .filter(p -> p.getQuantiteStock() <= p.getSeuilAlerte())
                .count();
        long lowCount  = produits.stream()
                .filter(p -> p.getQuantiteStock() > p.getSeuilAlerte()
                        && p.getQuantiteStock() <= p.getSeuilAlerte() * 2)
                .count();
        double totalVal = produits.stream()
                .mapToDouble(p -> p.getPrix() * p.getQuantiteStock()).sum();

        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingAfter(16);
        summaryTable.addCell(summaryCard("Total Produits",
                String.valueOf(produits.size()), PRIMARY));
        summaryTable.addCell(summaryCard("Stock Critique",
                String.valueOf(critCount), NAVY));
        summaryTable.addCell(summaryCard("Stock Faible",
                String.valueOf(lowCount), TERTIARY));
        summaryTable.addCell(summaryCard("Valeur Stock",
                String.format("%.2f TND", totalVal), PRIMARY));
        doc.add(summaryTable);

        //  Legend 
        PdfPTable legendTable = new PdfPTable(3);
        legendTable.setWidthPercentage(60);
        legendTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        legendTable.setSpacingAfter(10);
        legendTable.addCell(legendItem(WHITE,      "Stock normal"));
        legendTable.addCell(legendItem(LOW_STOCK,  "Stock faible ( 2x seuil)"));
        legendTable.addCell(legendItem(CRIT_STOCK, " Stock critique ( seuil)"));
        doc.add(legendTable);

        //  Main table 
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.5f, 2.5f, 1.5f, 1f, 1f, 1f, 1.2f, 1.2f});

        Font headerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, WHITE);
        for (String h : new String[]{"#","Nom","Categorie","Prix (TND)",
                "Stock","Seuil","Statut","Date Ajout"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(PRIMARY);
            cell.setPadding(8);
            cell.setBorderColor(TERTIARY);
            cell.setBorderWidth(0.5f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }

        Font cellFont      = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, TEXT_DARK);
        Font cellFontBold  = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD,   PRIMARY);
        Font cellFontMuted = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, TEXT_MUTED);

        int rowNum = 1;
        for (Produit p : produits) {
            boolean isCritical = p.getQuantiteStock() <= p.getSeuilAlerte();
            boolean isLow      = !isCritical
                    && p.getQuantiteStock() <= p.getSeuilAlerte() * 2;
            BaseColor rowBg    = isCritical ? CRIT_STOCK
                    : isLow ? LOW_STOCK
                    : (rowNum % 2 == 0) ? ROW_ALT : WHITE;

            addCell(table, String.valueOf(rowNum),          cellFontMuted, rowBg, Element.ALIGN_CENTER);
            addCell(table, (isCritical ? " " : "") + p.getNom(),
                    isCritical ? cellFontBold : cellFont,   rowBg, Element.ALIGN_LEFT);
            addCell(table, p.getCategorie(),                cellFont,      rowBg, Element.ALIGN_CENTER);
            addCell(table, String.format("%.2f", p.getPrix()), cellFont,   rowBg, Element.ALIGN_CENTER);
            addCell(table, String.valueOf(p.getQuantiteStock()),
                    isCritical ? cellFontBold : cellFont,   rowBg, Element.ALIGN_CENTER);
            addCell(table, String.valueOf(p.getSeuilAlerte()), cellFontMuted, rowBg, Element.ALIGN_CENTER);
            Font statutFont = new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD,
                    "ACTIF".equals(p.getStatut()) ? PRIMARY : TEXT_MUTED);
            addCell(table, p.getStatut(), statutFont,       rowBg, Element.ALIGN_CENTER);
            String dateStr = p.getDateAjout() != null
                    ? p.getDateAjout().toLocalDate()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-";
            addCell(table, dateStr, cellFontMuted,          rowBg, Element.ALIGN_CENTER);
            rowNum++;
        }
        doc.add(table);

        //  Footer 
        doc.add(Chunk.NEWLINE);
        Font footerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, TEXT_MUTED);
        Paragraph footer = new Paragraph("7anouti-E   " + date, footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        doc.close();
        return tempFile;
    }

    // 
    // EXPORT FOURNISSEURS  same layout as produits
    // 
    public static File exportFournisseurs(List<Fournisseur> fournisseurs,
                                          String vendeurName) throws Exception {

        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 40, 40);
        File tempFile = File.createTempFile("7anouti_fournisseurs_", ".pdf");
        PdfWriter.getInstance(doc, new FileOutputStream(tempFile));
        doc.open();

        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        //  Header banner 
        doc.add(buildHeader("Liste des Fournisseurs", vendeurName,
                fournisseurs.size() + " fournisseur(s)", date));
        doc.add(Chunk.NEWLINE);

        //  Summary cards 
        long actifCount   = fournisseurs.stream().filter(Fournisseur::isActif).count();
        long inactifCount = fournisseurs.size() - actifCount;

        PdfPTable summaryTable = new PdfPTable(3);
        summaryTable.setWidthPercentage(75);
        summaryTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        summaryTable.setSpacingAfter(16);
        summaryTable.addCell(summaryCard("Total Fournisseurs",
                String.valueOf(fournisseurs.size()), PRIMARY));
        summaryTable.addCell(summaryCard("Actifs",
                String.valueOf(actifCount), NAVY));
        summaryTable.addCell(summaryCard("Inactifs",
                String.valueOf(inactifCount), TERTIARY));
        doc.add(summaryTable);

        //  Main table 
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.5f, 2f, 3f, 1.8f, 4f, 1.2f});

        Font headerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, WHITE);
        for (String h : new String[]{"Societe","Contact","Email",
                "Telephone","Adresse","Statut"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(PRIMARY);
            cell.setPadding(8);
            cell.setBorderColor(TERTIARY);
            cell.setBorderWidth(0.5f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }

        Font cellFont      = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, TEXT_DARK);
        Font cellFontMuted = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, TEXT_MUTED);

        int rowNum = 1;
        for (Fournisseur f : fournisseurs) {
            BaseColor rowBg = (rowNum % 2 == 0) ? ROW_ALT : WHITE;

            addCell(table, f.getNomSociete(),    cellFont,      rowBg, Element.ALIGN_LEFT);
            addCell(table, f.getContactNom(),    cellFont,      rowBg, Element.ALIGN_LEFT);
            addCell(table, f.getEmail(),         cellFontMuted, rowBg, Element.ALIGN_LEFT);
            addCell(table, f.getTelephone(),     cellFontMuted, rowBg, Element.ALIGN_CENTER);

            // Adresse  truncated
            String adresse = f.getAdresse() != null
                    ? (f.getAdresse().length() > 60
                    ? f.getAdresse().substring(0, 60) + "..."
                    : f.getAdresse())
                    : "-";
            addCell(table, adresse, cellFontMuted, rowBg, Element.ALIGN_LEFT);

            // Statut badge coloured
            Font statutFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD,
                    f.isActif() ? GREEN : RED);
            addCell(table, f.isActif() ? "Actif" : "Inactif",
                    statutFont, rowBg, Element.ALIGN_CENTER);

            rowNum++;
        }
        doc.add(table);

        //  Footer 
        doc.add(Chunk.NEWLINE);
        Font footerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, TEXT_MUTED);
        Paragraph footer = new Paragraph("7anouti-E   " + date, footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        doc.close();
        return tempFile;
    }

    // 
    // SHARED HELPERS
    // 

    private static PdfPTable buildHeader(String reportTitle,
                                         String vendeurName,
                                         String countLabel,
                                         String date) throws Exception {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{3f, 1f});

        Font logoFont     = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, WHITE);
        Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL,
                new BaseColor(180,190,255));
        Font dateFont     = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL,
                new BaseColor(180,190,255));
        Font totalFont    = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, WHITE);

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBackgroundColor(NAVY);
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(20);
        leftCell.addElement(new Paragraph("7anouti-E", logoFont));
        leftCell.addElement(new Paragraph(reportTitle + "    " + vendeurName, subtitleFont));
        headerTable.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBackgroundColor(PRIMARY);
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(20);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(new Paragraph("Exporte le: " + date, dateFont));
        rightCell.addElement(new Paragraph(countLabel, totalFont));
        headerTable.addCell(rightCell);

        return headerTable;
    }

    private static PdfPCell summaryCard(String label, String value, BaseColor bg) {
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 8,  Font.NORMAL,
                new BaseColor(180,190,255));
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, WHITE);
        PdfPCell cell  = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(12);
        cell.addElement(new Paragraph(label, labelFont));
        cell.addElement(new Paragraph(value, valueFont));
        return cell;
    }

    private static PdfPCell legendItem(BaseColor bg, String label) {
        Font f = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, TEXT_DARK);
        PdfPCell cell = new PdfPCell(new Phrase(label, f));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setBorderColor(new BaseColor(200,210,255));
        cell.setBorderWidth(0.5f);
        return cell;
    }

    private static void addCell(PdfPTable table, String text,
                                Font font, BaseColor bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setBorderColor(new BaseColor(220,225,255));
        cell.setBorderWidth(0.3f);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }
}