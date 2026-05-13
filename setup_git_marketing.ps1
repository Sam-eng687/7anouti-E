# ========================================
# Script Git - Intégration Module Marketing
# ========================================
# Ce script automatise le workflow Git pour le module Marketing
# IMPORTANT : Exécutez ce script depuis PowerShell en tant qu'administrateur

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Git Setup - Module Marketing" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Vérifier si Git est installé
Write-Host "[1/9] Vérification de Git..." -ForegroundColor Yellow
$gitInstalled = Get-Command git -ErrorAction SilentlyContinue

if (-not $gitInstalled) {
    Write-Host "❌ Git n'est pas installé !" -ForegroundColor Red
    Write-Host ""
    Write-Host "📥 Téléchargez Git depuis : https://git-scm.com/download/win" -ForegroundColor Yellow
    Write-Host "Après l'installation, relancez ce script." -ForegroundColor Yellow
    Write-Host ""
    Read-Host "Appuyez sur Entrée pour quitter"
    exit
}

Write-Host "✅ Git est installé : $($gitInstalled.Version)" -ForegroundColor Green
Write-Host ""

# Demander l'URL du dépôt
Write-Host "[2/9] Configuration du dépôt distant" -ForegroundColor Yellow
$repoUrl = Read-Host "Entrez l'URL du dépôt GitHub (ex: https://github.com/oueslatiwejden3-ship-it/7anouti-Premium-Final.git)"

if ([string]::IsNullOrWhiteSpace($repoUrl)) {
    Write-Host "❌ URL du dépôt requise !" -ForegroundColor Red
    Read-Host "Appuyez sur Entrée pour quitter"
    exit
}

Write-Host "✅ URL du dépôt : $repoUrl" -ForegroundColor Green
Write-Host ""

# Vérifier si Git est déjà initialisé
Write-Host "[3/9] Initialisation de Git..." -ForegroundColor Yellow

if (-not (Test-Path ".git")) {
    git init
    Write-Host "✅ Git initialisé" -ForegroundColor Green
} else {
    Write-Host "✅ Git déjà initialisé" -ForegroundColor Green
}
Write-Host ""

# Configurer l'identité Git
Write-Host "[4/9] Configuration de l'identité Git" -ForegroundColor Yellow
$userName = Read-Host "Entrez votre nom (ex: Wejden Oueslati)"
$userEmail = Read-Host "Entrez votre email (ex: wejden@exemple.com)"

git config --global user.name "$userName"
git config --global user.email "$userEmail"

Write-Host "✅ Identité configurée" -ForegroundColor Green
Write-Host ""

# Ajouter le dépôt distant
Write-Host "[5/9] Configuration du dépôt distant..." -ForegroundColor Yellow

$remoteExists = git remote | Select-String -Pattern "origin"

if ($remoteExists) {
    Write-Host "⚠️  Remote 'origin' existe déjà, mise à jour..." -ForegroundColor Yellow
    git remote set-url origin $repoUrl
} else {
    git remote add origin $repoUrl
}

Write-Host "✅ Dépôt distant configuré" -ForegroundColor Green
Write-Host ""

# Récupérer les branches distantes
Write-Host "[6/9] Récupération des branches distantes..." -ForegroundColor Yellow
git fetch origin

Write-Host "✅ Branches récupérées" -ForegroundColor Green
Write-Host ""

# Checkout sur dev.Alachat
Write-Host "[7/9] Basculement sur dev.Alachat..." -ForegroundColor Yellow

$branchExists = git branch -r | Select-String -Pattern "origin/dev.Alachat"

if ($branchExists) {
    git checkout dev.Alachat
    git pull origin dev.Alachat
    Write-Host "✅ Sur dev.Alachat (à jour)" -ForegroundColor Green
} else {
    Write-Host "⚠️  Branche dev.Alachat introuvable sur le dépôt distant" -ForegroundColor Yellow
    Write-Host "Création de la branche dev.Alachat localement..." -ForegroundColor Yellow
    git checkout -b dev.Alachat
    Write-Host "✅ Branche dev.Alachat créée" -ForegroundColor Green
}
Write-Host ""

