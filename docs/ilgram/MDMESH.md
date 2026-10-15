# ILGRAM'ni MDMesh orqali o'rnatish va yangilash

MDMesh imkoniyatlari manba kodi bo'yicha tekshirildi (`kmehmonov/MDMesh`, commit `301dc0f`, `agent-android`):

| MDMesh imkoniyati | Manba | ILGRAM uchun ahamiyati |
|---|---|---|
| Device Owner sifatida jim o'rnatish/yangilash (`app.silentInstall`, PackageInstaller) | `agent-android/proto/.../AppManagement.kt`, `app/.../install/InstallResultReceiver.kt` | APK'ni o'quvchi aralashuvisiz o'rnatish va yangilash |
| Kiosk (LockTask) — `setLockTaskPackages` | `agent-android/kiosk/.../LockTaskKioskController.kt` | ILGRAM'ni kiosk allowlistga qo'shish |
| Ruxsatlarni oldindan berish — `setPermissionGrantState` | `agent-android/app/.../admin/AdminReceiver.kt` | Bildirishnoma, mikrofon, kamera, media ruxsatlari |
| **Managed configuration** (`setApplicationRestrictions`) | **topilmadi** | ILGRAM uni qo'llab-quvvatlaydi, lekin MDMesh hozircha yubora olmaydi (pastga qarang) |
| Battery optimization whitelist | **topilmadi** | Qo'lda yoki MDMesh'ga qo'shish kerak |

## 1. O'rnatish
1. GitHub Actions'dan `ILGRAM-N.apk` ni yuklab oling (Artifacts).
2. MDMesh konsolida Applications → yangi ilova → APK'ni yuklang. Paket nomi: **`uz.istiqbol.ilgram`**.
3. Qurilmalar konfiguratsiyasiga qo'shing va o'rnatishni ishga tushiring.
4. Ilova birinchi marta ochilganda o'quvchi telefon raqami bilan Telegram'ga kiradi (oddiy Telegram login).

## 2. Ruxsatlar (MDMesh permission policy)
Oldindan berish tavsiya etiladi (`uz.istiqbol.ilgram`):
* `POST_NOTIFICATIONS` (Android 13+) — bildirishnomalar va fon ulanish belgisi
* `RECORD_AUDIO` — ovozli xabar
* `CAMERA` — rasm/video olish
* `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO` (yoki eski Android'da `READ_EXTERNAL_STORAGE`) — rasm/video/fayl yuborish
* `READ_CONTACTS` — **bermang** (ILGRAM kontaktlarni o'qimaydi)

## 3. Fon rejimida xabar olish (muhim)
ILGRAM o'z api_id'si bilan ishlaydi, shuning uchun Telegram unga Firebase push yubormaydi.
Xabarlar foreground xizmat ("ILGRAM — xabarlar fon rejimida qabul qilinmoqda") orqali keladi. Ishonchli ishlashi uchun:
* ILGRAM'ni **batareya optimizatsiyasidan chiqaring** (Settings → Apps → ILGRAM → Battery → Unrestricted).
  Samsung: "Never sleeping apps" ro'yxatiga qo'shing. Redmi/MIUI/HyperOS: Autostart = ON, Battery saver = No restrictions.
* MDMesh hozircha buni masofadan qila olmaydi — qurilmani tayyorlashda bir marta qo'lda bajaring
  yoki MDMesh agentiga Device Owner orqali whitelist qo'shish funksiyasini kiriting.

## 4. Kiosk rejimi
* LockTask allowlistga `uz.istiqbol.ilgram` ni qo'shing.
* Fayl biriktirish tizim fayl tanlagichini ochadi — allowlistga `com.google.android.documentsui`
  (ba'zi qurilmalarda `com.android.documentsui`) ni ham qo'shing, aks holda PDF yuborib bo'lmaydi.
* Rasm/video ILGRAM'ning ichki kamerasi va galereyasi orqali yuboriladi (tashqi ilova kerak emas).
* Ovozli/video qo'ng'iroqlar ILGRAM ichida.

## 5. Yangilash
* Har bir GitHub build yangi `versionCode` beradi va **bir xil kalit** bilan imzolanadi → MDMesh yangi APK'ni ustidan o'rnatadi.
* Telegram sessiyasi, sozlamalar va kesh saqlanadi (ma'lumotlar o'chirilmaydi).
* Imzolash kalitini yo'qotmang: `D:\python\ILGRAM_KEYS\ilgram-release.jks` (+ GitHub secretlari). Kalit o'zgarsa,
  ilovani o'chirib qayta o'rnatishga to'g'ri keladi va o'quvchilar qayta login qiladi.

## 6. Ruxsatlar ro'yxatini yangilash
**Hozir (MDMesh o'zgarmasdan):** `ILGRAM_CONFIG` secretini tahrirlang → *ILGRAM build* → yangi APK → MDMesh orqali tarqating.

**Kelajakda (APK'siz):** ILGRAM Android *managed configuration* ni allaqachon qo'llab-quvvatlaydi
(kalitlar: `allowed_group_ids`, `allowed_user_ids`, `student_mode_enabled`, `allow_service_notifications`,
`allow_secret_chats`, `help_text`). MDMesh agentiga Device Owner orqali quyidagini qo'shish kifoya:

```kotlin
val bundle = Bundle().apply {
    putString("allowed_group_ids", "-1001234567890,-1009876543210")
    putString("allowed_user_ids", "111111111,222222222")
}
dpm.setApplicationRestrictions(adminComponent, "uz.istiqbol.ilgram", bundle)
```
ILGRAM `ACTION_APPLICATION_RESTRICTIONS_CHANGED` ni tinglaydi va chatlar ro'yxatini darhol qayta quradi.

## 7. Tekshiruv ro'yxati (har bir qurilma turi uchun)
- [ ] APK o'rnatildi (Samsung, Redmi)
- [ ] APK yangilandi, sessiya saqlandi
- [ ] Qayta ishga tushirgandan keyin ILGRAM ulanish belgisi paydo bo'ldi va xabar keldi
- [ ] Kiosk rejimida PDF biriktirish ishladi
- [ ] Android 8–15 dagi qurilmalarda ochildi
