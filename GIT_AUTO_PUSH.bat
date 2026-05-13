@echo off
chcp 65001 >nul
color 0B
title Git Auto Push - Module Marketing

echo.
echo ╔══════════════════════════════════════════════════════════════════════════════╗
echo ║                                                                              ║
echo ║                    🚀 GIT AUTO PUSH - MODULE MARKETING                       ║
echo ║                                                                              ║
echo ╚══════════════════════════════════════════════════════════════════════════════╝
echo.

REM Chercher Git Bash
set GIT_BASH=
if exist "C:\Program Files\Git\bin\bash.exe" set GIT_BASH=C:\Program Files\Git\bin\bash.exe
if exist "C:\Program Files (x86)\Git\bin\bash.exe" set GIT_BASH=C:\Program Files (x86)\Git\bin\bash.exe
if exist "%LOCALAPPDATA%\Programs\Git\bin\bash.exe" set GIT_BASH=%LOCALAPPDATA%\Programs\Git\bin\bash.exe

if "%GIT_BASH%"=="" (
    color 0C
    echo ❌ Git Bash introuvable !
    echo.
    echo Git n'est pas installé ou n'est pas dans un emplacement standard.
    echo.
    echo 📥 Téléchargez Git depuis : https://git-scm.com/download/win
    echo.
    echo Après l'installation, relancez ce script.
    echo.
    pause
    exit /b
)

echo ✅ Git Bash trouvé : %GIT_BASH%
echo.

