@echo off
echo "Очистка проекта..."
call ./gradlew clean

echo "Сборка релизного APK..."
call ./gradlew :app:assembleRelease --stacktrace

echo "Проверка результатов сборки..."
if exist "app\build\outputs\apk\release\app-release.apk" (
    echo "APK создан успешно!"
    echo "Путь: app\build\outputs\apk\release\app-release.apk"
) else (
    echo "Ошибка при создании APK. Проверьте логи сборки."
)

echo "Сборка релизного Bundle (AAB)..."
call ./gradlew :app:bundleRelease --stacktrace

echo "Проверка результатов сборки Bundle..."
if exist "app\build\outputs\bundle\release\app-release.aab" (
    echo "Bundle создан успешно!"
    echo "Путь: app\build\outputs\bundle\release\app-release.aab"
) else (
    echo "Ошибка при создании Bundle. Проверьте логи сборки."
)

echo "Готово!" 