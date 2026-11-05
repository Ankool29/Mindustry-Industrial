# Compiler et installer depuis un smartphone Android

Cette branche dérive de Mindustry **v160.7**, commit amont `17012d61d9e4c3cbd9820d87e81c24495486ac53`. La Mission 6C ajoute une campagne progressive séparée à la **campagne industrielle planétaire séparée**, en plus du mode Industriel des parties personnalisées. Les campagnes classiques restent disponibles. Elle n’est pas une version officielle de Mindustry.

## Lancer une compilation sans ordinateur

1. Ouvrir le dépôt GitHub dans Chrome ou Samsung Internet et se connecter à GitHub.
2. Choisir **Actions**, puis **Android cloud APK**. Si nécessaire, utiliser le menu du navigateur **Version pour ordinateur**.
3. Choisir **Run workflow**, sélectionner la branche `industrial-android`, puis confirmer **Run workflow**.
4. Attendre la coche verte du workflow. Un changement poussé sur `industrial-android` lance aussi la compilation automatiquement.
5. Ouvrir l’exécution terminée et descendre à **Artifacts**.
6. Télécharger **Mindustry-Industrial-Android**. GitHub demande normalement d’être connecté pour télécharger un artefact. L’autre artefact, **Android-build-evidence**, contient les logs et les preuves techniques.

Aucune compilation ni application de développement n’est nécessaire sur le téléphone. Les artefacts sont conservés 30 jours ; après expiration, relancer le workflow. Les limites et éventuels quotas de GitHub Actions restent ceux du compte GitHub.

## Installer sur le Galaxy S25 Ultra

1. Dans **Mes fichiers → Téléchargements**, ouvrir le ZIP téléchargé puis choisir **Extraire**.
2. Ouvrir `Mindustry-Industrial-v160.7-m7b.apk`.
3. Si Android le demande, autoriser temporairement **Installation d’applis inconnues** pour l’application qui ouvre le fichier, par exemple **Mes fichiers**, puis revenir à l’APK et toucher **Installer**.
4. Sur certaines versions One UI, **Paramètres → Sécurité et confidentialité → Bloqueur automatique** peut empêcher cette installation. Si c’est le cas, le désactiver uniquement le temps d’installer cet APK de confiance, puis le réactiver. Les libellés peuvent varier selon la version de One UI.
5. Lancer **Mindustry Industrial**. Révoquer ensuite l’autorisation d’installation d’applications inconnues si elle avait été activée.

## Mise à jour de M3/M4/M5/M6B et sauvegardes

M7B conserve l’identifiant `io.anuke.mindustry.industrial.m3` et la signature de M3, avec un code de version Android porté à 9. Installer l’APK par-dessus M3, M4, M5, M6B, M6C ou M6D : **ne pas désinstaller l’application**. Les parties existantes gardent leur mode et leurs fichiers. Une exportation de sauvegarde depuis les paramètres reste recommandée avant une mise à jour de test.

L’application coexiste avec Mindustry officiel et Mission 2 (`io.anuke.mindustry.industrial`). Pour transférer leurs données, exporter puis importer l’archive depuis les paramètres du jeu. Une partie importée ne devient pas industrielle automatiquement.

## Démarrer la campagne industrielle

1. Ouvrir **Jouer → Campagne**. Au premier lancement, choisir **Campagne industrielle**, puis valider. Si une campagne existe déjà, sélectionner **Serpulo industrielle** dans la liste des planètes de la carte planétaire.
2. Sélectionner le secteur de départ **170**, puis **Jouer**. Il reçoit le stock normal limité de **100 cuivre**. Extraire et produire les ressources avec les machines existantes.
3. Revenir à la carte planétaire depuis le menu de campagne. Après établissement d’une base, les secteurs adjacents deviennent accessibles sans conquête ni amélioration militaire du noyau.
4. Sélectionner un secteur voisin : le chargement de déploiement et son coût utilisent l’interface d’origine. Il faut produire les ressources nécessaires avant de confirmer le départ. Aucun second stock gratuit n’est ajouté.
5. Revenir à un secteur possédant une base pour poursuivre son usine. Les secteurs et stocks sont sauvegardés séparément de Serpulo classique.

Cette planète réutilise le générateur de Serpulo sans modifier son algorithme. Elle supprime les ennemis, points d’apparition, vagues, invasions et fins de partie militaires. Les coûts, recettes, besoins électriques et ressources finies du noyau restent normaux. Les gisements gardent le comportement infini de Mindustry.

