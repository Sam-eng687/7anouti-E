@echo off
chcp 65001 >nul
color 0B

echo ========================================
echo   Git Setup - Module Marketing
echo ========================================
echo.

REM Vérifier si Git est installé
where git >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    color 0C
    echo ❌ Git n'est pas installé !
    echo.
    echo 📥 Téléchargez Git depuis : https://git-scm.com/download/win
    echo Après l'installation, relancez ce script.
    echo.
    pause
    exit /b
)

echo ✅ Git est installé
echo.

REM Demander l'URL du dépôt
set /p REPO_URL="Entrez l'URL du dépôt GitHub : "

if "%REPO_URL%"=="" (
    color 0C
    echo ❌ URL du dépôt requise !
    pause
    exit /b
)

echo ✅ URL du dépôt : %REPO_URL%
echo.

REM Initialiser Git si nécessaire
if not exist ".git" (
    echo [1/9] Initialisation de Git...
    git init
    echo ✅ Git initialisé
) else (
    echo [1/9] ✅ Git déjà initialisé
)
echo.

REM Configurer l'identité
echo [2/9] Configuration de l'identité Git
set /p USER_NAME="Entrez votre nom : "
set /p USER_EMAIL="Entrez votre email : "

git config --global user.name "%USER_NAME%"
git config --global user.email "%USER_EMAIL%"

echo ✅ Identité configurée
echo.

REM Ajouter le dépôt distant
echo [3/9] Configuration du dépôt distant...
git remote remove origin 2>nul
git remote add origin %REPO_URL%
echo ✅ Dépôt distant configuré
echo.

REM Récupérer les branches
echo [4/9] Récupération des branches distantes...
git fetch origin
echo ✅ Branches récupérées
echo.

REM Checkout sur dev.Alachat
echo [5/9] Basculement sur dev.Alachat...
git checkout dev.Alachat 2>nul
if %ERRORLEVEL% NEQ 0 (
    git checkout -b dev.Alachat
)
git pull origin dev.Alachat 2>nul
echo ✅ Sur dev.Alachat
echo.

REM Créer la branche marketing
echo [6/9] Création de la branche dev.Alachat.marketing...
git checkout -b dev.Alachat.marketing 2>nul
if %ERRORLEVEL% NEQ 0 (
    git checkout dev.Alachat.marketing
)
echo ✅ Branche dev.Alachat.marketing créée
echo.

REM Ajouter les fichiers
echo [7/9] Ajout des fichiers...
git add .
echo ✅ Fichiers ajoutés
echo.

REM Commit
echo [8/9] Commit des modifications...
git commit -m "feat: intégration module Marketing vendeur - Ajout du package edu.hanouti.modules.marketing - Création de MarketingDashboardView avec cartes KPI - Intégration dans la navigation (SidebarNav + HanoutiDashboard) - Réutilisation de MyConnection et thème dark/light - Ajout .gitignore pour protéger les fichiers sensibles"
echo ✅ Modifications commitées
echo.

REM Demander confirmation pour le push
echo ========================================
echo   Prêt à pousser vers GitHub !
echo ========================================
echo.
echo Branche cible : dev.Alachat.marketing
echo Dépôt distant : %REPO_URL%
echo.

set /p CONFIRM="Voulez-vous pousser maintenant ? (O/N) : "

if /i "%CONFIRM%"=="O" (
    echo.
    echo [9/9] 🚀 Push en cours...
    git push -u origin dev.Alachat.marketing
    
    if %ERRORLEVEL% EQU 0 (
        color 0A
        echo.
        echo ========================================
        echo   ✅ SUCCÈS !
        echo ========================================
        echo.
        echo Vos modifications ont été poussées sur dev.Alachat.marketing
        echo.
        echo 📋 Prochaines étapes :
        echo 1. Allez sur GitHub : %REPO_URL%
        echo 2. Cliquez sur 'Compare ^& pull request'
        echo 3. Base: dev.Alachat ← Compare: dev.Alachat.marketing
        echo 4. Créez la Pull Request
        echo.
    ) else (
        color 0C
        echo.
        echo ❌ Erreur lors du push
        echo.
        echo Causes possibles :
        echo - Authentification échouée (utilisez un Personal Access Token)
        echo - Pas de connexion internet
        echo - Permissions insuffisantes sur le dépôt
        echo.
        echo Pour créer un Personal Access Token :
        echo 1. Allez sur https://github.com/settings/tokens
        echo 2. Generate new token → Cochez 'repo'
        echo 3. Utilisez le token comme mot de passe
        echo.
    )
) else (
    color 0E
    echo.
    echo ⏸️  Push annulé
    echo.
    echo Pour pousser manuellement plus tard :
    echo git push -u origin dev.Alachat.marketing
    echo.
)

echo.
pause
