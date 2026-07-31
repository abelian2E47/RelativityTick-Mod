package net.minecraft.text;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;

public record ParsedSelector(String comp_3067, EntitySelector comp_3068) {
    public static final Codec<ParsedSelector> CODEC = Codec.STRING.comapFlatMap(ParsedSelector::parse, ParsedSelector::comp_3067);

    public static DataResult<ParsedSelector> parse(String selector) {
        try {
            EntitySelectorReader entitySelectorReader = new EntitySelectorReader(new StringReader(selector), true);
            return DataResult.success(new ParsedSelector(selector, entitySelectorReader.read()));
        } catch (CommandSyntaxException commandSyntaxException) {
            return DataResult.error(() -> "Invalid selector component: " + selector + ": " + commandSyntaxException.getMessage());
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ParsedSelector parsedSelector && this.comp_3067.equals(parsedSelector.comp_3067);
    }

    @Override
    public int hashCode() {
        return this.comp_3067.hashCode();
    }

    @Override
    public String toString() {
        return this.comp_3067;
    }
}

