# Mindustry Industrial — consignes Codex

Ce guide concerne le fork `Ankool29/Mindustry-Industrial`, pas une contribution à Anuken.

## Démarrage économe

1. Lire [ROADMAP](docs/codex/ROADMAP.md), puis seulement les documents utiles ci-dessous.
2. Vérifier branche, état Git et périmètre demandé ; chercher avec `rg` avant de lire des fichiers entiers.
3. Faire un changement ciblé, vérifier les risques concernés et livrer un rapport bref. Ne pas relire tous les rapports historiques ni relancer des vérifications déjà concluantes sans changement pertinent.

## Règles permanentes

- Expérience industrielle inspirée de Factorio, pacifique ; Android prioritaire.
- Préserver campagnes classiques, contenus natifs et sauvegardes ; ne jamais effacer les données du joueur pour tester.
- Maintenir les campagnes industrielle libre et progressive réellement distinctes : secteurs, stocks, recherches et compteurs.
- Un seul monde sectoriel intégralement simulé ; ailleurs, modèle agrégé performant. Ne pas ajouter de production application fermée sans décision explicite.
- Aucune création artificielle de ressources par une nouvelle logistique/simulation. Le natif actuel a des limites connues : ne pas les déclarer corrigées ni les corriger hors mission.
- Privilégier bâtiments/mécanismes natifs, compatibilité des sauvegardes et tests isolés reproductibles.
- Ni refonte non demandée, ni mission suivante sans validation explicite. H0 n’autorise pas M7C.

## Lecture à la demande

- [Vision](docs/codex/PROJECT_VISION.md) : objectif et limites.
- [Architecture](docs/codex/ARCHITECTURE.md) : où intervenir.
- [Décisions](docs/codex/DECISIONS.md) : statuts et rapports existants.
- [Tests et format de mission](docs/codex/TESTING.md) : contrôles proportionnés, Android et livraison.

Documentation seule : vérifier liens et diff ; pas de compilation Gradle/APK. Code : choisir les tests ciblés, puis les contrôles requis. Créer une branche/PR vers la base vérifiée ; ne pas fusionner ni modifier une branche protégée sans autorisation. Respecter licences et conventions applicables ; voir les particularités amont dans DECISIONS.
