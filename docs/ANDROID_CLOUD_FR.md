# Compiler et installer depuis un smartphone Android

Cette branche dérive de Mindustry **v160.7**, commit amont `17012d61d9e4c3cbd9820d87e81c24495486ac53`. La Mission 5 simplifie les menus industriels de la **campagne industrielle planétaire séparée**, en plus du mode Industriel des parties personnalisées. Les campagnes classiques restent disponibles. Elle n’est pas une version officielle de Mindustry.

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
2. Ouvrir `Mindustry-Industrial-v160.7-m5.apk`.
3. Si Android le demande, autoriser temporairement **Installation d’applis inconnues** pour l’application qui ouvre le fichier, par exemple **Mes fichiers**, puis revenir à l’APK et toucher **Installer**.
4. Sur certaines versions One UI, **Paramètres → Sécurité et confidentialité → Bloqueur automatique** peut empêcher cette installation. Si c’est le cas, le désactiver uniquement le temps d’installer cet APK de confiance, puis le réactiver. Les libellés peuvent varier selon la version de One UI.
5. Lancer **Mindustry Industrial**. Révoquer ensuite l’autorisation d’installation d’applications inconnues si elle avait été activée.

## Mise à jour de M3/M4 et sauvegardes

M5 conserve l’identifiant `io.anuke.mindustry.industrial.m3` et la signature de M3, avec un code de version Android porté à 5. Installer l’APK par-dessus M3 ou M4 : **ne pas désinstaller l’application**. Les parties existantes gardent leur mode et leurs fichiers. Une exportation de sauvegarde depuis les paramètres reste recommandée avant une mise à jour de test.

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
