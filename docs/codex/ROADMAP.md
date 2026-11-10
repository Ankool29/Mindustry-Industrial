# Feuille de route

État vérifié le 10 novembre 2026 ; voir [DECISIONS](DECISIONS.md) pour les preuves.

| État | Étape |
| --- | --- |
| Implémenté | M6C : progression indépendante ; M6D : colonisation économique. |
| Réalisé | M7A : audit. |
| Implémenté | M7B : diagnostic économique en lecture seule. Aucun transport conservatif nouveau. |
| Fusionné | H0 : harnais documentaire, base `c207f5bbb`. |
| Autorisé, en validation | M7C : manifestes finis, tampons natifs, journal et reprise par marqueurs ; [rapport](../../MISSION_7C_VALIDATION.md). |
| Autorisé, en validation | M7C.1 : diagnostic du tampon virtuel et achèvement des arrivées finies appariées ; [rapport](../../MISSION_7C1_VALIDATION.md). |
| Architecture validée par l’utilisateur | M7D-A : [architecture de simulation économique hors écran](../../MISSION_7D_AUDIT.md), audit de référence, PR #3 non fusionnée. |
| Autorisé, en validation | M7D-B : capture économique détachée et diagnostic en lecture seule ; [rapport](../../MISSION_7D_B_VALIDATION.md). |
| Proposition, non autorisée | M7D-C à F : autorité/reprise, prototype cuivre, convoyeurs/recettes et utilités ; voir le découpage de l’audit. |

**Arrêt après M7D-B, sans fusion automatique.** L’architecture de M7D-A est validée ; cela n’autorise aucune simulation autonome dans B. M7C.1 reste le moteur de transport. Attendre les essais Android et la validation explicite avant C. Un ordre de feuille de route ne vaut pas autorisation.

## Idées exploratoires — ni engagements ni autorisations

- Centre de contrôle logistique planétaire et routage multidestination.
- Automatisation avancée, éventuellement avec la logique native.
- Équilibrage des recherches et diversification des frais de colonisation.
- Nouvelles ressources, recettes et machines.
- Transport intersectoriel de liquides et d’énergie.
- Fusée et colonisation industrielle d’Erekir.

Chaque sujet demande un périmètre, ses risques de sauvegarde/performance et une décision propre. Aucun prototype de ces idées n’est inclus dans H0. Format commun et validation : [TESTING](TESTING.md).