REM Créer un script bash temporaire
echo #!/bin/bash > git_commands.sh
echo set -e >> git_commands.sh
echo echo "════════════════════════════════════════════════════════════════" >> git_commands.sh
echo echo "🔧 Configuration Git" >> git_commands.sh
echo echo "════════════════════════════════════════════════════════════════" >> git_commands.sh
echo echo "" >> git_commands.sh
echo read -p "Entrez votre nom (ex: Wejden Oueslati) : " USER_NAME >> git_commands.sh
echo read -p "Entrez votre email (ex: wejden@exemple.com) : " USER_EMAIL >> git_commands.sh
echo echo "" >> git_commands.sh
echo git config --global user.name "$USER_NAME" >> git_commands.sh
echo git config --global user.email "$USER_EMAIL" >> git_commands.sh
echo echo "✅ Identité configurée" >> git_commands.sh
echo echo "" >> git_commands.sh
echo echo "════════════════════════════════════════════════════════════════" >> git_commands.sh
echo echo "📥 Initialisation et connexion au dépôt" >> git_commands.sh
echo echo "════════════════════════════════════════════════════════════════" >> git_commands.sh
echo echo "" >> git_commands.sh
echo git init >> git_commands.sh
echo git remote remove origin 2^>^/dev/null ^|^| true >> git_commands.sh
echo git remote add origin https://github.com/Sam-eng687/7anouti-E.git >> git_commands.sh
echo echo "✅ Dépôt distant configuré" >> git_commands.sh
echo echo "" >> git_commands.sh
echo echo "════════════════════════════════════════════════════════════════" >> git_commands.sh
echo echo "🔄 Récupération des branches distantes" >> git_commands.sh
echo echo "════════════════════════════════════════════════════════════════" >> git_commands.sh
echo echo "" >> git_commands.sh
echo git fetch origin >> git_commands.sh
echo echo "✅ Branches récupérées" >> git_commands.sh
echo echo "" >> git_commands.sh
echo echo "════════════════════════════════════════════════════════════════" >> git_commands.sh
echo echo "🌿 Checkout sur dev.Alachat" >> git_commands.sh
echo echo "════════════════════════════════════════════════════════════════" >> git_commands.sh
echo echo "" >> git_commands.sh
echo git checkout -b dev.Alachat origin/dev.Alachat 2^>^/dev/null ^|^| git checkout dev.Alachat >> git_commands.sh
echo git pull origin dev.Alachat >> git_commands.sh
echo echo "✅ Sur dev.Alachat (à jour)" >> git_commands.sh
echo echo "" >> git_commands.sh
echo echo "════════════════════════════════════════════════════════════════" >> git_commands.sh
echo echo "🆕 Création de la branche dev.Alachat.marketing" >> git_commands.sh
echo echo "════════════════════════════════════════════════════════════════" >> git_commands.sh
echo echo "" >> git_commands.sh
echo git checkout -b dev.Alachat.marketing 2^>^/dev/null ^|^| git checkout dev.Alachat.marketing >> git_commands.sh
echo echo "✅ Branche dev.Alachat.marketing créée" >> git_commands.sh
echo echo "" >> git_commands.sh
echo echo "════════════════════════════════════════════════════════════════" >> git_commands.sh
echo echo "📝 Ajout des fichiers" >> git_commands.sh
echo echo "════════════════════════════════════════════════════════════════" >> git_commands.sh
echo echo "" >> git_commands.sh
echo git add . >> git_commands.sh
echo echo "✅ Fichiers ajoutés" >> git_commands.sh
echo echo "" >> git_commands.sh
echo echo "════════════════════════════════════════════════════════════════" >> git_commands.sh
echo echo "💾 Commit des modifications" >> git_commands.sh
echo echo "════════════════════════════════════════════════════════════════" >> git_commands.sh
echo echo "" >> git_commands.sh
echo git commit -m "feat: intégration module Marketing vendeur" -m "- Ajout du package edu.hanouti.modules.marketing" -m "- Création de MarketingDashboardView avec cartes KPI" -m "- Intégration dans la navigation (SidebarNav + HanoutiDashboard)" -m "- Réutilisation de MyConnection et thème dark/light" -m "- Ajout .gitignore pour protéger les fichiers sensibles" >> git_commands.sh
echo echo "✅ Commit effectué" >> git_commands.sh
echo echo "" >> git_commands.sh
echo echo "════════════════════════════════════════════════════════════════" >> git_commands.sh
echo echo "🚀 Push vers GitHub" >> git_commands.sh
echo echo "════════════════════════════════════════════════════════════════" >> git_commands.sh
echo echo "" >> git_commands.sh
echo echo "⚠️  Vous allez être invité à entrer vos identifiants GitHub" >> git_commands.sh
echo echo "   Username: Sam-eng687" >> git_commands.sh
echo echo "   Password: Utilisez un Personal Access Token (PAT)" >> git_commands.sh
echo echo "" >> git_commands.sh
echo read -p "Appuyez sur Entrée pour continuer..." >> git_commands.sh
echo echo "" >> git_commands.sh
echo git push -u origin dev.Alachat.marketing >> git_commands.sh
echo echo "" >> git_commands.sh
echo echo "════════════════════════════════════════════════════════════════" >> git_commands.sh
echo echo "✅ SUCCÈS !" >> git_commands.sh
echo echo "════════════════════════════════════════════════════════════════" >> git_commands.sh
echo echo "" >> git_commands.sh
echo echo "Vos modifications ont été poussées sur dev.Alachat.marketing" >> git_commands.sh
echo echo "" >> git_commands.sh
echo echo "📋 Prochaines étapes :" >> git_commands.sh
echo echo "1. Allez sur : https://github.com/Sam-eng687/7anouti-E" >> git_commands.sh
echo echo "2. Cliquez sur 'Compare & pull request'" >> git_commands.sh
echo echo "3. Base: dev.Alachat ← Compare: dev.Alachat.marketing" >> git_commands.sh
echo echo "4. Créez la Pull Request" >> git_commands.sh
echo echo "" >> git_commands.sh
echo read -p "Appuyez sur Entrée pour terminer..." >> git_commands.sh

REM Exécuter le script bash
"%GIT_BASH%" git_commands.sh

REM Nettoyer
del git_commands.sh

pause
