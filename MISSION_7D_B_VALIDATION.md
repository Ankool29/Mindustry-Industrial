# M7D-B — capture économique en lecture seule

## Périmètre et verdict

Mission B uniquement, sur `feature/m7d-capture`, à partir de l’audit approuvé de la PR #3 et du code M7C.1. Aucun merge, aucune simulation autonome ni mission C à F. Cette branche contient le rapport A approuvé ; elle ne fusionne pas automatiquement sa PR.

Le graphe est un **relevé**, pas un nouveau propriétaire des ressources. Les mécanismes M7C.1 restent inchangés. Les moyennes de production natives et leurs limites restent présentes ; B ne les corrige pas et ne les transforme jamais en stock expédiable. Les essais physiques sur S25 restent à effectuer avant validation utilisateur.

## Fonctionnalités et architecture

- Capture du seul monde progressif chargé, après chargement, après sauvegarde sectorielle canonique et sur demande explicite. Pas de scan de mondes inactifs, pas d’update par tour économique ou image.
- DTO détachés : identité planète/secteur/équipe/génération, date/origine, modules d’objets/liquides, batteries et circuits, bâtiments, états de progression décodés, configurations, recettes de base et liaisons typées. Aucune référence à un monde/module/bâtiment natif conservée.
- Déduplication **par identité des modules**, avec un propriétaire et une liste de bâtiments membres. Les noyaux et stockages liés ne multiplient pas leur inventaire. La représentation interne des convoyeurs est un miroir contrôlé, pas un second stock.
- Chargements physiques d’unités et anciens vols natifs non encore adoptés recensés séparément. Les unités sont lues dans `Groups.unit`, filtrées par équipe ; le cache natif `TeamData.units` peut être périmé juste après chargement et n’est pas utilisé.
- Références détachées aux manifestes M7C.1, états et propriété transport/reçu. **Jamais additionnées aux stocks physiques** : un reçu de crédit n’est pas un deuxième lot. Aucun total global ne mélange les relevés de dates différentes.
- Énergie des batteries = statut × capacité du module tampon, une seule fois. Le statut de couverture d’un consommateur n’est pas une réserve. Les entrées/sorties électriques sont les dernières observations natives, pas une alimentation future garantie.
- Recettes et consommations décrites, jamais exécutées. Les consommations déclenchées/manuelles sont distinctes des consommations continues : notamment le seuil natif de **1 500 eau/lot** d’une réception n’est pas présenté comme un débit par tick.
- Empreinte SHA-256 de structure/configuration, distincte des quantités et progressions. Ce n’est pas un verrou de transaction ni une preuve d’autorité de sauvegarde.

Le cache est **en mémoire de session seulement**, au plus 100 secteurs / 100 000 nœuds. Un relevé est borné à 10 000 bâtiments / 80 000 liens ; dépassement = relevé explicitement partiel et inéligible. Éviction = disparition du diagnostic, jamais des ressources du jeu. Aucune nouvelle clé Settings, aucun fichier de graphe, aucune migration de sauvegarde. Après redémarrage, revisiter les secteurs pour les relever. La sérialisation JSON utilisée par les tests mesure une taille ; le produit ne l’écrit pas.

### Éligibilité et couverture

Le diagnostic se réfère au **prototype minimal proposé par M7D-A**, pas à une simulation disponible : cuivre mécanique sans boost, noyau simple, déchargeur filtré, rampes/réceptions natives, eau/pétrole, batterie/nœuds électriques et chemins simples par adjacence. Il distingue proximité, liaison typée reconnue et liaison non validée ; une proximité ne prouve aucun débit.

Les machines de transformation et les convoyeurs peuvent être relevés tout en restant hors du prototype minimal. Les états opaques, payloads, contenus modifiés, modules incohérents, limites de taille, équipements et configurations non supportés portent des motifs explicites ; l’inventaire est signalé incomplet lorsque des stocks cachés ne sont pas garantis. **Aucune estimation ne remplace ces données.**

Incohérence importante pour un futur prototype : `coreIncinerates` est activé dans les règles natives des colonies testées, alors que le prototype A exclut l’incinération. B conserve cette règle et affiche un blocage. Un secteur existant peut donc être « NON ÉLIGIBLE » sans aucun dysfonctionnement ; une future décision sera nécessaire, pas un changement silencieux de règle dans B.

