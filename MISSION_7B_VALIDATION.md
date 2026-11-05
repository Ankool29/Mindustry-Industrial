# Mission 7B — diagnostic économique intersectoriel

**Étape A uniquement. Aucun moteur 7C n’est activé.** Le diagnostic consulte le moteur natif et distingue les observations actives, les moyennes mémorisées et les estimations. Il ne corrige pas les incohérences économiques identifiées en 7A.

## APK et installation

**[Télécharger l’APK M7B et ses sources](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/42072442237/artifacts/13435874229)** · [Compilation GitHub Actions réussie](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/42072442237) · [Code compilé `1260a5c9`](https://github.com/Ankool29/Mindustry-Industrial/commit/1260a5c9a2a6758613ec2f76bfe26e963148b7e9).

Extraire le ZIP et installer `Mindustry-Industrial-v160.7-m7b.apk` **par-dessus M6D, sans désinstaller**. Le package reste `io.anuke.mindustry.industrial.m3`, le code de version Android passe à **9** (`versionName=8-industrial-m7b-160.7`). L’APK téléchargé a été contrôlé : signature valide avec le certificat précédent SHA-256 `61254fd2280bf34aa64bd06405cf0e2bc9b2d8fe7df4cc615c39983bcdfd8086`, somme de contrôle et alignement Android 16 Kio validés.

SHA-256 de l’APK : `fa435e294f172006cb20d7c7e3140957a3ba71a55b1a76b7d098153c6a066e76`. Java 17, Gradle 8.14.3, AGP 8.10.1, SDK 36 et Android minimal 21. Les sources correspondantes Mindustry/Arc et leurs licences sont dans l’archive. Artefact conservé jusqu’au 5 décembre 2026 ; le workflow permet de le reconstruire avec la clé précédente.

## 1. Fonctionnalités réalisées

### Accès et navigation

Dans **Serpulo industrielle — progression**, ouvrir la carte planétaire, sélectionner un secteur, puis **Diagnostic économique**. Ce bouton apparaît uniquement dans cette campagne, y compris si le secteur sélectionné n’a pas encore de base ; la liste du diagnostic contient seulement les colonies établies.

La vue propose une recherche textuelle, une liste de colonies, une fiche sectorielle, les détails des matières et les détails des plateformes du monde actif. **Retour** remonte dans cette navigation ; **Actualiser** relit un instantané. Les panneaux défilent verticalement, sans défilement horizontal activé. La consultation solo est en pause, comme la carte planétaire native.

### Signification des valeurs

| Information | Présentation / provenance |
| --- | --- |
| Stock du secteur actif | Copie de l’inventaire réel du noyau actif |
| Stock d’un secteur non chargé | Stock courant de `SectorInfo.items`, explicitement estimé par le modèle natif ; pas une inspection physique du noyau sauvegardé |
| Production brute par minute | `rawProduction.mean × 60`, issue des événements natifs de production ; observations actives ou moyenne mémorisée hors écran |
| Solde du noyau par minute | `production.mean × 60`, avec son signe ; ce n’est pas une dérivée exhaustive de tous les changements de stock |
| Indicateur de prélèvement net | `max(-solde, 0)` ; **pas la consommation totale des machines** |
| Consommation totale | **NON MESURÉE** : le natif ne donne pas les consommations agrégées de tous les intrants internes, liquides et carburants |
| Exports et réceptions | Moyennes natives par minute, pas nombres de cargaisons ni totaux cumulés livrés |
| Offre importable | Somme des exports moyens des sources éligibles visant ce secteur ; **estimation partagée entre récepteurs**, pas livraison réservée |
| Destination | Destination commune aux exports du secteur ; aucun bouton de reconfiguration n’est ajouté |
| Saturation | Indicateur de stock à la capacité native pour une matière, sans plafonnement ni modification de stock par le diagnostic |
| État | Monde actif, actif en pause, ou monde non chargé avec estimations natives |

Les données sont détachées des structures économiques. Modifier ou détruire un objet de présentation ne modifie pas le jeu. La lecture n’appelle ni `Universe.runTurn`, ni `getImportRates`/`refreshImportRates`, ni une méthode de production ou de livraison.

### Fraîcheur

Deux anciennetés sont séparées : **date de lecture de l’instantané** et **dernier échantillonnage natif du secteur dans la session**. M6D ne conserve pas de date fiable de cet échantillonnage : au démarrage, elle est donc **inconnue**, au lieu d’utiliser abusivement la date du fichier ou celle de consultation.

Un crochet en fin d’échantillonnage natif mémorise une heure monotone en mémoire pour la campagne progressive seulement. Cette mémoire est bornée par les identités natives de ses 272 secteurs, sans historique. Un changement d’objet `SectorInfo` invalide la date associée ; un changement de paramètres de session réinitialise le cache. Aucun champ de sauvegarde ni clé de paramètres n’est ajouté. Le jeu conserve ses sauvegardes et écritures natives habituelles ; le diagnostic n’ajoute aucune écriture économique ou historique propre.

La date concerne l’échantillonnage du secteur, pas une garantie de mesure individuelle de chaque machine. Une moyenne native conserve une fenêtre d’environ 60 échantillons : un échantillon récent ne renouvelle pas toute cette fenêtre. L’âge de session n’est pas un horodatage civil persistant.

### Plateformes

Dans le monde actif, les plateformes du joueur sont lues directement : matière/configuration, contenu du tampon, destination d’export, charge, efficacité connue, liquide disponible et besoins déclarés. Les plafonds nominaux sont calculés avec les paramètres des blocs existants : notamment **200 objets/min** pour le lancement avancé et **1200 objets/min** au maximum théorique de réception, sous réserve de l’offre, de l’eau, des délais et des sorties.

Les états signalés comprennent chargement incomplet, charge incomplète sans efficacité, bloc désactivé, destination invalide, réception non configurée, sortie occupée, refroidissement, manque d’eau ou absence d’offre native positive. Une charge déjà complète n’est pas déclarée bloquée uniquement parce que la consommation devient nulle. Les contenus mélangés d’une plateforme ancienne sont listés.

**Le débit observé d’une plateforme individuelle est indiqué non mesuré.** Le moteur fournit des moyennes sectorielles, pas un compteur d’envois attribué à chaque plateforme ; elles sont affichées séparément. Une animation locale `arriving` est signalée si elle existe, sans être présentée comme un manifeste de cargaisons entre secteurs.

Pour les secteurs non chargés, les détails des plateformes sont **indisponibles**. Le diagnostic ne charge pas leur monde ni leur fichier `.msav` pour inventer une situation actuelle. Aucune quantité de cargaisons en transit n’est déduite d’un débit ou d’un délai.

## 2. Classes et fichiers modifiés

| Fichier | Modification |
| --- | --- |
| [IndustrialDiagnostics.java](https://github.com/Ankool29/Mindustry-Industrial/blob/1260a5c9a2a6758613ec2f76bfe26e963148b7e9/core/src/mindustry/game/IndustrialDiagnostics.java) | Nouveau collecteur de copies, index d’offres sans mutation des caches natifs, états et ancienneté de session |
| [IndustrialDiagnosticsDialog.java](https://github.com/Ankool29/Mindustry-Industrial/blob/1260a5c9a2a6758613ec2f76bfe26e963148b7e9/core/src/mindustry/ui/dialogs/IndustrialDiagnosticsDialog.java) | Nouvelle vue tactile, instantanés manuels et détails ; seules les étiquettes d’âge sont actualisées chaque seconde réelle |
| [SectorInfo.java](https://github.com/Ankool29/Mindustry-Industrial/blob/1260a5c9a2a6758613ec2f76bfe26e963148b7e9/core/src/mindustry/game/SectorInfo.java) | Deux lignes après l’échantillonnage pour dater celui-ci en mémoire ; aucune formule ni structure sérialisée modifiée |
| [PlanetDialog.java](https://github.com/Ankool29/Mindustry-Industrial/blob/1260a5c9a2a6758613ec2f76bfe26e963148b7e9/core/src/mindustry/ui/dialogs/PlanetDialog.java) | Point d’accès limité à la planète progressive et réutilisation d’un seul dialogue |
| [IndustrialDiagnosticsTests.java](https://github.com/Ankool29/Mindustry-Industrial/blob/1260a5c9a2a6758613ec2f76bfe26e963148b7e9/tests/src/test/java/IndustrialDiagnosticsTests.java) | Douze scénarios automatisés isolés |
| Bundles anglais/français, guide Android, workflow Android | Traductions, procédure et version APK 9 |

`Universe`, `LaunchPad`, `LandingPad`, les recettes, l’arbre de recherche, `IndustrialExpansion`, les règles d’exploration et la génération planétaire ne sont pas modifiés. Serpulo classique et la campagne industrielle libre n’ont pas de nouveau bouton ; l’API refuse leurs lectures de diagnostic et ne leur mémorise pas de dates.

## 3. Tests automatisés et protocoles économiques

**397 tests, 0 échec, 0 erreur, 0 ignoré**, en local et dans la compilation GitHub Actions. Les XML finaux ont été téléchargés et recomptés : [rapports JUnit](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/42072442237/artifacts/13435939041) · [preuves de compilation](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/42072442237/artifacts/13435504520).

Pour rejouer le protocole dans un environnement cloud configuré comme le dépôt : `./gradlew tests:test --tests IndustrialDiagnosticsTests --no-daemon --max-workers=2`. Le workflow Android exécute lui aussi la suite complète ; aucune compilation sur téléphone n’est nécessaire.

Les douze nouveaux tests couvrent :

1. Valeurs exactes des stocks, production brute, solde, prélèvement net, exports, réceptions et offre, comparées aux structures natives.
2. Lecture du noyau actif plutôt que d’une ancienne métadonnée, et copies détachées.
3. Datation d’un véritable échantillon natif, absence de persistance et invalidation d’une information remplacée.
4. Date inconnue des anciennes sauvegardes, sans ouvrir de monde ou de fichier sectoriel pour la lire.
5. Refus des deux autres campagnes, absence de mutation de leurs données.
6. Somme des offres, éligibilité des sources et distinction offre/réception, sans initialiser un cache natif.
7. Paramètres et blocages des vraies classes de plateformes, lecture sans modification et animation locale distincte d’un transport engagé.
8. Indicateur de capacité native, sans écrêter les stocks.
9. Sauvegarde et paramètres conservés octet pour octet pendant cent lectures ; recherche et compteur M6D inchangés ; rechargement du même format de monde.
10. Protocole usine réelle active → hors écran → retour.
11. Réception native comparée avec et sans lectures du diagnostic.
12. Lectures de 5, 20, 50 et 100 colonies synthétiques sans modification des valeurs natives.

### Usine active puis non visitée : résultat reproductible

Le test `realFactoryActiveInactiveAndReturnProtocolUsesIsolatedCheckpoint` crée un monde temporaire de 48 × 48, un noyau en (6,6), un convoyeur orienté à gauche en (8,6) et une presse à graphite en (9,6). Le banc fournit initialement **10 charbons** à cette machine native ; cette préparation n’est pas une fonctionnalité du jeu livré.

1. Après 700 ticks, la presse a consommé le charbon et livré **5 graphites** au noyau par le convoyeur. Une sauvegarde temporaire du monde et des informations est conservée.
2. Dans une branche restant active 8000 ticks supplémentaires, le stock reste **5**, puis la moyenne de production retombe à zéro : absence d’intrant réelle.
3. Le banc restaure son checkpoint isolé et charge un second secteur. Il exécute **trois tours natifs de 120 secondes de simulation**, sans attendre six minutes réelles.
4. Le modèle natif extrapole encore la moyenne historique : le stock estimé de graphite atteint **35**. Les lectures de diagnostic correspondent exactement aux informations natives et ne les changent pas.
5. Au retour, le noyau reçoit **35** selon le mécanisme natif de réapplication des informations ; le charbon interne de la presse reste **0**. Le diagnostic passe de l’origine « estimation hors écran » à la lecture du noyau actif, sans crédit propre.

Ce résultat expose l’incohérence native ; **il n’est pas corrigé par M7B**. Le checkpoint n’a aucun lien avec les sauvegardes du joueur. Le test est reproductible dans le module `tests` et ses données temporaires sont nettoyées après exécution.

### Saturation

Le test de capacité vérifie la lecture d’un noyau au plafond natif de 4000. Un banc headless complémentaire, lui aussi isolé, compare une vraie presse et un noyau initialement plein, trois tours non visités puis le retour.

Résultat : **4000 graphites en actif, 4000 après trois tours et 4000 au retour**, avec une moyenne brute native de 5/min issue de l’essai initial. Le marqueur de saturation est vrai et chaque lecture conserve les informations natives. [Banc reproductible](docs/mission7b/SaturationProbe.java) · [résultat brut](docs/mission7b/saturation-results.txt).

### Réceptions

Le test `nativeReceptionHasIdenticalResultWithAndWithoutDiagnosticReads` utilise une vraie plateforme de réception, 1500 unités d’eau et une offre native de 2 cuivres/s issue d’un autre secteur. Après 3500 ticks, il obtient **100 cuivres dans le tampon et l’eau consommée**, aussi bien sans diagnostic qu’avec des lectures toutes les 60 images. Les compteurs natifs de réception sont lus tels quels ; le stock de la source reste nul dans cet état de test.

Cette réception est celle du système statistique natif, **pas une transaction conservatrice ajoutée**. Le diagnostic ne la crée pas et ne la présente pas comme une preuve de paiement ou de conservation.

## 4. Compatibilité et vérification graphique

Une copie indépendante des données de validation **M6D** a été utilisée pour le client graphique : trois colonies strictes, recherche de presse déjà acquise, compteur économique à trois et noyau source à **2650 cuivre / 2950 plomb**. Aucun effacement ni conversion des données d’origine.

Deux processus distincts ont vérifié : consultation depuis le menu et la carte, dates inconnues avant observation, stocks et provenance, prélèvement net affiché avec sa limite, date connue après échantillonnage actif, détails des deux plateformes, absence de changement du stock et du registre M6D après dix actualisations, réutilisation du même dialogue et libération des widgets à sa fermeture. Les campagnes libre et classique n’affichent pas le point d’accès ; la campagne classique rechargée conserve ses vagues et ses règles.

Après fermeture complète et redémarrage, le stock et le registre M6D sont conservés ; la date d’échantillonnage est de nouveau **inconnue**, car elle n’a pas été écrite. Un autre processus a contrôlé les listes de 5/20/50/100 colonies synthétiques en portrait 600 × 1000 et le défilement horizontal désactivé.

[Résultats graphiques](docs/mission7b/ui-results.txt) · [script fonctionnel](docs/mission7b/ui-check.js) · [banc graphique synthétique](docs/mission7b/ui-perf.js).

**Ces scripts ne doivent pas être installés sur les sauvegardes du joueur.** Ils sont absents de l’APK : les fixtures graphiques préinstallent des plateformes et des états de mesure uniquement dans une copie jetable pour vérifier les libellés. Les noms anglais des contenus dans certaines captures proviennent du chargement initial de la configuration de test ; les nouvelles commandes françaises sont chargées ensuite.

| Contrôle | Capture |
| --- | --- |
| Liste portrait | [Colonies](docs/mission7b/colonies-portrait.png) |
| Stock non visité et date inconnue | [Fiche hors écran](docs/mission7b/sector-offscreen.png) · [Matière](docs/mission7b/material-offscreen.png) |
| Origine active et prélèvement net distingué de la consommation totale | [Fiche active](docs/mission7b/sector-active.png) · [Solde négatif](docs/mission7b/material-net-withdrawal.png) |
| Plafond de débit et conditions natives | [Lancement](docs/mission7b/launch-platform.png) · [Réception](docs/mission7b/landing-platform.png) · [Conditions](docs/mission7b/landing-conditions.png) |
| Autres campagnes | [Libre](docs/mission7b/free-map-unchanged.png) · [Classique](docs/mission7b/classic-map-unchanged.png) |
| Reprise sans date inventée | [Après redémarrage](docs/mission7b/restart-unknown-age.png) |
| Cent colonies synthétiques | [Liste défilée](docs/mission7b/100-colonies.png) |

## 5. Mesures de performance

### Collecteur de données — serveur uniquement

Environnement cloud **Intel Xeon Platinum 8573C, x86-64, Java 17.0.21**, heap 256 Mio à 1 Gio. La sonde headless utilise un espace de données distinct, 272 secteurs disponibles, 22 objets natifs et 12 matières renseignées dans les quatre dictionnaires natifs. Chaque base synthétique possède une destination ; aucun monde n’est ouvert pour les quatre premiers profils.

200 lectures d’échauffement puis **500 lectures** par taille. Les allocations sont mesurées par `ThreadMXBean`. Les stocks, caches natifs et l’état modifié des paramètres sont vérifiés inchangés.

| Colonies | Lecture médiane / p95, ms | CPU moyen, ms | Allocation par lecture, Kio |
| ---: | ---: | ---: | ---: |
| 5 | 0,0268 / 0,0698 | 0,0369 | 8,52 |
| 20 | 0,0541 / 0,1507 | 0,0700 | 31,97 |
| 50 | 0,1322 / 0,3652 | 0,1649 | 79,17 |
| 100 | 0,3787 / 1,6428 | 0,4282 | 158,53 |

Un profil supplémentaire avec **100 colonies et un monde actif contenant 5000 convoyeurs + un noyau** obtient 0,3853 ms en médiane, 1,2111 ms au p95, 0,4476 ms CPU et 158,53 Kio alloués. Ce profil teste le parcours des bâtiments ; il ne simule pas 5000 plateformes avec leurs fiches.

Ce sont des coûts **par consultation**, pas par image. Le collecteur ne relit le monde qu’à l’ouverture ou sur **Actualiser** ; une rotation reconstruit la présentation de la copie existante. Quand la vue est fermée, les copies et widgets sont libérés. L’instrumentation conserve uniquement la dernière date par secteur canonique, sans liste de valeurs antérieures.

Le crochet fermé a été mesuré sur un million d’appels après échauffement : **environ 57,47 ns CPU/appel** et allocation amortie proche de zéro (0,0001 octet/appel, incluant le bruit du banc). En fonctionnement normal, cet appel intervient seulement après un échantillon natif, environ une fois par seconde de simulation dans le secteur actif. Les autres campagnes sortent immédiatement du crochet. Aucun historique ni fichier de diagnostic permanent n’est écrit.

Les allocations par lecture ne sont pas une mesure du heap retenu total ou de la RAM Android. Elles incluent des objets transitoires ; le cache de dates est borné par les 272 secteurs. [Source du banc](docs/mission7b/DiagnosticsBench.java) · [résultats bruts](docs/mission7b/collector-results.txt).

### Construction et validation de la liste graphique — cloud uniquement

Le banc utilise Xvfb, rendu logiciel, disposition mobile portrait 600 × 1000, un dialogue réutilisé, une ouverture d’échauffement puis dix ouvertures chronométrées pour chaque taille. Il mesure la construction + `validate` du dialogue, avec surcoût du script Rhino ; **pas le temps de rendu complet, les FPS ou la mémoire retenue du téléphone**. L’accès au compteur d’allocation Java 17 nécessite `--add-exports=jdk.management/com.sun.management.internal=ALL-UNNAMED` dans ce banc externe seulement.

| Colonies | Construction + validation médiane / p95, ms | Allocation médiane approximative, Kio |
| ---: | ---: | ---: |
| 5 | 3,360 / 22,700 | 80 |
| 20 | 2,990 / 6,940 | 200 |
| 50 | 2,530 / 7,970 | 300 |
| 100 | 4,790 / 7,320 | 700 |

Une première série complète avait montré des pics plus importants : p95 de 51 ms à 20 colonies, 154 ms à 50 et 146 ms à 100 ([résumé conservé](docs/mission7b/ui-perf-initial-summary.txt)). La répétition ci-dessus ne les reproduit pas systématiquement. Ces petites séries dans Xvfb/Rhino ne permettent pas de garantir une borne de latence sur téléphone ni de conclure que les pics ont été corrigés ; le code de production est identique. L’allocation graphique inclut widgets, chaînes et surcoût du banc, pas seulement la copie de données.

Les pics sont signalés, pas masqués : leur origine ne peut pas être attribuée au seul collecteur sans profilage plus fin. Le dialogue est en pause en solo et n’est pas reconstruit à chaque image. L’essai tactile physique et la latence d’ouverture sur S25 Ultra restent à confirmer ; aucune équivalence avec les mesures du serveur n’est annoncée.

## 6. Procédure de test sur téléphone

1. Installer l’APK par-dessus M6D **sans désinstaller ni réinitialiser**. Une exportation complète préalable conserve une copie de la progression de test.
2. Ouvrir **Serpulo industrielle — progression**, afficher la carte, sélectionner un secteur puis **Diagnostic économique**. Parcourir la liste et rechercher une colonie.
3. Ouvrir une matière : vérifier le stock, l’origine active/estimée, le solde signé, l’indicateur de prélèvement et la mention de consommation totale non mesurée. Ne pas lire les moyennes comme des livraisons.
4. Pour obtenir une date connue, fermer les vues, visiter le secteur et laisser le jeu fonctionner quelques secondes. Rouvrir le diagnostic : l’ancienneté du dernier échantillon de session apparaît. Les secteurs jamais observés cette session peuvent rester de date inconnue.
5. Dans le secteur actif, consulter une plateforme existante : matière, destination, nominal, tampon et conditions. Ne pas construire de machine uniquement pour ce test si elle n’est pas encore recherchée. Dans un autre secteur, les détails locaux doivent être indisponibles.
6. Noter le stock, appuyer plusieurs fois sur **Actualiser** pendant la consultation en pause : le diagnostic ne doit ni prélever ni ajouter de ressources. Revenir au jeu puis à la vue pour observer les changements natifs ordinaires.
7. Fermer normalement le jeu puis le rouvrir : recherches et compteur M6D restent conservés ; les dates de mesures redeviennent inconnues tant qu’aucun nouvel échantillon n’a été observé. Ce n’est pas une perte des données économiques.
8. Vérifier **Serpulo** et **Serpulo industrielle** : aucun bouton de diagnostic. Aucun abandon, import de script ou suppression de secteur n’est nécessaire.

Pour comparer des pénuries et saturations, utiliser le protocole automatisé isolé décrit plus haut, ou une **copie indépendante des données**. Les scripts de fixtures ne sont pas destinés aux sauvegardes du téléphone. Des ressources peuvent toujours changer hors consultation selon le moteur natif : cela ne doit pas être attribué à l’observateur.

## 7. Limites et suite

- Consommation totale, débit réellement attribuable à chaque plateforme, manifeste de cargaisons en transit et conservation économique : non fournis par cette étape.
- Moyennes natives anciennes, moyennes encore influencées par une ancienne fenêtre et projections incohérentes : exposées, pas corrigées.
- Aucun chargement de monde non visité pour inspecter ses bâtiments ; aucune production application fermée, aucun transport conservatif ajouté.
- Aucun historique persistant, CSV ou journal illimité dans le jeu. L’instantané est manuel ; les valeurs peuvent être anciennes jusqu’à une nouvelle consultation/actualisation.
- Le contrôle graphique cloud ne remplace pas l’installation et l’essai physique du nouvel APK sur S25 Ultra. Le multijoueur et les mods qui modifient le modèle économique ne sont pas validés.

**La mission 7C n’est pas commencée.** Elle nécessite une nouvelle validation après l’essai de ce diagnostic.
