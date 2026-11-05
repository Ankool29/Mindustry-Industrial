# Mission 7A — audit de la logistique intersectorielle

**Statut : audit uniquement, architecture future à valider.** Aucun fichier de gameplay, recette, recherche, règle d’exploration, coût M6D ou sauvegarde existante n’a été modifié. Aucun APK ni workflow de compilation n’a été lancé pour cette mission. Seul ce rapport est publié.

## 1. Périmètre et conclusion

L’audit porte sur **M6D, commit `cec7286a79fb4bd3d48b7864818d43d1305f6339`**, basé sur Mindustry **v160.7**, et Arc **`7445105cd2683daf5b9ac7033ebb07265d3e814d`**. Les branches distantes ont été vérifiées avant publication. Les quatre fichiers natifs `LaunchPad`, `LandingPad`, `Universe` et `SectorInfo` n’ont aucune différence avec la base amont v160.7 dans ce fork.

**Le natif suffit à proposer des expéditions et une usine multisecteur approximative. Il ne constitue pas un moteur économique conservatif.** Les débits hors écran prolongent des observations ; ils ne calculent ni les recettes, ni leurs intrants, ni le carburant restant. Un réseau A → B → C est possible, mais son fonctionnement apparent ne prouve pas que ses matières ont été intégralement payées.

Recommandation : conserver le fonctionnement actuel pour les parties existantes, commencer par un diagnostic en lecture seule, puis, **après validation**, développer une agrégation conservatrice des stocks et transports dans un périmètre industriel explicite. Réutiliser les contenus, interfaces et sauvegardes natives ; éviter les mondes complets permanents et le double calcul natif/personnalisé. La simulation de recettes agrégées vient ensuite, avec ses propres critères de validation.

Il faut distinguer **secteur non visité**, **autre planète** et **application fermée** : ces trois situations n’avancent pas de la même façon.

## 2. Cartographie du code

Tous les liens ci-dessous sont figés sur le commit audité.

