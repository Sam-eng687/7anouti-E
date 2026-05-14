# Générateur Automatique de Conseils IA

## Vue d'ensemble

Le système de génération automatique de conseils IA analyse les données de la table `interaction_utilisateur` pour générer des recommandations marketing intelligentes et les insérer automatiquement dans la table `conseils_ia`.

## Fonctionnalités

### 1. Génération Automatique Planifiée
- **Intervalle par défaut**: 30 minutes
- **Démarrage**: Automatique au lancement de l'application
- **Analyse**: 5 types de conseils basés sur les comportements utilisateurs

### 2. Types de Conseils Générés

#### A. **PROMOTION** - Produits avec beaucoup de vues mais peu d'achats
- **Critère**: Produits avec ≥10 vues mais taux de conversion <10%
- **Urgence**: MOYEN
- **Score**: 85-95
- **Recommandation**: Promotion de 10-15% pour booster les ventes

#### B. **PROMOTION URGENTE** - Paniers abandonnés
- **Critère**: Produits avec ≥5 ajouts au panier mais taux d'achat <30%
- **Urgence**: URGENT
- **Score**: 90-95
- **Recommandation**: Promotion flash ou livraison gratuite

#### C. **BUNDLE** - Produits populaires
- **Critère**: Produits avec ≥15 interactions totales (top 5)
- **Urgence**: MOYEN
- **Score**: 70-95
- **Recommandation**: Créer des bundles avec produits complémentaires

#### D. **DESTOCKAGE** - Stock faible + demande élevée
- **Critère**: Stock ≤ seuil d'alerte ET ≥5 interactions
- **Urgence**: URGENT
- **Score**: 88-95
- **Recommandation**: Destockage rapide ou réapprovisionnement

#### E. **MISE EN AVANT** - Produits peu visibles
- **Critère**: <5 interactions sur 14 jours ET stock >10
- **Urgence**: NORMAL
- **Score**: 70-80
- **Recommandation**: Mise en avant page d'accueil ou campagne email

### 3. Prévention des Doublons
- Vérifie si un conseil similaire existe déjà (même produit + même type)
- Ne génère pas de nouveau conseil si un conseil existe avec:
  - État = 'NOUVEAU', OU
  - Créé il y a moins de 3 jours

## Utilisation

### Interface Utilisateur

Dans le module "Conseils IA", vous trouverez:

1. **Barre de statut verte** en haut:
   - Indicateur lumineux pulsant (vert = actif)
   - "Génération Automatique Active"
   - Intervalle de génération affiché
   - Bouton "⚡ Générer Maintenant" pour génération manuelle

2. **Génération manuelle**:
   - Cliquez sur "⚡ Générer Maintenant"
   - Le système analyse immédiatement les données
   - Notification de succès après génération
   - Page rafraîchie automatiquement

### API Programmatique

```java
// Démarrer la génération automatique
conseilsIaService.startAutoGeneration();

// Arrêter la génération automatique
conseilsIaService.stopAutoGeneration();

// Générer immédiatement (sans attendre le prochain cycle)
conseilsIaService.generateConseilsNow();

// Vérifier si la génération est active
boolean isRunning = conseilsIaService.isAutoGenerationRunning();

// Configurer l'intervalle (en minutes)
conseilsIaService.setAutoGenerationInterval(60); // Toutes les heures

// Récupérer l'intervalle actuel
int interval = conseilsIaService.getAutoGenerationInterval();
```

## Configuration

### Modifier l'intervalle de génération

Par défaut: 30 minutes. Pour modifier:

```java
// Dans AutoConseilGeneratorService.java
private int intervalMinutes = 30; // Changer cette valeur
```

Ou via l'API:
```java
conseilsIaService.setAutoGenerationInterval(60); // 60 minutes
```

### Modifier les seuils d'analyse

Dans `AutoConseilGeneratorService.java`:

```java
// Minimum d'interactions pour analyser un produit
private int minInteractionsThreshold = 5; // Modifier cette valeur
```

## Architecture Technique

### Classes Principales

1. **AutoConseilGeneratorService**
   - Service singleton avec scheduler
   - Gère le cycle de génération automatique
   - Contient toute la logique d'analyse

2. **ConseilsIAService**
   - Interface publique pour démarrer/arrêter la génération
   - Méthodes CRUD pour les conseils
   - Wrapper autour de AutoConseilGeneratorService

3. **HanoutiDashboard**
   - Démarre automatiquement la génération au lancement
   - Affiche le statut et les contrôles UI

### Tables Utilisées

#### Lecture:
- `interaction_utilisateur` - Données comportementales
- `produit` - Informations produits (stock, nom, etc.)

#### Écriture:
- `conseils_ia` - Insertion des nouveaux conseils

### Requêtes SQL

Toutes les analyses utilisent des requêtes optimisées avec:
- Jointures LEFT JOIN pour éviter les erreurs
- Filtres temporels (7 ou 14 derniers jours)
- Agrégations (SUM, COUNT) pour les métriques
- CAST pour compatibilité des types (VARCHAR ↔ INT)

## Logs et Monitoring

### Logs Console

Le système affiche des logs détaillés:

```
[AutoConseilGenerator] Démarré - génération toutes les 30 minutes
[AutoConseilGenerator] Début de l'analyse à 2026-05-14T10:30:00
[AutoConseilGenerator] Conseil PROMOTION créé pour: Chargeur Rapide 65W
[AutoConseilGenerator] Conseil BUNDLE créé pour: Cable USB-C
[AutoConseilGenerator] Analyse terminée avec succès
```

### Erreurs

En cas d'erreur:
```
[AutoConseilGenerator] Erreur lors de la génération: [message]
```

Le système continue de fonctionner et réessaiera au prochain cycle.

## Avantages

1. **Automatique**: Aucune intervention manuelle requise
2. **Intelligent**: Analyse comportementale réelle des utilisateurs
3. **Adaptatif**: Scores et urgences calculés dynamiquement
4. **Performant**: Requêtes optimisées, exécution en arrière-plan
5. **Fiable**: Prévention des doublons, gestion d'erreurs
6. **Flexible**: Configuration facile de l'intervalle et des seuils

## Dépannage

### Les conseils ne sont pas générés

1. Vérifier que la génération est active:
   ```java
   boolean isRunning = conseilsIaService.isAutoGenerationRunning();
   ```

2. Vérifier les logs console pour les erreurs

3. Vérifier que la table `interaction_utilisateur` contient des données:
   ```sql
   SELECT COUNT(*) FROM Interaction_Utilisateur;
   ```

4. Essayer une génération manuelle via le bouton UI

### Trop de conseils générés

- Augmenter `minInteractionsThreshold` dans AutoConseilGeneratorService
- Augmenter l'intervalle de génération
- Les conseils existants empêchent les doublons pendant 3 jours

### Pas assez de conseils générés

- Diminuer `minInteractionsThreshold`
- Vérifier que les produits ont suffisamment d'interactions
- Diminuer l'intervalle de génération

## Évolutions Futures

Possibilités d'amélioration:

1. **Machine Learning**: Utiliser un vrai modèle ML pour prédire les meilleures actions
2. **A/B Testing**: Tester différentes recommandations et mesurer l'efficacité
3. **Personnalisation**: Conseils adaptés par segment de clientèle
4. **Intégration API externe**: Claude AI, OpenAI pour descriptions plus riches
5. **Notifications**: Alertes email/SMS pour conseils urgents
6. **Dashboard Analytics**: Métriques sur l'efficacité des conseils appliqués

## Support

Pour toute question ou problème:
- Consulter les logs console
- Vérifier la configuration dans AutoConseilGeneratorService.java
- Tester manuellement via le bouton "Générer Maintenant"
