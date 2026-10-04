# AutoJs6 3-Stack Archive

Un écran autonome ouvre les archives pour les consulter et extraire; la création et la modification sur place restent accessibles depuis AutoJs6.
Les paramètres communs proposent la langue, le mode sombre, la couleur et quatre icônes de lanceur.
L'identifiant passe de io.github.supermonster003.autojs6.plugin.archivemanager à io.github.supermonster003.autojs6.plugin.three.stack.archive. Android installe une application distincte; les anciennes applications et leurs données peuvent être conservées, sans migration automatique des paramètres.


3-Stack Archive 2.22.0 nécessite Android 7 ou une version ultérieure et une version appariée d'AutoJs6 6.8.0 (versionCode 5276 ou plus) qui annonce Explorer Action v21.

1. Installez et activez le plugin `3-Stack Archive`.
2. Ouvrez le menu supplémentaire d'une archive prise en charge.
3. Sélectionnez `Ouvrir l'archive`.
4. Parcourez les dossiers, recherchez, utilisez la barre de chemin ou ouvrez les entrées prises en charge avec les visionneuses de l'hôte.
5. Retour remonte d'abord dans l'archive avant de revenir à la liste de fichiers ordinaire.

Pour extraire immédiatement toute l'archive, sélectionnez `Extraire vers...` dans son menu, puis choisissez le dossier de sortie.

Pour créer une archive, sélectionnez `Compresser...` dans le menu d'un fichier ou dossier ordinaire. Vous pouvez aussi sélectionner plusieurs éléments du même dossier et utiliser `Compresser...` dans la barre inférieure. Choisissez le nom, le format et le niveau de compression; ZIP et 7Z acceptent aussi un mot de passe facultatif.

La version actuelle lit et extrait les archives de la famille ZIP, les 7Z ordinaires ou solid, RAR4/RAR5, TAR, TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 et TAR.ZST/TZST. Elle crée des ZIP ordinaires ou fractionnés standard, des 7Z non solid, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 et TAR.ZST; ZIP et 7Z acceptent un mot de passe facultatif et les noms d'une sortie chiffrée restent visibles. Les ZIP ordinaires à volume unique, les 7Z non chiffrés, non solid, à volume unique et dans le budget du décodeur, ainsi que les archives TAR compatibles permettent d'ajouter, renommer et supprimer du contenu par reconstruction entièrement vérifiée. RAR, les 7Z chiffrés, solid, fractionnés, dangereux, non pris en charge ou hors budget et les entrées TAR spéciales restent en lecture seule. Les ensembles complets `.z01 + .zip`, `partN.rar`, `.zip.001` et `.7z.001` peuvent être parcourus et extraits via des descripteurs voisins approuvés et bornés par l'hôte; tous les ensembles multivolumes restent en lecture seule. La création ZIP fractionnée propose une taille MiB prédéfinie ou personnalisée. La sortie est fractionnée si elle dépasse la taille choisie; une sortie plus petite reste un seul `.zip`.

Après vérification et validation de toutes les sorties physiques, une option désactivée par défaut peut déplacer la sélection source complète vers la corbeille de l'hôte. Explorer Action v21 conserve un historique de récupération persistant et borné; restaurer les sources ne supprime jamais l'archive créée.

Le plugin ne demande aucun accès général au stockage ni au réseau. Les archives ordinaires sont consultées par des canaux à position indépendante sur le descripteur en lecture seule de l'hôte, sans copie intégrale ; les entrées ou lecteurs incompatibles (actuellement les ZIP chiffrés) utilisent le cache privé, nettoyé à la fermeture ou après expiration. La création d'archive lit via une session courte de l'hôte liée au plugin et ne peut écrire qu'une sortie transactionnelle dans le dossier parent actuel. Les contrôles du chemin, de la taille source et des limites de sortie restent actifs.