Les machines Serpulo sont disponibles sans recherche dans cette première campagne. Construire et produire ici ne débloque pas la recherche classique. Le bouton **Machines** ouvre le catalogue industriel filtré et rappelle cette limite ; un arbre industriel progressif reste à développer. Pour effectuer des recherches classiques, rejoindre une campagne classique. Les contenus exclusivement militaires sont masqués dans les menus industriels, sans être supprimés du moteur ni des sauvegardes.

Le transport utilise uniquement les chargements et systèmes originaux : aucune fusée personnalisée ni consommation de carburant supplémentaire. La production des secteurs absents conserve la simulation statistique de Mindustry, sans simulation économique nouvelle. Le mode Industriel sur carte personnalisée reste disponible.

## Interface industrielle (M5)

Les tourelles offensives, murs défensifs, mines, boucliers et équipements exclusivement militaires sont masqués dans la construction, les configurations, les schémas et la base de données en partie industrielle. Les drones capables de construire, extraire ou transporter sont conservés avec leurs fabriques/reconstructeurs ; les nouvelles fabriques sélectionnent une option civile par défaut ; leurs options de combat pur et leurs statistiques d’armes/munitions sont masquées. Les convoyeurs et infrastructures de charges utiles restent disponibles.

Les matériaux à usage mixte restent visibles : graphite, silicium, thorium, pyratite et composé explosif servent aussi aux recettes, à la construction ou à l’énergie. Ce filtre ne change aucune recette, coût, recherche débloquée, progression de secteur ni production hors écran. Un bâtiment militaire déjà construit dans une ancienne partie n’est pas supprimé. Les menus classiques retrouvent leur contenu militaire habituel après changement de mode.

Dans **Campagne → Serpulo industrielle → Machines**, consulter ou rechercher les machines, matières et drones industriels. Ce catalogue est consultatif : M4 ne possédait pas d’arbre industriel et M5 n’ajoute pas de nouvelle progression. L’arbre classique reste intact et accessible dans la campagne classique.

## État de recherche indépendant (M6B)

L’état industriel est enregistré dans les données de l’application, sous une clé distincte `industrial-research-v1-industrial-serpulo`. Il contient les technologies étudiées, les découvertes de matières et les contributions de recherche partielles. Les noms de contenus servent d’identifiants stables ; les clés de recherche classiques restent inchangées. Cette donnée suit l’export/import natif des données du jeu ; un seul fichier de secteur exporté ne contient pas à lui seul tout le profil de recherche.

Le catalogue **Machines** est conservé, sans nouveau bouton de paiement. En M6B, toutes les technologies restaient utilisables ; M6C maintient cet accès sur la planète industrielle libre, y compris les anciennes M4/M5/M6B. La découverte d’une matière ne déclenche aucun achat. Il n’y a ni coût supplémentaire de colonisation, ni verrou de secteur, ni modification de production hors écran.

Un arbre technique détaché réutilise les nœuds et contenus natifs sans remplacer `content.techNode` ni entrer dans les listes de déblocage automatique classiques. Les objectifs de découverte, de recherche et de nombre de bases industrielles sont préparés pour la suite. La fonction de paiement, réservée au futur écran de recherche et testée séparément, ne peut puiser que dans les bases de la planète industrielle. M6C propose désormais le graphe de recherche sur la nouvelle planète progressive, en conservant le catalogue.

## Tester la progression stricte sans risquer les anciennes parties (M6C)

1. Installer la mise à jour par-dessus l’application existante, sans désinstallation ni suppression de données. Une exportation complète des données reste conseillée avant toute mise à jour de test.
2. Ouvrir **Jouer → Campagne**, puis sélectionner **Serpulo industrielle — progression** dans la liste des planètes. Au premier choix de campagne, l’option **Campagne industrielle — progression** mène à cette même planète.
3. Sélectionner le secteur **170**, puis **Décoller**. Le dialogue précise que cette campagne est indépendante et qu’aucune ancienne campagne ne sera réinitialisée. Confirmer seulement si l’on souhaite commencer cette nouvelle campagne.
4. Le noyau reçoit 100 cuivre. L’unité initiale, la foreuse mécanique et le convoyeur de base sont disponibles. Extraire cuivre/plomb, puis charbon et sable ; les autres machines se recherchent progressivement. Les découvertes sont enregistrées à la production ou à l’entrée en stock selon les mécanismes natifs.
5. Ouvrir **Recherche industrielle** depuis le HUD ou la carte planétaire. Le graphe natif se déplace et se zoome au toucher. **Parcourir** propose une liste avec recherche textuelle ; toucher une technologie ouvre ses conditions, son parent, ses débouchés, ses coûts, les ressources déjà versées et le stock de cette seule campagne.
6. Toucher **Contribuer** pour verser les ressources disponibles. Un paiement incomplet est conservé. Une technologie financée devient constructible ; les plans, schémas et fabriques ne permettent pas de contourner les recherches.
7. Le bouton **Catalogue** conserve la consultation des machines, sans achat ni construction gratuite. Revenir à **Serpulo industrielle** (sans « progression ») pour retrouver les anciennes bases en accès libre. Revenir à **Serpulo** pour la campagne classique.

