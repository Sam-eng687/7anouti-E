# 📊 Module Marketing Vendeur - 7anouti-E+

## 🎯 Vue d'ensemble

Ce module permet aux vendeurs de gérer leurs campagnes marketing, suivre leurs performances de vente et optimiser leur stratégie commerciale.

---

## ✨ Fonctionnalités

### Dashboard Marketing
- 📊 **Cartes KPI** : Campagnes actives, total campagnes, budget utilisé, produits suivis
- ⚡ **Actions rapides** : Nouvelle campagne, voir statistiques, gérer produits
- 🎨 **Thème adaptatif** : Support complet dark/light mode
- 🔄 **Temps réel** : Données synchronisées avec la base de données

### Intégration
- ✅ Navigation intégrée dans la sidebar principale
- ✅ Réutilisation de la connexion DB existante (`MyConnection`)
- ✅ Respect du thème global de l'application
- ✅ Aucune dépendance externe supplémentaire

---

## 📁 Structure des fichiers

```
7anouti_Premium_Final/
├── src/main/java/edu/hanouti/
│   ├── modules/
│   │   └── marketing/
│   │       └── MarketingDashboardView.java    ← NOUVEAU MODULE
│   ├── gui/
│   │   ├── HanoutiDashboard.java              ← MODIFIÉ (ajout navigation)
│   │   └── components/
│   │       └── SidebarNav.java                ← MODIFIÉ (ajout item)
│   ├── entities/
│   │   ├── CampagneMarketing.java             ← EXISTANT (réutilisé)
│   │   └── StatistiquesVentes.java            ← EXISTANT (réutilisé)
│   ├── services/
│   │   ├── CampagneMarketingService.java      ← EXISTANT (réutilisé)
│   │   └── StatistiquesVentesService.java     ← EXISTANT (réutilisé)
│   └── utils/
│       └── MyConnection.java                  ← EXISTANT (réutilisé)
├── .gitignore                                 ← NOUVEAU (sécurité)
├── GUIDE_GIT_MARKETING.md                     ← Guide Git complet
├── GUIDE_INTELLIJ_GIT.md                      ← Guide IntelliJ rapide
├── setup_git_marketing.bat                    ← Script automatique (Windows)
└── setup_git_marketing.ps1                    ← Script PowerShell
```

---

## 🚀 Installation et Déploiement

### Méthode 1 : Script automatique (Recommandé)

1. **Double-cliquez** sur `setup_git_marketing.bat`
2. Suivez les instructions
3. C'est fait ! ✨

### Méthode 2 : IntelliJ IDEA

Consultez le guide détaillé : **[GUIDE_INTELLIJ_GIT.md](GUIDE_INTELLIJ_GIT.md)**

### Méthode 3 : Git Bash

Consultez le guide complet : **[GUIDE_GIT_MARKETING.md](GUIDE_GIT_MARKETING.md)**

---

## 🔧 Configuration Technique

### Prérequis
- ✅ Java 17+
- ✅ JavaFX 17.0.6
- ✅ MySQL 8.0.30
- ✅ Maven 3.8+
- ✅ Git 2.40+

### Dépendances (déjà présentes)
- `mysql-connector-java` : Connexion base de données
- `javafx-controls` : Interface graphique
- Aucune dépendance supplémentaire requise

### Base de données
Le module utilise les tables existantes :
- `campagne_marketing` : Campagnes marketing
- `statistiques_ventes` : Statistiques de vente
- `produit` : Produits

---

## 🎨 Architecture

### Respect des principes du guide d'intégration

#### ✅ Connexion DB
```java
// ✅ CORRECT - Réutilisation de MyConnection
Connection cnx = MyConnection.getConnection();

// ❌ INCORRECT - Ne jamais créer une nouvelle connexion
Connection cnx = DriverManager.getConnection(...);
```

#### ✅ Thème Dark/Light
```java
// Lecture du thème depuis HanoutiDashboard
boolean isDark = HanoutiDashboard.darkMode;

String bgDeep = isDark ? "#0a0d1a" : "#f0f4f8";
String bgCard = isDark ? "#111425" : "#ffffff";
String text1 = isDark ? "#F1F5F9" : "#0f172a";
```

#### ✅ Navigation
```java
// Dans HanoutiDashboard.naviguerVers()
case "marketing":
    targetBtn = btnMarketing;
    nextView = new edu.hanouti.modules.marketing.MarketingDashboardView()
            .buildView(darkMode);
    navId = "marketing";
    moduleDisplayName = "Marketing Vendeur";
    break;
```

