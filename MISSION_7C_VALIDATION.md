# M7C — transports intersectoriels conservatifs

Statut : **implémentation et validation en cours**, branche `feature/m7c-conservative-transports`, base H0 fusionnée `c207f5bbb`. PR vers `industrial-android`, sans fusion automatique. M7D non commencée.

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

## Validation — à compléter avec les résultats exécutés

Les tests utilisent des fichiers et paramètres temporaires isolés. Aucun fichier du téléphone n’est disponible ou modifié. Contrôles prévus : stocks insuffisants/vides, plusieurs rampes, saturation/eau, reprise avant/après débit et crédit, répétition, sauvegarde M7B, conservation totale, isolation, suppression du double chemin natif et référence de duplication M7B ; mesures serveur 5/20/50/100 colonies ; interface portrait cloud ; suite complète ; APK GitHub Actions exact SHA et signature M7B.

## Android — protocole utilisateur

1. Avant essai : Paramètres → Données → exporter **toutes les données**. Conserver ce ZIP hors du répertoire de l’application. Ne pas désinstaller, effacer les données ou supprimer le journal.
2. Installer l’APK M7C par-dessus M7B. Vérifier la version puis ouvrir la campagne **industrielle progressive** existante.
3. Choisir deux colonies voisines déjà accessibles, équiper la source d’une rampe avancée chargée avec 100 cuivre (électricité/pétrole natifs), définir sa destination ; équiper la destination d’une réception configurée cuivre et d’eau.
4. Consulter carte → diagnostic : vérifier le lot préparé puis expédié confirmé après sauvegarde/changement de secteur. Vérifier l’identifiant/destination et les moyennes distinctes.
5. Visiter la destination : au plus 100 objets reçus par manifeste. Tester sortie pleine puis libérée ; manque d’eau puis réapprovisionnement ; plusieurs rampes. Ne pas utiliser les variations de noyau estimées comme preuve de transport.
6. Passer par un troisième secteur, attendre des tours puis revenir : tampon hors écran borné, lots excédentaires en attente. Fermer normalement/reprendre aux différents états ; pas de nouvelle production application fermée.
7. Si une vieille arrivée M7B est suspendue, vérifier le message avant de confirmer l’écartement de son animation. En cas d’ambiguïté de journal, arrêter les essais et conserver/exporter les fichiers ; ne rien réinitialiser.
8. Vérifier séparément classique et industrielle libre, recherches/coûts M6D et stocks existants. Signaler les résultats sur S25 ; ils ne sont pas remplacés par les essais cloud.

## Limites / suite

Pas de routage multidestination, centre logistique, consommation agrégée des recettes ou nouveaux contenus. Aucun M7D sans validation explicite. Validation physique S25 attendue après livraison ; PR non fusionnée.
