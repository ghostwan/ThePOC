#!/bin/bash

echo "🚀 Compilation de l'application..."
./gradlew assembleDebug

if [ $? -ne 0 ]; then
    echo "❌ Erreur lors de la compilation"
    exit 1
fi

echo "📱 Installation de l'application..."
./gradlew installDebug

if [ $? -ne 0 ]; then
    echo "❌ Erreur lors de l'installation"
    exit 1
fi

echo "▶️ Lancement de l'application..."
adb shell am start -n com.ghostwan.thepoc/.MainActivity

if [ $? -ne 0 ]; then
    echo "❌ Erreur lors du lancement"
    exit 1
fi

echo "✅ Application lancée avec succès !" 