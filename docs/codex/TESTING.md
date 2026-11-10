# Tests et format des missions

## Choisir le minimum suffisant

- Documentation seule : vérifier chemins, liens, cohérence avec les sources et liste exacte du diff. Pas de Gradle, APK, nouvelle dépendance ou banc économique inutile.
- Code : commencer par les classes concernées ; élargir aux régressions pertinentes et à la suite requise. Ne répéter une vérification concluante que si un changement ou un doute le justifie.
- Sauvegardes/économie : copies temporaires isolées ; comparer stocks, compteurs, recherches et reprise. Ne jamais tester sur les données du joueur. Ne pas installer les scripts de fixtures des rapports sur le téléphone.

## Commandes et repères existants

Cloud : Java 17, wrapper Gradle du dépôt ; Arc correspondant au workflow, actuellement `7445105cd2683daf5b9ac7033ebb07265d3e814d`, idéalement dans `../Arc` (voir `settings.gradle`). Pas de chemin `/workspace` ou cache propre à une session supposé permanent.

```sh
./gradlew tests:test --tests IndustrialDiagnosticsTests --no-daemon --max-workers=2
./gradlew tests:test --no-daemon --max-workers=2
```

Le module et ses rapports sont définis dans [build.gradle](../../build.gradle) ; le choix de `../Arc` est dans [settings.gradle](../../settings.gradle). Choisir la classe plutôt que lancer les deux commandes systématiquement :

| Risque | Classes dans `tests/src/test/java/` |
| --- | --- |
| Campagnes et paix | `IndustrialCampaignTests`, `IndustrialModeTests` |
| Filtrage civil | `IndustrialContentTests` |
| Recherche et progression | `IndustrialResearchTests`, `IndustrialProgressionTests` |
| Frais, abandons, reprise | `IndustrialExpansionTests` |
| Diagnostic et comparaison actif/hors écran | `IndustrialDiagnosticsTests` |
| Manifestes finis, journal, reprise, conservation et tampons | `IndustrialTransportTests` |
| Capture détachée, modules partagés, conservation en lecture seule, éligibilité et coût synthétique | `IndustrialCaptureTests` |

Les tests Gradle sont headless ; ils ne prouvent ni installation Android ni confort tactile. Rapports JUnit : `tests/build/test-results/test/` ; HTML : `tests/build/reports/tests/test/`. Les scripts graphiques/bancs historiques sont liés depuis les [rapports existants](DECISIONS.md#preuves-existantes), pas dupliqués ici.

## Android

Suivre le [guide cloud existant](../ANDROID_CLOUD_FR.md). Workflow : Java 17, Gradle 8.14.3, AGP 8.10.1, SDK 36 au point H0 ; vérifier les fichiers avant de les actualiser. Garder `io.anuke.mindustry.industrial.m3` et la clé de signature précédente ; ne jamais régénérer une clé manquante ni proposer une désinstallation comme solution de mise à jour.

Le workflow se déclenche sur `industrial-android`, les branches de validation M7C et M7D-B, ou par lancement manuel/réutilisable. Le dispatch demande maintenant `build_ref` : utiliser le **SHA complet testé** pour une PR. Un push de la branche M7C construit `github.sha`. Vérifier le SHA dans `build-evidence/versions.txt` et `dist/BUILD-INFO.txt` ; ne jamais présenter un APK stable comme validation d’une PR. Aucun push/fusion sur stable pour provoquer un build. M7C.1 conserve l’identité/signature M7C et utilise versionCode 11 (M7C : 10). Les régressions hors écran sont dans `IndustrialTransportTests` ; voir le [rapport M7C.1](../../MISSION_7C1_VALIDATION.md).

Ne compiler un APK que si le changement le justifie ou si la mission le demande. Séparer build réussi, tests automatisés, contrôles graphiques cloud et essai réel sur S25. En cas de modification UI, vérifier portrait et tactile ; pour la compatibilité, installer par-dessus avec la même signature, sans effacement. Inclure SHA construit, lien et limites dans le rapport.

## Format bref d’une mission et de sa livraison

```text
Objectif et périmètre autorisé :
Contraintes et risques :
Fichiers concernés :
Tests ciblés (prévus puis exécutés, résultats) :
Résultats et limitations :
Validation Android requise / réalisée / non applicable :
Questions restant à trancher :
```

Un seul `MISSION_<ID>_VALIDATION.md` proportionné, plus les preuves utiles. Référencer les documents existants ; mettre à jour seulement les décisions/états réellement changés. Branche et PR pour revue, sans fusion automatique. Ne pas lancer la mission suivante.

M7D-B : `feature/m7d-capture`, versionCode 12, type `industrial-m7db`, même identifiant/clé. Le test `measureFiveTwentyFiftyHundredSyntheticGraphs` mesure des captures indépendantes d’une fixture de 201 bâtiments / 734 adjacences sous des identités sectorielles synthétiques, pas plusieurs mondes chargés. CPU/allocations serveur et heap après GC sont distincts d’une mesure Android. JSON est uniquement une mesure de taille dans le test ; aucun fichier de graphe produit par le jeu. Pour l’écran, vérifier depuis la carte progressive → Diagnostic → colonie → Graphe économique : provenance, motifs, détail des modules partagés, capture inactive désactivée, portrait sans défilement horizontal. [Résultats et limites B](../../MISSION_7D_B_VALIDATION.md).
