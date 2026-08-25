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
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

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
            context.getSource().sendFailure(Component.literal("Неизвестный язык: " + languageId));
            return 0;
        }
        Language language = opt.get();
        LanguageData data = target.getData(ModAttachments.LANGUAGE_DATA);
        boolean changed = data.setProgress(language, percent);
        target.setData(ModAttachments.LANGUAGE_DATA, data);
        NetworkHandler.sendSync(target);

        if (!changed) {
            context.getSource().sendFailure(Component.literal(
                    percent <= 0 ? "Этот язык нельзя забрать или игрок и так его не знает."
                            : "У игрока уже такой уровень владения этим языком."));
            return 0;
        }

        if (percent <= 0) {
            context.getSource().sendSuccess(() -> Component.literal(
                    target.getGameProfile().getName() + " больше не знает язык: " + language.displayName()), true);
            target.sendSystemMessage(Component.literal("Вы забыли язык: " + language.displayName())
                    .withStyle(style -> style.withColor(language.color())));
        } else {
            context.getSource().sendSuccess(() -> Component.literal(
                    target.getGameProfile().getName() + " теперь знает язык " + language.displayName() + " на " + percent + "%"), true);
            target.sendSystemMessage(Component.literal("Ваш уровень владения языком " + language.displayName() + ": " + percent + "%")
                    .withStyle(style -> style.withColor(language.color())));
        }
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        LanguageData data = target.getData(ModAttachments.LANGUAGE_DATA);
        String known = data.allProgress().entrySet().stream()
                .map(e -> e.getKey().displayName() + " (" + e.getValue() + "%)")
                .reduce((a, b) -> a + ", " + b).orElse("-");
        context.getSource().sendSuccess(() -> Component.literal(
                target.getGameProfile().getName() + " знает: " + known + ". Сейчас говорит на: " + data.current().displayName()), false);
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
            context.getSource().sendFailure(Component.literal("Неизвестный дефект речи: " + defectId));
            return 0;
        }
        SpeechDefect defect = opt.get();
        LanguageData data = target.getData(ModAttachments.LANGUAGE_DATA);
        boolean changed = give ? data.addDefect(defect) : data.removeDefect(defect);
        target.setData(ModAttachments.LANGUAGE_DATA, data);

        if (!changed) {
            context.getSource().sendFailure(Component.literal(
                    give ? "У игрока уже есть этот дефект речи." : "У игрока и так нет такого дефекта речи."));
            return 0;
        }

        if (give) {
            context.getSource().sendSuccess(() -> Component.literal(
                    target.getGameProfile().getName() + " теперь говорит с дефектом: " + defect.displayName()), true);
            target.sendSystemMessage(Component.literal("Вам выдан дефект речи: " + defect.displayName()));
        } else {
            context.getSource().sendSuccess(() -> Component.literal(
                    target.getGameProfile().getName() + " больше не имеет дефекта: " + defect.displayName()), true);
            target.sendSystemMessage(Component.literal("У вас забрали дефект речи: " + defect.displayName()));
        }
        return 1;
    }

    private static int listDefects(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        LanguageData data = target.getData(ModAttachments.LANGUAGE_DATA);
        String known = data.defects().stream().map(SpeechDefect::displayName)
                .reduce((a, b) -> a + ", " + b).orElse("нет");
        context.getSource().sendSuccess(() -> Component.literal(
                target.getGameProfile().getName() + " — дефекты речи: " + known), false);
        return 1;
    }

    private LanguageCommand() {
    }
}
