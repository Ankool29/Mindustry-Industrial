# Architecture — repères utiles

État vérifié pour H0 : code M7B sur `industrial-android`, commit `1260a5c9a`. `master` publie les rapports et le lanceur Android manuel ; ce n’est pas la base du code industriel jouable. Vérifier ces références au début d’une mission.

| Domaine | Points d’entrée dans ce dépôt |
| --- | --- |
| Planètes et frontières pacifiques | [Planets](../../core/src/mindustry/content/Planets.java), [IndustrialPlanet](../../core/src/mindustry/type/IndustrialPlanet.java), [IndustrialCampaign](../../core/src/mindustry/game/IndustrialCampaign.java) |
| Recherche indépendante | [IndustrialResearch](../../core/src/mindustry/game/IndustrialResearch.java), [UnlockableContent](../../core/src/mindustry/ctype/UnlockableContent.java) ; nœuds détachés, pas de remplacement des pointeurs classiques |
| Colonisation M6D | [IndustrialExpansion](../../core/src/mindustry/game/IndustrialExpansion.java), [Control](../../core/src/mindustry/core/Control.java), [LaunchLoadoutDialog](../../core/src/mindustry/ui/dialogs/LaunchLoadoutDialog.java) |
| Économie native | [Universe](../../core/src/mindustry/game/Universe.java), [SectorInfo](../../core/src/mindustry/game/SectorInfo.java), [Sector](../../core/src/mindustry/type/Sector.java) |
| Transports natifs | [LaunchPad](../../core/src/mindustry/world/blocks/campaign/LaunchPad.java), [LandingPad](../../core/src/mindustry/world/blocks/campaign/LandingPad.java) |
| Diagnostic M7B | [IndustrialDiagnostics](../../core/src/mindustry/game/IndustrialDiagnostics.java), [dialogue](../../core/src/mindustry/ui/dialogs/IndustrialDiagnosticsDialog.java), [PlanetDialog](../../core/src/mindustry/ui/dialogs/PlanetDialog.java) |
| Persistance | [Saves](../../core/src/mindustry/game/Saves.java), [SaveVersion](../../core/src/mindustry/io/SaveVersion.java) |
| Tests et Android | [TESTING](TESTING.md), [workflow](../../.github/workflows/android-cloud.yml) |

## Espaces de données

- Classique : `serpulo` ; libre : `industrial-serpulo` ; progressive : `industrial-serpulo-progressive`.
- Mondes : `sector-<planète>-<id>.msav` ; informations : `<planète>-s-<id>-info` dans les paramètres.
- Recherches industrielles : `industrial-research-v1-<planète>` ; expansion : `industrial-expansion-v1-<planète>`, utilisée par M6D dans la progressive.
- Un numéro de secteur seul n’identifie pas une colonie. Ne pas partager ces espaces entre profils ni restaurer un fichier isolé en supposant un profil complet.

## Limites à préserver jusqu’à mission validée

`Universe` utilise des moyennes hors écran, pas les recettes machine par machine ; temps de campagne joué, pas temps civil application fermée. Les groupes planétaires ne font pas avancer automatiquement les autres campagnes. La destination d’export est sectorielle. M7B lit les structures et date les échantillons en mémoire ; il ne livre ni ne produit.

Le journal M6D n’est pas une garantie ACID couvrant paramètres et mondes. Pour le détail, lire seulement le document nécessaire : [séparation M6C](../MISSION_6C_ARCHITECTURE.md), [transactions M6D](../MISSION_6D_TRANSACTIONS.md), ou les [rapports référencés](DECISIONS.md#preuves-existantes).

## M7C — transport progressif

[IndustrialTransports](../../core/src/mindustry/game/IndustrialTransports.java) branche uniquement les rampes/réceptions de la planète progressive et neutralise leur ancien crédit statistique. Journal fini `saves/industrial-transports-v1.bin`, marqueurs débit/crédit dans `Rules.tags`, points de sauvegarde `SaveIO` ; état agrégé de tampons connus, sans second monde chargé. Ne pas restaurer séparément un secteur et son journal. Les moyennes natives de **production** restent inchangées (M7D). Atomicité, migration et limites : [validation M7C](../../MISSION_7C_VALIDATION.md).
