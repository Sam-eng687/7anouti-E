# 🎯 Guide Rapide Git avec IntelliJ IDEA

## 🚀 Méthode Rapide (Recommandée)

### Option 1 : Utiliser le script automatique

1. **Double-cliquez** sur `setup_git_marketing.bat`
2. Suivez les instructions à l'écran
3. Entrez l'URL de votre dépôt GitHub
4. Entrez vos identifiants
5. Le script fait tout automatiquement ! ✨

---

### Option 2 : Avec IntelliJ IDEA (Étape par étape)

## 📥 ÉTAPE 1 : Cloner le dépôt (Première fois)

### Si vous n'avez pas encore le projet :

1. **Ouvrir IntelliJ IDEA**
2. **Get from VCS** (sur l'écran d'accueil)
   - Ou **File** → **New** → **Project from Version Control**
3. **URL** : `https://github.com/oueslatiwejden3-ship-it/7anouti-Premium-Final.git`
4. **Directory** : Choisissez où sauvegarder
5. **Clone**

### Si vous avez déjà le projet téléchargé :

1. **Ouvrir le projet** dans IntelliJ
2. **VCS** → **Enable Version Control Integration**
3. Sélectionnez **Git**
4. **VCS** → **Git** → **Remotes**
5. **+** → Ajoutez :
   - **Name** : `origin`
   - **URL** : `https://github.com/oueslatiwejden3-ship-it/7anouti-Premium-Final.git`

---

## 🔄 ÉTAPE 2 : Récupérer la branche dev.Alachat

### Méthode visuelle (IntelliJ) :

1. **En bas à droite** de la fenêtre IntelliJ
2. Cliquez sur **Git: main** (ou la branche actuelle)
3. Dans le menu déroulant :
   - Cherchez **origin/dev.Alachat**
   - Cliquez dessus → **Checkout**
4. IntelliJ va créer une branche locale `dev.Alachat`

### Si la branche n'apparaît pas :

1. **VCS** → **Git** → **Fetch**
2. Attendez que les branches distantes soient récupérées
3. Répétez l'étape ci-dessus

---

## 🌿 ÉTAPE 3 : Créer votre branche marketing

1. **En bas à droite** → Cliquez sur **Git: dev.Alachat**
2. Cliquez sur **+ New Branch**
3. **Nom** : `dev.Alachat.marketing`
4. ✅ **Cochez** "Checkout branch"
5. **Create**

✅ Vous êtes maintenant sur `dev.Alachat.marketing` !

---

## 📝 ÉTAPE 4 : Copier vos fichiers modifiés

### Fichiers à copier depuis le dossier téléchargé :

```
Source : C:\Users\Dell\Downloads\7anouti_Premium_Final_v3_fixed\7anouti_Premium_Final\

Vers votre projet cloné :

✅ src/main/java/edu/hanouti/modules/marketing/MarketingDashboardView.java
✅ src/main/java/edu/hanouti/gui/HanoutiDashboard.java
✅ src/main/java/edu/hanouti/gui/components/SidebarNav.java
✅ .gitignore
```

### Comment copier :

1. Ouvrez **deux fenêtres de l'Explorateur Windows**
2. **Fenêtre 1** : Dossier téléchargé
3. **Fenêtre 2** : Projet cloné
4. **Glissez-déposez** les fichiers
5. IntelliJ détectera automatiquement les changements

---

## 💾 ÉTAPE 5 : Commit vos modifications

### Méthode 1 : Raccourci clavier (Rapide)

1. **Ctrl+K** (ou **Cmd+K** sur Mac)
2. Fenêtre de commit s'ouvre
3. ✅ **Cochez tous les fichiers** modifiés
4. **Message de commit** :
   ```
   feat: intégration module Marketing vendeur
   
   - Ajout du package edu.hanouti.modules.marketing
   - Création de MarketingDashboardView avec cartes KPI
   - Intégration dans la navigation (SidebarNav + HanoutiDashboard)
   - Réutilisation de MyConnection et thème dark/light
   - Ajout .gitignore pour protéger les fichiers sensibles
   ```
5. **Commit** (bouton en bas)

### Méthode 2 : Menu (Détaillé)

1. **VCS** → **Commit** (ou clic droit sur le projet → **Git** → **Commit Directory**)
2. Fenêtre de commit s'ouvre à gauche
3. ✅ **Cochez les fichiers** à commiter
4. **Message de commit** (voir ci-dessus)
5. **Commit**

---

## 🚀 ÉTAPE 6 : Push vers GitHub

### Méthode 1 : Raccourci clavier (Rapide)

1. **Ctrl+Shift+K** (ou **Cmd+Shift+K** sur Mac)
2. Fenêtre de push s'ouvre
3. Vérifiez :
   - **Branch** : `dev.Alachat.marketing`
   - **Remote** : `origin/dev.Alachat.marketing`
4. **Push**

### Méthode 2 : Menu (Détaillé)

