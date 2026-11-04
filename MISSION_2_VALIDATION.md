# Mission 2 — compilation Android validée

La compilation **GitHub Actions réelle** a réussi. L’APK a été publié comme artefact et son téléchargement a été vérifié. Aucun code de gameplay ou système de combat n’a été modifié.

## Télécharger sur le téléphone

1. Se connecter à GitHub dans Chrome ou Samsung Internet.
2. Ouvrir [l’artefact Mindustry-Industrial-Android](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/41878845453/artifacts/13349165446). Autre accès : [exécution réussie](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/41878845453), puis **Artifacts → Mindustry-Industrial-Android**.
3. Dans **Mes fichiers → Téléchargements**, extraire le ZIP et ouvrir **Mindustry-Industrial-v160.7-test.apk**.
4. Autoriser temporairement l’installation depuis **Mes fichiers** si Android le demande, puis installer. Si le Bloqueur automatique de Samsung empêche l’installation, le désactiver temporairement pour cet APK de confiance et le réactiver ensuite.

Le ZIP pèse environ **168 Mo**, car les sources et licences sont incluses. L’application de test peut coexister avec Mindustry officiel. L’installation physique sur le Galaxy S25 Ultra reste à confirmer par son utilisateur.

[Guide Android détaillé et procédure de relance](https://github.com/Ankool29/Mindustry-Industrial/blob/industrial-android/docs/ANDROID_CLOUD_FR.md).

## Preuves et versions

| Élément | Résultat constaté |
|---|---|
| Exécution | [41878845453](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/41878845453), événement `push`, résultat `success` |
| Branche de travail | `industrial-android` |
| Base amont | Mindustry `v160.7`, commit `17012d61d9e4c3cbd9820d87e81c24495486ac53` |
| Commit compilé | `0543434d13822620bd5e451dba02b4229161977e` |
| Arc | `7445105cd2683daf5b9ac7033ebb07265d3e814d` |
| Runner GitHub | `ubuntu-22.04` |
| Java | Eclipse Temurin **17.0.21** |
| Gradle | **8.14.3**, distribution contrôlée par SHA-256 |
| Android Gradle Plugin | **8.10.1** |
| Android SDK / cible | **36** |
| Android Build Tools | **36.0.0** |
| SDK minimal déclaré | **21** |
| Identifiant de test | `io.anuke.mindustry.industrial` |
| Signature APK | Clé Android debug ; vérification réussie, signature v2 présente |
| Contenu | Bibliothèques ARM64 et atlas de sprites présents |
| Alignement ZIP | Vérification `zipalign -P 16` réussie |
| Publication | Artefact **13349165446**, non expiré lors de la validation ; téléchargement réussi |

Le journal GitHub constate `tools:pack` réussi en **1 min 17 s**, puis `android:assembleDebug` réussi en **1 min 25 s**. Toutes les étapes du job, y compris la vérification et la publication, ont réussi.

Le [lancement manuel 41878340205](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/41878340205), déclenché via `workflow_dispatch` sur `master`, a également terminé avec le résultat **success**. Le parcours de relance depuis le bouton GitHub est donc lui aussi validé par une compilation réelle.

SHA-256 de l’APK téléchargé, contrôlé contre `SHA256SUMS.txt` :

```text
8a7f68f5719c93d4ad39ce8f663140da8367a894d7ec65d203dc06f4254a81d2
```

Empreinte SHA-256 du certificat de test de cette exécution :

```text
362c6d6fd3bb01173aee9475eed252fc3d2234214c1f5cf1a0446198fa588b9d
```

L’artefact **Android-build-evidence** de la même exécution contient les versions, journaux, informations de signature et contrôles de contenu. Les artefacts sont conservés **30 jours**, jusqu’au 4 décembre 2026 pour cette exécution. Relancer le workflow après leur expiration.

## Modifications limitées à cette mission

- Workflow automatique pour les pushes sur `industrial-android`, lancement manuel et lanceur sur la branche par défaut `master`.
- Retrait des workflows de maintenance et publication propres au dépôt officiel sur les deux branches concernées.
- Installation des paquets Android actuels, sans demander le paquet obsolète `tools`. Utilisation des commandes disponibles sur les runners.
- Ajustement du wrapper de Gradle **9.3.1 → 8.14.3** et d’AGP **8.2.2 → 8.10.1**. L’essai initial réel dans le cloud avait atteint R8 puis échoué sur un fichier intermédiaire `base.jar` manquant ; la chaîne ajustée a ensuite compilé réellement dans le cloud et dans GitHub Actions.
- Exécution explicite du packing des sprites avant la collecte des assets Android.
- Identifiant et libellé Android distincts uniquement pour la variante de test. Ni refonte graphique, ni mode industriel, ni modification de `core/src`.
- Conservation de la GPL et fourniture des sources correspondantes du fork et d’Arc avec les licences.

La signature est une signature de **test**, conservée dans le cache GitHub. Si ce cache disparaît, la clé peut changer : exporter les sauvegardes de l’application de test avant une désinstallation nécessaire au changement de signature. Une clé durable devra être mise en place avant une distribution de production.

## Relancer depuis le téléphone

Ouvrir [Actions → Android cloud APK](https://github.com/Ankool29/Mindustry-Industrial/actions/workflows/android-cloud.yml), choisir **Run workflow**, puis confirmer. Le lanceur sur `master` appelle la branche de travail basée sur v160.7. Les modifications de code poussées sur `industrial-android` lancent aussi le build automatiquement. Aucun ordinateur ni compilation sur le téléphone n’est nécessaire.