Les secteurs stricts portent le préfixe `sector-industrial-serpulo-progressive-` et leur recherche la clé `industrial-research-v1-industrial-serpulo-progressive`. Les secteurs libres et leur clé M6B restent inchangés. Les stocks ne sont pas partagés. Les machines et recettes sont les mêmes objets natifs ; leurs contenus ne sont pas dupliqués.

**Une seule campagne stricte est disponible dans cette version.** Choisir de nouveau cette planète reprend ses bases et recherches ; cela ne crée pas un profil supplémentaire. Aucun bouton de réinitialisation n’a été ajouté. Ne pas renommer/copier les fichiers d’une campagne vers l’autre. Pour recommencer ultérieurement avec plusieurs profils, une architecture dédiée sera nécessaire ; voir `docs/MISSION_6C_ARCHITECTURE.md`.

Aucun frais supplémentaire de colonisation ni verrou géographique n’est ajouté. Les nouveaux voisins restent accessibles selon les règles industrielles existantes avec les chargements natifs. L’accès aux minerais dépend du terrain : l’exploration reste possible avec le noyau de base. L’arbre de recherche est organisé sans dépendances aux conquêtes ; les coûts de recherche natifs sont conservés et peuvent demander plusieurs versements.

## Signature et mises à jour de test

La clé de test M3 est réutilisée depuis le cache GitHub. Son empreinte SHA-256 est `61254fd2280bf34aa64bd06405cf0e2bc9b2d8fe7df4cc615c39983bcdfd8086`. Le workflow refuse désormais de compiler si cette clé est absente ou différente : il ne la remplace pas automatiquement. Le cache peut expirer ; une clé durable protégée dans GitHub Secrets sera nécessaire avant une distribution de production. Ne pas désinstaller l’application pour contourner une erreur de signature.

## Vérification et diagnostic

- Une exécution verte doit avoir compilé `android:assembleDebug`, vérifié la signature, la présence des bibliothèques natives ARM64 et celle des sprites, puis publié l’artefact.
- Le fichier `SHA256SUMS.txt` donne le SHA-256 de l’APK. `BUILD-INFO.txt` indique le commit exact et le lien de l’exécution.
- Une compilation réussie ne prouve pas encore le lancement sur un S25 Ultra. L’essai physique ci-dessus doit être confirmé séparément.
- En cas d’échec, ouvrir l’étape rouge ou télécharger `Android-build-evidence`, puis transmettre le lien de l’exécution à Codex. Aucun mot de passe ni token GitHub ne doit être envoyé dans le chat.

## Versions et reproductibilité

- JDK : **Temurin 17**.
- Android : plateforme **36**, Build Tools **36.0.0**, SDK minimal déclaré **21**.
- Arc : commit `7445105cd2683daf5b9ac7033ebb07265d3e814d`, correspondant au `archash` amont.
- Gradle : **8.14.3**, distribution vérifiée par SHA-256. Android Gradle Plugin : **8.10.1**, compatible avec la plateforme Android 36. Les valeurs réellement utilisées sont archivées par le workflow. L’amont v160.7 déclare Gradle 9.3.1 et AGP 8.2.2 ; un essai réel dans le cloud a atteint R8 puis échoué sur un fichier intermédiaire `base.jar` manquant. Les deux versions d’outillage sont ajustées ; le code du jeu reste inchangé.
- Le build utilise les sources Arc à la révision verrouillée plutôt que sa branche `master`. Les autres dépendances restent celles de Mindustry. L’accès à Google Maven, Maven Central et JitPack reste nécessaire.
- `CI=false` évite la configuration de la clé de publication privée de l’amont. Le job construit une APK debug, pas une release officielle.
- `tools:pack` est exécuté avant l’assemblage Android : les atlas doivent exister avant que le plugin Android ne collecte les assets d’un checkout neuf.

Les workflows de publication et maintenance propres au dépôt officiel sont remplacés sur cette branche par le seul workflow Android. Aucun secret ni droit d’écriture sur les dépôts amont n’est requis.

La branche par défaut `master` contient un lanceur manuel qui appelle le workflow de `industrial-android`. Le bouton **Run workflow** fonctionne ainsi sans réglage administratif du dépôt ; un lancement manuel compile toujours la branche de travail v160.7. Les constructions déclenchées par un push compilent le commit exact de ce push.

## Licence et sources

