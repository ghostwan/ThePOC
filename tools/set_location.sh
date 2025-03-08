#!/bin/bash

# Vérifier si les arguments sont fournis
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <longitude> <latitude>"
    echo "Example: $0 1.55857365578413 47.436056863171146"
    exit 1
fi

# Exécuter la commande adb
adb emu geo fix $1 $2

# Vérifier si la commande a réussi
if [ $? -eq 0 ]; then
    echo "Position GPS mise à jour avec succès !"
    echo "Longitude: $1"
    echo "Latitude: $2"
else
    echo "Erreur lors de la mise à jour de la position GPS"
    exit 1
fi 