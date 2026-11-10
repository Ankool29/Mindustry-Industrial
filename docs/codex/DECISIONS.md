# Décisions et preuves

## Implémenté, vérifié dans le dépôt

| Étape | Résultat |
| --- | --- |
| M6C | Planète progressive distincte, recherche indépendante effective ; campagne libre conservée. Un seul espace strict, pas un gestionnaire général de profils. |
| M6D | Surcharge progressive de colonisation, compteur persistant et journal de reprise ; visites distinctes des implantations. Aucun frais logistique ajouté par cette décision. |
| M7A | Audit documentaire : modèles natifs ancien/moderne et limites de conservation identifiés. |
| M7B | Diagnostic en lecture seule limité à la progressive, données observées/estimées distinguées, ancienneté seulement en mémoire. |

M7B rapporte 397 tests réussis au commit `1260a5c9a` ; ce nombre est une preuve historique, pas un résultat à annoncer sans exécution lors d’une nouvelle modification. La validation physique du nouvel APK sur S25 reste indiquée à confirmer dans ce rapport.

## Validé comme orientation, pas encore implémenté

Parcours **A diagnostic → B transports agrégés conservatifs → C production agrégée contrainte**. A est livré avec M7B ; B/C nécessitent leurs missions et validations. Une moyenne ne prouve ni stock réservé, ni consommation réelle, ni livraison. Aucun second moteur parallèle ne doit doubler les ressources natives.

Le choix de conserver les voisins M6C/M6D a été explicite : **six autour de 170**, pas cinq. H0 n’en modifie aucun.

## Particularités documentaires

Les rapports ci-dessous sont sur `master`, contrairement au code jouable. Garder des liens vers eux plutôt que les copier dans cette branche. Les rapports décrivent leur étape : « pas de surcharge » dans M6C est historique, M6D l’ajoute ensuite.

[CONTRIBUTING.md](../../CONTRIBUTING.md) et le [modèle de PR](../../.github/pull_request_template.md) sont hérités d’Anuken : ils incluent une politique contre les PR/textes générés par IA et des consignes amont. H0 est explicitement demandé à Codex dans le fork Ankool29 ; sa PR reste dans ce fork et indique cette provenance. Ne pas affirmer respecter une case incompatible ni envoyer de contribution amont sans demande explicite. Les licences et conventions de code restent à respecter.

## Preuves existantes

- [M6C — validation](https://github.com/Ankool29/Mindustry-Industrial/blob/master/MISSION_6C_VALIDATION.md).
- [M6D — validation](https://github.com/Ankool29/Mindustry-Industrial/blob/master/MISSION_6D_VALIDATION.md).
- [M7A — audit](https://github.com/Ankool29/Mindustry-Industrial/blob/master/MISSION_7A_AUDIT.md).
- [M7B — validation](https://github.com/Ankool29/Mindustry-Industrial/blob/master/MISSION_7B_VALIDATION.md).

Pour les prochaines étapes : [ROADMAP](ROADMAP.md). Ne modifier une décision qu’avec sa justification et l’autorisation applicable, sans réécrire l’historique des preuves.

## M7C — autorisation et choix en validation

La mission M7C est explicitement autorisée après fusion de H0. Réutiliser les rampes/réceptions, une destination par secteur, lots physiques finis et journal avec marqueurs de sauvegarde. Attendre un point de sauvegarde pour rendre un débit livrable ; hors écran, seulement des tampons connus et bornés, sans extrapoler des cargaisons. En cas d’ambiguïté, blocage signalé et conservation des fichiers plutôt que crédit supposé. Pas de garantie ACID entre fichiers, ni de correction des recettes estimées M7D. Voir [rapport M7C](../../MISSION_7C_VALIDATION.md) pour les résultats réellement exécutés. PR sans fusion, essais S25 requis ; M7D non autorisée.

## M7C.1 — correctif autorisé, en validation

Une réservation hors écran n’est pas un crédit physique du noyau : le diagnostic les distingue. Corriger l’achèvement des arrivées finies dont l’appariement et l’eau déjà consommée sont attestés par une sauvegarde ; une ancienne liaison inconnue attend une visite, sans stock supposé. Pas de convoyeurs/recettes simulés, ni M7D. Preuves et limites : [M7C.1](../../MISSION_7C1_VALIDATION.md). Arrêt avant fusion, essais S25 requis.