| Référence | Classes et points d’entrée | Rôle |
| --- | --- | --- |
| [R1 — plateformes](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/world/blocks/campaign/LaunchPad.java#L83) | `LaunchPadBuild.acceptItem`, `shouldConsume`, `updateTile`, `buildConfiguration` ; `LaunchPayloadComp.remove` | Remplissage, charge, départ, destination, statistiques d’export et ancien crédit direct |
| [R2 — réception](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/world/blocks/campaign/LandingPad.java#L136) | `LandingPadBuild.updateTimers`, `updateTile`, `handleLanding`, `canRedirectExports`, `read/write` | Débits importables, file d’attente, eau, animation, livraison et sauvegarde de réception |
| [R3 — paramètres des blocs](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/content/Blocks.java#L6840) | `launchPad`, `advancedLaunchPad`, `landingPad` | Capacités, durées, puissance, pétrole, eau et coûts de construction |
| [R4 — univers](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/game/Universe.java#L63) | `update`, `runTurn`, `save/load` | Horloge de campagne, sélection des planètes, estimations hors écran |
| [R5 — métadonnées](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/game/SectorInfo.java#L20) | `production`, `rawProduction`, `export`, `imports`, `items`, `ExportStat` ; `update`, `prepare`, `write`, `eachImport` | Stocks, moyennes, caches, échantillonnage et reprise d’un secteur |
| [R6 — stocks des secteurs](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/type/Sector.java#L127) | `hasBase`, `isAttacked`, `isBeingPlayed`, `addItems`, `removeItems`, `items`, `saveInfo/loadInfo` | Accès actif au noyau ou hors écran à `SectorInfo`, plafonds et sérialisation des informations |
| [R7 — événements de production](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/entities/comp/BuildingComp.java#L1040) | `BuildingComp.produced` ; [MinerComp](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/entities/comp/MinerComp.java#L96) ; [CoreBlock](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/world/blocks/storage/CoreBlock.java#L789) ; [StorageBlock](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/world/blocks/storage/StorageBlock.java#L78) | Production brute et mouvements de noyau effectivement suivis |
| [R8 — carte et statistiques](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/ui/dialogs/PlanetDialog.java#L433) | `showSelect`, `canSelect`, dessin des arcs ; `showStats` à partir de L1076 | Sélection de bases, liens source/destination, stocks et moyennes par minute |
| [R9 — cycle du jeu](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/core/Logic.java#L510) | `Logic.update`, écoute de `SaveLoadEvent` à L70 ; [Control.pause/resume](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/core/Control.java#L648) | Temps joué, pause, arrière-plan Android et réapplication des informations |
| [R10 — persistance](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/io/SaveVersion.java#L114) | `SaveVersion.writeMeta`, [Saves.saveSector/getSectorFile](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/game/Saves.java#L249) ; [Arc Settings](https://github.com/Anuken/Arc/blob/7445105cd2683daf5b9ac7033ebb07265d3e814d/arc-core/src/arc/Settings.java#L327) | Fichiers `.msav`, informations dans les paramètres et sérialisation UBJSON |
| [R11 — campagnes industrielles](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/type/IndustrialPlanet.java#L30) | `IndustrialPlanet.peacefulRules/applyRules`, [IndustrialCampaign](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/game/IndustrialCampaign.java#L29), [Planets](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/content/Planets.java#L173) | Isolation des planètes, paix et exploration existante |
| [R12 — recherche](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/game/IndustrialResearch.java#L174) | Arbre détaché, découvertes, financement ; [UnlockableContent.unlockedNow](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/ctype/UnlockableContent.java#L281) | Conditions industrielles indépendantes |
| [R13 — M6D](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/game/IndustrialExpansion.java#L67) | `prepare`, `stamp`, `recover`, `established` ; [Control](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/core/Control.java#L443) | Paiement de colonisation, journal et pause transitoire de départ |
| [R14 — variantes et arbre classique](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/world/meta/BuildVisibility.java#L17) | Visibilité ancien/moderne ; [SerpuloTechTree](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/content/SerpuloTechTree.java#L15), [CampaignRules](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/game/CampaignRules.java#L7) | Recherche militaire native et choix du modèle logistique |

## 3. Les deux transports natifs

### 3.1 Choix du modèle et destination

Le commutateur est `planet.campaignRules.legacyLaunchPads`, conservé sous la clé `<planète>-campaign-rules` ([Planet](https://github.com/Ankool29/Mindustry-Industrial/blob/cec7286a79fb4bd3d48b7864818d43d1305f6339/core/src/mindustry/type/Planet.java#L251)). **Par défaut neuf, les trois planètes auditées utilisent le modèle moderne**. Serpulo classique autorise l’option ancienne ; les planètes industrielles ne l’exposent pas dans leur configuration. Une ancienne valeur importée doit être lue, pas supposée identique au défaut.

Une source possède **une destination au niveau du secteur**, `SectorInfo.destination`. Toutes ses plateformes l’utilisent : pas de destination indépendante par plateforme ou matière. L’interface choisit une base existante de la **même planète**, sans exiger le voisinage ni une distance maximale. Ce choix de transport est distinct des règles M6D d’exploration et de colonisation ; il ne découvre ni ne colonise un secteur.

La réception moderne propose aussi de rediriger vers elle les secteurs exportant la matière choisie. Ce bouton remplace leur destination entière, donc peut déplacer **tous leurs autres exports**, pas seulement cette matière. Il faut le signaler avant d’en faire un outil de routage industriel fin.

### 3.2 Départ chargé

`LaunchPadBuild` accepte les objets jusqu’à 100 unités. L’ancienne plateforme accepte des mélanges ; la moderne charge un type à la fois. Un départ demande un inventaire plein et un compteur de charge arrivé à son terme. Le compteur avance via `edelta()`, donc dépend de l’efficacité et des consommations nécessaires à son avancement. Les ressources entrent physiquement depuis le réseau local : elles ne doivent pas nécessairement transiter par le noyau.

Le chargement est retiré de la plateforme et copié dans une entité `LaunchPayload`. Après une durée visuelle de 120 ticks, soit environ deux secondes de simulation, `remove()` traite les objets et leurs statistiques. **La destination est relue à ce moment**, elle n’est pas figée dans le colis au décollage. Une redirection pendant ce délai peut donc changer la destination ; un choix nul ou le secteur lui-même ne donne pas de crédit intersectoriel. Les exigences de validité de destination ne constituent pas un précontrôle bloquant du départ physique.

Il ne faut pas assimiler cette entité à un journal de livraison garanti : son schéma généré la rend sérialisable avec les piles et le temps restant, mais elle n’a pas de reçu transactionnel source/destination. Cette mission n’a pas testé tous les arrêts Android au milieu d’un vol.

### 3.3 Capacités et consommations

Valeurs R2/R3 à vitesse normale, pleine alimentation et sans saturation ; calculs théoriques de régime, pas mesures de cadence sur téléphone.

| Élément | Lot / délai | Débit idéal | Consommation native |
| --- | --- | --- | --- |
| Ancienne plateforme | 100 / 20 s de charge | 5 objets/s | puissance 4 unités/tick, soit 240 unités/s pendant la charge ; pas de carburant liquide |
| Plateforme avancée | 100 / 30 s de charge | 3,33 objets/s | puissance 8/tick, soit 480/s ; pétrole 9 unités/s pendant la charge, nominalement 270 pour une charge complète |
| Plateforme d’atterrissage | 100 ; arrivée 2,5 s + refroidissement 2,5 s | au plus environ 20 objets/s par plateforme si le débit annoncé le permet et si sa sortie est vidée | **1500 unités d’eau par arrivée**, réservoir 3000 ; pas de consommation électrique déclarée sur ce bloc |

La puissance et le pétrole de départ sont des consommations de charge, pas un reçu économique indivisible par fusée. Une plateforme déjà chargée peut attendre son chargement matériel. Les surcadences et l’efficacité modifient les plafonds de départ. La réception n’est pas surcadencée ; son débit réel est limité par l’eau, les sorties, son inventaire vide, ses délais et le débit importable partagé par matière.

La logistique native transporte des **objets**, pas des cargaisons liquides intersectorielles. Le pétrole et l’eau sont des entrées locales des plateformes, pas des liquides exportés par celles-ci. Les futurs carburants personnalisés ne sont pas développés ici.

### 3.4 Crédit et réception

**Ancien modèle :** à la disparition normale du colis, `destination.addItems` crédite directement le noyau actif ou `destination.info.items` hors écran. Une plateforme d’atterrissage n’est pas nécessaire. Le crédit est plafonné à la capacité native ; l’excédent n’est pas mis en attente. Une destination sans base ne reçoit pas les objets. Il n’existe pas de transaction d’aller-retour assurant leur remboursement.

**Modèle moderne :** le colis augmente les statistiques d’export, **sans crédit direct de la destination**. `SectorInfo.eachImport` retrouve les sources de la même planète pointant vers cette destination ; `getImportRates` additionne leurs moyennes. Ces moyennes alimentent un compteur par matière. Une plateforme de réception configurée, vide, suffisamment alimentée en eau et refroidie est choisie dans une file priorisée ; elle matérialise ensuite 100 objets et enregistre `produced`/`handleItemImport`. Les plateformes d’une matière partagent le compteur : il ne faut pas multiplier leur débit annoncé par leur nombre.

Ce chemin ne retire pas à nouveau 100 objets d’un stock source persistant au moment de l’arrivée. La relation entre départs et arrivées est **statistique**, sans identité de cargaison ni réservation finie. Le débit importable ne vérifie pas à lui seul la disponibilité actuelle de pétrole, de matières premières ou de cargaisons déjà expédiées.

Attention aux essais en partie personnalisée : `LandingPadBuild.isFake()` peut traiter une réception hors campagne comme une source artificielle pour les cartes. Une démonstration dans ce contexte ne valide pas une économie de campagne.

### 3.5 Combinaisons de chargement

En solo, un seul monde est actif (`Vars.world`, groupes d’entités et état global). Source et destination distinctes ne sont donc pas simultanément simulées machine par machine.

| Situation | Ancien modèle | Modèle moderne |
| --- | --- | --- |
| Source active, destination non visitée | Colis réel ; crédit direct des informations de stock après le vol, sous plafond | Départ réel ; export moyen actualisé ; pas de crédit direct hors écran. La destination continue avec ses anciennes statistiques de production |
| Source non visitée, destination active | Crédit par lots estimés au tour d’univers ; la source n’est pas un monde simulé | Réception réelle alimentée par les moyennes d’export de la source, eau et sorties locales réelles ; pas de débit unitaire apparié à la source |
| Les deux non visités, joueur dans un troisième secteur de la même planète | Passes d’import/export estimés puis production au tour d’univers | Stocks modifiés par anciennes moyennes de production ; pas de fonctionnement des plateformes ni de passe moderne de transport hors écran |
| Joueur sur une autre planète non groupée | Pas d’évolution de ces secteurs par `runTurn` | Idem |
| Jeu en pause, au menu ou application fermée | Pas de progression normale de l’horloge de campagne | Idem ; aucun rattrapage natif de temps civil à la réouverture |

La table décrit le modèle de code, pas une campagne physique de transports testée sur S25 Ultra.

## 4. Production hors écran : calcul exact et portée

### 4.1 Ce qui est mesuré dans le monde actif

`SectorInfo.update()` échantillonne environ chaque **60 ticks de simulation** :

- `production` : moyenne des événements d’entrée/sortie du noyau suivis par `handleCoreItem`. Ce n’est pas une dérivée exhaustive de tout changement d’inventaire : constructions, recherches et modifications directes de modules peuvent emprunter d’autres chemins.
- `rawProduction` : objets annoncés par `Building.produced` et l’extraction suivie. Une réception moderne compte elle aussi comme production brute.
- `export` et `imports` : moyennes des événements correspondants.

Chaque `ExportStat` possède une fenêtre de **60 échantillons**, donc environ une minute de simulation à vitesse normale. Seule la moyenne est persistante ; fenêtre, compteurs instantanés et certains caches sont transitoires. Au rechargement, la fenêtre est initialisée avec la moyenne conservée, pas réapprise immédiatement à zéro.

Le moteur borne `production.mean` par `rawProduction.mean`, et l’export par `rawProduction + max(-production, 0)`. Il limite aussi les imports observés par le débit importable. Ces bornes réduisent certains excès ; elles ne reconstruisent pas un bilan de recettes.

Lors d’une sauvegarde, `prepare` copie le stock du noyau et sa capacité, retient le meilleur noyau et efface les exports si aucune plateforme signalée n’est efficace. **Avoir une plateforme efficace à cet instant n’atteste pas que ses intrants dureront pendant toute l’absence.**

### 4.2 Ce qui est calculé dans les secteurs non visités

`Vars.turnDuration = 7200 ticks`, soit **120 secondes de simulation**. `Universe.update` déclenche `runTurn` à ce rythme. Les secteurs attaqués sont gelés pour cette évolution hors écran ; les sources anciennes attaquées n’exportent pas dans la passe d’estimation. Certaines opérations directes de crédit ont leurs propres gardes : ne pas assimiler ce gel à une interdiction universelle de modifier le stock.

Pour chaque matière `i` d’une base non active et non attaquée, la règle de production est essentiellement :

```text
stock[i] += min(troncature(moyenneProduction[i] × 120), capacité - stock[i])
puis suppression des valeurs négatives
```

Les conversions vers `int` tronquent ; il n’existe pas de reste fractionnaire conservé par cette formule. Un débit positif inférieur à 1/120 objet/s peut ainsi ne rien ajouter à chaque tour. La capacité est native, par matière, pas un nouveau plafond industriel.

En ancien mode, une passe précédente ajoute à la destination `troncature(exportMoyen × 120)`. Ce montant n’est pas explicitement retiré d’un stock de colis source dans cette passe : le moteur s’appuie sur les deltas observés de noyau. Un correctif réduit ensuite l’export lorsque le stock source est épuisé **et** sa production est négative, en fonction des imports reçus pendant le tour. Ce correctif conditionnel, appliqué après le crédit, n’est pas un solveur de conservation.

En mode moderne, cette passe directe d’import/export **n’est pas exécutée**. Une destination non visitée ne reçoit donc pas chaque cargaison dans un inventaire de réception ; sa moyenne de production mémorisée représente approximativement son ancien fonctionnement, y compris l’effet des imports déjà observés.

Aucun `updateTile()` de foreuse, usine, convoyeur, générateur ou plateforme de ce secteur n’est exécuté. Il n’y a ni recherche de chemin des objets, ni validation de recette, ni bilan d’électricité ou de liquides, ni épuisement de gisement calculé hors écran.

### 4.3 Consommations et limites économiques

Un retrait de noyau par un déchargeur peut donner une moyenne négative de matière. Mais les intrants transitant directement de foreuse à usine, les liquides, les stocks internes, les carburants et la puissance n’ont pas de contraintes agrégées équivalentes dans `runTurn`. Même si une matière suivie atteint zéro, la moyenne positive d’une autre matière n’est pas recalculée en fonction de cette pénurie.

Conséquences possibles : production virtuelle maintenue après épuisement d’un stock ; export importable prolongé sans carburant réel ; poursuite d’une chaîne après arrêt de son fournisseur ; reprise décalée lorsque l’usine active doit de nouveau respecter ses intrants. Les changements de destination et boucles amplifient les erreurs ; le code contient lui-même un avertissement sur la redirection des plateformes dans `SectorInfo`.

Ce sont des limites du modèle statistique, pas la preuve que toute usine crée constamment des ressources. Les scénarios de la section 7 vérifient l’algorithme sur des états contrôlés ; une validation de chaînes complètes reste à réaliser avant un futur changement.

### 4.4 Revenir dans un secteur et sauvegarder

Le fichier `.msav` contient le monde, les bâtiments et leurs inventaires locaux. Les informations du secteur sont également sérialisées dans les paramètres sous `<planète>-s-<id>-info`. `SaveVersion.writeMeta` appelle `SectorInfo.prepare` ; le fichier sectoriel porte le nom `sector-<planète>-<id>.msav`.

Lors du chargement, `Logic` appelle `SectorInfo.write` : le noyau chargé est vidé puis reçoit le stock conservé dans les informations, avec plafonnement à la capacité. Les machines, convoyeurs, réservoirs et cargaisons sauvegardées reprennent leur état local ; ils n’ont pas été avancés pendant l’absence. Une réception moderne sauvegarde sa configuration, sa priorité, ses compteurs, la matière arrivant et l’eau déjà consommée.

C’est une frontière importante pour un futur moteur : créditer à la fois un inventaire interne du `.msav` et le stock agrégé, ou réappliquer deux fois une livraison lors de la visite, peut dupliquer les ressources. Conserver uniquement l’un des fichiers lors d’une restauration peut aussi désynchroniser l’économie.

`sector.saveInfo()` prépare du **UBJSON en mémoire dans `Settings`** ; ce n’est pas une réécriture de tous les `.msav` à chaque tour. La persistance des paramètres et la sauvegarde du monde actif sont des opérations distinctes. Le journal M6D reconnaît déjà les limites de l’écriture multifichier ; il ne fournit pas automatiquement une transaction logistique durable.

### 4.5 Temps, planètes et téléphone en arrière-plan

`Logic.update` appelle `Universe.update` seulement dans une campagne non pausée. `Universe.load` relit des compteurs de jeu ; il ne calcule pas un temps écoulé depuis une date de fermeture. `Control.pause` met normalement la partie solo en pause quand l’application passe en arrière-plan. La survie d’un processus Android en arrière-plan ne doit pas être promise comme un service de production.

`Universe.runTurn` ne met à jour que la planète courante et les éventuelles relations `updateGroup`. Les trois planètes auditées ont un groupe vide par défaut : jouer sur une autre campagne n’avance pas les deux autres. Cette séparation est cohérente avec l’isolation du projet et ne doit pas être supprimée pour gagner artificiellement des ressources.

## 5. Compatibilité avec les trois campagnes et M6D

| Sujet | Serpulo classique | Industrielle libre | Industrielle progressive |
| --- | --- | --- | --- |
| Moteur de production et transports | Natif | Même moteur natif | Même moteur natif |
| Transport neuf par défaut | Moderne ; ancien optionnel | Moderne ; ancien non exposé | Moderne ; ancien non exposé |
| Recherche des plateformes | Arbre classique ; la plateforme avancée demande notamment `SectorComplete(extractionOutpost)` | Accès libre industriel | Nœud avancé sous routeur, réception sous plateforme avancée ; ressources et recherches indépendantes, objectifs de conquête omis |
| Vagues / conquête | Peuvent intervenir dans l’acquisition et le gel des secteurs | Désactivées | Désactivées |
| Colonisation M6D | Sans effet | Sans effet | Frais et compteur uniquement pour les implantations, pas les expéditions |
| Autres planètes/campagnes | Pas de partage automatique | Espace de planète propre | Espace de planète propre |

Les plateformes ne sont pas filtrées comme militaires par `IndustrialContent`. La construction avancée demande cuivre, plomb, silicium et titane ; la réception demande cuivre, graphite et titane. Cette logistique arrive donc après l’accès à ces matières, sans devoir conditionner l’exploration initiale ou le premier gisement de titane. Le pétrole et l’eau doivent être produits localement pour le fonctionnement réel. Le bloc ancien `launchPad` n’a pas de nœud dans l’arbre Serpulo moderne ; `advancedLaunchPad` et `landingPad` sont bien copiés dans l’arbre industriel. Les gardes de construction M6C s’appliquent aux machines non recherchées ; la logistique ne les remplace pas.

**Point de vigilance strict :** la configuration d’une réception vérifie `item.unlockedNow()`. Dans la campagne progressive, un matériau qui n’est pas toujours débloqué doit avoir sa découverte industrielle, indépendamment des découvertes classiques. Importer pour la première fois une matière inconnue peut donc être bloqué au moment de configurer le récepteur. L’exploration/minage reste un moyen existant de la découvrir ; une politique d’import permettant une nouvelle découverte demanderait une validation explicite ultérieure. Les crédits hors écran ne déclenchent pas systématiquement les mêmes événements `produced/unlock` qu’une réception active ; le parcours des objets du noyau par `Control` lors d’une visite doit aussi être pris en compte.

L’ancienne visibilité de plateforme consulte un déblocage classique de la plateforme avancée ; ce détail deviendrait problématique si l’on activait arbitrairement l’ancien modèle dans une planète industrielle stricte. L’audit ne modifie pas cette option et ne recommande pas ce basculement comme correctif économique.

M6D conserve ses fonctions séparées : la sélection d’un nouveau secteur exige toujours le voisinage existant, tandis qu’un transport vise une base déjà établie de la même planète. Les six voisins initiaux restent ceux de M6C/M6D. **Ne jamais appeler `playNewSector`, `IndustrialExpansion.prepare` ou `established` pour simuler une livraison.** Un colis n’est pas une colonisation et ne doit ni augmenter le compteur, ni payer une nouvelle surcharge.

Recherche et colonisation prélèvent également des stocks sectoriels. La recherche industrielle peut actuellement financer un nœud depuis plusieurs bases de sa planète, sans transporter physiquement ces ressources vers une capitale. Une logistique future doit coordonner ces dépenses, pas ajouter un second débit ou modifier cette règle dans son dos. La pause transitoire M6D pendant un départ payant doit être respectée par tout futur calcul périodique.

## 6. Faisabilité du réseau industriel envisagé

Une chaîne simple **A matières premières → B transformation → C composants avancés** est représentable aujourd’hui : bases existantes, contenu civil partagé, destination unique par secteur, plusieurs types de ressources expédiés en lots séparés, réception configurée par matière. La spécialisation et la construction locale sont réalisables.

Les limites concernent surtout les garanties :

1. Une source ne peut pas distribuer finement différentes matières à plusieurs destinations avec des routes indépendantes natives. Une redirection change tout le secteur.
2. Le modèle moderne ne tient pas une liste finie de colis à livrer. La chaîne ne possède donc pas de bilan exact source → transport → destination.
3. Les trois étages ne sont pas simultanément exécutés. Une pénurie ou saturation peut mettre longtemps à se refléter dans les estimations.
4. Les stocks de noyaux sont visibles, les buffers internes et l’encours économique global ne le sont pas.
5. La carte sait déjà dessiner des arcs, afficher stocks, production, imports et exports moyens par minute. Elle n’affiche pas un livre de livraisons, les réservations, pertes, retards, carburant restant ou la fraîcheur des mesures. Certains débits faibles ou négatifs ne sont pas montrés par `displayItems`.

Un futur écran tactile devrait distinguer **stock constaté**, **débit observé**, **prévision hors écran** et **cargaison engagée**. Une liste filtrable par secteur et matière, une fiche de route et une mise à jour d’environ une seconde sont préférables à un grand graphe animé de cent usines. Ces écrans ne sont pas implémentés ici.

## 7. Vérifications et mesures réalisées

### 7.1 Méthode et limites

Une sonde temporaire extérieure au dépôt du jeu a appelé les classes **natives compilées M6D**, dans une application headless et avec ses propres paramètres sous `/workspace/audit-sources/mission7a/probe-data`. Les bases sont des **états synthétiques** : références de sauvegardes factices, informations et moyennes injectées, sans lecture de sauvegarde joueur, sans génération de carte et sans nouvelle machine. `runTurn` est invoqué directement pour observer le calcul ; en jeu normal, il ne tourne pas au menu.

Ce banc mesure le code existant, pas une nouvelle implémentation. Aucune modification du jeu ni compilation Android. Les 385 tests M6D antérieurs ne sont pas présentés comme des tests logistiques nouveaux : ils n’ont pas été relancés pour cet audit.

### 7.2 Résultats fonctionnels contrôlés

| État injecté / appel natif | Résultat observé | Ce que cela établit |
| --- | --- | --- |
| Stock charbon nul ; moyenne charbon négative et graphite +10/s ; un tour | Charbon 0, graphite **1200** | Les moyennes ne sont pas couplées par une disponibilité d’intrants. Il s’agit d’un état statistique synthétique, pas d’une recette ou d’une usine réelle validée |
| Moderne, source à 0 cuivre, export moyen 5/s, destination non visitée | Source 0, destination 0, débit importable annoncé **5/s** | L’annonce n’est pas un stock réservé ; absence de crédit moderne direct hors écran |
| Même état sous ancien modèle, pour comparaison de code uniquement | Destination **+600**, source 0 | La passe de crédit ne vérifie pas directement un stock de colis source. Le correctif conditionnel ne couvre pas une moyenne de production source nulle |
| Ancienne destination déjà à 3990/4000, nouveau tour | Destination **4000** | Excédent non conservé dans une file d’attente native |
| Joueur situé sur la planète progressive ; moyenne cuivre +1/s dans chaque planète testée séparément | Classique 0 ; libre 0 ; progressive **+120** | Groupes vides : seul le périmètre planétaire courant avance |

Ces constats ciblent des propriétés de l’algorithme. Ils ne mesurent pas un réseau d’usines réelles, un envoi physique complet ni une interruption Android pendant un vol. Ce sont des scénarios à compléter en 7B, en campagne réelle et avec les stocks internes.

### 7.3 CPU et sauvegardes : mesures cloud

Environnement : **x86-64, Intel Xeon Platinum 8573C, Java 17.0.21, `-Xms256m -Xmx1024m`**, sans rendu. Planète de **272 secteurs**, **22 objets natifs** ; N bases non actives, 12 matières suivies dans chacun des quatre dictionnaires, une destination par base et caches d’import activés. C’est un profil de travail explicite, pas le pire cas absolu ni une copie de partie S25 Ultra.

Par taille : 150 tours d’échauffement, puis 200 appels chronométrés. Chaque appel représente le calcul natif d’un tour de 120 s, exécuté sans attendre. Les écritures ont été mesurées séparément sur 15 appels `Settings.saveValues` ; elles ne sont pas incluses dans la colonne `runTurn`.

| Bases | `runTurn` médiane / p95, ms | CPU moyen/tour, ms | Allocations thread/tour, Kio | Taille du fichier de paramètres synthétique, octets | Écriture médiane / p95, ms |
| ---: | ---: | ---: | ---: | ---: | ---: |
| 5 | 0,267 / 0,472 | 0,300 | 42,7 | 7193 | 0,449 / 0,916 |
| 20 | 0,676 / 1,194 | 0,764 | 140,3 | 28708 | 0,541 / 1,613 |
| 50 | 1,725 / 3,166 | 1,972 | 351,0 | 71758 | 0,611 / 1,419 |
| 100 | 3,687 / 6,381 | 4,199 | 702,2 | 143508 | 0,752 / 1,246 |

Les allocations sont mesurées par `ThreadMXBean`, pas une estimation de mémoire retenue. Les différences de heap après GC se sont révélées bruitées, dont une valeur négative à 20 bases : **elles ne permettent pas de donner une consommation mémoire fiable par secteur** et ne sont pas utilisées comme conclusion. La taille mesurée des paramètres n’inclut ni des cartes `.msav`, ni un historique de livraisons, ni tous les paramètres d’une véritable partie.

L’écriture est servie par le système de fichiers du cloud et ses caches ; ce n’est pas une mesure de stockage flash Android ni de synchronisation durable. Les tâches de sauvegarde secondaires ne sont pas toutes imputées au thread mesuré. Aucune conclusion de FPS, autonomie ou temps de démarrage du S25 Ultra ne découle de cette table.

### 7.4 Mémoire et charge Android : estimations, pas mesures téléphone

Pour le profil de 12 matières × 4 dictionnaires, les seuls tableaux des fenêtres représentent `12 × 4 × 60 × 4 = 11520 octets/base`, auxquels s’ajoutent notamment 88 octets d’`ItemSeq` et 88 de cache d’import pour 22 objets. **Plancher partiel**, sans en-têtes, tables, chaînes, copies UBJSON, compteurs ou monde actif :

| Bases | Plancher calculé des tableaux précédents | Charge détaillée fictive si 5000 bâtiments/base étaient exécutés à 60 Hz |
| ---: | ---: | ---: |
| 5 | ~57 Kio | 1,5 million d’actualisations de bâtiments/s |
| 20 | ~228 Kio | 6 millions/s |
| 50 | ~571 Kio | 15 millions/s |
| 100 | ~1142 Kio | 30 millions/s |

La dernière colonne est un **calcul de scénario**, pas un benchmark : elle exclut encore convoyage fin, unités, physique et autres frais. Elle illustre pourquoi une centaine de mondes complets permanents est exclue. Charger un seul monde supplémentaire à la demande limite la mémoire de pointe, mais n’annule pas le travail nécessaire pour rattraper tous les secteurs en simulation détaillée.

Dans le natif, les scans de caches d’import peuvent coûter approximativement `B × P`, avec B bases et P secteurs planétaires, plus traitement des matières et sérialisation. Les caches non initialisés ne déclenchent pas tous ces scans : le benchmark les active volontairement. Un index de routes mis à jour lors des changements permettrait à un futur modèle de viser un traitement en `B × K + E`, K matières suivies et E routes, sans rescanner toutes les bases à chaque image.

**Conclusion Android :** 5 à 100 états agrégés sont une piste raisonnable ; aucune capacité garantie de 100 usines complètes n’a été démontrée sur téléphone. Il faut profiler sur S25 Ultra, à chaud, avec un monde actif représentatif, sauvegardes, rotation portrait/paysage et application mise en arrière-plan. Les tâches périodiques doivent avoir un budget et pouvoir être réparties entre images, sans limiter artificiellement le stockage.

## 8. Architectures envisageables

| Architecture | Avantages | Inconvénients / précision | Complexité | Coût Android envisagé | Sauvegardes / risques de ressources |
| --- | --- | --- | --- | --- | --- |
| **A. Natif conservé, diagnostic enrichi** | Réutilisation maximale, progression et contenus inchangés ; carte déjà exploitable | Débits statistiques ; aucune garantie de conservation ou d’intrants. N’améliore pas l’économie sous-jacente | Faible à moyenne | Proche du natif mesuré ; UI à limiter aux éléments affichés | Meilleure compatibilité immédiate ; erreurs natives, pertes par saturation et absence de reçus restent présentes |
| **B. Stocks et transports agrégés conservatifs** | Un monde actif, états légers, lots réels et débits bornés ; `ItemSeq`, secteurs et interfaces réutilisables | Demande un modèle explicite de ce qui appartient au noyau, aux buffers et au transit ; des capacités issues d’observations ne suffisent pas à simuler les recettes | Moyenne à élevée | Traitement périodique des stocks/routes ; ordre visé `B×K+E`, sans simulation permanente des bâtiments. Temps S25 non mesuré | Métadonnées versionnées et migration contrôlée ; un seul producteur/créditeur par domaine indispensable. Risques de double crédit ou débit entre `.msav`, paramètres et journal si mal coordonnés |
| **C. Modèle hybride B + groupes de recettes** | Production hors écran liée aux matières, liquides et budgets d’énergie ; précision économique nettement supérieure aux moyennes libres | Agrégation des machines, buffers, cycles, énergie et rendements difficile ; ne reproduit pas chaque convoyeur ou incident local | Élevée à très élevée | Coût lié aux groupes de recettes et routes, périodique et borné ; adapté à 20–100 secteurs sous réserve de mesures | État supplémentaire et captures de sortie/entrée du monde ; risques de double production et de stocks internes comptés deux fois ; migration facultative nécessaire |
| **D. Rejeu détaillé à la demande, un monde hors écran à la fois** | Réutilise davantage les comportements précis de blocs dans un monde chargé | `Vars`, `Groups`, événements et équipes sont globaux ; isoler un autre monde n’est pas un simple `new World()`. Rattrapage coûteux et précision dépendante de l’ordre de visite | Très élevée | Pics CPU, chargements et écritures `.msav` importants ; mémoire de pointe d’au moins un autre monde. Pas de coût chiffré fiable sans scènes | Format `.msav` réutilisable en théorie, mais risque fort d’événements de recherche, sauvegardes et crédits répétés pendant le rejeu. Non recommandé pour ce projet Android |

Une simulation détaillée permanente de dizaines ou centaines de mondes n’est pas une option retenue. Les modèles B et C ne doivent pas laisser les anciennes extrapolations `Universe` produire les mêmes ressources en parallèle ; un nouveau registre ajouté à côté du natif, sans désactivation ciblée de l’ancien calcul, serait incorrect.

## 9. Architecture recommandée et conditions de validation

### 9.1 Choix proposé

**A pour rendre le système actuel observable, puis B comme socle de transport, et C progressivement pour la production.** Ce chemin garde les actifs, recettes et mécanismes de construction de Mindustry, borne la charge mobile et traite d’abord la conservation plutôt que de masquer les incohérences derrière une nouvelle interface.

Il ne s’agit pas de remplacer immédiatement les productions des campagnes M6D. L’activation d’un nouveau moteur demanderait une option explicite, un schéma versionné, une sauvegarde complète et une validation des données migrées. Une ancienne partie ne doit jamais être convertie silencieusement. Le sort des stocks internes et de l’encours non représenté doit être spécifié avant cette conversion. Les campagnes classique et industrielle libre doivent conserver leur moteur actuel.

Deux choix de distribution sont à distinguer : une **migration consentie de la campagne progressive**, avec export complet et retour par restauration cohérente, ou un **vrai profil expérimental séparé** possédant ses propres secteurs, recherches, compteur M6D et journal. Un simple bouton qui partagerait les mêmes secteurs ne serait pas un profil indépendant. La réversibilité ne doit pas être annoncée comme un changement de commutateur instantané après des livraisons ; elle peut nécessiter de restaurer l’archive complète d’avant migration. Le banc de développement peut rester dans un répertoire de données isolé sans ajouter de planète au jeu livré.

### 9.2 Principes à décider avant implémentation

1. **Identité complète :** `(planète, secteur, route, transaction)`, jamais l’identifiant numérique de secteur seul. Les trois secteurs 170 sont distincts.
2. **Autorité unique :** définir quand le monde actif ou l’état agrégé possède chaque stock. Une visite transfère cette autorité, elle ne crée pas une deuxième copie disponible.
3. **Conservation :** pour chaque matière, `stock + buffers + transit + pertes explicitement comptées = initial + production - consommation`. Les conversions suivent les recettes ; un simple total toutes matières confondues ne suffit pas.
4. **Transport borné :** partir d’une quantité effectivement disponible, réserver une destination ou une file d’attente, conserver les colis engagés. Une destination pleine ne doit pas faire disparaître un excédent à travers `addItems` sans règle annoncée.
5. **Journal et reprise :** identifiant unique, phases et reçus idempotents ; coûts et livraisons appliqués une seule fois. Coordonner les écritures avec recherche et M6D. Ne pas annoncer une atomicité multifichier que les paramètres natifs n’offrent pas ; tester sauvegarde partielle, restauration cohérente et interruption à chaque étape.
6. **Horloge explicite :** conserver d’abord le temps de campagne natif. Une production pendant fermeture du téléphone serait une nouvelle règle économique distincte, non impliquée par « hors écran ».
7. **Intrants réels :** B sécurise les transferts, mais ne rend pas automatiquement les vieilles prévisions de production conservatrices. C doit borner les recettes par les stocks et budgets nécessaires. Le pétrole et l’eau natifs ne sont pas à ignorer lorsqu’un secteur devient inactif.
8. **Validation de route :** base source/destination valide, même planète, recherche et découverte applicables. Ne pas confondre la portée de transport avec les règles de voisinage de colonisation.
9. **Stockage et performance :** métadonnées compactes, index de routes, calcul périodique ou à la demande avec cache, écritures regroupées. Pas de sauvegarde de cent mondes à chaque tick ni de nouveau plafond artificiel de stock.

### 9.3 Ce que l’on peut réutiliser

Les blocs, leurs recettes et visuels ; les secteurs et leurs espaces de sauvegarde ; `Item`/`ItemStack`/`ItemSeq` ; le sélecteur de bases, les arcs et fiches de la carte ; les objectifs et recherches industriels ; la capacité native des noyaux ; les événements observés pour calibrer les capacités ; le principe de journal idempotent de M6D, **à adapter et tester**, pas à considérer comme une garantie logistique déjà fournie.

Il faudra en revanche distinguer débit prévu et reçu réel, représenter les cargaisons engagées, traiter les buffers et empêcher le double calcul actif/hors écran. Aucun de ces ajouts n’est réalisé dans cette mission.

## 10. Feuille de route proposée

Chaque étape reste à autoriser ; ce rapport ne lance pas son implémentation.

| Mission | Périmètre proposé | Validation avant étape suivante |
| --- | --- | --- |
| **7B — diagnostic et preuves en campagne** | Fiches de flux en lecture seule, fraîcheur des moyennes, mode ancien/moderne, inventaires observables ; scénarios A→B→C avec blocs natifs, sans changer l’économie | Tester source active/destination inactive, inverse et toutes deux inactives ; pénurie de pétrole/eau/charbon, stockage plein, redirection multimatériau, disparition d’une base, pause, reprise, changement de planète. Mesurer S25 à 5/20/50/100 métadonnées et une usine active représentative ; aucun nouveau contenu, sonde ou verrou |
| **7C — transport conservatif pilote, après choix B** | Lots finis et reçus idempotents, d’abord objets et routes simples avec blocs existants ; activation expérimentale explicite ; coexistence préservée des anciens états | Bilans exacts, absence de double débit/crédit, destination pleine, annulation/reconfiguration, recherches et frais M6D concurrents, interruption avant/après chaque écriture, visite source/destination, export/import complet et aucune régression classique/libre |
| **7D — production agrégée contrainte, première tranche C** | Quelques chaînes natives documentées, par exemple graphite et silicium, avec captures de stocks/buffers et budgets nécessaires ; aucune modification des recettes partagées | Comparer la même usine active et inactive sur une durée contrôlée ; intrant nul = pas de sortie correspondante ; énergie/liquides/carburant bornés, capacités conservées, résultat indépendant de l’ordre de visite dans les limites déclarées |
| **7E et suivantes — réseau plus riche et optimisation** | Routage par matière, priorités et spécialisation, puis extension prudente des recettes ; travail mobile et sauvegardes profilé | Boucles, embranchements, équité entre destinataires, engorgements, 100 secteurs agrégés, chauffe/autonomie, interruption Android et migration réversible. Aucun monde complet permanent supplémentaire |

Les objectifs de performance seront des **budgets à valider sur appareil**, pas des extrapolations du Xeon : mesurer p95/p99 des lots, latence tactile, mémoire retenue, volume écrit et pointe de sauvegarde avec rendu actif. Le fractionnement des calculs ne doit pas modifier l’ordre économique ni permettre de financer deux dépenses avec le même stock.

## 11. Limites de l’audit et décision attendue

Le code, les défauts neufs des trois campagnes, sept cas de calcul et quatre tailles synthétiques ont été examinés. Aucune installation Android nouvelle ni campagne de transports réels sur S25 Ultra n’a été effectuée. Les pertes/duplications lors d’interruptions de vol et les écarts d’une chaîne réelle sont des **risques identifiés à tester**, pas des taux mesurés. Les performances d’un futur moteur B/C restent théoriques tant qu’il n’existe pas.

**Décision proposée : valider le parcours A → B → C**, avec 7B d’abord en observation seule. La précision économique doit venir d’un propriétaire unique des stocks et de bilans contraints, plutôt que de la mise en mémoire de nombreux mondes ou de l’augmentation aveugle des moyennes natives.

## Annexe — reproductibilité des mesures

La sonde extérieure au dépôt utilise les classes du JAR desktop M6D et le backend headless Arc correspondant, avec uniquement des états synthétiques. Son SHA-256 est `b13e5086474053b4ddf4a4f579304805e7000948da5acdddf1d1c3e8f211e4e4`. Le code ci-dessous documente le banc d’audit ; **ce n’est ni un mod à installer ni une fonctionnalité livrée dans le jeu**. À exécuter seulement dans un espace cloud de test isolé, avec les classes M6D déjà disponibles, jamais sur le répertoire de données du joueur.

```sh
# Hors du dépôt du jeu ; le chemin de données est dédié au banc.
/workspace/toolchains/jdk17/bin/javac \
  -cp /workspace/Mindustry/desktop/build/libs/Mindustry.jar:/workspace/Arc/backends/backend-headless/build/libs/backend-headless-1.0.jar \
  /workspace/audit-sources/mission7a/AuditProbe.java
MINDUSTRY_DATA_DIR=/workspace/audit-sources/mission7a/probe-data/init \
  /workspace/toolchains/jdk17/bin/java -Xms256m -Xmx1024m \
  -cp /workspace/audit-sources/mission7a:/workspace/Mindustry/desktop/build/libs/Mindustry.jar:/workspace/Arc/backends/backend-headless/build/libs/backend-headless-1.0.jar \
  AuditProbe
# Répertoire courant : /workspace/Mindustry/core/assets
```

<details>
<summary>Code du banc headless utilisé pour l’audit</summary>

```java
import arc.*;
import arc.assets.*;
import arc.backend.headless.*;
import arc.files.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.game.SectorInfo.*;
import mindustry.io.*;
import mindustry.net.*;
import mindustry.type.*;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import static mindustry.Vars.*;

/** Read-only audit of game code. All writable data and synthetic states are external to the repository and player saves. */
public class AuditProbe{
 static Saves slots;
 static Fi root=new Fi("/workspace/audit-sources/mission7a/probe-data");
 static com.sun.management.ThreadMXBean meter=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
 static void settings(String name){Core.settings=new Settings();Core.settings.setJson(JsonIO.json);Core.settings.setDataDirectory(root.child(name));Core.settings.load();}
 static void reset(Planet p){
  for(Planet planet:content.planets())for(Sector s:planet.sectors){s.save=null;s.info=new SectorInfo();s.info.hasCore=false;}
  state.rules=new Rules();state.rules.planet=p;state.set(GameState.State.menu);
 }
 static Sector base(Planet p,int index){Sector s=p.sectors.get(index);s.save=slots.new SaveSlot(root.child("synthetic-"+index+".msav"));s.info.hasCore=true;s.info.waves=s.info.attack=false;s.info.storageCapacity=4000;return s;}
 static void rate(arc.struct.ObjectMap<Item,ExportStat> m,Item i,float v){ExportStat x=new ExportStat();x.mean=v;m.put(i,x);}
 static void scenarios(){
  Planet p=Planets.industrialSerpuloProgressive;settings("cases");reset(p);p.campaignRules.legacyLaunchPads=false;
  Sector a=base(p,0),b=base(p,1);rate(b.info.production,Items.coal,-5);rate(b.info.production,Items.graphite,10);
  universe.runTurn();System.out.println("CASE empty_input coal="+b.info.items.get(Items.coal)+" graphite="+b.info.items.get(Items.graphite));
  reset(p);a=base(p,0);b=base(p,1);a.info.destination=b;rate(a.info.export,Items.copper,5);
  universe.runTurn();System.out.println("CASE modern_offscreen sourceCu="+a.info.items.get(Items.copper)+" destinationCu="+b.info.items.get(Items.copper)+" advertised="+b.info.getImportRate(p,Items.copper));
  reset(p);p.campaignRules.legacyLaunchPads=true;a=base(p,0);b=base(p,1);a.info.destination=b;rate(a.info.export,Items.copper,5);
  universe.runTurn();System.out.println("CASE legacy_empty_source sourceCu="+a.info.items.get(Items.copper)+" destinationCu="+b.info.items.get(Items.copper));
  b.info.items.set(Items.copper,3990);universe.runTurn();System.out.println("CASE legacy_capacity destinationCu="+b.info.items.get(Items.copper));
  p.campaignRules.legacyLaunchPads=false;
  for(Planet other:new Planet[]{Planets.serpulo,Planets.industrialSerpulo,Planets.industrialSerpuloProgressive}){
   reset(p);a=base(other,0);rate(a.info.production,Items.copper,1);universe.runTurn();
   System.out.println("CASE planet_scope current="+p.name+" tested="+other.name+" Cu="+a.info.items.get(Items.copper)+" updateGroup="+other.updateGroup.size);
  }
 }
 static long used(){Runtime r=Runtime.getRuntime();return r.totalMemory()-r.freeMemory();}
 static void gc()throws Exception{System.gc();Thread.sleep(80);}
 static double quant(long[] v,double q){long[] c=v.clone();Arrays.sort(c);return c[(int)Math.floor((c.length-1)*q)]/1e6;}
 static void bench(int n)throws Exception{
  Planet p=Planets.industrialSerpuloProgressive;settings("bench-"+n);reset(p);gc();long before=used();
  for(int x=0;x<n;x++){
   Sector s=base(p,x);s.info.destination=p.sectors.get((x+1)%n);s.info.importRateCache=new float[content.items().size];
   for(int k=0;k<12;k++){Item i=content.item(k);rate(s.info.production,i,1);rate(s.info.rawProduction,i,2);rate(s.info.export,i,0.5f);rate(s.info.imports,i,0.5f);s.info.items.set(i,1000);}
  }
  for(int i=0;i<150;i++)universe.runTurn();gc();long retained=used()-before;
  long[] wall=new long[200];long tid=Thread.currentThread().getId(),alloc=meter.getThreadAllocatedBytes(tid),cpu=meter.getCurrentThreadCpuTime();
  for(int i=0;i<wall.length;i++){long t=System.nanoTime();universe.runTurn();wall[i]=System.nanoTime()-t;}
  long bytes=meter.getThreadAllocatedBytes(tid)-alloc;cpu=meter.getCurrentThreadCpuTime()-cpu;
  long[] writes=new long[15];for(int i=0;i<writes.length;i++){long t=System.nanoTime();Core.settings.saveValues();writes[i]=System.nanoTime()-t;}
  System.out.printf(java.util.Locale.ROOT,"BENCH N=%d P=%d items=%d turn_p50_ms=%.3f turn_p95_ms=%.3f cpu_ms=%.3f allocation_KiB=%.1f live_delta_KiB=%.1f settings_bytes=%d write_p50_ms=%.3f write_p95_ms=%.3f%n",n,p.sectors.size,content.items().size,quant(wall,.5),quant(wall,.95),cpu/1e6/wall.length,bytes/1024.0/wall.length,retained/1024.0,Core.settings.getSettingsFile().length(),quant(writes,.5),quant(writes,.95));
 }
 public static void main(String[]args){
  new HeadlessApplication(new ApplicationListener(){public void init(){try{
   Log.useColors=false;Core.settings.setDataDirectory(root.child("init"));headless=true;net=new Net(null);tree=new FileTree();Vars.init();
   content.createBaseContent();logic=new Logic();netServer=new NetServer();content.init();Core.assets=new AssetManager();slots=new Saves();
   System.out.println("META java="+System.getProperty("java.version")+" arch="+System.getProperty("os.arch")+" turn_ticks="+turnDuration);
   for(Planet p:new Planet[]{Planets.serpulo,Planets.industrialSerpulo,Planets.industrialSerpuloProgressive}){
    System.out.println("PLANET "+p.name+" legacy="+p.campaignRules.legacyLaunchPads+" allowLegacy="+p.allowLegacyLaunchPads+" updateGroup="+p.updateGroup.size);
   }
   var tr=IndustrialResearch.tree(Planets.industrialSerpuloProgressive);
   for(var block:new mindustry.world.Block[]{Blocks.launchPad,Blocks.advancedLaunchPad,Blocks.landingPad}){
    var node=tr.nodes.get(block);System.out.println("RESEARCH "+block.name+" node="+(node!=null)+" parent="+(node==null||node.parent==null?"none":node.parent.content.name)+" objectives="+(node==null?"none":node.objectives));
   }
   scenarios();for(int n:new int[]{5,20,50,100})bench(n);
   System.exit(0);
  }catch(Throwable failure){failure.printStackTrace();System.exit(1);}}});
 }
}
```

</details>