# Créer la branche marketing
Write-Host "[8/9] Création de la branche dev.Alachat.marketing..." -ForegroundColor Yellow

$marketingBranchExists = git branch | Select-String -Pattern "dev.Alachat.marketing"

if ($marketingBranchExists) {
    Write-Host "⚠️  Branche dev.Alachat.marketing existe déjà" -ForegroundColor Yellow
    git checkout dev.Alachat.marketing
} else {
    git checkout -b dev.Alachat.marketing
    Write-Host "✅ Branche dev.Alachat.marketing créée" -ForegroundColor Green
}
Write-Host ""

# Ajouter et commiter les fichiers
Write-Host "[9/9] Ajout et commit des modifications..." -ForegroundColor Yellow

git add .

$commitMessage = @"
feat: intégration module Marketing vendeur

- Ajout du package edu.hanouti.modules.marketing
- Création de MarketingDashboardView avec cartes KPI
- Intégration dans la navigation (SidebarNav + HanoutiDashboard)
- Réutilisation de MyConnection et thème dark/light
- Ajout .gitignore pour protéger les fichiers sensibles
"@

git commit -m $commitMessage

Write-Host "✅ Modifications commitées" -ForegroundColor Green
Write-Host ""

# Demander confirmation pour le push
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Prêt à pousser vers GitHub !" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Branche cible : dev.Alachat.marketing" -ForegroundColor Yellow
Write-Host "Dépôt distant : $repoUrl" -ForegroundColor Yellow
Write-Host ""

$confirm = Read-Host "Voulez-vous pousser maintenant ? (O/N)"

if ($confirm -eq "O" -or $confirm -eq "o") {
    Write-Host ""
    Write-Host "🚀 Push en cours..." -ForegroundColor Yellow
    
    git push -u origin dev.Alachat.marketing
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "  ✅ SUCCÈS !" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Vos modifications ont été poussées sur dev.Alachat.marketing" -ForegroundColor Green
        Write-Host ""
        Write-Host "📋 Prochaines étapes :" -ForegroundColor Cyan
        Write-Host "1. Allez sur GitHub : $repoUrl" -ForegroundColor White
        Write-Host "2. Cliquez sur 'Compare & pull request'" -ForegroundColor White
        Write-Host "3. Base: dev.Alachat ← Compare: dev.Alachat.marketing" -ForegroundColor White
        Write-Host "4. Créez la Pull Request" -ForegroundColor White
        Write-Host ""
    } else {
        Write-Host ""
        Write-Host "❌ Erreur lors du push" -ForegroundColor Red
        Write-Host ""
        Write-Host "Causes possibles :" -ForegroundColor Yellow
        Write-Host "- Authentification échouée (utilisez un Personal Access Token)" -ForegroundColor White
        Write-Host "- Pas de connexion internet" -ForegroundColor White
        Write-Host "- Permissions insuffisantes sur le dépôt" -ForegroundColor White
        Write-Host ""
        Write-Host "Pour créer un Personal Access Token :" -ForegroundColor Yellow
        Write-Host "1. Allez sur https://github.com/settings/tokens" -ForegroundColor White
        Write-Host "2. Generate new token → Cochez 'repo'" -ForegroundColor White
        Write-Host "3. Utilisez le token comme mot de passe" -ForegroundColor White
        Write-Host ""
    }
} else {
    Write-Host ""
    Write-Host "⏸️  Push annulé" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Pour pousser manuellement plus tard :" -ForegroundColor Cyan
    Write-Host "git push -u origin dev.Alachat.marketing" -ForegroundColor White
    Write-Host ""
}

Write-Host ""
Read-Host "Appuyez sur Entrée pour quitter"
