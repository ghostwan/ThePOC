#!/bin/bash

# Fonction pour trouver le chemin de l'émulateur
find_emulator() {
    # Essayer d'abord avec ANDROID_HOME
    if [ -n "$ANDROID_HOME" ]; then
        if [ -f "$ANDROID_HOME/emulator/emulator" ]; then
            echo "$ANDROID_HOME/emulator/emulator"
            return 0
        fi
    fi

    # Essayer avec le chemin par défaut sur macOS
    local default_path="$HOME/Library/Android/sdk/emulator/emulator"
    if [ -f "$default_path" ]; then
        echo "$default_path"
        return 0
    fi

    echo "Impossible de trouver l'émulateur Android. Veuillez définir ANDROID_HOME."
    return 1
}

# Trouver l'émulateur
EMULATOR=$(find_emulator)
if [ $? -ne 0 ]; then
    exit 1
fi

# Lister les AVDs disponibles
echo "Émulateurs disponibles :"
"$EMULATOR" -list-avds

# Vérifier si des AVDs existent
if [ $? -ne 0 ] || [ -z "$("$EMULATOR" -list-avds)" ]; then
    echo "Aucun émulateur trouvé. Veuillez en créer un dans Android Studio."
    exit 1
fi

# Prendre le premier AVD disponible ou utiliser celui spécifié en argument
AVD_NAME=$1
if [ -z "$AVD_NAME" ]; then
    AVD_NAME=$("$EMULATOR" -list-avds | head -n 1)
fi

echo "Lancement de l'émulateur : $AVD_NAME"

# Lancer l'émulateur en arrière-plan
"$EMULATOR" -avd "$AVD_NAME" -no-snapshot-load &

# Attendre que l'émulateur soit complètement démarré
echo "Attente du démarrage de l'émulateur..."
while [ "$(adb shell getprop sys.boot_completed 2>/dev/null)" != "1" ]; do
    sleep 2
done

echo "L'émulateur est prêt !" 