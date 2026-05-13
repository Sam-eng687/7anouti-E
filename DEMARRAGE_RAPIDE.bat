@echo off
chcp 65001 >nul
color 0B
title Module Marketing - Démarrage Rapide

:MENU
cls
echo.
echo ╔══════════════════════════════════════════════════════════════════════════════╗
echo ║                                                                              ║
echo ║                    📊 MODULE MARKETING VENDEUR - 7anouti-E+                  ║
echo ║                                                                              ║
echo ║                          🚀 DÉMARRAGE RAPIDE                                 ║
echo ║                                                                              ║
echo ╚══════════════════════════════════════════════════════════════════════════════╝
echo.
echo.
echo   Que voulez-vous faire ?
echo.
echo   [1] 🚀 Lancer le script Git automatique (RECOMMANDÉ)
echo.
echo   [2] 📖 Ouvrir le guide IntelliJ (débutants)
echo.
echo   [3] 📖 Ouvrir le guide Git complet
echo.
echo   [4] ✅ Ouvrir la checklist d'intégration
echo.
echo   [5] 📄 Ouvrir le README du module
echo.
echo   [6] 📝 Ouvrir le fichier LISEZMOI
echo.
echo   [7] 🌐 Ouvrir GitHub (créer Personal Access Token)
echo.
echo   [8] 💾 Télécharger Git (si pas installé)
echo.
echo   [0] ❌ Quitter
echo.
echo ═══════════════════════════════════════════════════════════════════════════════
echo.

set /p CHOICE="Votre choix (0-8) : "

if "%CHOICE%"=="1" goto SCRIPT
if "%CHOICE%"=="2" goto GUIDE_INTELLIJ
if "%CHOICE%"=="3" goto GUIDE_GIT
if "%CHOICE%"=="4" goto CHECKLIST
if "%CHOICE%"=="5" goto README
if "%CHOICE%"=="6" goto LISEZMOI
if "%CHOICE%"=="7" goto GITHUB
if "%CHOICE%"=="8" goto DOWNLOAD_GIT
if "%CHOICE%"=="0" goto EXIT

echo.
echo ❌ Choix invalide !
timeout /t 2 >nul
goto MENU

:SCRIPT
cls
echo.
echo 🚀 Lancement du script Git automatique...
echo.
timeout /t 1 >nul
call setup_git_marketing.bat
pause
goto MENU

:GUIDE_INTELLIJ
cls
echo.
echo 📖 Ouverture du guide IntelliJ...
echo.
start "" "GUIDE_INTELLIJ_GIT.md"
timeout /t 2 >nul
goto MENU

:GUIDE_GIT
cls
echo.
echo 📖 Ouverture du guide Git complet...
echo.
start "" "GUIDE_GIT_MARKETING.md"
timeout /t 2 >nul
goto MENU

:CHECKLIST
cls
echo.
echo ✅ Ouverture de la checklist...
echo.
start "" "CHECKLIST_INTEGRATION.md"
timeout /t 2 >nul
goto MENU

:README
cls
echo.
echo 📄 Ouverture du README...
echo.
start "" "README_MODULE_MARKETING.md"
timeout /t 2 >nul
goto MENU

:LISEZMOI
cls
echo.
echo 📝 Ouverture du fichier LISEZMOI...
echo.
start "" "LISEZMOI_IMPORTANT.txt"
timeout /t 2 >nul
goto MENU

:GITHUB
cls
echo.
echo 🌐 Ouverture de GitHub...
echo.
echo Pour créer un Personal Access Token :
echo 1. Connectez-vous à GitHub
echo 2. Settings → Developer settings → Personal access tokens
echo 3. Generate new token (classic)
echo 4. Cochez "repo" (accès complet aux dépôts)
echo 5. Copiez le token et utilisez-le comme mot de passe
echo.
start "" "https://github.com/settings/tokens"
timeout /t 3 >nul
goto MENU

:DOWNLOAD_GIT
cls
echo.
echo 💾 Ouverture de la page de téléchargement Git...
echo.
echo Installez Git avec les options par défaut.
echo Après l'installation, redémarrez IntelliJ.
echo.
start "" "https://git-scm.com/download/win"
timeout /t 3 >nul
goto MENU

:EXIT
cls
echo.
echo ═══════════════════════════════════════════════════════════════════════════════
echo.
echo   ✅ Module Marketing prêt à être intégré !
echo.
echo   📋 Prochaines étapes :
echo      1. Exécutez le script Git automatique (option 1)
echo      2. Ou suivez le guide IntelliJ (option 2)
echo      3. Créez la Pull Request sur GitHub
echo.
echo   🎉 Bon développement !
echo.
echo ═══════════════════════════════════════════════════════════════════════════════
echo.
timeout /t 3 >nul
exit
