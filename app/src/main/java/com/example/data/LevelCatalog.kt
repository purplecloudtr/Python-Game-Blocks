package com.example.data

import com.example.model.BlockPalette
import com.example.model.BoardState
import com.example.model.CommandDoc
import com.example.model.Orientation
import com.example.model.PuzzleBlock
import com.example.model.PuzzleLevel

object LevelCatalog {

    private val sharedCommands = listOf(
        CommandDoc(
            signature = "hareket_et(blok, yon, adim)",
            description = "Belirtilen bloğu verilen yönde (\"sag\", \"sol\", \"yukari\", \"asagi\") adım sayısı kadar kaydırır.",
            example = "hareket_et(\"kirmizi\", \"sag\", 2)",
        ),
        CommandDoc(
            signature = "yol_acik_mi(blok, yon)",
            description = "Bloğun belirtilen yönde en az 1 adım ilerleyip ilerleyemeyeceğini True veya False olarak döndürür.",
            example = "if yol_acik_mi(\"turuncu\", \"yukari\"):",
        ),
        CommandDoc(
            signature = "print(deger)",
            description = "Değişken değerlerini veya kontrol mesajlarını sonuç paneline yazdırır.",
            example = "print(\"Yol açık mı:\", yol_acik_mi(\"kirmizi\", \"sag\"))",
        ),
    )

