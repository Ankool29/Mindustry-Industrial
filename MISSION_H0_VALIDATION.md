# H0 — validation du harnais de développement

## Périmètre et livraison

Documentation uniquement, préparée avec Codex à la demande du propriétaire du fork. Branche `docs/h0-development-harness`, PR vers **`industrial-android`**, base vérifiée `1260a5c9a2a6758613ec2f76bfe26e963148b7e9` (M7B). Revue utilisateur requise ; aucune fusion automatique.

| Fichier créé | Rôle |
| --- | --- |
| [AGENTS.md](AGENTS.md) | Point d’entrée court, règles permanentes et lecture à la demande |
| [PROJECT_VISION.md](docs/codex/PROJECT_VISION.md) | Vision industrielle pacifique, Android et limites |
| [ARCHITECTURE.md](docs/codex/ARCHITECTURE.md) | Carte des sources et espaces de sauvegarde |
| [DECISIONS.md](docs/codex/DECISIONS.md) | Implémenté / validé / historique, liens vers les rapports |
| [ROADMAP.md](docs/codex/ROADMAP.md) | État vérifié, M7C en attente et idées non autorisées |
| [TESTING.md](docs/codex/TESTING.md) | Tests ciblés existants, Android et format de mission |
| Ce rapport | Résultats et limites de H0 |

Aucun document antérieur, workflow, script de test, recette ou fichier exécutable n’est modifié. Pas de nouveau mécanisme de CI, dépendance, modèle de sauvegarde ou scénario économique. Les documents du harnais se renvoient entre eux et pointent vers les preuves existantes.

## Instructions permanentes inscrites

Industrie inspirée de Factorio sans combat obligatoire ; Android prioritaire ; campagnes originales et sauvegardes intactes ; séparation libre/progressive ; un monde détaillé et ailleurs agrégation performante ; aucune création artificielle par une nouvelle économie ; réutilisation du natif ; changements ciblés et tests isolés ; pas de refonte non demandée ; aucune mission suivante sans validation explicite.

La conservation des ressources est un invariant cible : le natif actuel ne la garantit pas, comme le montre M7A. H0 ne le corrige pas. M6C/M6D/M7A/M7B sont distingués des transports conservatifs prévus pour M7C et des idées futures. Le format commun comporte périmètre, contraintes/risques, fichiers, tests, résultats/limites, validation Android et questions ouvertes ; un seul rapport proportionné par mission.

## Audit et particularités découvertes

- Aucun `AGENTS.md` ni dossier `docs/codex` préalable dans les branches auditées ; les consignes et modèles amont restent en place.
- **Branches séparées :** code M7B sur `industrial-android` ; rapports et README de suivi sur `master` (`9ad5dbece30be90881a35781edd37928f7cecdef` à l’audit). Le harnais vise la branche du code ; les rapports de `master` sont liés, pas recopiés. Aucune synchronisation générale des branches ni réécriture de README.
- **Contribution amont :** `CONTRIBUTING.md` et le modèle de PR héritent de restrictions sur les contributions générées par IA. H0 est explicitement autorisé dans le fork ; sa provenance est déclarée et aucune PR n’est envoyée à Anuken. Aucune fausse attestation de conformité à cette case amont.
- **APK des branches de mission :** le workflow existant n’a pas de déclencheur PR ; les lancements manuels ciblent la branche stable industrielle. Cette limite est documentée, sans modifier la CI dans H0. Un futur changement de code devra vérifier le SHA effectivement construit.
- **Historique :** M6C sans surcharge et M6D avec surcharge décrivent deux étapes successives, pas deux règles actuelles concurrentes. Le voisinage confirmé est de six secteurs autour de 170. Les 397 tests et contrôles cloud M7B sont des résultats historiques ; l’essai physique du nouvel APK reste à confirmer dans son rapport.

## Vérifications réalisées

- Têtes distantes confrontées aux deux checkouts ; point d’application des instructions choisi sur la branche industrielle.
- 46 liens locaux/ancres vérifiés ; 4 rapports externes vérifiés dans l’arbre du commit `master` correspondant à la tête distante auditée.
- Classes et modules référencés présents ; commandes ciblées cohérentes avec `tests:test`, JUnit et `build.gradle`. Versions Java/Gradle/AGP/SDK et signature confrontées au workflow et au guide existants.
- `git diff --check` ; liste autorisée de sept fichiers Markdown ; comparaison des arbres avec la base : aucune modification de gameplay, simulation, sauvegarde, tests ou workflow.
- Pas de test du jeu, compilation Gradle/APK ou installation de dépendance : sans pertinence pour ce diff documentaire. Les vérifications H0 ne requalifient pas les performances ou la compatibilité Android de M7B.

## Légèreté et limites

Le point d’entrée contient 287 mots (2184 octets). Les six fichiers de consignes totalisent 15 328 octets, sous 20 Kio ; ils sont lus selon la mission, pas intégralement à chaque tour. Aucune infrastructure à maintenir en tâche de fond, aucun historique illimité, aucune nouvelle convention de rapport concurrente.

L’économie de quota n’est pas chiffrée : elle dépend des missions et des lectures effectives. Le gain visé vient du routage documentaire, des recherches ciblées et de l’absence de répétitions/builds inutiles. Mettre à jour un état court lors d’une vraie décision plutôt que recopier l’historique.

**Arrêt après H0.** Le harnais est proposé en PR, pas encore fusionné ; M7C et les orientations exploratoires ne sont pas commencées. L’application automatique d’AGENTS aux travaux futurs suppose son adoption dans leur branche de base.
