@echo off
mkdir -p keystore
keytool -genkey -v -keystore keystore/release-key.jks -alias maychurchkey -keyalg RSA -keysize 2048 -validity 10000 -storepass maychurch -keypass maychurch 