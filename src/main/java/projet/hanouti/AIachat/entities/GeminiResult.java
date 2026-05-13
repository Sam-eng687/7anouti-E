package projet.hanouti.AIachat.entities;

import java.util.List;

public class GeminiResult {

    private boolean isShoppingRequest;
    private List<String> keywords;
    private String categorie;
    private String reformulation;
    private String rejectionReason;
    private boolean usedFallback;

    public GeminiResult() {
    }

    public GeminiResult(boolean isShoppingRequest, List<String> keywords, String categorie,
                        String reformulation, String rejectionReason, boolean usedFallback) {
        this.isShoppingRequest = isShoppingRequest;
        this.keywords = keywords;
        this.categorie = categorie;
        this.reformulation = reformulation;
        this.rejectionReason = rejectionReason;
        this.usedFallback = usedFallback;
    }

    public boolean isShoppingRequest() {
        return isShoppingRequest;
    }

    public void setShoppingRequest(boolean shoppingRequest) {
        isShoppingRequest = shoppingRequest;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public String getReformulation() {
        return reformulation;
    }

    public void setReformulation(String reformulation) {
        this.reformulation = reformulation;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public boolean isUsedFallback() {
        return usedFallback;
    }

    public void setUsedFallback(boolean usedFallback) {
        this.usedFallback = usedFallback;
    }
}