## Interface de diagnostic

Carte de **Serpulo industrielle — progression** → Diagnostic économique → colonie → **Graphe économique — capture**.

Vue verticale : date/origine, couverture, motifs, stocks uniques, énergie, références de manifestes, liste filtrable et paginée de bâtiments, détail des modules partagés, états et liaisons. Boutons principaux 64 unités UI ; pas de défilement horizontal imposé. « Capturer » est désactivé pour un secteur inactif ; consulter son ancien relevé ne charge pas son monde. Le texte d’ancienneté est actualisé une fois/seconde ; aucun inventaire n’est relu à chaque image. Fermer l’écran libère ses widgets, le cache reste borné. Le dialogue est réutilisé, sans nouvel abonnement de redimensionnement à chaque ouverture ; son fond opaque évite la superposition du texte avec le diagnostic parent.

## Fichiers concernés

| Fichier | Changement |
| --- | --- |
| `core/src/mindustry/game/IndustrialEconomyCapture.java` | DTO, déduplication, capture, limites et éligibilité. |
| `core/src/mindustry/game/IndustrialTransports.java` | Vue de manifestes strictement consultative, sans initialisation/reprise/écriture du moteur. |
| `core/src/mindustry/io/SaveIO.java` | Deux appels de capture après les opérations natives existantes. |
| `core/src/mindustry/ui/dialogs/IndustrialGraphDialog.java` | Écran tactile de lecture seule. |
| `core/src/mindustry/ui/dialogs/IndustrialDiagnosticsDialog.java` | Accès depuis une colonie progressive. |
| `tests/src/test/java/IndustrialCaptureTests.java` | Tests et banc synthétique isolé. |
| `.github/workflows/android-cloud.yml` | Branche de validation B, nom/version APK ; signature et identité conservées. |
| `docs/codex/{ARCHITECTURE,DECISIONS,ROADMAP,TESTING}.md` | État B et arrêt avant C, références au présent rapport. |

Aucune recette, règle de campagne, recherche, frais M6D, génération, progression de secteur ou méthode de production/transfert n’est modifiée. Pas de nouvelle dépendance. Les deux campagnes classique et libre n’activent pas la capture.

## Tests et preuves

Suite locale : **461 tests réussis, 0 échec, 0 erreur, 0 ignoré** (17 classes JUnit), dont 23 nouveaux tests de capture. Les 41 tests de transport M7C.1 et les 12 tests du diagnostic M7B restent verts. Après ajout de la mesure CPU, le seul banc modifié a été réexécuté avec succès ; deux scénarios ont aussi été renforcés et réussis pour le filtre/progression du déchargeur et le seuil d’eau de réception. Les preuves CI du commit livré sont indiquées ci-dessous.

Les scénarios couvrent les modules d’objets/liquides/batteries partagés, convoyeurs et incohérences, états de machines, minerais réellement couverts, routes vs proximité, configurations opaques, empreinte, capture aux chargements/checkpoints, détachement, cache borné et dépassement de limite. Les captures répétées comparent octet à octet monde sauvegardé, Settings et journal M7C.1, ainsi que stocks, progressions et `SectorInfo` en RAM. Les modes classique/libre restent sans graphe.

La fixture historique M7B est chargée sans réécriture : **2 650 cuivre au noyau + 10 dans le tampon de réception = 2 660 physiques**, pas 2 650 au total. Un scénario M7C.1 relève 100 unités expédiées puis créditées : le reçu est affiché mais n’augmente pas le total physique à 200.

## Performance

Banc serveur : un seul monde synthétique de **201 bâtiments et 734 adjacences**, capturé sous 5/20/50/100 identités sectorielles distinctes. Aucun de ces graphes ne simule une recette. Dix captures de chauffe ; temps total mural et allocations du thread par lot ; heap global après GC avant/après, donc approximation dépendante du GC. La taille JSON UTF-8 est informative, pas un coût de sauvegarde ajouté.

Mesure ciblée locale (Java 17 serveur, pas Android), extraite des lignes `M7DB_PERF` :

