package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.function.Function;

public class NuclearLangProvider extends LanguageProvider {

    private final Function<LangEntries.Entry, String> text;

    private NuclearLangProvider(PackOutput output, String locale, Function<LangEntries.Entry, String> text) {
        super(output, Create_NuclearIndustry.MODID, locale);
        this.text = text;
    }

    public static NuclearLangProvider english(PackOutput output) {
        return new NuclearLangProvider(output, "en_us", LangEntries.Entry::english);
    }

    public static NuclearLangProvider spanish(PackOutput output) {
        return new NuclearLangProvider(output, "es_es", LangEntries.Entry::spanish);
    }

    @Override
    protected void addTranslations() {
        for (LangEntries.Entry entry : LangEntries.all()) {
            add(entry.key(), text.apply(entry));
        }
    }
}
