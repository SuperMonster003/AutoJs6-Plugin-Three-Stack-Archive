<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-archive-browser-ic-launcher" border="0" width="128" />
  </p>

  <p>Navigation en lecture seule dans les archives de la famille ZIP et extraction SAF sélective pour l'explorateur AutoJs6</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Browser?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Browser?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Browser?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Langues (Languages)

******

Le fichier README.md actuel prend en charge les langues suivantes:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hans.md)
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hant-HK.md)
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-en.md)
- Français [fr] # actuel
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ar.md)

******

### Introduction

******

Le plugin AutoJs6 Archive Browser ajoute la navigation en lecture seule dans les archives à l'explorateur AutoJs6. Il ouvre les conteneurs basés sur ZIP dans une vue hiérarchique dédiée et extrait uniquement les entrées sélectionnées par l'utilisateur vers un dossier de sortie choisi avec Android Storage Access Framework.

******

### Fonctionnalités

******

- Enregistre une action supplémentaire en lecture seule pour un seul fichier via le protocole partagé `org.autojs.plugin.EXPLORER_ACTION`.
- Parcourt les dossiers de l'archive et affiche la taille décompressée, la taille compressée, le CRC et la date de modification.
- Recherche les chemins d'entrée normalisés et permet de sélectionner des fichiers, des dossiers ou toutes les entrées visibles.
- Extrait les entrées sélectionnées vers une arborescence SAF choisie par l'utilisateur avec progression et annulation.
- Accepte les conteneurs ZIP, JAR, AAR et WAR utilisant des méthodes de compression ZIP prises en charge.
- Copie l'entrée en lecture seule dans le cache privé et supprime les données temporaires à la fermeture de la vue.

******

### Formats pris en charge

******

La version 1 reconnaît les extensions suivantes de la famille ZIP:

```text
zip, jar, aar, war
```

******

### Interface du plugin

******

AutoJs6 découvre et exécute le plugin avec les identités suivantes:

```text
service action: org.autojs.plugin.EXPLORER_ACTION
execute action: org.autojs.plugin.EXPLORER_ACTION_EXECUTE
plugin id: archive-browser
engine: explorer-action
variant: default
```

La version 1 se limite à une action supplémentaire en lecture seule pour un seul fichier dans l'explorateur AutoJs6 principal.

******

### Sécurité

******

Le plugin ne demande aucune autorisation de stockage ou de réseau. L'hôte accorde un accès temporaire en lecture seule au content URI d'entrée, tandis que l'accès en sortie est limité au dossier SAF explicitement choisi par l'utilisateur. Les chemins absolus, la remontée vers un dossier parent, les préfixes de lecteur, les barres obliques inverses, les caractères Unicode non sûrs, les chemins en double, les conflits entre fichiers et dossiers, les méthodes de compression non prises en charge, les différences de taille et les différences de CRC sont rejetés.

******

### Limites de sécurité

******

- Taille maximale de l'entrée mise en cache: `4 GiB`.
- Nombre maximal de noeuds de chemin dans l'archive, y compris les dossiers implicites: `20,000`.
- Taille maximale du répertoire central ZIP: `64 MiB`.
- Longueur maximale du chemin normalisé: `1,024` caractères.
- Profondeur maximale du chemin: `64` segments.
- Taille décompressée maximale d'une entrée: `512 MiB`.
- Taille décompressée totale maximale: `2 GiB`.
- Taux de compression maximal: `1000:1`.

******

### Historique des versions

******

# v1.0.0

###### 2026/08/02

* `Fonctionnalité` Plugin Archive Browser avec l'ID `archive-browser`, le moteur `explorer-action` et la variante `default`
* `Fonctionnalité` Action de l'explorateur en lecture seule pour un seul conteneur ZIP, JAR, AAR ou WAR avec navigation hiérarchique, recherche de chemins et sélection d'entrées
* `Fonctionnalité` Extraction sélective vers une arborescence SAF choisie par l'utilisateur avec progression et annulation, accès temporaire en lecture seule à l'entrée et aucune autorisation de stockage ou de réseau
* `Fonctionnalité` Limites de sécurité de 4 GiB en entrée, 20,000 noeuds de chemin avec les dossiers implicites, 64 MiB pour le répertoire central ZIP, 512 MiB par entrée décompressée, 2 GiB de données décompressées au total et un taux de compression de 1000:1
* `Fonctionnalité` Validation des chemins non sûrs, des entrées en double ou en conflit, des méthodes de compression non prises en charge, des changements de source, des différences de taille et des différences de CRC
* `Fonctionnalité` Métadonnées, interface, instructions, README et CHANGELOG localisés en espagnol, français, russe, arabe, japonais, coréen, anglais, chinois simplifié, chinois traditionnel de Hong Kong et chinois traditionnel de Taïwan

##### Pour consulter davantage de versions

* [CHANGELOG-fr.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/app/src/main/assets/doc/CHANGELOG-fr.md)

******

### Compilation

******

```powershell
.\gradlew.bat :app:assembleDebug
```

Compilation Release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Les paramètres de compilation proviennent de `version.properties`. Le SDK minimal actuel est 24 et le SDK cible est 36.

******

### Structure des ressources

******

```text
.readme/lang_*.json
.changelog/lang_*.json
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/values-*/strings.xml
app/src/main/res/values-*/plurals.xml
app/src/main/res/raw-*/plugin_instruction.md
```

`strings.xml` localise les métadonnées du plugin et les textes fixes du navigateur, tandis que `plurals.xml` localise les textes liés aux quantités. `plugin_instruction.md` fournit les instructions visibles depuis l'hôte. `.python/generate_markdown.py` génère les fichiers README et les historiques depuis les sources JSON.

******

### Liens

******

- Documentation AutoJs6: https://docs.autojs6.com
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