    val levels: List<PuzzleLevel> = listOf(
        PuzzleLevel(
            id = 1,
            title = "Bölüm 1: Değişkenler",
            conceptTitle = "Değişken Nedir?",
            conceptExplanation = "Değişken, bir değeri isimlendirerek sakladığımız kutudur. " +
                "Örneğin mesafe = 4 yazdığımızda 4 sayısını \"mesafe\" adlı değişkende saklarız ve " +
                "hareket_et(\"kirmizi\", \"sag\", mesafe) içinde kullanabiliriz.",
            taskDescription = "Görev: Önce \"mavi\" bloğu aşağı 2 adım kaydırarak Satır 3'ü boşalt. " +
                "Ardından \"mesafe\" değişkenine doğru sayıyı atayıp \"kirmizi\" bloğu çıkışa ulaştır.",
            starterCode = """
                # 1. Önce yolu kapatan mavi bloğu aşağı kaydır
                hareket_et("mavi", "asagi", 2)

                # 2. Hedef bloğun çıkışa gitmesi gereken adımı değişkende sakla
                mesafe = 1
                hareket_et("kirmizi", "sag", mesafe)
            """.trimIndent(),
            hints = listOf(
                "İpucu 1: \"kirmizi\" bloğu şu an Satır 3, Sütun 1–2 konumunda. Çıkış kapısı Sütun 6'nın sağ kenarındadır. Sütun 2'den Sütun 6'ya ulaşmak için kaç birim sağa gitmeli?",
                "İpucu 2: \"mesafe = 1\" satırını \"mesafe = 4\" olarak değiştir. İstersen önce print(mesafe) ile değerini görebilirsin.",
                "Örnek çözüm:\nhareket_et(\"mavi\", \"asagi\", 2)\nmesafe = 4\nhareket_et(\"kirmizi\", \"sag\", mesafe)",
            ),
            initialBoard = BoardState(
                gridSize = 6,
                exitRow = 3,
                blocks = listOf(
                    PuzzleBlock(
                        id = "kirmizi",
                        displayName = "Kırmızı Hedef",
                        palette = BlockPalette.RED,
                        orientation = Orientation.HORIZONTAL,
                        row = 3,
                        col = 1,
                        length = 2,
                        isTarget = true,
                    ),
                    PuzzleBlock(
                        id = "mavi",
                        displayName = "Mavi Engel",
                        palette = BlockPalette.BLUE,
                        orientation = Orientation.VERTICAL,
                        row = 3,
                        col = 4,
                        length = 2,
                    ),
                    PuzzleBlock(
                        id = "sari",
                        displayName = "Sarı Blok",
                        palette = BlockPalette.YELLOW,
                        orientation = Orientation.VERTICAL,
                        row = 1,
                        col = 6,
                        length = 2,
                    ),
                ),
            ),
            commandDocs = sharedCommands,
        ),
        PuzzleLevel(
            id = 2,
            title = "Bölüm 2: Koşullar",
            conceptTitle = "if ve else ile Yol Kontrolü",
            conceptExplanation = "if koşulu, bir durum doğruysa (True) altındaki girintili kodları çalıştırır; " +
                "yanlışsa (False) else bloğuna geçer. yol_acik_mi(blok, yon) fonksiyonu ile " +
                "bir bloğun önünün açık olup olmadığını test edebilirsin.",
            taskDescription = "Görev: \"turuncu\" bloğun üstü kapalı olduğu için if / else ile açık yöne kaydır. " +
                "Ardından \"yesil\" bloğun yukarı yolunu if ile kontrol edip Satır 3'ten çıkar ve " +
                "yol açıksa \"kirmizi\" bloğu çıkışa götür.",
            starterCode = """
                # Turuncu bloğun önünü if / else ile kontrol et
                if yol_acik_mi("turuncu", "yukari"):
                    hareket_et("turuncu", "yukari", 2)
                else:
                    hareket_et("turuncu", "asagi", 2)

                # Yeşil bloğun yukarı yolu açıksa Satır 3'ten çıkar
                if yol_acik_mi("yesil", "yukari"):
                    hareket_et("yesil", "yukari", 1)

                # Kırmızı bloğun önü açıksa çıkışa ilerlet
                if yol_acik_mi("kirmizi", "sag"):
                    hareket_et("kirmizi", "sag", 2)
            """.trimIndent(),
            hints = listOf(
                "İpucu 1: \"yesil\" bloğu Satır 3–4, Sütun 5 konumundadır. Yukarı 1 adım giderse Satır 2–3'e gelir ve hâlâ Satır 3'ü kapatır. Kaç adım yukarı çıkmalı?",
                "İpucu 2: \"yesil\" bloğu yukarı 2 adım kaydır (hareket_et(\"yesil\", \"yukari\", 2)) ve \"kirmizi\" bloğu sağa 4 adım ilerlet.",
                "Örnek çözüm:\nif yol_acik_mi(\"turuncu\", \"yukari\"):\n    hareket_et(\"turuncu\", \"yukari\", 2)\nelse:\n    hareket_et(\"turuncu\", \"asagi\", 2)\n\nif yol_acik_mi(\"yesil\", \"yukari\"):\n    hareket_et(\"yesil\", \"yukari\", 2)\n\nif yol_acik_mi(\"kirmizi\", \"sag\"):\n    hareket_et(\"kirmizi\", \"sag\", 4)",
            ),
            initialBoard = BoardState(
                gridSize = 6,
                exitRow = 3,
                blocks = listOf(
                    PuzzleBlock(
                        id = "kirmizi",
                        displayName = "Kırmızı Hedef",
                        palette = BlockPalette.RED,
                        orientation = Orientation.HORIZONTAL,
                        row = 3,
                        col = 1,
                        length = 2,
                        isTarget = true,
                    ),
                    PuzzleBlock(
                        id = "sari",
                        displayName = "Sarı Engel",
                        palette = BlockPalette.YELLOW,
                        orientation = Orientation.HORIZONTAL,
                        row = 1,
                        col = 3,
                        length = 2,
                    ),
                    PuzzleBlock(
                        id = "turuncu",
                        displayName = "Turuncu Engel",
                        palette = BlockPalette.ORANGE,
                        orientation = Orientation.VERTICAL,
                        row = 2,
                        col = 3,
                        length = 2,
                    ),
                    PuzzleBlock(
                        id = "yesil",
                        displayName = "Yeşil Engel",
                        palette = BlockPalette.GREEN,
                        orientation = Orientation.VERTICAL,
                        row = 3,
                        col = 5,
                        length = 2,
                    ),
                    PuzzleBlock(
                        id = "mavi",
                        displayName = "Mavi Engel",
                        palette = BlockPalette.BLUE,
                        orientation = Orientation.HORIZONTAL,
                        row = 5,
                        col = 4,
                        length = 2,
                    ),
                ),
            ),
            commandDocs = sharedCommands,
        ),
        PuzzleLevel(
            id = 3,
            title = "Bölüm 3: Döngüler",
            conceptTitle = "for Döngüsü ile Tekrarlanan Hareketler",
            conceptExplanation = "Aynı komutu art arda yazmak yerine for döngüsü kullanırız. " +
                "for blok in engeller: listedeki her blok için aynı işlemi tekrarlar. " +
                "for adim in range(4): ise altındaki komutu 4 kez çalıştırır.",
            taskDescription = "Görev: Satır 3'ü kapatan \"mavi\", \"yesil\" ve \"mor\" engellerini ilk for döngüsüyle " +
                "aşağı 2'şer adım kaydır. Sonra ikinci for döngüsünün tekrar sayısını düzelterek \"kirmizi\" bloğu çıkışa ulaştır.",
            starterCode = """
                engeller = ["mavi", "yesil", "mor"]

                # Listedeki her engeli aşağı kaydır
                for blok in engeller:
                    hareket_et(blok, "asagi", 1)

                # Kırmızı bloğu adım adım sağa ilerlet
                for adim in range(2):
                    hareket_et("kirmizi", "sag", 1)
            """.trimIndent(),
            hints = listOf(
                "İpucu 1: İlk döngüdeki engellerin Satır 3'ten tamamen çıkması için her birinin aşağı 2 adım gitmesi gerekiyor.",
                "İpucu 2: İkinci döngüde \"kirmizi\" bloğun Sütun 1–2'den çıkışa ulaşması için range(2) yerine range(4) kullan.",
                "Örnek çözüm:\nengeller = [\"mavi\", \"yesil\", \"mor\"]\nfor blok in engeller:\n    hareket_et(blok, \"asagi\", 2)\n\nfor adim in range(4):\n    hareket_et(\"kirmizi\", \"sag\", 1)",
            ),
            initialBoard = BoardState(
                gridSize = 6,
                exitRow = 3,
                blocks = listOf(
                    PuzzleBlock(
                        id = "kirmizi",
                        displayName = "Kırmızı Hedef",
                        palette = BlockPalette.RED,
                        orientation = Orientation.HORIZONTAL,
                        row = 3,
                        col = 1,
                        length = 2,
                        isTarget = true,
                    ),
                    PuzzleBlock(
                        id = "sari",
                        displayName = "Sarı Blok",
                        palette = BlockPalette.YELLOW,
                        orientation = Orientation.HORIZONTAL,
                        row = 1,
                        col = 4,
                        length = 2,
                    ),
                    PuzzleBlock(
                        id = "mavi",
                        displayName = "Mavi Engel",
                        palette = BlockPalette.BLUE,
                        orientation = Orientation.VERTICAL,
                        row = 2,
                        col = 3,
                        length = 2,
                    ),
                    PuzzleBlock(
                        id = "yesil",
                        displayName = "Yeşil Engel",
                        palette = BlockPalette.GREEN,
                        orientation = Orientation.VERTICAL,
                        row = 3,
                        col = 4,
                        length = 2,
                    ),
                    PuzzleBlock(
                        id = "mor",
                        displayName = "Mor Engel",
                        palette = BlockPalette.PURPLE,
                        orientation = Orientation.VERTICAL,
                        row = 2,
                        col = 5,
                        length = 2,
                    ),
                ),
            ),
            commandDocs = sharedCommands,
        ),
    )

    fun getLevelById(id: Int): PuzzleLevel = levels.firstOrNull { it.id == id } ?: levels.first()
}
