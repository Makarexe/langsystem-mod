package com.langsystem.command;

import com.langsystem.Language;
import com.langsystem.SpeechDefect;
import com.langsystem.data.LanguageData;
import com.langsystem.data.ModAttachments;
import com.langsystem.network.NetworkHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Map;

public final class LanguageCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("language")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("language", StringArgumentType.word())
                                        .suggests(LanguageCommand::suggestLanguages)
                                        // без процента — сразу 100% (полное знание)
                                        .executes(ctx -> setProgress(ctx, 100))
                                        .then(Commands.argument("percent", IntegerArgumentType.integer(1, 100))
                                                .executes(ctx -> setProgress(ctx, IntegerArgumentType.getInteger(ctx, "percent")))))))
                .then(Commands.literal("take")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("language", StringArgumentType.word())
                                        .suggests(LanguageCommand::suggestLanguages)
                                        .executes(ctx -> setProgress(ctx, 0)))))
                .then(Commands.literal("list")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(LanguageCommand::list)))
                .then(Commands.literal("defect")
                        .then(Commands.literal("give")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("defect", StringArgumentType.word())
                                                .suggests(LanguageCommand::suggestDefects)
                                                .executes(ctx -> setDefect(ctx, true)))))
                        .then(Commands.literal("take")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("defect", StringArgumentType.word())
                                                .suggests(LanguageCommand::suggestDefects)
                                                .executes(ctx -> setDefect(ctx, false)))))
                        .then(Commands.literal("list")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(LanguageCommand::listDefects))))
        );
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestLanguages(
            CommandContext<CommandSourceStack> context, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        Arrays.stream(Language.values()).map(Language::id).forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static int setProgress(CommandContext<CommandSourceStack> context, int percent) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        String languageId = StringArgumentType.getString(context, "language");
        var opt = Language.byId(languageId);
        if (opt.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("langsystem.command.unknown_language", languageId));
            return 0;
        }
        Language language = opt.get();
        LanguageData data = target.getData(ModAttachments.LANGUAGE_DATA);
        boolean changed = data.setProgress(language, percent);
        target.setData(ModAttachments.LANGUAGE_DATA, data);
        NetworkHandler.sendSync(target);

        if (!changed) {
            context.getSource().sendFailure(Component.translatable(
                    percent <= 0 ? "langsystem.command.take.no_change" : "langsystem.command.give.no_change"));
            return 0;
        }

        if (percent <= 0) {
            context.getSource().sendSuccess(() -> Component.translatable("langsystem.command.take.success",
                    target.getGameProfile().getName(), language.translatable()), true);
            target.sendSystemMessage(Component.translatable("langsystem.command.take.notify", language.translatable())
                    .withStyle(style -> style.withColor(language.color())));
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("langsystem.command.give.success",
                    target.getGameProfile().getName(), language.translatable(), percent), true);
            target.sendSystemMessage(Component.translatable("langsystem.command.give.notify", language.translatable(), percent)
                    .withStyle(style -> style.withColor(language.color())));
        }
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        LanguageData data = target.getData(ModAttachments.LANGUAGE_DATA);

        MutableComponent known = Component.empty();
        boolean any = false;
        for (Map.Entry<Language, Integer> entry : data.allProgress().entrySet()) {
            if (any) {
                known.append(", ");
            }
            known.append(Component.translatable("langsystem.command.list.entry", entry.getKey().translatable(), entry.getValue()));
            any = true;
        }
        if (!any) {
            known = Component.translatable("langsystem.command.list.none");
        }

        MutableComponent knownFinal = known;
        context.getSource().sendSuccess(() -> Component.translatable("langsystem.command.list.success",
                target.getGameProfile().getName(), knownFinal, data.current().translatable()), false);
        return 1;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestDefects(
            CommandContext<CommandSourceStack> context, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        Arrays.stream(SpeechDefect.values()).map(SpeechDefect::id).forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static int setDefect(CommandContext<CommandSourceStack> context, boolean give) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        String defectId = StringArgumentType.getString(context, "defect");
        var opt = SpeechDefect.byId(defectId);
        if (opt.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("langsystem.command.unknown_defect", defectId));
            return 0;
        }
        SpeechDefect defect = opt.get();
        LanguageData data = target.getData(ModAttachments.LANGUAGE_DATA);
        boolean changed = give ? data.addDefect(defect) : data.removeDefect(defect);
        target.setData(ModAttachments.LANGUAGE_DATA, data);

        if (!changed) {
            context.getSource().sendFailure(Component.translatable(
                    give ? "langsystem.command.defect.give.no_change" : "langsystem.command.defect.take.no_change"));
            return 0;
        }

        if (give) {
            context.getSource().sendSuccess(() -> Component.translatable("langsystem.command.defect.give.success",
                    target.getGameProfile().getName(), defect.translatable()), true);
            target.sendSystemMessage(Component.translatable("langsystem.command.defect.give.notify", defect.translatable()));
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("langsystem.command.defect.take.success",
                    target.getGameProfile().getName(), defect.translatable()), true);
            target.sendSystemMessage(Component.translatable("langsystem.command.defect.take.notify", defect.translatable()));
        }
        return 1;
    }

    private static int listDefects(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        LanguageData data = target.getData(ModAttachments.LANGUAGE_DATA);

        MutableComponent known;
        if (data.defects().isEmpty()) {
            known = Component.translatable("langsystem.command.defect.list.none");
        } else {
            known = Component.empty();
            boolean first = true;
            for (SpeechDefect defect : data.defects()) {
                if (!first) {
                    known.append(", ");
                }
                known.append(defect.translatable());
                first = false;
            }
        }

        MutableComponent knownFinal = known;
        context.getSource().sendSuccess(() -> Component.translatable("langsystem.command.defect.list.success",
                target.getGameProfile().getName(), knownFinal), false);
        return 1;
    }

    private LanguageCommand() {
    }
}
