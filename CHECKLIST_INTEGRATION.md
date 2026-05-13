# ✅ Checklist d'Intégration - Module Marketing

## 📋 Avant de commencer

- [ ] Git est installé sur votre machine
- [ ] IntelliJ IDEA est installé
- [ ] Vous avez accès au dépôt GitHub
- [ ] Vous avez un Personal Access Token GitHub (si nécessaire)

---

## 🔧 Installation et Configuration

### Étape 1 : Installation de Git
- [ ] Git téléchargé depuis https://git-scm.com/download/win
- [ ] Git installé avec les options par défaut
- [ ] Git accessible dans le terminal (vérifier avec `git --version`)
- [ ] IntelliJ redémarré après l'installation

### Étape 2 : Configuration Git
- [ ] Identité configurée :
  ```bash
  git config --global user.name "Votre Nom"
  git config --global user.email "votre.email@exemple.com"
  ```
- [ ] Configuration vérifiée :
  ```bash
  git config --list
  ```

---

## 📥 Clonage et Branches

### Étape 3 : Cloner le dépôt
- [ ] Dépôt cloné depuis GitHub
- [ ] URL correcte : `https://github.com/oueslatiwejden3-ship-it/7anouti-Premium-Final.git`
- [ ] Projet ouvert dans IntelliJ

### Étape 4 : Récupérer dev.Alachat
- [ ] Branches distantes récupérées (`git fetch origin`)
- [ ] Branche `dev.Alachat` checkoutée
- [ ] Dernières modifications pullées (`git pull origin dev.Alachat`)

### Étape 5 : Créer dev.Alachat.marketing
- [ ] Nouvelle branche créée : `dev.Alachat.marketing`
- [ ] Branche basée sur `dev.Alachat`
- [ ] Branche active vérifiée (doit afficher `dev.Alachat.marketing`)

---

## 📁 Copie des Fichiers

### Étape 6 : Copier les fichiers modifiés
- [ ] `MarketingDashboardView.java` copié dans `src/main/java/edu/hanouti/modules/marketing/`
- [ ] `HanoutiDashboard.java` remplacé dans `src/main/java/edu/hanouti/gui/`
- [ ] `SidebarNav.java` remplacé dans `src/main/java/edu/hanouti/gui/components/`
- [ ] `.gitignore` copié à la racine du projet

### Étape 7 : Vérifier les fichiers
- [ ] Tous les fichiers sont présents
- [ ] Aucune erreur de compilation dans IntelliJ
- [ ] Les imports sont corrects

---

## 🧪 Tests et Vérification

### Étape 8 : Compilation
- [ ] Projet compilé sans erreur (`Build → Build Project`)
- [ ] Aucune erreur dans la console
- [ ] Aucun warning critique

### Étape 9 : Test de l'application
- [ ] Application lancée (`HanoutiDashboard.java`)
- [ ] Module Marketing visible dans la sidebar (icône 💼)
- [ ] Clic sur l'icône Marketing fonctionne
- [ ] Dashboard Marketing s'affiche correctement
- [ ] Cartes KPI affichent les bonnes données
- [ ] Thème dark/light fonctionne
- [ ] Navigation vers d'autres modules fonctionne
- [ ] Retour au module Marketing fonctionne

---

## 💾 Commit et Push

### Étape 10 : Vérifier les modifications
- [ ] Branche actuelle : `dev.Alachat.marketing` (vérifier en bas à droite)
- [ ] Fichiers modifiés listés dans Git
- [ ] Aucun fichier sensible dans les modifications :
  - [ ] Pas de `gemini.properties`
  - [ ] Pas de fichiers `.key`
  - [ ] Pas de mots de passe

### Étape 11 : Commit
- [ ] Tous les fichiers ajoutés (`git add .` ou via IntelliJ)
- [ ] Message de commit clair et descriptif :
  ```
  feat: intégration module Marketing vendeur
  
  - Ajout du package edu.hanouti.modules.marketing
  - Création de MarketingDashboardView avec cartes KPI
  - Intégration dans la navigation (SidebarNav + HanoutiDashboard)
  - Réutilisation de MyConnection et thème dark/light
  - Ajout .gitignore pour protéger les fichiers sensibles
  ```
- [ ] Commit effectué

