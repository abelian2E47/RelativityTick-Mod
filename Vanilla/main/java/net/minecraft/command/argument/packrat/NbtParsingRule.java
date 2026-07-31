package net.minecraft.command.argument.packrat;

import com.mojang.brigadier.StringReader;
import java.util.Optional;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.StringNbtReader;

public class NbtParsingRule implements ParsingRule<StringReader, NbtElement> {
    public static final ParsingRule<StringReader, NbtElement> INSTANCE = new NbtParsingRule();

    private NbtParsingRule() {
    }

    @Override
    public Optional<NbtElement> parse(ParsingState<StringReader> state) {
        state.getReader().skipWhitespace();
        int i = state.getCursor();

        try {
            return Optional.of(new StringNbtReader(state.getReader()).parseElement());
        } catch (Exception exception) {
            state.getErrors().add(i, exception);
            return Optional.empty();
        }
    }
}

