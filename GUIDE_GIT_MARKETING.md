# 🚀 Guide Git — Intégration Module Marketing

## 📋 Workflow Git pour le Module Marketing

### ⚠️ RÈGLES IMPORTANTES
- ✅ **TOUJOURS** travailler sur la branche `dev.Alachat.marketing`
- ❌ **JAMAIS** pousser directement sur `dev.Alachat`
- ✅ Pull depuis `dev.Alachat` pour rester à jour
- ✅ Push sur `dev.Alachat.marketing` pour vos modifications

---

## 🔧 MÉTHODE 1 : Avec IntelliJ IDEA (Recommandé)

### Étape 1 : Cloner le dépôt (première fois seulement)

1. **Ouvrir IntelliJ IDEA**
2. **File** → **New** → **Project from Version Control**
3. **URL** : `https://github.com/oueslatiwejden3-ship-it/7anouti-Premium-Final.git`
   - *(Remplacez par l'URL exacte de votre dépôt)*
4. **Directory** : Choisissez où sauvegarder le projet
5. Cliquez sur **Clone**

### Étape 2 : Configurer Git (première fois seulement)

1. **File** → **Settings** (ou `Ctrl+Alt+S`)
2. **Version Control** → **Git**
3. Vérifiez que le chemin vers `git.exe` est correct
4. Si Git n'est pas détecté, téléchargez-le : https://git-scm.com/download/win

### Étape 3 : Récupérer la branche dev.Alachat

1. **En bas à droite** d'IntelliJ, cliquez sur **Git: main** (ou la branche actuelle)
2. Dans le menu qui s'ouvre :
   - Cliquez sur **dev.Alachat** → **Checkout**
   - Si la branche n'apparaît pas, cliquez sur **+ New Branch from Selected...**

### Étape 4 : Créer votre branche marketing

1. **En bas à droite**, cliquez sur **Git: dev.Alachat**
2. Cliquez sur **+ New Branch**
3. **Nom de la branche** : `dev.Alachat.marketing`
4. ✅ **Cochez** "Checkout branch"
5. Cliquez sur **Create**

### Étape 5 : Copier vos fichiers modifiés

1. Copiez les fichiers suivants depuis `C:\Users\Dell\Downloads\7anouti_Premium_Final_v3_fixed\7anouti_Premium_Final\` vers votre projet cloné :

   **Fichiers à copier :**
   ```
   src/main/java/edu/hanouti/modules/marketing/MarketingDashboardView.java
   src/main/java/edu/hanouti/gui/HanoutiDashboard.java
   src/main/java/edu/hanouti/gui/components/SidebarNav.java
   .gitignore
   ```

2. IntelliJ détectera automatiquement les changements

### Étape 6 : Commit vos modifications

1. **Ctrl+K** (ou **VCS** → **Commit**)
2. Dans la fenêtre de commit :
   - ✅ Cochez tous les fichiers modifiés
   - **Message de commit** :
     ```
     feat: intégration module Marketing vendeur
     
     - Ajout du package edu.hanouti.modules.marketing
     - Création de MarketingDashboardView avec cartes KPI
     - Intégration dans la navigation (SidebarNav + HanoutiDashboard)
     - Réutilisation de MyConnection et thème dark/light
     - Ajout .gitignore pour protéger les fichiers sensibles
     ```
3. Cliquez sur **Commit**

### Étape 7 : Push vers GitHub

1. **Ctrl+Shift+K** (ou **VCS** → **Git** → **Push**)
2. Vérifiez que la branche est bien **dev.Alachat.marketing**
3. Cliquez sur **Push**
4. Si demandé, entrez vos identifiants GitHub :
   - **Username** : `oueslatiwejden3-ship-it`
   - **Password** : Utilisez un **Personal Access Token** (PAT)
     - Créez-le sur : https://github.com/settings/tokens
     - Permissions : `repo` (accès complet aux dépôts)

### Étape 8 : Créer une Pull Request (sur GitHub)

1. Allez sur GitHub : https://github.com/oueslatiwejden3-ship-it/7anouti-Premium-Final
2. Vous verrez un message : **"dev.Alachat.marketing had recent pushes"**
3. Cliquez sur **Compare & pull request**
4. **Base branch** : `dev.Alachat`
5. **Compare branch** : `dev.Alachat.marketing`
6. **Titre** : `feat: Intégration module Marketing vendeur`
7. **Description** :
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
   
   ### Tests effectués
   - [x] Compilation réussie
   - [x] Navigation fonctionnelle
   - [x] Thème dark/light appliqué
   - [x] Connexion DB réutilisée
   ```
8. Cliquez sur **Create pull request**

---

## 🔧 MÉTHODE 2 : Avec Git Bash (Alternative)

### Installation de Git

1. Téléchargez Git : https://git-scm.com/download/win
2. Installez avec les options par défaut
3. Redémarrez IntelliJ après l'installation

### Commandes Git

```bash
# 1. Aller dans le dossier du projet
cd "C:\Users\Dell\Downloads\7anouti_Premium_Final_v3_fixed\7anouti_Premium_Final"

# 2. Initialiser Git (si pas déjà fait)
git init

# 3. Configurer votre identité
git config --global user.name "Votre Nom"
git config --global user.email "votre.email@exemple.com"

# 4. Ajouter le dépôt distant
git remote add origin https://github.com/oueslatiwejden3-ship-it/7anouti-Premium-Final.git

# 5. Récupérer toutes les branches
git fetch origin

# 6. Checkout sur dev.Alachat
git checkout dev.Alachat

# 7. Pull les dernières modifications
git pull origin dev.Alachat

# 8. Créer et basculer sur la nouvelle branche marketing
git checkout -b dev.Alachat.marketing

# 9. Ajouter tous les fichiers modifiés
git add .

# 10. Vérifier les fichiers ajoutés
git status

# 11. Commit avec un message clair
git commit -m "feat: intégration module Marketing vendeur

- Ajout du package edu.hanouti.modules.marketing
- Création de MarketingDashboardView avec cartes KPI
- Intégration dans la navigation (SidebarNav + HanoutiDashboard)
- Réutilisation de MyConnection et thème dark/light
- Ajout .gitignore pour protéger les fichiers sensibles"

# 12. Push vers la branche marketing (PAS dev.Alachat)
git push -u origin dev.Alachat.marketing
```

---

## 📝 Workflow Quotidien (après la première fois)

### Avant de commencer à travailler (chaque jour)

**Avec IntelliJ :**
1. **En bas à droite** → **Git: dev.Alachat.marketing**
2. Cliquez sur **dev.Alachat** → **Update** (ou `Ctrl+T`)
3. Revenez sur **dev.Alachat.marketing**
4. **VCS** → **Git** → **Merge** → Sélectionnez **dev.Alachat**

**Avec Git Bash :**
```bash
# Récupérer les dernières modifications de dev.Alachat
git checkout dev.Alachat
git pull origin dev.Alachat

# Retourner sur votre branche et fusionner
git checkout dev.Alachat.marketing
git merge dev.Alachat
```

### Après avoir fait des modifications

**Avec IntelliJ :**
1. **Ctrl+K** → Cocher les fichiers → Message de commit → **Commit**
2. **Ctrl+Shift+K** → **Push**

**Avec Git Bash :**
```bash
# Voir les changements
git status

# Ajouter les fichiers modifiés
git add .

# Commit
git commit -m "fix: correction bug X"

# Push
git push origin dev.Alachat.marketing
```

---

## 🎯 Messages de Commit Recommandés

| Préfixe | Usage | Exemple |
|---------|-------|---------|
| `feat:` | Nouvelle fonctionnalité | `feat: ajouter filtre par date dans dashboard` |
| `fix:` | Correction de bug | `fix: corriger calcul budget utilisé` |
| `style:` | Changement visuel/CSS | `style: améliorer cartes KPI en mode clair` |
| `refactor:` | Réorganisation du code | `refactor: extraire méthode createKpiCard` |
| `docs:` | Documentation | `docs: ajouter javadoc pour MarketingDashboardView` |
| `chore:` | Mise à jour config | `chore: mettre à jour dépendances Maven` |

---

## ⚠️ Erreurs Courantes et Solutions

### Erreur : "Git is not installed"
**Solution :** Téléchargez Git depuis https://git-scm.com/download/win et redémarrez IntelliJ

### Erreur : "Authentication failed"
**Solution :** Utilisez un Personal Access Token au lieu du mot de passe
1. Allez sur https://github.com/settings/tokens
2. **Generate new token** → Cochez `repo`
3. Copiez le token et utilisez-le comme mot de passe

### Erreur : "Cannot push to dev.Alachat"
**Solution :** Vérifiez que vous êtes bien sur `dev.Alachat.marketing`
```bash
git branch  # Voir la branche actuelle (doit être dev.Alachat.marketing)
```

### Erreur : "Merge conflict"
**Solution :** Résolvez les conflits dans IntelliJ
1. **VCS** → **Git** → **Resolve Conflicts**
2. Choisissez les modifications à garder
3. Cliquez sur **Apply**

---

## 📞 Aide Rapide

### Voir la branche actuelle
```bash
git branch
```

### Voir l'historique des commits
```bash
git log --oneline
```

### Annuler le dernier commit (avant push)
```bash
git reset --soft HEAD~1
```

### Voir les différences avant commit
```bash
git diff
```

---

## ✅ Checklist Finale

Avant de créer la Pull Request, vérifiez :

- [ ] Vous êtes sur la branche `dev.Alachat.marketing`
- [ ] Tous les fichiers sensibles sont dans `.gitignore`
- [ ] Le code compile sans erreur
- [ ] Les tests passent (si applicable)
- [ ] Le message de commit est clair et descriptif
- [ ] Vous avez pull les dernières modifications de `dev.Alachat`
- [ ] Vous n'avez PAS poussé sur `dev.Alachat` directement

---

## 🎉 Félicitations !

Votre module Marketing est maintenant intégré et prêt pour la revue de code ! 🚀
