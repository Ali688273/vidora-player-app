package com.vidora.player

import android.content.Context

object VidoraTextTranslator {

    fun translate(
        context: Context,
        text: String
    ): String {

        if (
            VidoraLanguageManager.isPersian(context)
        ) {
            return text
        }

        return translations[text] ?: text
    }

    private val translations =
        mapOf(

            "ویدئو" to "Video",
            "ویدئوها" to "Videos",
            "پخش" to "Play",
            "توقف" to "Pause",
            "توقف یا پخش" to "Play / Pause",

            "بازگشت" to "Back",
            "تمام صفحه" to "Fullscreen",
            "ویدئوی قبلی" to "Previous video",
            "ویدئوی بعدی" to "Next video",

            "تکرار" to "Repeat",
            "صدا" to "Audio",
            "زیرنویس" to "Subtitle",
            "اشتراک" to "Share",
            "اشتراک‌گذاری" to "Share",
            "حذف" to "Delete",
            "بیشتر" to "More",
            "قفل" to "Lock",

            "علاقه‌مندی‌ها" to "Favorites",
            "افزودن به علاقه‌مندی‌ها" to "Add to favorites",
            "حذف از علاقه‌مندی‌ها" to "Remove from favorites",

            "افزودن به پلی‌لیست" to "Add to playlist",
            "پلی‌لیست" to "Playlist",
            "پلی‌لیست‌ها" to "Playlists",
            "پلی‌لیست جدید" to "New playlist",
            "مدیریت پلی‌لیست‌ها" to "Manage playlists",

            "تغییر نام" to "Rename",
            "تغییر نام ویدئو" to "Rename video",

            "ذخیره" to "Save",
            "لغو" to "Cancel",
            "بستن" to "Close",
            "باشه" to "OK",
            "ساختن" to "Create",

            "مرتب‌سازی" to "Sort",
            "جدیدترین" to "Newest",
            "نام" to "Name",
            "حجم" to "Size",

            "همه پوشه‌ها" to "All folders",
            "پوشه" to "Folder",
            "ویدئو ندارد" to "No videos",
            "هیچ ویدئویی پیدا نشد." to "No videos found.",
            "نتیجه‌ای برای جستجو پیدا نشد." to "No search results.",

            "حذف ویدئو" to "Delete video",
            "آیا از حذف این ویدئو مطمئن هستید؟" to
                "Are you sure you want to delete this video?",

            "مخفی کردن" to "Hide",
            "نمایش فهرستی" to "List view",
            "نمایش شبکه‌ای" to "Grid view",

            "صدا" to "Audio",
            "سرعت" to "Speed",
            "کیفیت ویدئو" to "Video quality",
            "تنظیمات پخش" to "Playback settings",
            "همگام‌سازی صدا" to "Audio sync",
            "همگام‌سازی زیرنویس" to "Subtitle sync",
            "صف پخش" to "Playback queue",

            "افزودن به صف" to "Add to queue",
            "نمایش صف" to "Show queue",
            "پاک کردن صف" to "Clear queue",

            "خواب" to "Sleep timer",
            "زمان‌سنج خواب" to "Sleep timer",

            "قفل صفحه" to "Lock screen",
            "تمام صفحه" to "Fullscreen"
        )
}