1. **VCS** → **Git** → **Push**
2. Vérifiez la branche
3. **Push**

### Si demandé (première fois) :

- **Username** : `oueslatiwejden3-ship-it`
- **Password** : Utilisez un **Personal Access Token** (PAT)
  - Créez-le sur : https://github.com/settings/tokens
  - **Generate new token (classic)**
  - **Permissions** : Cochez `repo` (accès complet)
  - **Copiez le token** et utilisez-le comme mot de passe

---

## 🔀 ÉTAPE 7 : Créer une Pull Request

### Sur GitHub :

1. Allez sur : `https://github.com/oueslatiwejden3-ship-it/7anouti-Premium-Final`
2. Vous verrez : **"dev.Alachat.marketing had recent pushes"**
3. **Compare & pull request**
4. Configurez :
   - **Base** : `dev.Alachat` ← **Compare** : `dev.Alachat.marketing`
5. **Titre** : `feat: Intégration module Marketing vendeur`
6. **Description** :
   ```markdown
   ## 📊 Module Marketing Vendeur
   
   ### Fonctionnalités ajoutées
   - ✅ Dashboard marketing avec cartes KPI
   - ✅ Statistiques : campagnes actives, budget utilisé, produits suivis
   - ✅ Boutons d'actions rapides
   - ✅ Intégration dans la navigation principale
   - ✅ Support thème dark/light
   
   ### Fichiers modifiés
   - `MarketingDashboardView.java` (nouveau)
   - `HanoutiDashboard.java` (ajout cas "marketing")
   - `SidebarNav.java` (ajout item Marketing)
   - `.gitignore` (protection fichiers sensibles)
   ```
7. **Create pull request**

---

## 🔄 Workflow Quotidien (Après la première fois)

### Avant de commencer à travailler :

1. **En bas à droite** → **Git: dev.Alachat.marketing**
2. Cliquez sur **dev.Alachat**
3. **Update** (ou **Ctrl+T**)
4. Revenez sur **dev.Alachat.marketing**
5. **VCS** → **Git** → **Merge**
6. Sélectionnez **dev.Alachat**

### Après avoir fait des modifications :

1. **Ctrl+K** → Commit
2. **Ctrl+Shift+K** → Push

---

## 🎨 Raccourcis Clavier Utiles

| Action | Windows/Linux | Mac |
|--------|---------------|-----|
| Commit | `Ctrl+K` | `Cmd+K` |
| Push | `Ctrl+Shift+K` | `Cmd+Shift+K` |
| Pull/Update | `Ctrl+T` | `Cmd+T` |
| Voir l'historique | `Alt+9` | `Cmd+9` |
| Voir les changements | `Ctrl+D` | `Cmd+D` |

---

## ⚠️ Vérifications Importantes

### Avant chaque Push, vérifiez :

1. **Branche actuelle** (en bas à droite) : `dev.Alachat.marketing` ✅
2. **Pas de fichiers sensibles** dans le commit :
   - ❌ `gemini.properties`
   - ❌ Fichiers `.key`
   - ❌ Mots de passe
3. **Le code compile** : **Build** → **Build Project**

---

## 🆘 Résolution de Problèmes

### "Git is not installed"

**Solution :**
1. Téléchargez Git : https://git-scm.com/download/win
2. Installez avec les options par défaut
3. **Redémarrez IntelliJ**
4. **File** → **Settings** → **Version Control** → **Git**
5. Vérifiez que le chemin vers `git.exe` est correct

### "Authentication failed"

**Solution :**
1. Allez sur https://github.com/settings/tokens
2. **Generate new token (classic)**
3. Cochez `repo`
4. **Copiez le token**
5. Dans IntelliJ, utilisez le token comme **mot de passe**

### "Cannot push to dev.Alachat"

**Solution :**
1. Vérifiez la branche actuelle (en bas à droite)
2. Si vous êtes sur `dev.Alachat`, basculez sur `dev.Alachat.marketing`
3. **Git: dev.Alachat** → **dev.Alachat.marketing** → **Checkout**

### "Merge conflict"

**Solution :**
1. **VCS** → **Git** → **Resolve Conflicts**
2. IntelliJ ouvre un outil de fusion
3. **Gauche** : Vos modifications
4. **Droite** : Modifications distantes
5. **Centre** : Résultat final
6. Choisissez les modifications à garder
7. **Apply**

---

## ✅ Checklist Finale

Avant de créer la Pull Request :

- [ ] Je suis sur la branche `dev.Alachat.marketing`
- [ ] J'ai pull les dernières modifications de `dev.Alachat`
- [ ] Le code compile sans erreur
- [ ] Aucun fichier sensible n'est commité
- [ ] Le message de commit est clair
- [ ] J'ai testé le module dans l'application

---

## 🎉 Félicitations !

Vous maîtrisez maintenant le workflow Git avec IntelliJ ! 🚀

**Besoin d'aide ?** Consultez le guide complet : `GUIDE_GIT_MARKETING.md`
