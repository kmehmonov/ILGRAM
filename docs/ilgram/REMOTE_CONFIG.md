# Masofaviy ruxsatlar ro'yxati (remote allow-list)

Ruxsat berilgan guruh, kanal, foydalanuvchi va botlar ro'yxati maktabning boshqaruv botidan yangilanadi. Buning uchun yangi APK kerak emas.

## Oqim

1. Admin botda ro'yxatni o'zgartiradi. O'zgarish **qoralama**ga yoziladi.
2. Admin **🚀 E'lon qilish** tugmasini bosadi. Bot ro'yxatdan yangi versiya (v1, v2, …) yaratadi, uni AES-256-GCM bilan shifrlaydi va ECDSA P-256 bilan imzolaydi.
3. Bot faylni `https://ilgram.istiqbol-luck.uz/config` manzilida beradi (Cloudflare Tunnel orqali, ETag/304 bilan).
4. ILGRAM shu faylni yuklaydi:
   - ilova ishga tushganda;
   - ilova ochilganda (eng ko'pi bilan har 2 daqiqada bir marta);
   - har N daqiqada (N botda belgilanadi, 15 daqiqadan 7 kungacha).
5. ILGRAM avval imzoni tekshiradi, keyin faylni ochadi. Yangi versiya faqat joriydan **katta** bo'lsa qabul qilinadi. U `filesDir/ilgram_remote_config.json` ga saqlanadi va darhol qo'llanadi: chatlar ro'yxati qayta saralanadi.

## Nimani o'zgartirish mumkin

- **Masofadan o'zgaradi:** faqat `allowed_group_ids`, `allowed_user_ids` va tekshirish oralig'i.
- **Masofadan o'zgarmaydi:** o'quvchi rejimi, brauzer, qidiruv, akkauntlar va boshqa barcha cheklovlar. Ular APK ichida qotirilgan.

Ustunlik tartibi (pastdagisi ustun turadi):

1. APK ichidagi ro'yxat;
2. masofaviy ro'yxat (APK ro'yxatini butunlay **almashtiradi**);
3. MDM managed config.

## Xavfsizlik

- APK ichida faqat **ochiq** tekshiruv kaliti va AES kaliti bor. Imzo kaliti hech qachon botning serveridan chiqmaydi. Shu sababli APK'ni ochgan odam ham soxta ro'yxat yasay olmaydi.
- Quyidagilarning hammasi rad etiladi, ilova esa avvalgi ro'yxat bilan ishlashda davom etadi:
  - imzosi noto'g'ri yoki o'zgartirilgan fayl;
  - eski versiya;
  - HTTPS bo'lmagan manzil;
  - 1 MB dan katta javob.
- Keshdagi fayl ham har safar qayta tekshiriladi.
- So'rovda foydalanuvchi haqida hech qanday ma'lumot yuborilmaydi: bu oddiy `GET /config` so'rovi.

## Build sozlamalari

GitHub secretlari:
- `ILGRAM_REMOTE_PUBKEY`;
- `ILGRAM_REMOTE_AESKEY`;
- ixtiyoriy `ILGRAM_REMOTE_URL` (standart qiymati `https://ilgram.istiqbol-luck.uz/config`).

`ilgram/ci/prepare.sh` bu qiymatlarni `assets/ilgram.properties` ichiga quyidagi kalitlar bilan yozadi:
- `remote_config_url`;
- `remote_config_public_key`;
- `remote_config_aes_key`.

Secretlar bo'lmasa, funksiya o'chiq bo'ladi va ilova avvalgidek ishlaydi.

## Testlar

- `ilgram/tests/ILGRAMRemoteConfigTest.java` (JVM) — bot kodi yaratgan fayllarni Java tomonda ochadi. Unda buzilgan, begona imzoli va noto'g'ri kalitli holatlar ham tekshiriladi.
- `TMessagesProj_AppTests/.../ILGRAMRemoteSyncTest.java` (emulator) — quyidagilarni tekshiradi:
  - Android kriptografiyasi;
  - siyosatga qo'shilishi;
  - eski versiyani rad etish;
  - kesh qayta ishga tushirishdan keyin saqlanishi;
  - tarmoq xatosida joriy ro'yxat saqlanib qolishi.
- Fixture'lar `ilgram/tests/make_remote_fixture.py` bilan yaratiladi. Ular faqat TEST kalitlari bilan imzolangan.
