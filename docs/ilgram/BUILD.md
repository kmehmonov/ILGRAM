# ILGRAM build yo'riqnomasi

APK GitHub Actions'da yig'iladi. Kompyuterda faqat kod yoziladi (Antigravity, VS Code va h.k.).

## 1. Bir martalik sozlash (GitHub → Settings → Secrets and variables → Actions)

| Secret | Nima | Holati |
|---|---|---|
| `ILGRAM_KEYSTORE_B64`, `ILGRAM_KEYSTORE_PASSWORD`, `ILGRAM_KEY_ALIAS`, `ILGRAM_KEY_PASSWORD` | APK imzolash kaliti | ✅ o'rnatilgan. Zaxira nusxa: `D:\python\ILGRAM_KEYS` (repozitoriyga qo'shmang!) |
| `ILGRAM_API_ID`, `ILGRAM_API_HASH` | Telegram API kalitlari | ❗ **Siz qo'shishingiz kerak** — pastga qarang |
| `ILGRAM_CONFIG` | Ruxsat etilgan guruh / User ID ro'yxati | ❗ **Siz qo'shishingiz kerak** — pastga qarang |

### api_id / api_hash olish (5 daqiqa)
1. https://my.telegram.org saytiga maktab nomidagi Telegram raqami bilan kiring.
2. **API development tools** → ilova yarating: App title `ILGRAM`, Short name `ilgram`, Platform `Android`.
3. `App api_id` va `App api_hash` qiymatlarini GitHub secretlariga `ILGRAM_API_ID` va `ILGRAM_API_HASH` nomi bilan qo'shing.

Bu kalitlarsiz APK yig'iladi, lekin Telegram'ga kirishda `API_ID_INVALID` xatosi chiqadi.
Telegram'ning rasmiy api_id'sidan foydalanish Telegram API shartlariga zid, shuning uchun u kodda yo'q.

### ILGRAM_CONFIG (ruxsatlar ro'yxati)
Secret qiymati — `assets/ilgram.properties` bilan bir xil formatdagi qatorlar:

```
student_mode_enabled=true
allowed_group_ids=-1001234567890,-1009876543210,-123456789
allowed_user_ids=111111111,222222222
allow_service_notifications=true
allow_secret_chats=false
help_text=Muammo bo'lsa: sinf rahbari yoki IT bo'limi, +998 ...
```

Nega secret: User ID'lar shaxsiy ma'lumot, repozitoriy esa ochiq (GPL talabi).

**Chat ID / User ID'ni qanday bilish mumkin:**
* Guruhga ID ko'rsatuvchi botni (masalan, `@getidsbot`, `@RawDataBot`) vaqtincha qo'shing yoki guruh xabarini unga forward qiling — `-100...` ko'rinishidagi ID chiqadi. Keyin botni guruhdan chiqaring.
* Telegram Desktop: Settings → Advanced → Experimental settings → "Show Peer IDs in Profile".
* User ID: o'qituvchi `@userinfobot`'ga yozsa, o'z ID'sini ko'radi.

## 2. Build ishga tushirish
* `master`ga har bir push → **ILGRAM build** workflow avtomatik ishlaydi (~40–60 daqiqa, keyingilari ccache bilan tezroq).
* Qo'lda: GitHub → Actions → *ILGRAM build* → **Run workflow**.
* Natija: run sahifasidagi **Artifacts → ILGRAM-apk-N** (ichida `ILGRAM-N.apk` va `SHA256SUMS.txt`).
* Har bir build `versionCode` ni oshiradi (Telegram bazaviy kodi × 1000 + run raqami), shuning uchun MDMesh yangilanishni qabul qiladi.

Konfiguratsiya o'zgarsa: `ILGRAM_CONFIG` secretini tahrirlang → **Run workflow** → yangi APK'ni MDMesh orqali tarqating.

## 3. Workflowlar
| Workflow | Vazifa | Davomiyligi |
|---|---|---|
| `ILGRAM build` | Imzolangan release APK (armeabi-v7a + arm64-v8a) | 40–60 daq |
| `ILGRAM compile check` | Tez Java kompilyatsiya + policy unit testlari | ~6 daq |
| `ILGRAM device tests` | Emulatorda (Android 14) integratsion va smoke testlar, skrinshotlar | 60–90 daq |

## 4. Kompyuterda build (ixtiyoriy)
Android Studio (2025.1.4+), SDK 36, NDK 27.2.12479018, CMake 3.22.1, JDK 17, 16 GB RAM (Docker/WSL'ni yoping).

```
git clone https://github.com/kmehmonov/ILGRAM.git
cd ILGRAM
git submodule update --init --recursive --depth=1
set ILGRAM_API_ID=...            (PowerShell: $env:ILGRAM_API_ID="...")
set ILGRAM_API_HASH=...
gradlew :TMessagesProj_App:assembleAfatRelease
```
Mahalliy build `TMessagesProj/config/release.keystore` dagi namunaviy kalit bilan imzolanadi — uni tarqatmang.

## 5. Unit testlar (JDK yetarli)
```
bash ilgram/tests/run-tests.sh
```