Le fork reste sous **GNU GPL v3**, avec conservation du fichier `LICENSE` et de l’historique amont. Le ZIP d’artefact contient le texte de licence et une archive des sources du commit du fork correspondant à l’APK. Il contient aussi les sources Arc de la révision utilisée et sa licence Apache 2.0. Le workflow et cette procédure font partie des sources ; `BUILD-INFO.txt` référence aussi la révision Arc utilisée. Conserver et respecter les licences propres aux dépendances et assets. Avant diffusion durable au-delà des essais, compléter l’inventaire des notices et la fourniture des sources des dépendances embarquées selon leurs licences.

## Jouer en mode Industriel (Mission 3)

1. Depuis le menu principal, ouvrir **Partie personnalisée**, puis choisir une carte existante disposant d’un noyau joueur et de minerais.
2. Dans les modes, toucher **Industriel**, puis **Jouer**. Le panneau défile verticalement si nécessaire en paysage.
3. Le statut **Industriel — sans attaques** apparaît dans le HUD. Construire les foreuses, convoyeurs, machines, conduites, générateurs et stockage comme dans le jeu d’origine.

Les coûts de construction, intrants et besoins électriques restent actifs. Les stocks de départ sont ceux de la carte et restent limités. Les gisements utilisent encore le fonctionnement infini d’origine : leur épuisement et la progression technologique seront ajoutés ultérieurement.

Le mode désactive les vagues, les conditions militaires de victoire/défaite et les objectifs de carte ; il retire les bâtiments/unités ennemis de la copie chargée du monde. Les processeurs de monde sont désactivés pour empêcher les scripts de carte de réintroduire des attaques ; les processeurs logiques ordinaires restent disponibles. Le fichier de carte original n’est pas modifié. Le bouton de règles personnalisées est désactivé pour ce mode afin de conserver ces garanties.

Sauvegarder et reprendre normalement : le mode est enregistré avec la partie. Les anciennes sauvegardes restent dans leur mode d’origine ; elles ne sont pas converties automatiquement. Le multijoueur avec une version officielle ou un ancien APK du fork n’est pas garanti : utiliser la même version du fork sur les participants. Les mods modifiant les règles ou le moteur ne font pas partie de la validation de cette première version.

## Mission 6D — économie de colonisation

La surcharge concerne uniquement **Serpulo industrielle — progression**. Les choix de départ et le voisinage M6C restent inchangés (six voisins autour de 170 dans cette version). Les deux premières implantations économiques sont sans surcharge ; la troisième demande 250 cuivre/200 plomb, la quatrième 500/400, la cinquième 750/600, puis les paliers plafonnent à 2000/1600. Les coûts natifs du noyau/schéma et du chargement s’ajoutent et sont détaillés avant confirmation. Le chargement maximal réserve d’abord le schéma et la surcharge.

Les parties strictes M6C sont conservées et démarrent cette nouvelle économie au compteur 1, quel que soit leur nombre de bases existantes : aucun rattrapage de frais. Une visite est gratuite ; réimplanter après abandon compte comme une implantation supplémentaire et ne diminue jamais le compteur. Les deux autres campagnes ne changent pas.

Annuler avant départ ne prélève rien. Le paiement intervient après sauvegarde réussie de la destination ; un journal reprend une opération interrompue. Il ne faut pas restaurer isolément `settings.bin` ou un secteur d’une autre date : utiliser une exportation complète pour conserver la cohérence entre les secteurs, stocks et journal. Aucun effacement n’est requis pour tester.

## Mission 7B — diagnostic économique en lecture seule

Dans **Serpulo industrielle — progression**, ouvrir la carte planétaire, sélectionner un secteur, puis **Diagnostic économique**. La liste permet de consulter chaque colonie ; toucher une matière ou une plateforme ouvre son détail. Le bouton **Actualiser** relit un instantané, sans modifier les stocks. Le défilement est vertical. La consultation solo est en pause.

Le noyau actif fournit un stock lu dans le monde ; les secteurs non chargés affichent le stock du modèle natif et ses estimations. Le prélèvement net au noyau n’est pas la consommation totale des machines. Les moyennes d’exports et l’offre d’importation ne sont pas des quantités livrées. Les détails de plateformes sont disponibles uniquement dans le monde actif ; aucun monde n’est chargé pour les inspecter. La dernière date d’échantillonnage n’existe pas dans M6D : elle est donc indiquée inconnue après redémarrage, puis suivie en mémoire pendant la session. Aucun historique n’est écrit.

Aucun moteur économique nouveau, aucune production application fermée, aucune modification des recherches, coûts M6D ou règles de voisinage. Les campagnes classique et industrielle libre ne montrent pas ce diagnostic.
