package com.example.sihscrap.data.local;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

@Database(
    entities = {
        TransactionEntity.class,
        MaterialCatalogEntity.class,
        SyncQueueEntity.class
    },
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract TransactionDao transactionDao();
    public abstract MaterialCatalogDao materialCatalogDao();
    public abstract SyncQueueDao syncQueueDao();

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "sih_scrap_database.db"
                    )
                    .fallbackToDestructiveMigration()
                    .addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            Executors.newSingleThreadExecutor().execute(() -> {
                                seedCatalog(getInstance(context).materialCatalogDao());
                            });
                        }
                    })
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    public static void seedCatalog(MaterialCatalogDao dao) {
        if (dao.getCount() == 0) {
            List<MaterialCatalogEntity> seed = new ArrayList<>();
            seed.add(new MaterialCatalogEntity("copper_bare_bright", "Copper Bare Bright (Millberry)", "शुद्ध तांबा (मिलबेरी तार)", "शुद्ध तांब्याची तार (मिलबेरी)", "non_ferrous", 695.0, 5.0, 0.99, 4.5, "EMERALD_GREEN"));
            seed.add(new MaterialCatalogEntity("copper_armature", "Copper Armature / Heavy", "भारी तांबा (आर्मेचर)", "जड तांबे (आर्मेचर)", "non_ferrous", 640.0, 5.0, 0.95, 4.2, "EMERALD_GREEN"));
            seed.add(new MaterialCatalogEntity("brass_honey", "Brass Honey (Yellow Brass)", "पीतल (हनी ब्रास)", "पितळ (पिवळे पितळ)", "non_ferrous", 460.0, 4.0, 0.92, 3.8, "EMERALD_GREEN"));
            seed.add(new MaterialCatalogEntity("aluminium_extrusions", "Aluminium Extrusions 6063", "एल्युमिनियम एक्सट्रूज़न", "अ‍ॅल्युमिनियम एक्सट्रूजन", "non_ferrous", 195.0, 4.5, 0.98, 9.0, "EMERALD_GREEN"));
            seed.add(new MaterialCatalogEntity("aluminium_castings", "Aluminium Auto Castings", "एल्युमिनियम कास्टिंग", "अ‍ॅल्युमिनियम कास्टिंग", "non_ferrous", 165.0, 4.0, 0.90, 8.5, "EMERALD_GREEN"));
            seed.add(new MaterialCatalogEntity("aluminium_utensils", "Aluminium Utensils", "एल्युमिनियम बर्तन", "अ‍ॅल्युमिनियम भांडी", "non_ferrous", 140.0, 3.5, 0.88, 8.0, "EMERALD_GREEN"));
            seed.add(new MaterialCatalogEntity("heavy_steel_sariya", "Heavy Steel Rebar (Sariya)", "लोहा सरिया (HMS)", "लोखंडी सळई (सरिया)", "ferrous", 42.0, 3.0, 0.98, 1.8, "AMBER"));
            seed.add(new MaterialCatalogEntity("light_iron_patra", "Light Iron Sheet (Patra)", "हल्का लोहा (पतरा)", "हलके लोखंड (पत्रा)", "ferrous", 32.0, 3.0, 0.90, 1.5, "AMBER"));
            seed.add(new MaterialCatalogEntity("cast_iron", "Cast Iron Scrap", "कच्चा लोहा (कास्ट आयरन)", "बिडाचे लोखंड", "ferrous", 38.0, 3.0, 0.94, 1.7, "AMBER"));
            seed.add(new MaterialCatalogEntity("high_grade_server_pcb", "Server / Telecom PCB", "सर्वर ई-कचरा मदरबोर्ड", "सर्व्हर ई-कचरा मदरबोर्ड", "e_waste", 850.0, 7.0, 0.95, 12.0, "SLATE"));
            seed.add(new MaterialCatalogEntity("mobile_phone_pcb", "Mobile Phone Motherboards", "मोबाइल फोन पीसीबी", "मोबाइल फोन पीसीबी", "e_waste", 1200.0, 7.0, 0.96, 15.0, "SLATE"));
            seed.add(new MaterialCatalogEntity("lead_acid_battery", "Lead Acid Battery", "लेड एसिड बैटरी", "लेड अ‍ॅसिड बॅटरी", "battery_hazmat", 95.0, 6.0, 0.85, 2.5, "CRIMSON"));
            seed.add(new MaterialCatalogEntity("li_ion_cells", "Li-Ion 18650 & EV Cells", "लिथियम-आयन बैटरी", "लिथियम-आयन बॅटरी", "battery_hazmat", 250.0, 6.5, 0.90, 5.0, "CRIMSON"));
            seed.add(new MaterialCatalogEntity("cardboard_carton", "Cardboard / Carton Raddi", "गत्ता / कार्टन रद्दी", "पुठ्ठा / रद्दी कार्टन", "paper_cardboard", 12.0, 3.0, 0.95, 1.1, "AMBER"));
            seed.add(new MaterialCatalogEntity("pet_plastic", "PET Bottles & Rigid Plastics", "प्लास्टिक बोतल (PET)", "प्लॅस्टिक बाटल्या (PET)", "plastics", 26.0, 4.0, 0.92, 2.2, "AMBER"));
            dao.insertAll(seed);
        }
    }
}
