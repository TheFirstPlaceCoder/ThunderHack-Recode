package thunder.hack.cmd.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;
import thunder.hack.Thunderhack;
import thunder.hack.cmd.Command;
import thunder.hack.modules.client.MainSettings;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class PrefixCommand extends Command {
    public PrefixCommand() {
        super("prefix");
    }

    @Override
    public void executeBuild(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("set").then(arg("prefix", StringArgumentType.word()).executes(context -> {
            String prefix = context.getArgument("prefix", String.class);
            Thunderhack.commandManager.setPrefix(prefix);
            if (MainSettings.language.getValue() == MainSettings.Language.RU)
                sendMessage(Formatting.GREEN + "Префикс изменен на " + prefix);
            else sendMessage(Formatting.GREEN + "Prefix changed to " + prefix);

            return SINGLE_SUCCESS;
        })));

        builder.executes(context -> {
            if (MainSettings.language.getValue() == MainSettings.Language.RU)
                sendMessage(Formatting.GREEN + "Текущий префикс:" + Thunderhack.commandManager.getPrefix());
            else sendMessage(Formatting.GREEN + "Current prefix:" + Thunderhack.commandManager.getPrefix());
            return SINGLE_SUCCESS;
        });
    }
}
