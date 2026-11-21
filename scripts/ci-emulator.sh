#!/usr/bin/env bash
# Executado dentro do emulador Android no GitHub Actions.
set -uo pipefail
OUT=build/ci-report
mkdir -p "$OUT/screenshots"
IME="com.keyhistory/.keyboard.KeyHistoryImeService"
status=0

adb shell settings put secure show_ime_with_hard_keyboard 1
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

echo "::group::Instalar app auxiliar de campos"
./gradlew --no-daemon :testfield:installDebug || status=1
echo "::endgroup::"

echo "::group::Testes instrumentados (Room, criptografia, teclado E2E)"
./gradlew --no-daemon :app:connectedDebugAndroidTest || status=1
echo "::endgroup::"
adb pull /data/local/tmp/kh_keyboard_active.png "$OUT/screenshots/teclado_registro_ativo.png" || true
adb pull /data/local/tmp/kh_keyboard_protected.png "$OUT/screenshots/teclado_campo_senha.png" || true

echo "::group::Smoke test do APK de release (R8)"
APK=app/build/outputs/apk/release/app-release.apk
adb install -r "$APK" || status=1
adb shell pm list packages | grep -q '^package:com.keyhistory$' || { echo "Instalação falhou"; status=1; }
adb logcat -c
adb shell am start -W -n com.keyhistory/.MainActivity
sleep 6
adb exec-out screencap -p > "$OUT/screenshots/dashboard.png"
if ! adb shell pidof com.keyhistory >/dev/null; then echo "App de release não está em execução"; status=1; fi
if adb logcat -d | grep -E "FATAL EXCEPTION.*|AndroidRuntime: Process: com.keyhistory" ; then echo "Crash detectado"; status=1; fi

# Teclado no build de release: ativar, selecionar e verificar que as teclas aparecem.
adb shell ime enable "$IME"
adb shell ime set "$IME"
[ "$(adb shell settings get secure default_input_method | tr -d '\r')" = "$IME" ] || { echo "Seleção do teclado falhou"; status=1; }
adb shell am start -W -n com.keyhistory.testfield/.FieldActivity
sleep 2
adb shell uiautomator dump /data/local/tmp/field.xml >/dev/null
adb pull /data/local/tmp/field.xml "$OUT/field.xml" >/dev/null || true
XY=$(python3 -c "
import re,sys
s=open('$OUT/field.xml').read()
m=re.search(r'resource-id=\"com.keyhistory.testfield:id/field_text\"[^>]*bounds=\"\\[(\d+),(\d+)\\]\\[(\d+),(\d+)\\]', s)
print((int(m.group(1))+int(m.group(3)))//2, (int(m.group(2))+int(m.group(4)))//2) if m else print('300 220')
")
echo "Tocando no campo em $XY"
adb shell input tap $XY
sleep 3
adb shell uiautomator dump /data/local/tmp/ui.xml >/dev/null
adb pull /data/local/tmp/ui.xml "$OUT/ui_release.xml" || true
if grep -q 'Tecla q' "$OUT/ui_release.xml" && grep -q 'Tecla ç' "$OUT/ui_release.xml"; then
  echo "Teclado de release exibido corretamente"
else
  echo "Teclado de release NÃO encontrado na tela"; status=1
fi
adb exec-out screencap -p > "$OUT/screenshots/teclado_release.png"

# Funcionamento sem internet: o APK não declara a permissão INTERNET.
if adb shell dumpsys package com.keyhistory | grep -q "android.permission.INTERNET"; then
  echo "ERRO: permissão INTERNET presente"; status=1
else
  echo "OK: sem permissão INTERNET"
fi
adb shell dumpsys package com.keyhistory | grep -E "versionName|minSdk|targetSdk" | head -5 | tee "$OUT/package_info.txt"
echo "::endgroup::"

cp -r app/build/outputs/androidTest-results "$OUT/" 2>/dev/null || true
cp -r app/build/reports/androidTests "$OUT/" 2>/dev/null || true
exit $status