| Secteurs | Temps mural total / moyen (ms) | CPU du thread total (ms) | Allocations cumulées (octets) | Heap retenu après GC (octets, approx.) | JSON UTF-8 (octets) |
| --- | --- | --- | --- | --- | --- |
| 5 | 31,246 / 6,249 | 25,464 | 2 899 632 | 1 911 208 | 319 542 |
| 20 | 51,391 / 2,570 | 41,032 | 11 607 592 | 7 657 144 | 1 278 202 |
| 50 | 173,315 / 3,466 | 123,193 | 28 995 664 | 19 264 864 | 3 195 533 |
| 100 | 270,877 / 2,709 | 178,958 | 57 856 072 | 38 315 520 | 6 391 084 |

Les allocations cumulées comprennent les temporaires, elles ne sont pas la taille retenue du cache. Le temps mural varie avec le serveur/JIT ; il ne constitue pas une borne de latence. Le CPU est mesuré par `ThreadMXBean`, sans le GC forcé de mesure mémoire ni l’encodage JSON.

Ces chiffres ne sont pas des performances S25 et ne représentent pas cent grandes usines. Le plafond de nœuds borne le cache mais une capture complète synchrone peut provoquer une pause sur une grosse carte ; le temps individuel est affiché. Écran fermé : aucun listener par image ni tick économique de B ; seuls les chargements/sauvegardes déclenchent une capture. Pas de production application fermée.

## APK et contrôles Android

À compléter avec le commit réellement construit, workflow, checksum, version et certificat vérifiés avant publication finale.

Contrôle graphique cloud : client natif `-testMobile`, 600 × 1 000, copie temporaire de données M7B/M7C.1 et script externe **exclus du dépôt/APK**. Ouverture depuis la carte, provenance/éligibilité, capture répétée sans mutation de `SectorInfo`/stocks, détail de noyau, défilement vertical seul, retour, réutilisation du dialogue et consultation du secteur 170 pendant que 171 reste chargé : réussis. Captures overview/détail/inactif et marqueurs `M7DB_UI_ACTIVE_CAPTURE_READONLY_PORTRAIT_OK` / `M7DB_UI_INACTIVE_CACHED_NO_WORLD_LOAD_OK` conservés dans les preuves locales de travail. Le fond a été rendu opaque après contrôle de lisibilité.

Cet essai ne prouve ni l’installation ni les performances réelles du Samsung. Ne jamais désinstaller/effacer les données pour tester.

## Protocole sur téléphone

1. Exporter normalement les données par le menu du jeu, par précaution ; conserver cette exportation. Installer l’APK B **par-dessus** M7C.1, sans désinstallation.
2. Ouvrir une colonie progressive connue. Carte planétaire → Diagnostic → colonie → Graphe économique. Vérifier origine/âge, quantités des noyaux et tampons, partage des modules, liquides, batteries et motifs d’inéligibilité.
3. Inspecter une machine, ses progressions, coûts de base et liaisons. Capturer plusieurs fois en pause : aucune production/expédition/dépense ne doit être déclenchée par ce bouton. La reprise du jeu peut naturellement modifier les stocks.
4. Visiter une autre colonie, puis consulter le relevé de la première sans y revenir : ancienneté visible, bouton désactivé, monde courant inchangé. Les transports restent M7C.1 : aucune autonomie de production nouvelle n’est attendue.
5. Fermer/redémarrer : relevés absents jusqu’aux nouvelles visites, constructions/stocks/recherches et journal M7C.1 préservés. Vérifier aussi l’absence du nouvel écran dans les campagnes classique et industrielle libre.
6. Rapporter lisibilité portrait, temps de capture et éventuel blocage sur grosse carte ; ne pas tenter de « rendre éligible » une ancienne usine en supprimant des bâtiments.

## Limites et suite non autorisée

Le relevé n’est pas autoritatif ni durable. Il ne résout pas l’atomicité journal/monde, n’assure aucune reprise économique agrégée, ne supprime pas les moyennes natives et ne permet pas d’expédier de nouveaux lots depuis une source inactive. La capture automatique a lieu après les opérations M7C.1 existantes ; elle les observe sans les remplacer. Les références de manifestes ne constituent pas un bilan global de campagne.

Reste à décider/tester ultérieurement : persistance versionnée et registre commun, autorité/reprise, traitement des contenus non supportés et de l’incinération, précision des liquides/énergie, modèle de consommation et découpages de capture pour très grandes usines. **Arrêt après B ; aucune de ces étapes n’est implémentée ni autorisée par cette livraison.**
