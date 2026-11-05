## Mindustry Industrial — diagnostic économique Android (Mission 7B)

**[Télécharger l’APK M7B et ses sources](https://github.com/Ankool29/Mindustry-Industrial/actions/runs/42072442237/artifacts/13435874229)** · **[Rapport, tests, mesures et procédure téléphone](MISSION_7B_VALIDATION.md)**.

Dans **Serpulo industrielle — progression**, ouvrir la carte planétaire, sélectionner un secteur puis **Diagnostic économique** : colonies, stocks, moyennes natives, origine observée/estimée, ancienneté et détails des plateformes du monde actif. Le prélèvement net est distingué de la consommation totale non mesurée ; les moyennes ne sont pas des quantités livrées.

**Lecture seule :** aucun nouveau moteur économique, aucune modification des recettes, transports, recherches, coûts M6D ou règles de voisinage. Les campagnes classique et industrielle libre restent préservées. Aucune production application fermée ni cargaison fictive en transit. La mission 7C n’est pas commencée.

**397 tests réussis**, compilation GitHub Actions verte, signature précédente et alignement Android 16 Kio vérifiés. Les comparaisons d’usine et contrôles graphiques utilisent des données isolées ; les performances cloud ne sont pas des mesures Android. L’essai physique du nouvel APK reste à confirmer sur S25 Ultra.

Installer par-dessus M6D **sans désinstallation**, sans effacement ni conversion de sauvegardes. La dernière mesure est inconnue après redémarrage, puis datée en mémoire pendant la session : les stocks et recherches ne sont pas réinitialisés.

Historique : [Audit 7A](MISSION_7A_AUDIT.md) · [Mission 6D](MISSION_6D_VALIDATION.md) · [Mission 6C](MISSION_6C_VALIDATION.md) · [Mission 6B](MISSION_6B_VALIDATION.md) · [Mission 5](MISSION_5_VALIDATION.md) · [Mission 4](MISSION_4_VALIDATION.md) · [Mission 3](MISSION_3_VALIDATION.md) · [Mission 2](MISSION_2_VALIDATION.md).

Pour compiler depuis un smartphone : [Actions → Android cloud APK → Run workflow](https://github.com/Ankool29/Mindustry-Industrial/actions/workflows/android-cloud.yml). Le lancement manuel compile la branche `industrial-android`, même si le sélecteur affiche `master`.

[Guide Android : installation, sauvegardes et limites](https://github.com/Ankool29/Mindustry-Industrial/blob/industrial-android/docs/ANDROID_CLOUD_FR.md).

---

![Logo](core/assets-raw/sprites/ui/logo.png)

[![Build Status](https://github.com/Anuken/Mindustry/workflows/Tests/badge.svg?event=push)](https://github.com/Anuken/Mindustry/actions)
[![Discord](https://img.shields.io/discord/391020510269669376.svg?logo=discord&logoColor=white&logoWidth=20&labelColor=7289DA&label=Discord&color=17cf48)](https://discord.gg/mindustry)  

The automation tower defense RTS, written in Java.

_[Trello Board](https://trello.com/b/aE2tcUwF/mindustry-40-plans)_  
_[Wiki](https://mindustrygame.github.io/wiki)_  
_[Javadoc](https://mindustrygame.github.io/docs/)_ 

## Contributing

See [CONTRIBUTING](CONTRIBUTING.md) for general code style and PR guidelines.

If you are a first-time contributor looking for features to implement or bugs to fix, see the issues tagged with 'candidate' [in the Mindustry-Suggestions repostiory](https://github.com/Anuken/Mindustry-Suggestions/issues?q=is%3Aissue%20state%3Aopen%20label%3Acandidate).

## Building

Bleeding-edge builds are generated automatically for every commit. You can see them [here](https://github.com/Anuken/MindustryBuilds/releases).

If you'd rather compile on your own, follow these instructions.
First, make sure you have [JDK 17](https://adoptium.net/temurin/releases/?os=any&arch=any&version=17) installed. **Other JDK versions will not work.** Open a terminal in the Mindustry directory and run the following commands:

### Windows

_Running:_ `gradlew desktop:run`  
_Building:_ `gradlew desktop:dist`  
_Sprite Packing:_ `gradlew tools:pack`

### Linux/Mac OS

_Running:_ `./gradlew desktop:run`  
_Building:_ `./gradlew desktop:dist`  
_Sprite Packing:_ `./gradlew tools:pack`

### Server

Server builds are bundled with each released build (in Releases). If you'd rather compile on your own, replace 'desktop' with 'server', e.g. `gradlew server:dist`.

### Android

1. Install the Android SDK [here.](https://developer.android.com/studio#command-tools) Make sure you're downloading the "Command line tools only", as Android Studio is not required.
2. In the unzipped Android SDK folder, find the cmdline-tools directory. Then create a folder inside of it called `latest` and put all of its contents into the newly created folder.
3. In the same directory run the command `sdkmanager --licenses` (or `./sdkmanager --licenses` if on linux/mac)
4. Set the `ANDROID_HOME` environment variable to point to your unzipped Android SDK directory.
5. Enable developer mode on your device/emulator. If you are on testing on a phone you can follow [these instructions](https://developer.android.com/studio/command-line/adb#Enabling), otherwise you need to google how to enable your emulator's developer mode specifically.
6. Run `gradlew android:assembleDebug` (or `./gradlew` if on linux/mac). This will create an unsigned APK in `android/build/outputs/apk`.

To debug the application on a connected device/emulator, run `gradlew android:installDebug android:run`.

### Troubleshooting

#### Permission Denied

If the terminal returns `Permission denied` or `Command not found` on Mac/Linux, run `chmod +x ./gradlew` before running `./gradlew`. *This is a one-time procedure.*

#### Where is the `mindustry.gen` package?

As the name implies, `mindustry.gen` is generated *at build time* based on other code. You will not find source code for this package in the repository, and it should not be edited by hand.

The following is a non-exhaustive list of the "source" of generated code in `mindustry.gen`:

- `Call`, `*Packet` classes: Generated from methods marked with `@Remote`.
- All entity classes (`Unit`, `EffectState`, `Posc`, etc): Generated from component classes in the `mindustry.entities.comp` package, and combined using definitions in `mindustry.content.UnitTypes`.
- `Sounds`, `Musics`, `Tex`, `Icon`, etc: Generated based on files in the respective asset folders.

---

Gradle may take up to several minutes to download files. Be patient. <br>
After building, the output .JAR file should be in `/desktop/build/libs/Mindustry.jar` for desktop builds, and in `/server/build/libs/server-release.jar` for server builds.

## Feature Requests

Post feature requests and feedback [here](https://github.com/Anuken/Mindustry-Suggestions/issues/new/choose).

## Downloads

| [![](https://static.itch.io/images/badge.svg)](https://anuke.itch.io/mindustry)    |    [![](https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png)](https://play.google.com/store/apps/details?id=io.anuke.mindustry)   |    [![](https://fdroid.gitlab.io/artwork/badge/get-it-on.png)](https://f-droid.org/packages/io.anuke.mindustry)	| [![](https://flathub.org/assets/badges/flathub-badge-en.svg)](https://flathub.org/apps/details/com.github.Anuken.Mindustry)  
|---	|---	|---	|---	|
