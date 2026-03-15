package cn.zeromc.zero.command;

import cn.zeromc.zero.PurpurConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.Permissions;

import java.util.Locale;

public class SetTPSCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("settps")
            .requires((listener) -> listener.hasPermission(Permissions.COMMANDS_GAMEMASTER, "bukkit.command.settps"))
            .then(Commands.argument("entityTPS", DoubleArgumentType.doubleArg(0.1D, 20.0D))
                .executes((context) -> {
                    double entityTPS = DoubleArgumentType.getDouble(context, "entityTPS");
                    return execute(context.getSource(), entityTPS, null, null);
                })
                .then(Commands.argument("blockTPS", DoubleArgumentType.doubleArg(0.1D, 20.0D))
                    .executes((context) -> {
                        double entityTPS = DoubleArgumentType.getDouble(context, "entityTPS");
                        double blockTPS = DoubleArgumentType.getDouble(context, "blockTPS");
                        return execute(context.getSource(), entityTPS, blockTPS, null);
                    })
                    .then(Commands.argument("tileEntityTPS", DoubleArgumentType.doubleArg(0.1D, 20.0D))
                        .executes((context) -> {
                            double entityTPS = DoubleArgumentType.getDouble(context, "entityTPS");
                            double blockTPS = DoubleArgumentType.getDouble(context, "blockTPS");
                            double tileEntityTPS = DoubleArgumentType.getDouble(context, "tileEntityTPS");
                            return execute(context.getSource(), entityTPS, blockTPS, tileEntityTPS);
                        })
                    )
                )
            )
        );

        // Also register under zero: namespace
        dispatcher.register(Commands.literal("zero")
            .then(Commands.literal("settps")
                .requires((listener) -> listener.hasPermission(Permissions.COMMANDS_GAMEMASTER, "bukkit.command.settps"))
                .then(Commands.argument("entityTPS", DoubleArgumentType.doubleArg(0.1D, 20.0D))
                    .executes((context) -> {
                        double entityTPS = DoubleArgumentType.getDouble(context, "entityTPS");
                        return execute(context.getSource(), entityTPS, null, null);
                    })
                    .then(Commands.argument("blockTPS", DoubleArgumentType.doubleArg(0.1D, 20.0D))
                        .executes((context) -> {
                            double entityTPS = DoubleArgumentType.getDouble(context, "entityTPS");
                            double blockTPS = DoubleArgumentType.getDouble(context, "blockTPS");
                            return execute(context.getSource(), entityTPS, blockTPS, null);
                        })
                        .then(Commands.argument("tileEntityTPS", DoubleArgumentType.doubleArg(0.1D, 20.0D))
                            .executes((context) -> {
                                double entityTPS = DoubleArgumentType.getDouble(context, "entityTPS");
                                double blockTPS = DoubleArgumentType.getDouble(context, "blockTPS");
                                double tileEntityTPS = DoubleArgumentType.getDouble(context, "tileEntityTPS");
                                return execute(context.getSource(), entityTPS, blockTPS, tileEntityTPS);
                            })
                        )
                    )
                )
            )
        );
    }

    private static int execute(CommandSourceStack sender, double entityTPS, Double blockTPS, Double tileEntityTPS) {
        // Set target TPS values
        PurpurConfig.targetEntityTPS = entityTPS;
        if (blockTPS != null) PurpurConfig.targetBlockTPS = blockTPS;
        if (tileEntityTPS != null) PurpurConfig.targetTileEntityTPS = tileEntityTPS;

        String msg = String.format(Locale.ROOT, "Tick frequencies updated. [Entity: %.1f, Block: %.1f, TileEntity: %.1f]. Server clock remains at 20 TPS.",
                PurpurConfig.targetEntityTPS, PurpurConfig.targetBlockTPS, PurpurConfig.targetTileEntityTPS);

        sender.sendSuccess(net.kyori.adventure.text.Component.text(msg), true);
        return (int) entityTPS;
    }
}