### Étape 12 : Push vers GitHub
- [ ] Push vers `origin/dev.Alachat.marketing` (PAS dev.Alachat)
- [ ] Authentification réussie
- [ ] Push confirmé sur GitHub

---

## 🔀 Pull Request

### Étape 13 : Créer la Pull Request
- [ ] Aller sur GitHub : `https://github.com/oueslatiwejden3-ship-it/7anouti-Premium-Final`
- [ ] Cliquer sur "Compare & pull request"
- [ ] Configuration correcte :
  - [ ] **Base** : `dev.Alachat`
  - [ ] **Compare** : `dev.Alachat.marketing`
- [ ] Titre : `feat: Intégration module Marketing vendeur`
- [ ] Description complète ajoutée
- [ ] Pull Request créée

---

## 📝 Vérifications Finales

### Étape 14 : Revue de code
- [ ] Code respecte les conventions du projet
- [ ] Aucune duplication de code
- [ ] Commentaires ajoutés si nécessaire
- [ ] Javadoc présente pour les méthodes publiques

### Étape 15 : Documentation
- [ ] README_MODULE_MARKETING.md lu
- [ ] GUIDE_INTELLIJ_GIT.md consulté
- [ ] GUIDE_GIT_MARKETING.md disponible pour référence

---

## 🎯 Résumé des Commandes Git

### Commandes essentielles utilisées
```bash
# Configuration
git config --global user.name "Votre Nom"
git config --global user.email "votre.email@exemple.com"

# Clonage et branches
git clone https://github.com/oueslatiwejden3-ship-it/7anouti-Premium-Final.git
git fetch origin
git checkout dev.Alachat
git pull origin dev.Alachat
git checkout -b dev.Alachat.marketing

# Commit et push
git add .
git status
git commit -m "feat: intégration module Marketing vendeur"
git push -u origin dev.Alachat.marketing

# Vérifications
git branch                    # Voir la branche actuelle
git log --oneline            # Voir l'historique
git remote -v                # Voir les remotes
```

---

## ⚠️ Points d'Attention

### À NE PAS FAIRE
- ❌ Pousser directement sur `dev.Alachat`
- ❌ Commiter des fichiers sensibles (clés API, mots de passe)
- ❌ Modifier des fichiers non liés au module Marketing
- ❌ Oublier de tester avant de pousser

### À TOUJOURS FAIRE
- ✅ Travailler sur `dev.Alachat.marketing`
- ✅ Tester avant de commiter
- ✅ Écrire des messages de commit clairs
- ✅ Vérifier la branche avant de pousser
- ✅ Pull régulièrement depuis `dev.Alachat`

---

## 🆘 En cas de problème

### Git non reconnu
**Solution :**
- [ ] Installer Git depuis https://git-scm.com/download/win
- [ ] Redémarrer IntelliJ
- [ ] Vérifier dans Settings → Version Control → Git

### Authentification échouée
**Solution :**
- [ ] Créer un Personal Access Token sur GitHub
- [ ] Utiliser le token comme mot de passe
- [ ] Sauvegarder le token dans un endroit sûr

### Erreur de compilation
**Solution :**
- [ ] `mvn clean install`
- [ ] Vérifier les imports
- [ ] Redémarrer IntelliJ

### Mauvaise branche
**Solution :**
- [ ] Vérifier la branche actuelle : `git branch`
- [ ] Basculer sur la bonne branche : `git checkout dev.Alachat.marketing`

---

## 🎉 Félicitations !

Si toutes les cases sont cochées, votre module Marketing est correctement intégré ! 🚀

### Prochaines étapes
1. ⏳ Attendre la revue de code
2. ⏳ Apporter les modifications demandées (si nécessaire)
3. ⏳ Merge dans `dev.Alachat`
4. ✅ Module en production !

---

## 📊 Statistiques

- **Fichiers créés** : 1 (MarketingDashboardView.java)
- **Fichiers modifiés** : 2 (HanoutiDashboard.java, SidebarNav.java)
- **Fichiers de configuration** : 1 (.gitignore)
- **Lignes de code** : ~350
- **Temps estimé** : 30-45 minutes

---

**Date de création** : 2026-05-13  
**Version** : 1.0  
**Statut** : ✅ Prêt pour intégration
