# Mission 6C — arbre et progression industrielle

Une nouvelle campagne **Serpulo industrielle — progression** propose une recherche effective et des machines verrouillées jusqu’à leur étude. La planète **Serpulo industrielle** déjà utilisée en M4/M5/M6B reste en accès libre. Les deux campagnes ont des secteurs, stocks, statistiques et recherches distincts.

## APK et installation

**[Télécharger l’APK M6C et ses sources](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/41997396029/artifacts/13401326793)** · [Compilation GitHub Actions réussie](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/41997396029) · [Code compilé `92e1ea71`](https://github.com/Ankool29/Mindustry-Industrial/commit/92e1ea71fd08534e706045209903a9f24249c422).

APK réellement téléchargé et contrôlé : signature valide, même certificat SHA-256 `61254fd2280bf34aa64bd06405cf0e2bc9b2d8fe7df4cc615c39983bcdfd8086`, version `8-industrial-m6c-160.7`, `versionCode=7`, alignement Android 16 Kio vérifié. SHA-256 du fichier APK : `59aa6a75a32a468a45177e061bbccfd3cbb263e8a94eaa4471c77cd6f32112df`. Java 17, Gradle 8.14.3, AGP 8.10.1 et SDK 36. L’archive comprend les sources correspondantes de Mindustry et Arc et leurs licences. Artefact disponible jusqu’au 5 décembre 2026 ; le workflow permet de le reconstruire.

Télécharger l’artefact **Mindustry-Industrial-Android**, extraire le ZIP dans Mes fichiers puis ouvrir `Mindustry-Industrial-v160.7-m6c.apk`. Installer par-dessus M3/M4/M5/M6B **sans désinstallation**. Le package reste `io.anuke.mindustry.industrial.m3` et le code de version passe à **7**, avec la clé précédente. Une exportation complète avant mise à jour reste conseillée pour une version de test.

## Commencer sans risquer les anciennes sauvegardes

1. Ouvrir **Jouer → Campagne** et sélectionner **Serpulo industrielle — progression** dans la liste des planètes. Au premier choix de campagne, le bouton **Campagne industrielle — progression** mène à cette même planète.
2. Choisir le secteur **170**, puis **Décoller**. Lire le dialogue indiquant que la nouvelle campagne est séparée et que les anciennes parties ne seront pas réinitialisées. **Annuler** ne crée aucune sauvegarde stricte. Confirmer pour commencer.
3. Le noyau reçoit **100 cuivre**. L’unité initiale, la foreuse mécanique et le convoyeur de base sont disponibles. Extraire cuivre/plomb, puis charbon ; les autres machines passent par la recherche. Les ressources, les coûts de construction et les besoins électriques sont normaux.
4. Ouvrir **Recherche industrielle** depuis le HUD ou la carte planétaire. Le graphe natif se déplace et se zoome au toucher. **Parcourir** ouvre une liste avec recherche textuelle pour accéder rapidement à une technologie.
5. Toucher une technologie : sa fiche montre la machine, son parent, les conditions manquantes, les coûts, les contributions déjà versées, le stock de cette seule planète et les technologies suivantes. Les liens de parent/enfants permettent de parcourir les dépendances sans déplacer tout le graphe.
6. Toucher **Contribuer** pour verser les ressources disponibles. Un paiement incomplet est enregistré ; une fois financée, la machine devient disponible dans le menu de construction. Les contributions faites par ce bouton sont sauvegardées avec les paramètres et le secteur actif concerné.
7. Le bouton **Catalogue** conserve la consultation M5 ; il ne construit et ne finance rien. Revenir à la planète **Serpulo industrielle** sans le mot « progression » pour retrouver les anciennes bases et leur accès libre. Revenir à **Serpulo** pour la campagne originale.

**Une seule campagne stricte est disponible.** Sélectionner de nouveau sa planète reprend ses secteurs et recherches ; cela ne crée pas une autre partie. Aucun bouton de réinitialisation n’est ajouté. Ne pas supprimer les données de l’application, utiliser les commandes de remise à zéro ni renommer les fichiers pour tester un nouveau profil. Un gestionnaire de profils arbitraires est reporté.

## Séparation et options d’architecture

Le [document d’architecture préparé avant les modifications](https://github.com/Ankool29/Mindustry-Industrial/blob/industrial-android/docs/MISSION_6C_ARCHITECTURE.md) compare trois options : conversion de la planète existante, gestionnaire général de profils, ou seconde planète dédiée. La troisième est retenue parce qu’elle réutilise les espaces de noms natifs de secteurs et évite toute conversion. Le gestionnaire général demanderait une identité de profil dans les chemins de sauvegarde, import/export, statistiques, stocks et interface ; il n’est pas implémenté dans cette mission.

| Campagne | Fichiers de secteur | Recherche |
| --- | --- | --- |
| Serpulo classique | `sector-serpulo-<id>.msav` | Clés natives classiques, inchangées |
| Industrielle libre M4/M5/M6B | `sector-industrial-serpulo-<id>.msav` | Clé M6B `industrial-research-v1-industrial-serpulo`, inchangée |
| Industrielle progressive M6C | `sector-industrial-serpulo-progressive-<id>.msav` | `industrial-research-v1-industrial-serpulo-progressive` |

La nouvelle planète est ajoutée après les anciennes, sans décaler leurs identités. Elle possède ses propres objets `Sector` et réutilise le générateur Serpulo avec la même grille et graine, sans modifier son algorithme. Les machines, unités et recettes restent les objets natifs : aucun double de bloc ou de matériau n’est créé. Le seul contenu planétaire nouveau sert à séparer les campagnes.

Chaque arbre, objectif, découverte, lecture de contribution et prélèvement possède maintenant une portée planétaire explicite. Les profils ne copient pas les déblocages classiques ni les découvertes de l’autre planète. Une recherche consultée depuis une carte planétaire ne change pas de portée selon le secteur actuellement visité. Les stocks ne sont jamais regroupés entre campagnes.

## Arbre et déblocages effectifs

Les nœuds M6B détachés sont réutilisés avec copies des tableaux de coûts, sans inscription dans `TechTree.all`/`roots` ni modification de `content.techNode`/`techNodes`. Les objectifs classiques `SectorComplete` et `OnSector` ne sont pas repris. Les recherches et découvertes consultent leur profil industriel ; les jalons d’expansion restent préparés mais ne verrouillent aucun secteur.

Les branches principales sont extraction, transformation, énergie, logistique et automatisation, avec branches annexes de stockage, liquides et assistance civile. Exemples :

- Foreuse mécanique → pneumatique → laser → foreuse avancée. La pneumatique demande aussi la recherche de la presse à graphite et la découverte du graphite.
- Presse à graphite → silicium et métaverre ; le silicium ouvre notamment l’industrie des spores et les fabriques civiles.
- Combustion → réseau électrique, batteries et production avancée. Le pulvérisateur est accessible depuis la combustion, pour éviter de dépendre du métaverre quand le sable manque mais que du débris est disponible.
- Le pompage et l’extraction d’eau suivent le métaverre ; l’extracteur d’eau ne dépend plus de la foreuse laser.
- Le cryofluide suit le pompage avec ses conditions matérielles de titane ; la phase et l’alliage avancé ne restent pas derrière des machines sans rapport avec leur recette.
- Mono est rattaché à la fabrique aérienne, Nova à la fabrique terrestre et Retusa à la fabrique navale, sans passage obligé par Flare, Dagger ou Risso.

Les matériaux à usage mixte et les unités de construction/extraction/transport sont conservés, même s’ils ont aussi des usages militaires. Les technologies purement militaires restent exclues. Les coûts de recherche viennent des nœuds natifs ; aucune recette ni coût de construction partagé n’est modifié. Le montant réellement affiché est celui calculé par le moteur, sans table de prix approximative ajoutée.

Dans la campagne stricte, les contrôles portent sur le menu de construction, la validation de placement, la construction effective, les recettes des constructeurs, les configurations de fabriques, les schémas et les chargements de colonisation. Un schéma global créé dans la campagne libre ne permet donc pas de déployer une machine verrouillée dans la campagne stricte. Les fichiers de schémas restent conservés.

Les anciennes campagnes industrielles restent en accès libre. Leurs bâtiments, stocks, découvertes et contributions M6B ne sont ni retirés ni réinitialisés. Leurs fiches de recherche indiquent l’accès libre et ne permettent aucun paiement nécessaire à l’utilisation des machines.

## Secteurs et ressources

Aucun frais supplémentaire de colonisation, verrou de voisinage ou changement de simulation des secteurs non visités. Le noyau de base et les coûts de déploiement natifs restent utilisables pour chercher un minerai absent du secteur courant. Les plateformes de lancement conservent leurs recettes et règles d’origine.

Le contrôle du générateur a examiné six secteurs du voisinage initial : 170, 171, 175, 64, 15 et 124. Leur ensemble contient cuivre, plomb, charbon, sable, titane et thorium. Cela ne signifie pas que chaque secteur contient chaque minerai ; il faut explorer et organiser la production selon le terrain.

## Tests et validation

**367 tests, 0 échec, 0 erreur, 0 test ignoré**, en local et dans la compilation GitHub Actions finale. Les XML JUnit de cette compilation ont été téléchargés et recomptés : [rapports de tests](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/41997396029/artifacts/13401591116). [Preuves de compilation Android](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/41997396029/artifacts/13401302013).

Le client graphique final a également été exécuté en disposition mobile portrait 600 × 1000, puis paysage, dans **deux processus distincts**. Le premier a chargé une copie des sauvegardes de validation M6B : usine conservée, accès libre à la foreuse avancée et recherche antérieure conservée. L’annulation du dialogue de premier départ strict n’a créé aucune sauvegarde. Après confirmation, la campagne stricte a reçu 100 cuivre et la presse était verrouillée.

La fiche a refusé un paiement tant que les découvertes manquaient. Un clic réel sur **Contribuer** a ensuite versé 100/2400 cuivre et 0/900 plomb. Après arrêt complet et redémarrage, ce paiement et les stocks réservés étaient conservés. Un second clic a achevé la recherche, rendu la presse constructible et laissé les recherches classiques inchangées. Le retour à la campagne libre puis à Serpulo classique a restauré leurs règles et arbres respectifs. Le catalogue reste accessible.

Ces contrôles utilisent exclusivement un répertoire de test indépendant : aucun effacement ni réinitialisation de données du joueur. Le [script reproductible de contrôle graphique](docs/mission6c/ui-check.js) approvisionne **uniquement cette copie de test** avec les découvertes nécessaires, puis les 2300 cuivre et 900 plomb restants pour accélérer la vérification du redémarrage ; ce script n’est pas inclus dans l’APK et ne doit pas être installé sur les données du joueur. Les captures ont des noms de contenus anglais hérités de cette configuration de test, avec les nouvelles commandes françaises.

| Contrôle graphique | Capture |
| --- | --- |
| Confirmation d’une campagne réellement séparée | [Premier départ](docs/mission6c/start-confirmation.png) |
| Arbre natif et navigation portrait | [Portrait](docs/mission6c/strict-tree-portrait.png) |
| Paiement partiel réellement enregistré | [100/2400 cuivre](docs/mission6c/partial-contribution.png) |
| Recherche terminée après redémarrage | [Contributions complètes](docs/mission6c/research-completed.png) |
| Machine devenue disponible | [Construction](docs/mission6c/construction-unlocked.png) |
| Adaptation paysage et catalogue conservé | [Paysage](docs/mission6c/strict-tree-landscape.png) · [Catalogue](docs/mission6c/catalogue-retained.png) |
| Retour aux campagnes antérieures | [Industrielle libre](docs/mission6c/legacy-free-tree.png) · [Classique](docs/mission6c/classic-tree.png) |

La validation tactile sur le S25 Ultra physique reste à confirmer par le joueur ; ces contrôles graphiques dans le cloud ne constituent pas une installation sur son téléphone.

Huit nouveaux scénarios automatisés couvrent :

1. Espaces de sauvegarde et clés de recherche réellement distincts, absence d’héritage des découvertes libres et maintien de leur accès libre.
2. Nouveau départ à 100 cuivre, technologies initiales limitées, placement d’une presse refusé avant recherche puis construction réelle et débit de son coût après recherche.
3. Contributions partielles et rechargement des paramètres, sans prélèvement dans les stocks classiques ou libres et sans écriture dans leurs clés de recherche.
4. Chemin de production/extraction graphite → silicium → titane → thorium, sans exiger une matière non encore accessible pour la recherche. Les machines natives consomment leurs intrants et les générateurs à combustion alimentent réellement les machines électriques.
5. Arbre sans cycle, technologies civiles réattachées, absence de conditions militaires et pointeurs/listes classiques inchangés.
6. Inspection des six secteurs générés mentionnés ci-dessus, sans ajout de gisements au jeu.
7. Sauvegarde/rechargement d’un secteur strict avec contributions et stock conservés, puis retour à l’accès libre.
8. Refus d’une configuration de fabrique pour une unité non recherchée ; acceptation de Mono après recherche et refus d’une unité purement combattante.

Les montants de recherche sont approvisionnés par le banc de test uniquement avec des types de matières déjà obtenables à ce stade, afin de ne pas attendre l’extraction de milliers d’unités. La production et les forages sont simulés avec les machines et recettes réelles sur un terrain contrôlé. Ces aménagements de tests ne sont pas inclus dans l’APK ni appliqués aux données du joueur.

## Limites connues

- Une campagne stricte dédiée, sans gestionnaire de profils supplémentaires ni bouton de recommencement.
- Le graphe natif dessine les liens parentaux ; les autres prérequis croisés figurent dans les fiches. Le catalogue reste disponible comme consultation distincte.
- Les technologies initiales sont offertes ; leur disponibilité n’implique pas de ressources infinies. Les coûts natifs peuvent demander une production importante et plusieurs versements : l’équilibrage de durée reste à éprouver lors de longues sessions Android.
- Le contrôle graphique en disposition mobile dans le cloud ne remplace pas un essai du nouvel APK sur le S25 Ultra physique. Le multijoueur entre versions différentes et les mods qui contournent les règles du moteur ne sont pas couverts.
- L’export/import complet des données est nécessaire pour transférer l’ensemble des profils. Un fichier de secteur seul n’emporte pas toute la recherche planétaire.
- Signature de test conservée depuis M3 ; le workflow refuse de générer une nouvelle clé si la précédente manque. Artefacts conservés 30 jours, avec sources correspondantes et licences.