#### ✅ Sidebar
```java
// Dans SidebarNav.java
{"\uD83D\uDCBC", "Marketing", "Vendeur", "marketing"}
```

---

## 🧪 Tests

### Compilation
```bash
mvn clean compile
```

### Exécution
1. Ouvrez le projet dans IntelliJ
2. Lancez `HanoutiDashboard.java`
3. Cliquez sur l'icône 💼 dans la sidebar
4. Vérifiez que le module s'affiche correctement

### Vérifications
- [ ] Le module s'affiche sans erreur
- [ ] Les cartes KPI affichent les bonnes données
- [ ] Le thème dark/light fonctionne
- [ ] La navigation fonctionne (retour aux autres modules)
- [ ] Aucune erreur dans la console

---

## 🔀 Workflow Git

### Branches
- **dev.Alachat** : Branche principale de développement (NE PAS POUSSER DIRECTEMENT)
- **dev.Alachat.marketing** : Votre branche de travail (POUSSER ICI)

### Commandes essentielles
```bash
# Voir la branche actuelle
git branch

# Basculer sur dev.Alachat.marketing
git checkout dev.Alachat.marketing

# Mettre à jour depuis dev.Alachat
git checkout dev.Alachat
git pull origin dev.Alachat
git checkout dev.Alachat.marketing
git merge dev.Alachat

# Commit et push
git add .
git commit -m "feat: description de la modification"
git push origin dev.Alachat.marketing
```

---

## 📝 Conventions de Commit

| Préfixe | Usage | Exemple |
|---------|-------|---------|
| `feat:` | Nouvelle fonctionnalité | `feat: ajouter filtre par date` |
| `fix:` | Correction de bug | `fix: corriger calcul budget` |
| `style:` | Changement visuel | `style: améliorer cartes KPI` |
| `refactor:` | Réorganisation | `refactor: extraire méthode` |
| `docs:` | Documentation | `docs: ajouter javadoc` |
| `chore:` | Configuration | `chore: mettre à jour Maven` |

---

## 🛡️ Sécurité

### Fichiers protégés (.gitignore)
```
# Fichiers sensibles - NE JAMAIS COMMITER
gemini.properties
*.properties.local
*.key
*.env
*_API_KEY*
```

### Vérification avant commit
1. Aucun fichier sensible
2. Aucun mot de passe en dur
3. Aucune clé API

---

## 📚 Documentation

### Guides disponibles
- **[GUIDE_INTELLIJ_GIT.md](GUIDE_INTELLIJ_GIT.md)** : Guide rapide IntelliJ (recommandé)
- **[GUIDE_GIT_MARKETING.md](GUIDE_GIT_MARKETING.md)** : Guide Git complet
- **[README_MODULE_MARKETING.md](README_MODULE_MARKETING.md)** : Ce fichier

### Scripts automatiques
- **setup_git_marketing.bat** : Script Windows (double-clic)
- **setup_git_marketing.ps1** : Script PowerShell

---

## 🆘 Support

### Problèmes courants

#### Git non installé
**Solution :** Téléchargez depuis https://git-scm.com/download/win

#### Authentification échouée
**Solution :** Utilisez un Personal Access Token
1. https://github.com/settings/tokens
2. Generate new token → Cochez `repo`
3. Utilisez le token comme mot de passe

#### Erreur de compilation
**Solution :**
```bash
mvn clean install
```

#### Module ne s'affiche pas
**Solution :**
1. Vérifiez que vous êtes sur la bonne branche
2. Recompilez le projet
3. Redémarrez IntelliJ

---

## 👥 Contributeurs

- **Développeur** : [Votre Nom]
- **Projet** : 7anouti-E+ Marketing Intelligence
- **Date** : 2026

---

## 📄 Licence

Ce module fait partie du projet 7anouti-E+ et suit la même licence que le projet principal.

---

## 🎉 Prochaines étapes

1. ✅ Module créé et intégré
2. ✅ Guides et scripts fournis
3. ⏳ **À faire** : Push vers GitHub
4. ⏳ **À faire** : Créer la Pull Request
5. ⏳ **À faire** : Revue de code
6. ⏳ **À faire** : Merge dans dev.Alachat

---

## 📞 Contact

Pour toute question ou problème, consultez les guides ou contactez l'équipe de développement.

**Bon développement ! 🚀**
