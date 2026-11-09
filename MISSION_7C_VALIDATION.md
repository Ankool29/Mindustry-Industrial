# M7C — transports intersectoriels conservatifs

Statut au 9 novembre 2026 : **PR prête pour tests Android mais pas encore pour fusion**, branche `feature/m7c-conservative-transports`, base H0 fusionnée `c207f5bbb`. PR #2 vers `industrial-android`, conservée en brouillon et non fusionnée. M7D non commencée. Cette finalisation modifie uniquement le présent rapport ; aucun test déjà réussi ni build APK n’a été relancé.

## Périmètre et architecture

Uniquement `industrial-serpulo-progressive`. Contenus, recherches, frais M6D, voisinage et simulation native de production inchangés. Les [audits M7A/M7B](docs/codex/DECISIONS.md#preuves-existantes) restent les références historiques.

Chemins contrôlés avant modification : `LaunchPadBuild.updateTile` retire le tampon physique ; `LaunchPayloadComp.remove` alimentait les statistiques (et le noyau en ancien mode) ; `LandingPadBuild.updateTimers/updateTile` matérialisait des lots à partir de moyennes ; `Universe.runTurn` créditait les exports anciens hors écran. Ces chemins natifs d’expédition/réception sont contournés **seulement dans la progressive**. Les stocks de noyau `Sector.items/info.items` sont distincts des tampons réellement chargés. Aucun export moyen ne finance un manifeste.

Le journal `saves/industrial-transports-v1.bin`, accompagné d’une identité immuable `saves/industrial-transports-v1.identity` qui permet de signaler sa disparition, contient des manifestes finis avec identifiant UUID + séquence, destination figée, matières/quantités et états `prepared → transit → arriving → credit`, ou `transit → overlay` hors écran. Les phases et quantités sont consultables dans le diagnostic. Les compteurs confirmés et points de sauvegarde remplacent l’historique soldé ; plafond de 512 lots non soldés, puis les rampes attendent sans perdre leur stock.

### Monde actif et sauvegardes

1. Vérifier tampon plein et destination native valide ; journaliser `prepared` et synchroniser le fichier avant de vider le tampon en RAM. Charge, consommation, graphismes et interaction de destination restent natifs.
2. La sauvegarde sectorielle canonique inclut un marqueur de débit dans `Rules.tags`. Après écriture/synchronisation réussie, le manifeste devient `transit`. **Le lot attend donc l’autosauvegarde ou une sauvegarde/changement de secteur** ; pas de compression de monde par lancement.
3. Une plateforme configurée, vide et alimentée réserve un manifeste correspondant ; arrivée et eau restent natives. Le journal précède le crédit RAM et le marqueur de réception. La sauvegarde confirme une réception unique. Un identifiant de génération distingue une recolonisation native explicite d’une restauration d’un ancien monde ; il ne modifie ni la génération procédurale ni les frais M6D.
4. Reprise : lire les marqueurs dans les fichiers canoniques, vérifier leur flux comprimé complet ; annuler un débit non sauvegardé sans remboursement (le vieux monde détient encore son tampon), reprendre un crédit non sauvegardé contre ce même vieux monde, solder un crédit prouvé. Jamais de relecture « ajoute encore 100 ».
5. Une sauvegarde antérieure aux marqueurs confirmés ou un journal interrompu/corrompu suspend les transports et bloque le chargement ambigu sans supprimer le secteur. L’exception est distinguée de `SaveException` pour éviter le chemin natif qui supprime une carte corrompue.

### Hors écran

Un seul monde complet. Aux tours économiques de la planète, les manifestes déjà engagés peuvent occuper un **tampon virtuel fini**, correspondant à une réception vide et à son eau connue au dernier point de sauvegarde. Le noyau n’est pas crédité. Le journal possède le lot jusqu’à application de ce tampon au prochain chargement, avec vérification préalable de la plateforme/configuration/eau ; une nouvelle sauvegarde confirme sa réception.

Pas de remplissage de rampe, charge de carburant, vidage de réception ou recette simulée hors écran. Un tampon occupé bloque les lots suivants. Un secteur sans réception connue attend une première visite/sauvegarde. Aucun rattrapage d’heure civile. C’est volontairement borné et conservateur ; une chaîne continue à travers des convoyeurs inactifs relève de M7D.

### Migration et atomicité

Les stocks et bâtiments M7B restent présents. Les vrais `LaunchPayload.stacks` existants sont adoptés une fois et attendent la preuve de sauvegarde du retrait ; une destination invalide les maintient en vol. Les anciennes animations statistiques d’arrivée n’ont aucune preuve de marchandise source : elles restent suspendues, signalées, avec bouton de confirmation pour écarter uniquement l’animation, sans crédit/remboursement ni suppression du bâtiment.

Il n’existe **pas de transaction atomique entre journal et mondes**. Le remplacement du journal utilise fichier temporaire, longueur/CRC, `FileDescriptor.sync` puis renommage dans le même répertoire. Les marqueurs des mondes permettent la réconciliation des interruptions applicatives. Pas de retour silencieux à un vieux journal. Le fsync du répertoire n’est pas garanti de façon portable sur Android API 21 : panne de stockage/coupure électrique et restauration partielle restent des risques ; garder un **export complet des données**, journal et identité compris, et demander une réconciliation si le système bloque. Ne jamais effacer ces fichiers pour débloquer une partie.

La production native hors écran reste fondée sur des moyennes, sans preuve de consommation des intrants. Elle peut encore créer des stocks estimés : ce défaut M7D est **distinct** des manifestes finis M7C. Les tests de conservation des transferts isolent cette production ; le test M7B de graphite continue d’exposer sa limite.

## Fichiers concernés

- `IndustrialTransports.java` : journal, états, reprise et tampons agrégés.
- `LaunchPad.java`, `LandingPad.java` : branche progressive, manifeste et reprise des animations ; lecture des anciennes révisions conservée.
- `SaveIO.java`, `Saves.java`, `Control.java` : points de sauvegarde et blocage sans suppression sur ambiguïté.
- `Universe.java` : transports agrégés et neutralisation de l’ancien crédit statistique dans la progressive.
- `IndustrialDiagnostics.java`, `IndustrialDiagnosticsDialog.java`, bundles : mouvements réels séparés des débits estimés et stock expédiable.
- Tests de transport, adaptation de la référence de réception M7B, fixture synthétique M7B authentique.
- Workflow Android : branche M7C autorisée, dispatch avec référence explicite, versionCode 10, identité et clé inchangées.
- Harnais : état M7C, choix de transport et procédure de validation des SHA.

## Tests et contrôles exécutés

Les tests utilisent des fichiers et paramètres temporaires isolés. Aucun fichier du téléphone n’est disponible ou modifié.

Résultat de référence : **428 tests réussis, 0 échec, 0 erreur, 0 ignoré**, dans **16 suites JUnit**, au SHA `05fcd7ab89dc7dda7dd6e0c07c8b220a27fd4ed4`. Comptage effectué en lisant tous les `test-results/test/TEST-*.xml` de l’[artefact JUnit du workflow final](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/42692826538/artifacts/13706764763), et non en additionnant les exécutions locales.

| Groupe | Réussis | Échecs | Erreurs | Ignorés |
| --- | ---: | ---: | ---: | ---: |
| `IndustrialTransportTests` | 31 | 0 | 0 | 0 |
| `IndustrialDiagnosticsTests` | 12 | 0 | 0 | 0 |
| Autres suites industrielles : campagne, contenu, mode, recherche, progression, expansion | 50 | 0 | 0 | 0 |
| Autres tests natifs, logique, énergie, données et mod Allure | 335 | 0 | 0 | 0 |
| **Total** | **428** | **0** | **0** | **0** |

Le [job final](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/42692826538/job/129416884725) est terminé avec succès, compilation et vérifications APK incluses. Les journaux `tests.log`, `build.log`, `apk-signature.txt`, `apk-alignment.txt` et `versions.txt` sont dans l’[artefact de preuves](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/42692826538/artifacts/13708775097). L’archive JUnit téléchargée a aussi été contrôlée : SHA-256 `ef4869242b8d2385128d42e46ddf028e4d89bd7936e208c2d5ef78f3026577d5`.

Les 43 tests ciblés locaux antérieurs étaient également réussis ; ils ne s’ajoutent pas aux 428. Le premier échec réseau local du test amont Allure a été résolu avant la CI finale, sans modification du test. Les traces d’exceptions attendues par les tests de quarantaine ne constituent pas des échecs JUnit.

Détail vérifié dans les [tests de transport](tests/src/test/java/IndustrialTransportTests.java) et les [tests de diagnostic](tests/src/test/java/IndustrialDiagnosticsTests.java) :

- Débits/crédits uniques : source vide/insuffisante, destinations invalides, plusieurs rampes/identifiants, saturation et absence d’eau/réception, changement de secteur et animation sauvegardée.
- Reprises simulées par réouverture de paramètres/journal et des mondes : avant sauvegarde du débit, après débit, avant crédit, après crédit RAM non sauvegardé, après écriture du monde mais avant confirmation du journal ; relectures répétées. Références : `uncheckpointedDebitRestoresBufferWithoutAnotherShipment`, `restartBeforeCreditRetainsOneManifest`, `restartAfterUnsavedCreditReplaysOnlyAgainstUncreditedWorld`, `savedAnimationSurvivesSectorChange` et les deux tests `crashAfterWorld…WriteBeforeJournalCommitRecoversOnce`. Ce sont des interruptions applicatives reproduites, pas des arrêts forcés du processus Android ni des coupures électriques. Le point exact entre journalisation initiale et retrait RAM n’a pas son propre injecteur de panne ; la reprise depuis le dernier monde non débité est couverte par le test du débit non sauvegardé.
- Journal absent, identité restante, écriture temporaire interrompue, CRC corrompu, ancien point de sauvegarde, génération antérieure ou tampon incompatible : blocage signalé, fichiers conservés. Le test `SaveSlot` vérifie que l’exception économique n’est pas convertie en corruption autorisant la suppression native.
- Conservation : deux rampes engagent **200 objets** ; un seul tampon plein retient l’excédent ; après un transfert local explicitement simulé du tampon vers le noyau dans le test, puis second lot, exactement **200** dans les stocks de destination et **0** manifeste non soldé, compteurs envoyés/reçus à 200. Le journal d’un crédit déjà appliqué est un reçu en attente de confirmation, **pas un deuxième stock**. Références : `conservationAcrossTwoRampsOneFullReceiverAndRevisit`, `checkpointDebitCreditAndRepeatedRecoveryExactlyOnce` et `fullDestinationWaitsWithoutOverflow`. Ce test ne remplace pas un essai de convoyeurs sur téléphone.
- Référence M7B : export moyen de 2 objets/s et stock source nul ; classique/libre reçoivent encore leur lot natif de 100, progressive reçoit **0** sans manifeste. Ancienne passe d’export également neutralisée si le drapeau legacy est présent. Chemin natif de lancement testé avec animation sans second crédit. Références : `m7BPhantomReceptionRegression`, `classicAndFreeRetainNativeStatisticalReception`, `noAdditionalNativeLegacyExportPass`, `nativeLaunchPathEmitsVisualButNoSecondNativeCredit`. Ces essais ne prouvent pas la conservation globale de la production estimée native.
- Fixture M7B authentique issue d’un monde synthétique de validation, conservée dans `tests/src/test/resources/industrial/` : chargement sans conversion ni réécriture du fichier, stock 2650 préservé (`genuineM7BFixtureLoadsWithoutConversionOrStockMutation`). Ce fichier sectoriel accompagné d’un état de test ne valide pas tous les profils réels du joueur. Recherches/frais/sauvegardes et campagnes sont couverts par les tests industriels existants.
- Production native volontairement non corrigée : le protocole graphite M7B affiche encore **5 réels → 35 estimés hors écran → 35 au retour** malgré l’intrant épuisé. Ce résultat expose M7D ; il n’est pas annoncé comme conservation du transport.
- Interface graphique cloud : client reconstruit au SHA final, mode mobile **600 × 1000**, défilement exclusivement vertical, ouverture depuis la carte, manifeste/matière/destination lisibles, dix actualisations sans changement du journal, chargement de la destination M7B. Captures réinspectées à la finalisation ; ces preuves graphiques restent locales à l’environnement Codex et ne sont pas incluses dans les artefacts CI. Trace `M7C_UI_PORTRAIT_NATIVE_MAP_MANIFEST_READONLY_AND_M7B_TARGET_LOAD_OK`. Aucun essai physique sur S25 par Codex.
- `git diff --check`, liens documentaires et `actionlint` ; aucune recette, recherche, règle de frontière ou formule de production modifiée. Pas de dépendance ajoutée.

## Performance mesurée dans le cloud

Valeurs issues de `system-out` dans `TEST-IndustrialTransportTests.xml` du **workflow final**, et identiques aux lignes `M7C_PERF` de `tests.log` (mesure le 9 novembre 2026 à 20:28 UTC). Elles remplacent les mesures locales intermédiaires. Java 17 headless sur runner GitHub Ubuntu 22.04, secteurs synthétiques avec une réception connue chacun ; 50 appels de chauffe puis moyenne de 1000 tours **du service de transport** sans lot à déplacer. Ce n’est ni une mesure FPS, ni le coût de toute la simulation native, ni une mesure Android. Les petites durées varient avec le JIT et l’ordonnancement.

| Colonies | Temps moyen / appel | Allocations / appel | Journal sur disque |
| --- | ---: | ---: | ---: |
| 5 | 4,95 µs | 120 octets | 990 octets |
| 20 | 4,45 µs | 458 octets | 3 421 octets |
| 50 | 2,58 µs | 1 176 octets | 8 641 octets |
| 100 | 1,97 µs | 2 376 octets | 17 341 octets |

Allocations mesurées par `ThreadMXBean`, **pas mémoire résidente** ; taille sérialisée distincte du tas Java, plus identité de 36 octets. Le tour natif est de 120 secondes de jeu ; aucune écriture pour un tour inactif sans changement. Le diagnostic fermé ne crée pas de mise à jour par image ni d’historique.

Banc de file pleine : **512 admissions durables**, moyenne **1,12 ms/admission** incluant JSON, écriture et synchronisation ; journal **68 080 octets**. Le 513e lot reste intégralement dans la rampe. Les manifestes soldés sont résumés par compteurs, pas conservés indéfiniment. Les écritures du petit journal sont synchrones sur le fil du jeu ; les pics avec beaucoup de plateformes et les latences réelles du stockage S25 restent à mesurer. Aucune compression de monde par colis, aucun scan fréquent de mondes complets, aucun nouveau thread de simulation.

## APK et traçabilité

- Code final : `05fcd7ab89dc7dda7dd6e0c07c8b220a27fd4ed4`.
- Workflow : [Android cloud APK](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/42692826538), déclenché par push sur cette branche ; l’ancienne tentative `42691973234` a été remplacée après la correction de diagnostic et ne constitue pas la livraison.
- [Téléchargement direct de l’artefact APK](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/42692826538/artifacts/13707702858) : `Mindustry-Industrial-Android`, contient **`Mindustry-Industrial-v160.7-m7c.apk`** ; connexion GitHub éventuellement requise. Extraire le ZIP avant installation. Rétention annoncée jusqu’au **9 décembre 2026** ; le lien n’est pas une publication permanente.
- SHA-256 de l’**APK extrait et recalculé** : `d3d80beace4f1c39bff79c31835758ac0d4f56b39f6994b9d10a39e3ff8c395d`, identique à `SHA256SUMS.txt`.
- SHA-256 du **ZIP d’artefact**, distinct de celui de l’APK : `a3ed0d89ce4592e03247cc098e4030b3664549652054098611b407c8b28c0108`, recalculé et conforme au digest GitHub.
- `BUILD-INFO.txt` extrait et `versions.txt` identifient tous deux le SHA compilé ci-dessus, également retrouvé dans l’en-tête Git de l’archive de sources correspondantes et Arc `7445105cd2683daf5b9ac7033ebb07265d3e814d`. `aapt dump badging` sur le binaire téléchargé confirme **`8-industrial-m7c-160.7`**, **versionCode 10**, **`io.anuke.mindustry.industrial.m3`**, minSdk **21**, targetSdk **36**, présence ARM64 dans l’archive APK.
- `apksigner verify --verbose --print-certs` réexécuté sur les APK M7C **et M7B** téléchargés : signatures valides, même certificat SHA-256 **`61254fd2280bf34aa64bd06405cf0e2bc9b2d8fe7df4cc615c39983bcdfd8086`**. M7B a le même identifiant et un versionCode **9**. M7C vérifie les schémas v1/v2 ; `zipalign -c -P 16 4` réussit également. Aucun remplacement de clé. Cela établit les conditions de signature/version d’une mise à jour sans désinstallation, pas son installation effective sur le téléphone.
- [PR de validation #2](https://github.com/Ankool29/Mindustry-Industrial/pull/2), non fusionnée. Les mises à jour finales du rapport seules ne recompilent pas l’APK ; le SHA du code livré reste explicitement indiqué ci-dessus.

## Android — protocole utilisateur

1. Avant essai : Paramètres → Données → exporter **toutes les données**. Conserver ce ZIP hors du répertoire de l’application. Ne pas désinstaller, effacer les données ou supprimer le journal.
2. Installer l’APK M7C par-dessus M7B. Vérifier la version puis ouvrir la campagne **industrielle progressive** existante.
3. Choisir deux colonies voisines déjà accessibles, équiper la source d’une rampe avancée chargée avec 100 cuivre (électricité/pétrole natifs), définir sa destination ; équiper la destination d’une réception configurée cuivre et d’eau.
4. Consulter carte → diagnostic : vérifier le lot préparé puis expédié confirmé après sauvegarde/changement de secteur. Vérifier l’identifiant/destination et les moyennes distinctes.
5. Visiter la destination : au plus 100 objets reçus par manifeste. Tester sortie pleine puis libérée ; manque d’eau puis réapprovisionnement ; plusieurs rampes. Ne pas utiliser les variations de noyau estimées comme preuve de transport.
6. Passer par un troisième secteur, attendre des tours puis revenir : tampon hors écran borné, lots excédentaires en attente. Fermer normalement/reprendre aux différents états ; pas de nouvelle production application fermée.
7. Si une vieille arrivée M7B est suspendue, vérifier le message avant de confirmer l’écartement de son animation. En cas d’ambiguïté de journal, arrêter les essais et conserver/exporter les fichiers ; ne rien réinitialiser.
8. Vérifier séparément classique et industrielle libre, recherches/coûts M6D et stocks existants. Signaler les résultats sur S25 ; ils ne sont pas remplacés par les essais cloud.

## Risques résiduels et vérifications manquantes

- **Atomicité** : journal, identité et mondes sont plusieurs fichiers. Les 31 tests de transport couvrent les reprises applicatives citées ; ils ne démontrent pas une transaction ACID ni la durabilité après coupure électrique. Aucun fsync portable du répertoire, aucune campagne exhaustive d’injection d’erreurs de disque/renommage. Des fichiers peuvent rester bloqués en quarantaine ; pas d’outil automatique de réparation validé.
- **Restauration partielle** : le CRC et l’UUID prouvent une intégrité/identité, pas la fraîcheur absolue. Une ancienne copie valide du journal de la même époque remise seule n’est pas couverte par les tests de journal corrompu ou de monde ancien. La lecture du code ne permet pas de promettre la détection de toutes ces restaurations, ni d’exclure un rejeu de cargaison ou une perte si les fichiers sont désynchronisés. Restaurer uniquement un export complet cohérent ; ne pas effacer/recréer le journal pour lever un blocage. Ce risque de restauration partielle n’a pas été reproduit dans cette finalisation ; il doit être examiné avant une généralisation des garanties de conservation.
- **Portée des résultats** : aucune duplication/perte observée dans les scénarios automatisés cités ; aucune garantie générale pour toutes les interruptions de stockage, restaurations externes, mods ou versions futures. Un marqueur/stock incohérent peut bloquer le chargement d’un secteur plutôt que reconstituer une quantité supposée.
- **Hors écran** : seulement lots déjà engagés et tampons/eau connus ; pas de remplissage/vidage par convoyeurs ni de chaînes continues. Les lots attendent si la réception est inconnue, occupée, sans eau ou sans base. La file de 512 lots produit une attente explicite, pas un stockage illimité. Pas de progression application fermée.
- **Production native** : toujours estimée, peut créer des stocks sans intrants. Le cas graphite 5 → 35 reste reproductible ; M7C ne doit pas être annoncé comme une économie mondiale conservatrice. Traitement ultérieur relevant de M7D, non commencé ici.
- **Android** : APK signé/aligné vérifié, mais **installation par-dessus M7B, restauration d’un export complet, arrêts forcés Android, latences de stockage et confort tactile sur S25 non vérifiés physiquement**. Le contrôle 600 × 1000 utilise le client desktop en mode mobile, pas cet APK exécuté sur Android. L’absence de plantage sur Android n’est donc pas établie ; les écritures synchrones peuvent provoquer des à-coups avec beaucoup de plateformes.
- **Migration M7B** : inventaires conservés dans la fixture et anciennes arrivées statistiques suspendues par conception. Elles ne sont pas remboursées ; l’écartement explicite de l’animation n’est pas une livraison. Toutes les anciennes situations de campagne réelles ne sont pas représentées par cette seule fixture.

Pas de routage multidestination, centre logistique, consommation agrégée des recettes ou nouveaux contenus dans M7C.

## Verdict et arrêt

**PR prête pour tests Android mais pas encore pour fusion.** Le workflow final réussi, les **428/428 tests sans erreur ni ignoré**, l’APK identifié/signé avec la clé M7B et le contrôle portrait cloud justifient des essais contrôlés après export complet des données. Aucune régression bloquante n’a été observée dans ces preuves, mais elles ne suffisent pas à annoncer la PR prête à fusionner.

Avant une décision de fusion, attendre le retour S25 sur la mise à jour sans effacement, les lots/saturations/visites/redémarrages du protocole ci-dessus, les deux autres campagnes et les à-coups éventuels. Examiner aussi la limite de restauration partielle valide et la récupération des quarantaines ; aucune réparation n’est autorisée par cette finalisation documentaire. Ne pas utiliser les stocks de noyau extrapolés comme preuve de livraison.

**Finalisation : seul `MISSION_7C_VALIDATION.md` modifié ; aucune modification exécutable, de sauvegarde ou de paramètres joueur, aucun test de jeu relancé, aucun nouvel APK compilé, aucune fusion. Arrêt après publication du rapport ; validation utilisateur attendue, M7D non commencée.**
