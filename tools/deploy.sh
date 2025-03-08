#!/bin/bash

# Fonction pour incrémenter la version
increment_version() {
    local version=$1
    local position=$2
    local IFS='.'
    local array=($version)
    array[$position]=$((array[$position] + 1))
    echo "${array[*]}"
}

# Lire les versions actuelles depuis gradle.properties
GRADLE_PROPERTIES="gradle.properties"
CURRENT_VERSION_CODE=$(grep "VERSION_CODE=" $GRADLE_PROPERTIES | cut -d'=' -f2)
CURRENT_VERSION_NAME=$(grep "VERSION_NAME=" $GRADLE_PROPERTIES | cut -d'=' -f2 | tr -d ' ')

# Incrémenter les versions
NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))
NEW_VERSION_NAME=$(increment_version $CURRENT_VERSION_NAME 2)

echo "Version actuelle : $CURRENT_VERSION_NAME ($CURRENT_VERSION_CODE)"
echo "Nouvelle version : $NEW_VERSION_NAME ($NEW_VERSION_CODE)"

# Mettre à jour les versions dans gradle.properties
sed -i '' "s/VERSION_CODE=$CURRENT_VERSION_CODE/VERSION_CODE=$NEW_VERSION_CODE/" $GRADLE_PROPERTIES
sed -i '' "s/VERSION_NAME=$CURRENT_VERSION_NAME/VERSION_NAME=$NEW_VERSION_NAME/" $GRADLE_PROPERTIES

echo "✨ Versions mises à jour dans gradle.properties"

# Compiler l'application en mode release
echo "🏗️ Compilation en mode release..."
./gradlew assembleRelease

# Déployer sur Firebase Distribution
echo "🚀 Déploiement sur Firebase Distribution..."
./gradlew appDistributionUploadRelease

echo "✅ Déploiement terminé !" 